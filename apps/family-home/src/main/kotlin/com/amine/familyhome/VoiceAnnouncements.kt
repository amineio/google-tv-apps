package com.amine.familyhome

import java.util.Locale

enum class VoicePurpose { START, RESUME, FIVE_MINUTES, ONE_MINUTE, EXPIRY }
data class VoiceAnnouncement(val viewer: Viewer, val generation: Long, val purpose: VoicePurpose, val remainingMs: Long)
data class OfflineVoice(val name: String, val locale: Locale, val networkRequired: Boolean, val features: Set<String>)
enum class SpeechStatus(val label: String) {
    NOT_CHECKED("Checked when a session starts"), CHECKING("Checking installed offline voice"),
    READY("Offline voice ready"), UNAVAILABLE("No suitable installed offline voice · Open speech setup"),
    FAILED("Speech unavailable · Visual timer warnings remain active"),
    FOCUS_DENIED("Audio focus unavailable · Visual timer warnings remain active"),
}
object VoicePolicy {
    fun select(locale: Locale, voices: List<OfflineVoice>): OfflineVoice? {
        val language = locale.language.takeIf { it in setOf("en", "nl", "fr") } ?: "en"
        return voices.filter { it.locale.language == language && !it.networkRequired && "notInstalled" !in it.features }
            .sortedWith(compareByDescending<OfflineVoice> { it.locale.country == locale.country }.thenBy { it.name })
            .firstOrNull()
    }
}
object VoicePhrases {
    fun text(name: String, purpose: VoicePurpose, remainingMs: Long, language: String): String {
        if (purpose == VoicePurpose.EXPIRY) return when (language) {
            "nl" -> "$name, je tijd is om."
            "fr" -> "$name, ton temps est écoulé."
            else -> "$name, your time is up."
        }
        val threshold = when (purpose) { VoicePurpose.FIVE_MINUTES -> 5; VoicePurpose.ONE_MINUTE -> 1; else -> null }
        val minutes = threshold ?: ((remainingMs.coerceAtLeast(1) + 59_999) / 60_000).toInt()
        val less = threshold != null && remainingMs < threshold * 60_000L
        return when (language) {
            "nl" -> if (less) "$name, je hebt minder dan $minutes ${if (minutes == 1) "minuut" else "minuten"} over."
                else "$name, je hebt nog $minutes ${if (minutes == 1) "minuut" else "minuten"}."
            "fr" -> if (less) "$name, il te reste moins de $minutes ${if (minutes == 1) "minute" else "minutes"}."
                else "$name, il te reste $minutes ${if (minutes == 1) "minute" else "minutes"}."
            else -> "$name, you have ${if (less) "less than " else ""}$minutes ${if (minutes == 1) "minute" else "minutes"} left."
        }
    }
}

/** External output boundary. Android initialization and utterance callbacks never enter timer locking. */
interface SpeechOutput {
    fun initialize(ready: (Boolean) -> Unit)
    fun requestFocus(onLoss: () -> Unit): Boolean
    fun speak(text: String, finished: () -> Unit): Boolean
    fun stop()
    fun abandonFocus()
    fun close()
}

/** One bounded announcement. Every callback checks cancellation and current session eligibility. */
class SpeechPlayback(private val output: SpeechOutput, private val valid: () -> Boolean,
    private val text: () -> String, private val finished: () -> Unit) {
    private var ended = false
    fun start() {
        if (!eligible()) { cancel(); return }
        try {
            output.initialize { ready ->
                if (ended) return@initialize
                try {
                    if (!ready || !eligible()) { cancel(); return@initialize }
                    if (!output.requestFocus(::cancel) || !eligible()) { cancel(); return@initialize }
                    if (!output.speak(text(), ::cancel)) cancel()
                } catch (_: Exception) { cancel() }
            }
        } catch (_: Exception) { cancel() }
    }
    private fun eligible() = !ended && valid()
    fun cancel() {
        if (ended) return
        ended = true
        runCatching { output.stop() }
        runCatching { output.abandonFocus() }
        runCatching { output.close() }
        finished()
    }
}
