package com.amine.asktv

/** UI callbacks must carry the operation generation that created them. */
class AskController {
    private var generation = 0L
    private var cancel: (() -> Unit)? = null
    @Synchronized fun begin(cancelAction: () -> Unit): Long { stop(); cancel = cancelAction; return generation }
    @Synchronized fun isCurrent(token: Long): Boolean = cancel != null && generation == token
    @Synchronized fun finish(token: Long) { if (isCurrent(token)) cancel = null }
    @Synchronized fun stop() { generation++; val old = cancel; cancel = null; old?.invoke() }
    @Synchronized fun back(): Boolean { val busy = cancel != null; if (busy) stop(); return busy }
    companion object {
        fun recognitionError(reason: String): String = when(reason) {
            "permission" -> "Microphone permission is off. Type your question instead."
            "unavailable" -> "Speech recognition is unavailable on this TV. Type your question instead."
            else -> "No speech was recognized. Type your question or try Ask again."
        }
    }
}
