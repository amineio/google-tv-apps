package com.amine.battleship.viewmodel

import com.amine.battleship.game.ai.AttackKnowledge
import com.amine.battleship.game.ai.HuntTargetStrategy
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import com.amine.battleship.persistence.GameSaveRepository
import com.amine.battleship.persistence.SavedSession
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.random.Random

enum class SoundCue { SELECT, FIRE, MISS, HIT, SUNK, VICTORY, DEFEAT }

class GameSession(
    private val scope: CoroutineScope,
    private val repository: GameSaveRepository,
    private val aiDelayMs: Long = 850,
    private val seedSource: () -> Long = { Random.nextLong() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutableState = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = mutableState.asStateFlow()
    private val cues = Channel<SoundCue>(Channel.BUFFERED)
    val sounds: Flow<SoundCue> = cues.receiveAsFlow()
    private val saves = Channel<SavedSession>(Channel.CONFLATED)
    private var turnJob: Job? = null
    private var generation = 0L
    private var returnScreen = Screen.HOME

    init {
        scope.launch {
            val saved = withContext(ioDispatcher) { repository.load() }
            mutableState.value = SessionState(
                loaded = true,
                game = saved?.game?.takeIf { it.phase != GamePhase.GAME_OVER },
                selected = saved?.selected ?: Coordinate(0, 0),
                soundEnabled = saved?.soundEnabled ?: true,
                showHint = saved?.showHint ?: true,
                saveWarning = if (repository.recoveredInvalidSave) "The saved match did not open. Start a new game." else null,
            )
        }
        scope.launch {
            for (snapshot in saves) {
                try {
                    withContext(ioDispatcher) { repository.save(snapshot) }
                    mutableState.update { it.copy(saveWarning = null) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableState.update { it.copy(saveWarning = "The match cannot be saved. Free some storage and try again.") }
                }
            }
        }
    }

    fun action(action: GameAction) {
        if (!state.value.loaded) return
        if (action !is GameAction.Move && action != GameAction.Fire && action != GameAction.ToggleSound) cue(SoundCue.SELECT)
        when (action) {
            GameAction.Play -> if (unfinished()) confirm(Confirmation.NEW_GAME) else start()
            GameAction.Continue -> if (unfinished()) {
                update { copy(screen = Screen.GAME, paused = false, confirmation = null, resolving = false, message = "Your fleet is ready.") }
                scheduleComputer()
            }
            is GameAction.Move -> if (state.value.screen == Screen.GAME && !state.value.paused) {
                update { copy(selected = Coordinate((selected.row + action.rowDelta).coerceIn(0, 9),
                    (selected.column + action.columnDelta).coerceIn(0, 9))) }
            }
            GameAction.Fire -> fire()
            GameAction.Pause -> pause()
            GameAction.Resume -> {
                update { copy(paused = false, confirmation = null, screen = Screen.GAME, resolving = false) }
                scheduleComputer()
            }
            GameAction.Restart -> confirm(Confirmation.RESTART)
            GameAction.Home -> if (state.value.screen == Screen.GAME && unfinished()) confirm(Confirmation.MAIN_MENU) else home()
            GameAction.Help -> open(Screen.HELP)
            GameAction.Settings -> open(Screen.SETTINGS)
            GameAction.ToggleSound -> {
                update { copy(soundEnabled = !soundEnabled) }
                cue(SoundCue.SELECT)
                persist()
            }
            GameAction.Cancel -> update { copy(confirmation = null) }
            GameAction.Confirm -> when (state.value.confirmation) {
                Confirmation.RESTART, Confirmation.NEW_GAME -> start()
                Confirmation.MAIN_MENU -> home()
                null -> Unit
            }
            GameAction.Back -> back()
        }
    }

    fun background() {
        if (state.value.screen == Screen.GAME || returnScreen == Screen.GAME) pause()
        persist()
    }

    private fun unfinished() = state.value.game?.phase?.let { it != GamePhase.GAME_OVER } == true

    private fun start() {
        cancelTurn()
        update { copy(game = GameEngine.newGame(seedSource()), screen = Screen.GAME, selected = Coordinate(0, 0),
            paused = false, resolving = false, confirmation = null, message = "Your turn. Choose a coordinate.") }
        returnScreen = Screen.HOME
        persist()
    }

    private fun fire() {
        val current = state.value
        val game = current.game ?: return
        if (current.screen != Screen.GAME || current.paused || current.resolving || game.phase != GamePhase.PLAYER_TURN) return
        val result = GameEngine.attack(game, Player.HUMAN, current.selected)
        if (!result.accepted) {
            update { copy(message = "Already targeted. Choose another coordinate.") }
            return
        }
        cue(SoundCue.FIRE)
        update { copy(game = result.state, resolving = true, showHint = false, message = describe(Player.HUMAN, result.shot!!)) }
        persist()
        resolveSound(result.shot!!, result.state)
        if (result.state.phase == GamePhase.GAME_OVER) finishAfterFeedback() else scheduleComputer()
    }

    private fun scheduleComputer() {
        val current = state.value
        val game = current.game ?: return
        if (current.screen != Screen.GAME || current.paused || game.phase != GamePhase.COMPUTER_TURN) return
        cancelTurn()
        val expectedGeneration = generation
        turnJob = scope.launch {
            delay(aiDelayMs)
            val latest = state.value
            val match = latest.game ?: return@launch
            if (generation != expectedGeneration || latest.paused || latest.screen != Screen.GAME || match.phase != GamePhase.COMPUTER_TURN) return@launch
            val coordinate = HuntTargetStrategy(match.seed).chooseShot(AttackKnowledge.fromShots(match.playerBoard.shots))
            val result = GameEngine.attack(match, Player.COMPUTER, coordinate)
            if (!result.accepted) return@launch
            update { copy(game = result.state, resolving = true, message = describe(Player.COMPUTER, result.shot!!)) }
            persist()
            resolveSound(result.shot!!, result.state)
            delay(500)
            if (generation != expectedGeneration) return@launch
            update { copy(resolving = false, screen = if (result.state.phase == GamePhase.GAME_OVER) Screen.RESULTS else Screen.GAME) }
        }
    }

    private fun finishAfterFeedback() {
        cancelTurn()
        val expectedGeneration = generation
        turnJob = scope.launch {
            delay(650)
            if (generation == expectedGeneration) update { copy(screen = Screen.RESULTS, resolving = false) }
        }
    }

    private fun pause() {
        if (state.value.screen != Screen.GAME) return
        cancelTurn()
        if (state.value.game?.phase == GamePhase.GAME_OVER) update { copy(screen = Screen.RESULTS, resolving = false) }
        else update { copy(paused = true, resolving = false) }
        persist()
    }

    private fun open(screen: Screen) {
        if (state.value.screen == Screen.GAME) pause()
        returnScreen = state.value.screen
        update { copy(screen = screen) }
    }

    private fun back() {
        when {
            state.value.confirmation != null -> update { copy(confirmation = null) }
            state.value.screen == Screen.SETTINGS || state.value.screen == Screen.HELP -> update { copy(screen = returnScreen) }
            state.value.screen == Screen.GAME && state.value.paused -> action(GameAction.Resume)
            state.value.screen == Screen.GAME -> pause()
            state.value.screen == Screen.RESULTS -> home()
        }
    }

    private fun confirm(value: Confirmation) {
        if (state.value.screen == Screen.GAME) pause()
        update { copy(confirmation = value) }
    }

    private fun home() {
        cancelTurn()
        returnScreen = Screen.HOME
        update { copy(screen = Screen.HOME, paused = false, resolving = false, confirmation = null) }
        persist()
    }

    private fun cancelTurn() { generation++; turnJob?.cancel(); turnJob = null }
    private inline fun update(block: SessionState.() -> SessionState) { mutableState.update { it.block() } }
    private fun persist() {
        if (!state.value.loaded) return
        val value = state.value
        saves.trySend(SavedSession(game = value.game?.takeIf { it.phase != GamePhase.GAME_OVER }, selected = value.selected,
            soundEnabled = value.soundEnabled, showHint = value.showHint))
    }
    private fun cue(sound: SoundCue) { if (state.value.soundEnabled) cues.trySend(sound) }
    private fun resolveSound(shot: Shot, match: GameState) {
        cue(when {
            match.winner == Player.HUMAN -> SoundCue.VICTORY
            match.winner == Player.COMPUTER -> SoundCue.DEFEAT
            shot.outcome == ShotOutcome.SUNK -> SoundCue.SUNK
            shot.outcome == ShotOutcome.HIT -> SoundCue.HIT
            else -> SoundCue.MISS
        })
    }
    private fun describe(player: Player, shot: Shot): String {
        val actor = if (player == Player.HUMAN) "You" else "Computer"
        return when (shot.outcome) {
            ShotOutcome.MISS -> "$actor fired at ${shot.coordinate.label}: Miss."
            ShotOutcome.HIT -> "$actor fired at ${shot.coordinate.label}: Hit!"
            ShotOutcome.SUNK -> "$actor sank the ${shot.shipType?.label} at ${shot.coordinate.label}!"
        }
    }
}
