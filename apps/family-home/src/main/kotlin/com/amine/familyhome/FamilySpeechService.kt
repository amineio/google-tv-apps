package com.amine.familyhome

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

/** A real foreground service exists only for one speech attempt, never between announcements. */
class FamilySpeechService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var playback: SpeechPlayback? = null
    private var request: VoiceAnnouncement? = null
    private var generation = 0L
    private val timeout = Runnable {
        family.speechStatus(SpeechStatus.FAILED)
        playback?.cancel() ?: finishService()
    }

    override fun onCreate() {
        super.onCreate()
        // Main (not immediate) keeps Android cleanup outside controller StateFlow writes.
        scope.launch(Dispatchers.Main) {
            family.ui.collectLatest {
                request?.let { if (!valid(it)) playback?.cancel() }
            }
        }
        scope.launch(Dispatchers.Main) {
            family.voice.collectLatest { current ->
                if (request != null && current != request) playback?.cancel()
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android requires promotion even when this queued start became stale before delivery.
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Timer announcements", NotificationManager.IMPORTANCE_LOW).apply {
                setSound(null, null); enableVibration(false)
            })
            val home = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val notification = Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_timer_voice).setContentTitle("Family Home timer")
                .setContentText("Speaking a timer announcement").setContentIntent(home)
                .setCategory(Notification.CATEGORY_SERVICE).setOnlyAlertOnce(true).setOngoing(true).build()
            startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } catch (_: RuntimeException) {
            family.speechStatus(SpeechStatus.FAILED)
            playback?.cancel() ?: finishService()
            return START_NOT_STICKY
        }
        // Ignore an obsolete start without canceling a newer, already active request.
        val next = family.voice.value
        if (next == null || intent?.getLongExtra(REQUEST_GENERATION, -1) != next.generation ||
            intent?.getStringExtra(REQUEST_PURPOSE) != next.purpose.name || !valid(next)) {
            if (request == null) finishService()
            return START_NOT_STICKY
        }
        generation++
        val run = generation
        val previous = playback
        playback = null
        request = null
        previous?.cancel()
        request = next
        val output = AndroidSpeechOutput(this)
        playback = SpeechPlayback(output, { run == generation && valid(next) }, {
            val state = family.ui.value.state
            VoicePhrases.text(state.label(next.viewer), next.purpose,
                SessionEngine.remaining(state, next.viewer, SystemClock.elapsedRealtime()), output.language)
        }) {
            if (run == generation) finishService()
        }
        main.removeCallbacks(timeout)
        main.postDelayed(timeout, 15_000)
        playback?.start()
        return START_NOT_STICKY
    }
    private fun valid(announcement: VoiceAnnouncement): Boolean = FamilyAccessService.active &&
        getSystemService(PowerManager::class.java).isInteractive &&
        getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME) && family.isVoiceCurrent(announcement)

    private fun finishService() {
        main.removeCallbacks(timeout)
        request = null
        playback = null
        resetInitializingStatus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    override fun onDestroy() {
        generation++
        main.removeCallbacks(timeout)
        playback?.cancel()
        playback = null
        request = null
        scope.cancel()
        resetInitializingStatus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    private fun resetInitializingStatus() {
        if (family.ui.value.speechStatus == SpeechStatus.CHECKING) family.speechStatus(SpeechStatus.NOT_CHECKED)
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "timer_voice"
        private const val NOTIFICATION = 1901
        private const val REQUEST_GENERATION = "generation"
        private const val REQUEST_PURPOSE = "purpose"
        fun dispatch(context: Context, announcement: VoiceAnnouncement?) {
            // A queued foreground start must reach onStartCommand and promote before it stops.
            // Active output observes controller cancellation; never stop an unpromoted service here.
            if (announcement == null) return
            val service = Intent(context, FamilySpeechService::class.java)
            try {
                context.startForegroundService(service.putExtra(REQUEST_GENERATION, announcement.generation)
                    .putExtra(REQUEST_PURPOSE, announcement.purpose.name))
            } catch (_: RuntimeException) {
                context.family.speechStatus(SpeechStatus.FAILED)
            }
        }
    }
}

/** Android resources are confined to the main thread and one service attempt. */
private class AndroidSpeechOutput(private val context: Context) : SpeechOutput {
    private val main = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private var engine: TextToSpeech? = null
    private var focus: AudioFocusRequest? = null
    private var closed = false
    private var completion: (() -> Unit)? = null
    var language = "en"
        private set

    override fun initialize(ready: (Boolean) -> Unit) {
        context.family.speechStatus(SpeechStatus.CHECKING)
        try {
        engine = TextToSpeech(context) { status -> main.post {
            if (closed) return@post
            try {
                val tts = engine
                if (status != TextToSpeech.SUCCESS || tts == null) {
                    context.family.speechStatus(SpeechStatus.FAILED); ready(false); return@post
                }
                val voices = tts.voices.orEmpty()
                val deviceLocale = context.resources.configuration.locales[0] ?: Locale.getDefault()
                val selected = VoicePolicy.select(deviceLocale, voices.map {
                    OfflineVoice(it.name, it.locale, it.isNetworkConnectionRequired, it.features.orEmpty())
                })
                val installed = voices.firstOrNull { it.name == selected?.name }
                if (installed == null || tts.setVoice(installed) != TextToSpeech.SUCCESS || tts.voice?.name != installed.name ||
                    tts.setAudioAttributes(attributes) != TextToSpeech.SUCCESS) {
                    context.family.speechStatus(SpeechStatus.UNAVAILABLE); ready(false); return@post
                }
                language = installed.locale.language
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = Unit
                    override fun onDone(id: String?) { complete() }
                    @Deprecated("Android callback") override fun onError(id: String?) {
                        main.post { if (!closed) context.family.speechStatus(SpeechStatus.FAILED); complete() }
                    }
                    override fun onError(id: String?, errorCode: Int) { onError(id) }
                    override fun onStop(id: String?, interrupted: Boolean) { complete() }
                })
                context.family.speechStatus(SpeechStatus.READY)
                ready(true)
            } catch (_: Exception) {
                context.family.speechStatus(SpeechStatus.FAILED); ready(false)
            }
        } }
        } catch (_: RuntimeException) {
            context.family.speechStatus(SpeechStatus.FAILED)
            ready(false)
        }
    }
    private fun complete() { main.post { if (!closed) completion?.invoke() } }
    override fun requestFocus(onLoss: () -> Unit): Boolean {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes).setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener({ change ->
                if (!closed && change < 0) onLoss()
            }, main).build()
        focus = request
        return try {
            val granted = audio.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (!granted) context.family.speechStatus(SpeechStatus.FOCUS_DENIED)
            granted
        } catch (_: RuntimeException) {
            context.family.speechStatus(SpeechStatus.FAILED)
            false
        }
    }
    override fun speak(text: String, finished: () -> Unit): Boolean {
        if (closed) return false
        completion = finished
        val accepted = engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "timer") == TextToSpeech.SUCCESS
        if (!accepted) context.family.speechStatus(SpeechStatus.FAILED)
        return accepted
    }
    override fun stop() { engine?.stop() }
    override fun abandonFocus() { focus?.let { audio.abandonAudioFocusRequest(it) }; focus = null }
    override fun close() {
        closed = true
        completion = null
        engine?.shutdown()
        engine = null
    }
}
