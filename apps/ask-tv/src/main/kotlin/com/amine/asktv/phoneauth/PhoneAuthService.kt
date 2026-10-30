package com.amine.asktv.phoneauth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import com.amine.asktv.R
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The service, rather than the browser-backgrounded activity, owns all temporary sockets. */
class PhoneAuthService : Service() {
    inner class LocalBinder : Binder() {
        val snapshot: StateFlow<PhoneAuthUi> get() = session.snapshot
        fun start(link: PhonePairingLink) {
            ensureForeground()
            notificationGeneration = UUID.randomUUID().toString()
            showNotification()
            session.start(link)
        }
        fun takeBrowserLaunch(): PhoneAuthorization? = session.takeBrowserLaunch()
        fun cancel() { session.cancel() }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: PhoneAuthSession
    private val binder = LocalBinder()
    @Volatile private var foreground = false
    @Volatile private var notificationGeneration = UUID.randomUUID().toString()

    override fun onCreate() {
        super.onCreate()
        session = PhoneAuthSession({ PhoneRelayClient(it) }, { callback, failure -> PhoneLoopbackServer(callback, failure) }, SystemClock::elapsedRealtime)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "TV sign-in", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps the phone sign-in helper available while you use your browser."
                setSound(null, null)
            },
        )
        scope.launch {
            session.snapshot.collect { ui ->
                if (ui.phase == PhoneAuthPhase.FINISHED) {
                    foreground = false
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }
    override fun onBind(intent: Intent?): IBinder = binder
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            if (intent.getStringExtra(EXTRA_GENERATION) == notificationGeneration) {
                session.cancel()
                foreground = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return START_NOT_STICKY
        }
        if (session.snapshot.value.phase == PhoneAuthPhase.FINISHED) { stopSelf(); return START_NOT_STICKY }
        try {
            ensureForeground()
        } catch (_: Exception) {
            session.cancel()
            foreground = false
            stopSelf()
        }
        return START_NOT_STICKY
    }
    private fun ensureForeground() {
        if (!foreground) {
            startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            foreground = true
        }
    }
    private fun showNotification() {
        if (foreground && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification())
        }
    }
    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, PhoneAuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // A unique data URI keeps an old PendingIntent separate from a newer generation.
        val cancel = PendingIntent.getService(this, 0, Intent(this, PhoneAuthService::class.java).apply {
            action = ACTION_CANCEL
            data = android.net.Uri.parse("aichat-helper://cancel/$notificationGeneration")
            putExtra(EXTRA_GENERATION, notificationGeneration)
        }, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.phone_notification)
            .setContentTitle("AI Chat TV sign-in")
            .setContentText("Finish in your browser. Open this helper to check or cancel.")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, "Open", open).build())
            .addAction(Notification.Action.Builder(null, "Cancel", cancel).build())
            .build()
    }
    override fun onTaskRemoved(rootIntent: Intent?) {
        session.cancel()
        foreground = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
    override fun onDestroy() {
        session.close()
        foreground = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        scope.cancel()
        super.onDestroy()
    }
    private companion object {
        const val CHANNEL = "ai-chat-phone-sign-in"
        const val NOTIFICATION_ID = 1455
        const val ACTION_CANCEL = "com.amine.asktv.phoneauth.CANCEL"
        const val EXTRA_GENERATION = "generation"
    }
}
