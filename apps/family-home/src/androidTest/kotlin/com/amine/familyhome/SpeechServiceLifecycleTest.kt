package com.amine.familyhome

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ServiceInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.runBlocking

class SpeechServiceLifecycleTest {
    @get:Rule val screen = createAndroidComposeRule<MainActivity>()

    @Suppress("DEPRECATION")
    @Test fun staleServiceStartStopsWithoutCreatingAnEngineOrRetainingNotification() {
        screen.waitUntil(10_000) { screen.activity.family.ui.value.ready }
        runBlocking { screen.activity.family.chooseProfiles() }
        val context = screen.activity
        val before = context.family.ui.value.speechStatus
        screen.runOnIdle {
            context.startForegroundService(Intent(context, FamilySpeechService::class.java)
                .putExtra("generation", -1L).putExtra("purpose", "START"))
            // A cancellation can arrive while Android still has the foreground start queued.
            FamilySpeechService.dispatch(context, null)
        }
        screen.waitUntil(5_000) {
            context.getSystemService(ActivityManager::class.java).getRunningServices(100)
                .none { it.service.className == FamilySpeechService::class.java.name }
        }
        // Keep the runner alive across the foreground-start watchdog window.
        android.os.SystemClock.sleep(12_000)
        assertFalse(context.getSystemService(ActivityManager::class.java).getRunningServices(100)
            .any { it.service.className == FamilySpeechService::class.java.name })
        assertEquals(before, context.family.ui.value.speechStatus)
        assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.none { it.id == 1901 })
        val info = context.packageManager.getServiceInfo(ComponentName(context, FamilySpeechService::class.java), 0)
        assertFalse(info.exported)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, info.foregroundServiceType)
    }
}
