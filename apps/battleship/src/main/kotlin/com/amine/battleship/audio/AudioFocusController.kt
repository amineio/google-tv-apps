package com.amine.battleship.audio

enum class AudioFocusChange { GAIN, LOSS, TRANSIENT_LOSS, DUCK }

interface AudioFocusLease {
    val granted: Boolean
    fun abandon()
}

interface AudioFocusBackend {
    fun request(changed: (AudioFocusChange) -> Unit): AudioFocusLease
}

/** Main-thread focus bookkeeping. Only eligibility transitions or explicit input can request focus. */
class AudioFocusController(
    private val backend: AudioFocusBackend,
    private val changed: (focused: Boolean, ducked: Boolean) -> Unit,
) : AutoCloseable {
    var focused = false
        private set
    var ducked = false
        private set
    private var eligible = false
    private var closed = false
    private var generation = 0L
    private var lease: AudioFocusLease? = null

    fun update(eligible: Boolean, explicit: Boolean = false) {
        if (closed) return
        val becameEligible = eligible && !this.eligible
        this.eligible = eligible
        if (!eligible) { abandon(); return }
        if (lease != null || (!becameEligible && !explicit)) return
        val token = ++generation
        val requested = backend.request { event ->
            if (!closed && this.eligible && lease != null && token == generation) {
                when (event) {
                    AudioFocusChange.GAIN -> publish(true, false)
                    AudioFocusChange.DUCK -> publish(focused, true)
                    AudioFocusChange.LOSS -> abandon()
                    AudioFocusChange.TRANSIENT_LOSS -> publish(false, false)
                }
            }
        }
        lease = requested
        if (requested.granted) publish(true, false) else abandon()
    }

    private fun publish(granted: Boolean, duck: Boolean) {
        focused = granted
        ducked = duck
        changed(granted, duck)
    }

    private fun abandon() {
        val previous = lease
        lease = null
        generation++
        publish(false, false)
        previous?.abandon()
    }

    override fun close() {
        if (closed) return
        closed = true
        eligible = false
        abandon()
    }
}
