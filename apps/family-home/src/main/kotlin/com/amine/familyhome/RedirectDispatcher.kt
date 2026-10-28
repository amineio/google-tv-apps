package com.amine.familyhome

class RedirectDispatcher(
    private val now: () -> Long,
    private val schedule: (Long, () -> Unit) -> Unit,
    private val needed: () -> Boolean,
    private val open: () -> Unit,
) {
    private var lastOpen = -350L
    private var queued = false
    fun request() {
        if (queued) return
        queued = true
        schedule((350 - (now() - lastOpen)).coerceAtLeast(0)) {
            queued = false
            if (needed()) { lastOpen = now(); open() }
        }
    }
}
