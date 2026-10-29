package com.amine.battleship.ui

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amine.battleship.game.model.GamePhase
import com.amine.battleship.viewmodel.GameAction
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState

internal fun presentationEnabled(state: SessionState, resumed: Boolean): Boolean =
    resumed && state.screen == Screen.GAME && state.game != null &&
        (state.game.phase in listOf(GamePhase.PLAYER_TURN, GamePhase.COMPUTER_TURN) ||
            state.game.phase == GamePhase.GAME_OVER && state.resolving) &&
        !state.paused && state.confirmation == null

/** A monotonic clock and frame scheduler can also be supplied by an offline renderer. */
internal interface PresentationClock {
    fun nowNanos(): Long
    suspend fun awaitFrame(continuous: Boolean): Long
}

private object DisplayPresentationClock : PresentationClock {
    override fun nowNanos(): Long = System.nanoTime()
    override suspend fun awaitFrame(continuous: Boolean): Long = withFrameNanos { nowNanos() }
}

internal data class BoardPresentationEnvironment(val clock: PresentationClock = DisplayPresentationClock)
internal val LocalBoardPresentation = staticCompositionLocalOf { BoardPresentationEnvironment() }

internal class BoardFrameTime(val impactNanos: State<Long>, val enabled: Boolean)

@Composable
internal fun rememberBoardFrameTime(state: SessionState, onAction: (GameAction) -> Unit): BoardFrameTime {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.currentState == Lifecycle.State.RESUMED) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> resumed = lifecycle.currentState == Lifecycle.State.RESUMED }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val clock = LocalBoardPresentation.current.clock
    val impactNow = remember(clock) { mutableLongStateOf(clock.nowNanos()) }
    val dispatch by rememberUpdatedState(onAction)
    val enabled = presentationEnabled(state, resumed)
    val impact = state.activeImpact
    val completed = remember(impact?.id) { booleanArrayOf(false) }
    LaunchedEffect(impact?.id, enabled, clock) {
        if (impact == null || completed[0]) return@LaunchedEffect
        try {
            if (!enabled) return@LaunchedEffect
            impactNow.longValue = clock.nowNanos()
            while (impactProgress(impact.startedAtNanos, impactNow.longValue, impact.outcome) < 1f) {
                impactNow.longValue = clock.awaitFrame(continuous = false)
            }
        } finally {
            // A stale completion is safe: the session clears only the matching event ID.
            if (!completed[0]) {
                completed[0] = true
                dispatch(GameAction.CompleteImpact(impact.id))
            }
        }
    }
    return remember(impactNow, enabled) { BoardFrameTime(impactNow, enabled) }
}
