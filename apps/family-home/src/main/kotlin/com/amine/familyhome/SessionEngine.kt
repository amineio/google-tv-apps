package com.amine.familyhome

/** Pure elapsed-time rules. Android lifecycle and durable writes belong to the controller. */
object SessionEngine {
    fun remaining(state: FamilyState, viewer: Viewer, now: Long): Long {
        require(state.profile(viewer) != null) { "Unknown family profile" }
        val session = state.sessions[viewer] ?: return 0
        return (session.deadlineElapsedMs?.let { it - now } ?: session.remainingMs)
            .coerceIn(0, session.totalMs)
    }

    fun canUseApps(state: FamilyState, now: Long): Boolean = state.selected?.let {
        state.profile(it) != null && (!it.isChild || remaining(state, it, now) > 0)
    } ?: false

    fun checkpoint(state: FamilyState, now: Long): FamilyState = state.copy(
        sessions = state.sessions.mapValues { (viewer, session) ->
            val remaining = remaining(state, viewer, now)
            session.copy(remainingMs = remaining, deadlineElapsedMs = session.deadlineElapsedMs.takeIf { remaining > 0 })
        },
    )

    fun suspend(state: FamilyState, now: Long): FamilyState = checkpoint(state, now).let { saved ->
        saved.copy(selected = null, sessions = saved.sessions.mapValues { (_, session) ->
            session.copy(deadlineElapsedMs = null)
        }, revision = state.revision + 1)
    }

    fun select(state: FamilyState, viewer: Viewer, now: Long): FamilyState {
        require(state.profile(viewer) != null) { "Unknown family profile" }
        if (state.selected == viewer) return tick(state, now, state.revision)
        val paused = suspend(state, now)
        val session = paused.sessions[viewer]
        val sessions = if (session != null && session.remainingMs > 0) {
            paused.sessions + (viewer to session.copy(deadlineElapsedMs = now + session.remainingMs))
        } else paused.sessions
        return paused.copy(selected = viewer, sessions = sessions, recovered = false)
    }

    fun start(state: FamilyState, viewer: Viewer, minutes: Int, now: Long): FamilyState {
        require(state.profile(viewer) != null && viewer.isChild && minutes in 5..60 && minutes % 5 == 0) {
            "Choose a child and 5 to 60 minutes in five-minute steps"
        }
        val paused = suspend(state, now)
        val duration = minutes * 60_000L
        return paused.copy(selected = viewer,
            sessions = paused.sessions + (viewer to ChildSession(duration, duration, now + duration)), recovered = false)
    }

    fun tick(state: FamilyState, now: Long, expectedRevision: Long): FamilyState {
        if (state.revision != expectedRevision) return state
        return checkpoint(state, now)
    }

    fun end(state: FamilyState, viewer: Viewer, now: Long): FamilyState {
        require(state.profile(viewer) != null) { "Unknown family profile" }
        return suspend(state, now).let { it.copy(sessions = it.sessions - viewer) }
    }

    fun recover(state: FamilyState): FamilyState {
        state.validated()
        return state.copy(selected = null, sessions = state.sessions.mapValues { (_, session) ->
            session.copy(deadlineElapsedMs = null)
        }, revision = state.revision + 1, recovered = state.sessions.values.any { it.remainingMs > 0 })
    }
}
