package com.amine.battleship.viewmodel

import com.amine.battleship.game.ai.AttackKnowledge
import com.amine.battleship.game.ai.HuntTargetStrategy
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import com.amine.battleship.persistence.GameSaveRepository
import com.amine.battleship.persistence.SavedSession
import com.amine.battleship.ui.impactDurationMillis
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.random.Random

enum class SoundCue { SELECT, FIRE, MISS, HIT, DAMAGE, SUNK, VICTORY, DEFEAT, MATCH_START, STOP }

private const val REPEATED_SHOT_MESSAGE = "Already targeted"

class GameSession(
    private val scope: CoroutineScope,
    private val repository: GameSaveRepository,
    private val aiDelayMs: Long = 850,
    private val seedSource: () -> Long = { Random.nextLong() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nanoTimeSource: () -> Long = System::nanoTime,
) {
    private val mutableState = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = mutableState.asStateFlow()
    private val cues = Channel<SoundCue>(Channel.BUFFERED)
    val sounds: Flow<SoundCue> = cues.receiveAsFlow()
    private data class Write(val id: Long, val snapshot: SavedSession, val acknowledged: ((Boolean) -> Unit)? = null)
    private val saves = Channel<Write>(Channel.UNLIMITED)
    private var nextWriteId = 0L
    private var criticalWrite: Write? = null
    private var pendingStart: GameState? = null
    private var pendingStartInterrupted = false
    private var startReadyStage = 0
    private var shotSaved = false
    private var shotEffectDone = false
    private var localShotId: Long? = null
    private var turnJob: Job? = null
    private var generation = 0L
    private var nextImpactId = 0L
    private var returnScreen = Screen.HOME

    init {
        scope.launch {
            val saved = withContext(ioDispatcher) { repository.load() }
            mutableState.value = SessionState(
                loaded = true,
                game = saved?.game?.takeIf { it.mode == GameMode.LOCAL || it.phase != GamePhase.GAME_OVER },
                selected = saved?.selected ?: Coordinate(0, 0),
                soundEnabled = saved?.soundEnabled ?: true,
                language = saved?.language ?: com.amine.battleship.localization.AppLanguage.ENGLISH,
                showHint = saved?.showHint ?: true,
                saveWarning = if (repository.recoveredInvalidSave) "The saved match did not open. Start a new game." else null,
            )
        }
        scope.launch {
            for (write in saves) {
                try {
                    withContext(ioDispatcher) { repository.save(write.snapshot) }
                    if (write.id == nextWriteId) mutableState.update { it.copy(saveWarning = null) }
                    write.acknowledged?.invoke(true)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (write.id == nextWriteId) mutableState.update { it.copy(saveWarning = "The match cannot be saved. Free some storage and try again.") }
                    write.acknowledged?.invoke(false)
                }
            }
        }
    }

    fun action(action: GameAction) {
        if (!state.value.loaded) return
        if (action !is GameAction.Move && action !is GameAction.CompleteImpact && action != GameAction.Fire && action != GameAction.ReleaseConfirm && action != GameAction.ToggleSound) cue(SoundCue.SELECT)
        when (action) {
            GameAction.ChooseMode -> { if (state.value.screen == Screen.GAME) pause(); update { copy(screen = Screen.MODE) } }
            GameAction.LocalSetup -> { cancelTurn(); update { copy(screen = Screen.SETUP, setup = LocalSetupDraft(), confirmation = null) } }
            is GameAction.SelectPortrait -> editPortrait(action.id)
            is GameAction.SaveName -> editName(action.name)
            GameAction.NextParticipant -> update { copy(setup = setup?.copy(slot = (setup.slot + 1).coerceAtMost(2), nameError = false)) }
            GameAction.StartLocal -> if (state.value.setup != null) {
                if (state.value.game != null) confirm(Confirmation.NEW_GAME) else prepareLocal()
            }
            GameAction.Ready -> ready()
            GameAction.ReleaseConfirm -> update { copy(releaseRequired = false) }
            GameAction.RetrySave -> retrySave()
            GameAction.Play -> if (unfinished() || state.value.game?.mode == GameMode.LOCAL) confirm(Confirmation.NEW_GAME) else start()
            GameAction.Continue -> if (state.value.game != null) {
                val game = state.value.game!!
                if (game.mode == GameMode.LOCAL) {
                    val awaitingSave = criticalWrite != null
                    update { copy(screen = if (game.phase == GamePhase.GAME_OVER && !awaitingSave) Screen.RESULTS else Screen.GAME,
                        paused = game.phase != GamePhase.GAME_OVER || awaitingSave, confirmation = null, resolving = awaitingSave,
                        message = "", activeImpact = null, revealRemainingMs = null, readySeat = null) }
                } else {
                    update { copy(screen = Screen.GAME, paused = false, confirmation = null, resolving = false, message = "", activeImpact = null) }
                    scheduleComputer()
                }
            }
            is GameAction.Move -> if (state.value.screen == Screen.GAME && !state.value.paused && pendingStart == null &&
                (state.value.game?.mode != GameMode.LOCAL || localInputAllowed())) {
                update {
                    val destination = Coordinate((selected.row + action.rowDelta).coerceIn(0, 9),
                        (selected.column + action.columnDelta).coerceIn(0, 9))
                    copy(selected = destination,
                        message = if (destination != selected && message == REPEATED_SHOT_MESSAGE) "" else message)
                }
            }
            GameAction.Fire -> fire()
            is GameAction.CompleteImpact -> update {
                if (activeImpact?.id == action.id) copy(activeImpact = null) else this
            }
            GameAction.Pause -> pause()
            GameAction.Resume -> if (state.value.game?.mode == GameMode.LOCAL || pendingStart != null) resumeLocal() else {
                update { copy(paused = false, confirmation = null, screen = Screen.GAME, resolving = false, activeImpact = null) }
                scheduleComputer()
            }
            GameAction.Restart -> confirm(Confirmation.RESTART)
            GameAction.Home -> if (state.value.screen == Screen.GAME && unfinished()) confirm(Confirmation.MAIN_MENU) else home()
            GameAction.Help -> open(Screen.HELP)
            GameAction.Settings -> open(Screen.SETTINGS)
            GameAction.CycleLanguage -> {
                update { copy(language = language.next()) }
                persist()
            }
            GameAction.ToggleSound -> {
                cues.trySend(SoundCue.STOP)
                update { copy(soundEnabled = !soundEnabled) }
                cue(SoundCue.SELECT)
                persist()
            }
            GameAction.Cancel -> if (pendingStart != null && state.value.saveWarning != null) cancelFailedStart()
                else if (state.value.setup != null && state.value.confirmation == null) cancelSetup()
                else update { copy(confirmation = null) }
            GameAction.Confirm -> when (state.value.confirmation) {
                Confirmation.RESTART -> if (state.value.game?.mode == GameMode.LOCAL) prepareLocal(rematch = true) else start()
                Confirmation.NEW_GAME -> if (state.value.setup != null) prepareLocal() else start()
                Confirmation.MAIN_MENU -> home()
                null -> Unit
            }
            GameAction.Back -> back()
        }
    }

    fun background() {
        if (pendingStart != null) pendingStartInterrupted = true
        update { copy(activeImpact = null, revealRemainingMs = null, previewInterrupted = previewInterrupted || revealRemainingMs != null) }
        if (state.value.screen == Screen.GAME || returnScreen == Screen.GAME) pause()
        persist()
    }

    fun focusLost() {
        if (state.value.revealRemainingMs != null || pendingStart != null && criticalWrite != null) background()
    }

    private fun unfinished() = state.value.game?.phase?.let { it != GamePhase.GAME_OVER } == true

    private fun start() {
        cancelTurn()
        pendingStart = null; criticalWrite = null; localShotId = null
        update { copy(localStarting = false, setup = null, readySeat = null, revealRemainingMs = null, releaseRequired = false, previewInterrupted = false,
            readyLeftProfile = null, readyRightProfile = null, game = GameEngine.newGame(seedSource()), screen = Screen.GAME, selected = Coordinate(0, 0),
            paused = false, resolving = false, confirmation = null, message = "", activeImpact = null) }
        returnScreen = Screen.HOME
        cue(SoundCue.MATCH_START)
        persist()
    }

    private fun fire() {
        if (pendingStart != null) return
        val current = state.value
        val game = current.game ?: return
        if (game.mode == GameMode.LOCAL) { fireLocal(); return }
        if (current.screen != Screen.GAME || current.paused || current.resolving || game.phase != GamePhase.PLAYER_TURN) return
        val result = GameEngine.attack(game, Player.HUMAN, current.selected)
        if (!result.accepted) {
            update { copy(message = REPEATED_SHOT_MESSAGE) }
            return
        }
        val shot = result.shot!!
        val impact = impact(ImpactBoard.ENEMY, shot)
        cue(SoundCue.FIRE)
        update { copy(game = result.state, resolving = true, showHint = false,
            message = describe(Player.HUMAN, shot), activeImpact = impact) }
        persist()
        resolveSound(Player.HUMAN, shot)
        if (result.state.phase == GamePhase.GAME_OVER) finishAfterFeedback(shot) else scheduleComputer(impactDurationMillis(shot.outcome))
    }

    private fun scheduleComputer(outgoingDurationMs: Long = 0) {
        val current = state.value
        val game = current.game ?: return
        if (game.mode == GameMode.LOCAL || current.screen != Screen.GAME || current.paused || game.phase != GamePhase.COMPUTER_TURN) return
        cancelTurn()
        val expectedGeneration = generation
        turnJob = scope.launch {
            delay(maxOf(aiDelayMs, outgoingDurationMs))
            val latest = state.value
            val match = latest.game ?: return@launch
            if (match.mode == GameMode.LOCAL || generation != expectedGeneration || latest.paused || latest.screen != Screen.GAME || match.phase != GamePhase.COMPUTER_TURN) return@launch
            val coordinate = HuntTargetStrategy(match.seed).chooseShot(AttackKnowledge.fromShots(match.playerBoard.shots))
            val result = GameEngine.attack(match, Player.COMPUTER, coordinate)
            if (!result.accepted) return@launch
            val shot = result.shot!!
            val impact = impact(ImpactBoard.FRIENDLY, shot)
            update { copy(game = result.state, resolving = true,
                message = describe(Player.COMPUTER, shot), activeImpact = impact) }
            persist()
            resolveSound(Player.COMPUTER, shot)
            delay(impactDurationMillis(shot.outcome))
            if (generation != expectedGeneration) return@launch
            if (result.state.phase == GamePhase.GAME_OVER) cueResult(result.state)
            update {
                if (result.state.phase == GamePhase.GAME_OVER) copy(resolving = false, screen = Screen.RESULTS, activeImpact = null)
                else copy(resolving = false, screen = Screen.GAME, activeImpact = null)
            }
        }
    }

    private fun finishAfterFeedback(shot: Shot) {
        cancelTurn()
        val expectedGeneration = generation
        turnJob = scope.launch {
            delay(impactDurationMillis(shot.outcome))
            if (generation == expectedGeneration) {
                state.value.game?.let(::cueResult)
                update { copy(screen = Screen.RESULTS, resolving = false, activeImpact = null) }
            }
        }
    }

    private fun pause() {
        cues.trySend(SoundCue.STOP)
        if (state.value.screen != Screen.GAME) return
        cancelTurn()
        if (state.value.game?.mode == GameMode.LOCAL || pendingStart != null) {
            pendingStartInterrupted = true
            shotEffectDone = true
            update { copy(paused = true, revealRemainingMs = null, readySeat = null,
                previewInterrupted = previewInterrupted || revealRemainingMs != null, activeImpact = null,
                resolving = criticalWrite != null) }
            val candidate = pendingStart
            if (candidate != null && criticalWrite == null) commitLocalStart(candidate)
            persist()
            return
        }
        if (state.value.game?.phase == GamePhase.GAME_OVER) update { copy(screen = Screen.RESULTS, resolving = false, activeImpact = null) }
        else update { copy(paused = true, resolving = false, activeImpact = null) }
        persist()
    }

    private fun open(screen: Screen) {
        if (state.value.screen == Screen.GAME) pause()
        returnScreen = state.value.screen
        update { copy(screen = screen, activeImpact = null) }
    }

    private fun back() {
        when {
            state.value.confirmation != null -> update { copy(confirmation = null) }
            state.value.screen == Screen.SETUP -> cancelSetup()
            state.value.screen == Screen.MODE -> update { copy(screen = Screen.HOME) }
            state.value.screen == Screen.SETTINGS || state.value.screen == Screen.HELP -> update { copy(screen = returnScreen) }
            state.value.screen == Screen.GAME && state.value.paused -> action(GameAction.Resume)
            state.value.screen == Screen.GAME -> pause()
            state.value.screen == Screen.RESULTS -> home()
        }
    }

    private fun confirm(value: Confirmation) {
        if (state.value.screen == Screen.GAME) pause()
        update { copy(confirmation = value, activeImpact = null) }
    }

    private fun home() {
        cues.trySend(SoundCue.STOP)
        cancelTurn()
        returnScreen = Screen.HOME
        if (pendingStart != null) pendingStartInterrupted = true
        update { copy(screen = Screen.HOME, paused = false, resolving = false, confirmation = null, activeImpact = null,
            readySeat = null, revealRemainingMs = null, setup = null) }
        persist()
    }

    private fun impact(board: ImpactBoard, shot: Shot) = ImpactPresentation(
        id = ++nextImpactId,
        board = board,
        coordinate = shot.coordinate,
        outcome = shot.outcome,
        startedAtNanos = nanoTimeSource(),
        sunkCells = shot.sunkCells,
        shipType = shot.shipType,
    )

    private fun cancelTurn() { generation++; turnJob?.cancel(); turnJob = null }
    private inline fun update(block: SessionState.() -> SessionState) { mutableState.update { it.block() } }
    private fun snapshot(game: GameState? = state.value.game): SavedSession {
        val value = state.value
        return SavedSession(game = game?.takeIf { it.mode == GameMode.LOCAL || it.phase != GamePhase.GAME_OVER }, selected = value.selected,
            soundEnabled = value.soundEnabled, language = value.language, showHint = value.showHint)
    }
    private fun persist() {
        if (!state.value.loaded || pendingStart != null || criticalWrite != null) return
        saves.trySend(Write(++nextWriteId, snapshot()))
    }
    private fun saveCritical(snapshot: SavedSession, success: () -> Unit) {
        val writeId = ++nextWriteId
        val write = Write(writeId, snapshot) { accepted ->
            if (criticalWrite?.id == writeId && accepted) {
                criticalWrite = null
                success()
            }
        }
        criticalWrite = write
        saves.trySend(write)
    }
    private fun retrySave() {
        val previous = criticalWrite ?: return
        if (state.value.saveWarning == null) return
        update { copy(saveWarning = null) }
        // Reuse this acknowledged write. It is still the newest serialized request.
        saves.trySend(previous)
    }

    private fun editPortrait(id: String) {
        val draft = state.value.setup ?: return
        if (id !in PlayerProfile.portraitIds || draft.slot > 1 || draft.slot == 1 && id == draft.left.portraitId) return
        update { copy(setup = if (draft.slot == 0) draft.copy(left = draft.left.copy(portraitId = id),
            right = draft.right.restored(setOf(id))) else draft.copy(right = draft.right.copy(portraitId = id))) }
    }
    private fun editName(name: String) {
        val draft = state.value.setup ?: return
        val validated = runCatching { PlayerProfile.normalizeName(name) }
        if (validated.isFailure) { update { copy(setup = draft.copy(nameError = true)) }; return }
        update { copy(setup = if (draft.slot == 0) draft.copy(left = draft.left.copy(customName = validated.getOrNull()), nameError = false)
            else draft.copy(right = draft.right.copy(customName = validated.getOrNull()), nameError = false)) }
    }
    private fun cancelFailedStart() {
        pendingStart = null; criticalWrite = null
        update { copy(localStarting = false, readySeat = null, readyLeftProfile = null, readyRightProfile = null,
            screen = Screen.HOME, resolving = false, paused = false, saveWarning = null) }
    }
    private fun cancelSetup() { update { copy(setup = null, screen = Screen.HOME, confirmation = null) } }
    private fun prepareLocal(rematch: Boolean = false) {
        if (criticalWrite != null) return
        val current = state.value
        val left = if (rematch) current.game!!.leftProfile else current.setup?.left ?: return
        val right = if (rematch) current.game!!.rightProfile else current.setup?.right ?: return
        cancelTurn()
        val seed = seedSource().let { if (it == current.game?.seed) it + 1 else it }
        pendingStart = GameEngine.newGame(seed).copy(mode = GameMode.LOCAL, leftProfile = left, rightProfile = right, previewConsumed = true)
        pendingStartInterrupted = false; startReadyStage = 0; localShotId = null
        update { copy(localStarting = true, screen = Screen.GAME, setup = null, confirmation = null, paused = false, resolving = false,
            readyLeftProfile = left, readyRightProfile = right, readySeat = Player.HUMAN, revealRemainingMs = null, releaseRequired = false,
            previewInterrupted = false, activeImpact = null, message = "") }
    }
    private fun ready() {
        val current = state.value
        if (current.paused || current.releaseRequired || current.readySeat == null || criticalWrite != null) return
        val candidate = pendingStart
        if (candidate != null) {
            if (startReadyStage == 0) {
                startReadyStage = 1
                update { copy(readySeat = Player.COMPUTER, releaseRequired = true) }
            } else {
                commitLocalStart(candidate)
            }
        } else {
            update { copy(readySeat = null, releaseRequired = true) }
        }
    }
    private fun commitLocalStart(candidate: GameState) {
        update { copy(readySeat = null, resolving = true, releaseRequired = true) }
        saveCritical(snapshot(candidate).copy(selected = Coordinate(0, 0))) {
            if (pendingStart !== candidate) return@saveCritical
            pendingStart = null
            val interrupted = pendingStartInterrupted
            update { copy(localStarting = false, game = candidate, selected = Coordinate(0, 0), resolving = false, paused = interrupted,
                activeImpact = null, readySeat = null, previewInterrupted = interrupted,
                revealRemainingMs = if (interrupted) null else 10_000L) }
            if (!interrupted) { cue(SoundCue.MATCH_START); runReveal() }
        }
    }
    private fun runReveal() {
        cancelTurn()
        val expected = generation
        val deadline = nanoTimeSource() + 10_000_000_000L
        turnJob = scope.launch {
            while (generation == expected && state.value.revealRemainingMs != null) {
                val remaining = ((deadline - nanoTimeSource()) / 1_000_000L).coerceAtLeast(0)
                if (remaining == 0L) {
                    update { copy(revealRemainingMs = null, releaseRequired = false) }
                    return@launch
                }
                update { copy(revealRemainingMs = remaining) }
                delay(minOf(remaining, 50L))
            }
        }
    }
    private fun localInputAllowed(): Boolean {
        val current = state.value
        return current.screen == Screen.GAME && !current.paused && !current.resolving &&
            current.readySeat == null && current.revealRemainingMs == null && !current.releaseRequired &&
            pendingStart == null && criticalWrite == null && current.game?.phase != GamePhase.GAME_OVER
    }
    private fun activeSeat(game: GameState) = if (game.phase == GamePhase.COMPUTER_TURN) Player.COMPUTER else Player.HUMAN
    private fun fireLocal() {
        if (!localInputAllowed()) return
        val current = state.value; val game = current.game ?: return
        val attacker = activeSeat(game)
        val result = GameEngine.attack(game, attacker, current.selected)
        if (!result.accepted) { update { copy(message = REPEATED_SHOT_MESSAGE) }; return }
        cancelTurn()
        val event = impact(if (attacker == Player.HUMAN) ImpactBoard.RIGHT else ImpactBoard.LEFT, result.shot!!)
        localShotId = event.id; shotSaved = false; shotEffectDone = false
        update { copy(game = result.state, resolving = true, activeImpact = event, message = describe(Player.HUMAN, result.shot), showHint = false) }
        cue(SoundCue.FIRE); resolveSound(Player.HUMAN, result.shot)
        saveCritical(snapshot()) { if (localShotId == event.id) { shotSaved = true; completeLocalShot() } }
        val expected = generation
        turnJob = scope.launch {
            delay(impactDurationMillis(event.outcome))
            if (expected == generation && localShotId == event.id) { shotEffectDone = true; completeLocalShot() }
        }
    }
    private fun completeLocalShot() {
        if (!shotSaved || !shotEffectDone || state.value.paused || state.value.screen != Screen.GAME) return
        val game = state.value.game ?: return
        localShotId = null
        if (game.phase == GamePhase.GAME_OVER) {
            cueResult(game)
            update { copy(screen = Screen.RESULTS, resolving = false, activeImpact = null, readySeat = null) }
        } else update { copy(resolving = false, activeImpact = null, readySeat = activeSeat(game), releaseRequired = false) }
    }
    private fun resumeLocal() {
        if (pendingStart != null) {
            update { copy(screen = Screen.GAME, paused = false, confirmation = null) }
            return
        }
        val game = state.value.game ?: return
        update { copy(screen = if (game.phase == GamePhase.GAME_OVER && criticalWrite == null) Screen.RESULTS else Screen.GAME,
            paused = false, confirmation = null, revealRemainingMs = null, activeImpact = null,
            resolving = criticalWrite != null, readySeat = if (criticalWrite == null && game.phase != GamePhase.GAME_OVER) activeSeat(game) else null,
            releaseRequired = false) }
        if (criticalWrite == null) localShotId = null
        else { shotEffectDone = true; completeLocalShot() }
    }

    private fun cue(sound: SoundCue) { if (state.value.soundEnabled) cues.trySend(sound) }
    private fun resolveSound(attacker: Player, shot: Shot) {
        cue(when {
            shot.outcome == ShotOutcome.SUNK -> SoundCue.SUNK
            shot.outcome == ShotOutcome.HIT -> if (attacker == Player.HUMAN) SoundCue.HIT else SoundCue.DAMAGE
            else -> SoundCue.MISS
        })
    }
    private fun cueResult(match: GameState) {
        cue(if (match.mode == GameMode.LOCAL || match.winner == Player.HUMAN) SoundCue.VICTORY else SoundCue.DEFEAT)
    }
    private fun describe(player: Player, shot: Shot): String {
        return when (shot.outcome) {
            ShotOutcome.MISS -> "Miss"
            ShotOutcome.HIT -> if (player == Player.HUMAN) "Hit" else "Fleet hit"
            ShotOutcome.SUNK -> "${if (player == Player.COMPUTER) "Your " else ""}${shot.shipType?.label} sunk"
        }
    }
}
