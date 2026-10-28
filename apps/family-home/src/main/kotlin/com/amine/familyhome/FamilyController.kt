package com.amine.familyhome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

interface FamilyRepository {
    suspend fun load(): FamilyState
    suspend fun save(state: FamilyState)
}

data class FamilyUi(
    val state: FamilyState = FamilyState(),
    val ready: Boolean = false,
    val helper: Boolean = false,
    val storageFailed: Boolean = false,
    val notice: String? = null,
    val now: Long = 0,
)

sealed interface FamilyEffect {
    data object PauseMedia : FamilyEffect
    data object OpenHome : FamilyEffect
    data class Warning(val text: String) : FamilyEffect
}

/** One durable command queue shared by the UI and the Android accessibility helper. */
class FamilyController(private val repository: FamilyRepository, private val elapsed: () -> Long) {
    private val mutable = MutableStateFlow(FamilyUi())
    val ui = mutable.asStateFlow()
    private val mutex = Mutex()
    private var adminAuthorized = false
    private var lastCheckpoint = 0L
    var effect: (FamilyEffect) -> Unit = {}

    suspend fun load(): Boolean = mutex.withLock {
        try {
            val restored = SessionEngine.recover(repository.load())
            repository.save(restored)
            adminAuthorized = false
            mutable.update { it.copy(state = restored, ready = true, storageFailed = false, now = elapsed(), notice = null) }
            true
        } catch (_: Exception) {
            mutable.update { it.copy(ready = true, storageFailed = true,
                notice = "Family Home cannot open its saved state. Retry or use the recovery instructions.") }
            false
        }
    }

    fun helperConnected(connected: Boolean) { mutable.update { it.copy(helper = connected) } }

    private fun available(child: Boolean = false): Boolean {
        val current = mutable.value
        if (!current.ready || current.storageFailed) return false
        if (child && !current.helper) {
            mutable.update { it.copy(notice = "Enable the Family Home accessibility helper before starting a child session.") }
            return false
        }
        return true
    }

    private suspend fun authenticated(pin: String): Boolean {
        if (!mutable.value.state.pinEnabled || adminAuthorized) return true
        val state = mutable.value.state
        if (withContext(Dispatchers.Default) { ParentPin.accepts(state, pin) }) return true
        mutable.update { it.copy(notice = "The parent PIN is incorrect.") }
        return false
    }

    private fun reject(message: String): Boolean {
        mutable.update { it.copy(notice = message) }
        return false
    }

    private fun known(viewer: Viewer, childOnly: Boolean = false): Boolean {
        if (mutable.value.state.profile(viewer) == null) return reject("This family profile is unavailable.")
        if (childOnly && !viewer.isChild) return reject("Choose a child profile.")
        return true
    }

    private suspend fun commit(next: FamilyState): Boolean {
        return try {
            val saved = SessionEngine.checkpoint(next.validated(), elapsed())
            repository.save(saved)
            mutable.update { it.copy(state = saved, now = elapsed(), notice = null) }
            lastCheckpoint = elapsed()
            true
        } catch (_: Exception) {
            mutable.update { it.copy(storageFailed = true,
                notice = "Family Home could not save this change. Child apps are paused. Retry the saved state.") }
            if (mutable.value.state.selected?.isChild == true) {
                effect(FamilyEffect.PauseMedia)
                effect(FamilyEffect.OpenHome)
            }
            false
        }
    }

    suspend fun select(viewer: Viewer, pin: String = ""): Boolean = mutex.withLock {
        if (!known(viewer) || !available(viewer.isChild)) return@withLock false
        if (!viewer.isChild && !authenticated(pin)) return@withLock false
        val previous = mutable.value.state.selected
        val result = commit(SessionEngine.select(mutable.value.state, viewer, elapsed()))
        if (result) {
            adminAuthorized = !viewer.isChild
            if (previous?.isChild == true && previous != viewer) effect(FamilyEffect.PauseMedia)
        }
        result
    }

    suspend fun start(viewer: Viewer, minutes: Int, pin: String = ""): Boolean = mutex.withLock {
        if (!known(viewer, childOnly = true) || !available(true) || !authenticated(pin)) return@withLock false
        if (minutes !in 5..60 || minutes % 5 != 0)
            return@withLock reject("Choose 5 to 60 minutes in five-minute steps.")
        val previous = mutable.value.state.selected
        val result = commit(SessionEngine.start(mutable.value.state, viewer, minutes, elapsed()))
        if (result) {
            adminAuthorized = false
            if (previous?.isChild == true && previous != viewer) effect(FamilyEffect.PauseMedia)
        }
        result
    }

    suspend fun chooseProfiles(): Boolean = mutex.withLock { pauseLocked() }
    suspend fun screenOff(): Boolean = mutex.withLock { pauseLocked() }

    suspend fun wake(): Boolean = mutex.withLock {
        adminAuthorized = false
        val result = available() && commit(SessionEngine.suspend(mutable.value.state, elapsed()))
        effect(FamilyEffect.PauseMedia)
        effect(FamilyEffect.OpenHome)
        result
    }

    private suspend fun pauseLocked(): Boolean {
        adminAuthorized = false
        if (!available()) return false
        val child = mutable.value.state.selected?.isChild == true
        val result = commit(SessionEngine.suspend(mutable.value.state, elapsed()))
        if (child) effect(FamilyEffect.PauseMedia)
        return result
    }

    suspend fun end(viewer: Viewer, pin: String = ""): Boolean = mutex.withLock {
        if (!known(viewer) || !available() || !authenticated(pin)) return@withLock false
        val result = commit(SessionEngine.end(mutable.value.state, viewer, elapsed()))
        if (result) { adminAuthorized = false; effect(FamilyEffect.PauseMedia) }
        result
    }

    suspend fun setPin(pin: String?): Boolean = mutex.withLock {
        if (!available() || mutable.value.state.selected != Viewer.AMINE || !adminAuthorized) return@withLock false
        val state = mutable.value.state
        val next = if (pin == null) state.copy(pinSalt = "", pinHash = "")
            else withContext(Dispatchers.Default) { ParentPin.enable(state, pin) }
        commit(next.copy(revision = state.revision + 1))
    }

    suspend fun setAllowed(viewer: Viewer, pkg: String, allowed: Boolean): Boolean = mutex.withLock {
        if (!known(viewer, childOnly = true) || !available() || mutable.value.state.selected != Viewer.AMINE || !adminAuthorized) return@withLock false
        if (!pkg.matches(Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")))
            return@withLock reject("This app package is invalid.")
        val state = mutable.value.state
        val packages = state.allowed[viewer].orEmpty().let { if (allowed) it + pkg else it - pkg }
        commit(state.copy(allowed = state.allowed + (viewer to packages), revision = state.revision + 1))
    }

    suspend fun addChild(name: String): Viewer? = mutex.withLock {
        if (!available()) return@withLock null
        val state = mutable.value.state
        if (state.selected != Viewer.AMINE || !adminAuthorized) {
            reject("Open the parent profile before adding a child.")
            return@withLock null
        }
        val trimmed = name.trim()
        if (!validProfileName(trimmed) || name.any { it.isISOControl() }) {
            reject("Enter a name with 1 to 24 characters and no control characters.")
            return@withLock null
        }
        if (state.profiles.any { it.name.equals(trimmed, ignoreCase = true) }) {
            reject("This profile name is already in use.")
            return@withLock null
        }
        var viewer: Viewer
        do { viewer = Viewer("child_${UUID.randomUUID()}") } while (state.profile(viewer) != null)
        val next = state.copy(profiles = state.profiles + FamilyProfile(viewer, trimmed),
            allowed = state.allowed + (viewer to DefaultApps.labels.keys.toSet()), revision = state.revision + 1)
        if (commit(next)) viewer else null
    }

    fun mayLaunch(pkg: String): Boolean {
        val current = mutable.value
        if (!current.ready || current.storageFailed) return false
        if (current.state.selected?.isChild == true && !current.helper) return false
        return AccessPolicy.decide(current.state, pkg, "", elapsed()) == AccessDecision.ALLOW
    }

    suspend fun pulse() = mutex.withLock {
        val now = elapsed()
        mutable.update { it.copy(now = now) }
        if (!available()) return@withLock
        val state = mutable.value.state
        val viewer = state.selected
        val session = state.sessions[viewer]
        if (viewer == null || !viewer.isChild || session?.deadlineElapsedMs == null) return@withLock
        val remaining = SessionEngine.remaining(state, viewer, now)
        if (remaining == 0L) {
            if (commit(SessionEngine.tick(state, now, state.revision))) {
                effect(FamilyEffect.PauseMedia)
                effect(FamilyEffect.OpenHome)
            }
            return@withLock
        }
        val one = remaining <= 60_000 && !session.warnedOne
        val five = remaining <= 300_000 && session.totalMs > 300_000 && !session.warnedFive
        if (one || five) {
            val next = state.copy(sessions = state.sessions + (viewer to session.copy(
                warnedFive = session.warnedFive || five, warnedOne = session.warnedOne || one)))
            if (commit(next)) effect(FamilyEffect.Warning("${state.label(viewer)}: ${if (one) "1 minute" else "5 minutes"} left"))
        } else if (now - lastCheckpoint >= 10_000) commit(SessionEngine.checkpoint(state, now))
    }
}
