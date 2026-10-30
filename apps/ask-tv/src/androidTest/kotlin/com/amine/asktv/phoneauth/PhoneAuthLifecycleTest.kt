package com.amine.asktv.phoneauth

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amine.asktv.OAuthProtocol
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.Assume.assumeTrue
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.runner.RunWith
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Uses the actual activity/service, Android Keystore TLS, and the actual 1455 loopback socket. */
@RunWith(AndroidJUnit4::class)
class PhoneAuthLifecycleTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun await(condition: () -> Boolean) {
        val limit = SystemClock.elapsedRealtime() + 8_000
        while (!condition() && SystemClock.elapsedRealtime() < limit) Thread.sleep(20)
        assertTrue("Expected phone lifecycle state within 8 seconds", condition())
    }
    private fun permission(name: String, grant: Boolean) {
        val operation = if (grant) "grant" else "revoke"
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm $operation ${context.packageName} $name").use {
            java.io.FileInputStream(it.fileDescriptor).readBytes()
        }
    }
    private fun withBoundService(block: (PhoneAuthService.LocalBinder, ActivityScenario<PhoneAuthActivity>) -> Unit) {
        if (Build.VERSION.SDK_INT >= 37) permission("android.permission.ACCESS_LOCAL_NETWORK", true)
        ActivityScenario.launch<PhoneAuthActivity>(Intent(context, PhoneAuthActivity::class.java)).use { activity ->
            val bound = CountDownLatch(1); lateinit var binder: PhoneAuthService.LocalBinder
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) { binder = service as PhoneAuthService.LocalBinder; bound.countDown() }
                override fun onServiceDisconnected(name: ComponentName) { }
            }
            activity.onActivity { it.startForegroundService(Intent(it, PhoneAuthService::class.java)) }
            assertTrue(context.bindService(Intent(context, PhoneAuthService::class.java), connection, Context.BIND_AUTO_CREATE))
            assertTrue(bound.await(5, TimeUnit.SECONDS))
            try { block(binder, activity) } finally { binder.cancel(); context.unbindService(connection); context.stopService(Intent(context, PhoneAuthService::class.java)) }
        }
    }
    private fun withRelay(block: (PhonePairingLink, OAuthProtocol, AtomicInteger) -> Unit) {
        val dispatches = AtomicInteger()
        val oauth = OAuthProtocol("urn:uuid:00000000-0000-4000-8000-000000000001")
        lateinit var protocol: PhoneRelayProtocol
        protocol = PhoneRelayProtocol(oauth, SystemClock::elapsedRealtime, { grant ->
            oauth.commit(grant.attempt) { dispatches.incrementAndGet(); protocol.terminal(RelayState.CONNECTED) }
        })
        PhoneRelayServer(context, protocol).use { relay -> block(relay.start(), oauth, dispatches) }
    }
    @Test fun stoppedActivityRetainsRealLoopbackAndDispatchesCallbackOnlyOnce() {
        withBoundService { binder, activity -> withRelay { link, oauth, dispatches ->
            activity.moveToState(Lifecycle.State.CREATED)
            binder.start(link); await { binder.snapshot.value.canOpenBrowser }
            assertNotNull(binder.takeBrowserLaunch())
            val target = "/auth/callback?state=${oauth.pending!!.state}&code=synthetic&client_id=issued"
            Socket("127.0.0.1", 1455).use { peer ->
                peer.soTimeout = 5_000
                peer.outputStream.write("GET $target HTTP/1.1\r\nHost: 127.0.0.1:1455\r\n\r\n".toByteArray())
                peer.outputStream.flush(); assertTrue(peer.inputStream.bufferedReader().readLine().contains("200"))
            }
            await { binder.snapshot.value.phase == PhoneAuthPhase.FINISHED }
            assertEquals(1, dispatches.get())
        } }
    }
    @Test fun rotationAndRebindDoNotOfferBrowserTwice() {
        withBoundService { binder, activity -> withRelay { link, _, _ ->
            activity.moveToState(Lifecycle.State.CREATED); binder.start(link)
            await { binder.snapshot.value.canOpenBrowser }; assertNotNull(binder.takeBrowserLaunch())
            activity.recreate()
            assertNull(binder.takeBrowserLaunch())
            assertEquals(PhoneAuthPhase.WAITING_FOR_CALLBACK, binder.snapshot.value.phase)
        } }
    }
    @Test fun deniedNotificationPermissionDoesNotBlockForegroundSession() {
        permission("android.permission.POST_NOTIFICATIONS", false)
        withBoundService { binder, activity -> withRelay { link, _, _ ->
            activity.moveToState(Lifecycle.State.CREATED); binder.start(link)
            await { binder.snapshot.value.canOpenBrowser }; assertNotNull(binder.takeBrowserLaunch())
        } }
    }
    @Test fun staleNotificationCancelCannotCancelReplacementGeneration() {
        permission("android.permission.POST_NOTIFICATIONS", true)
        withBoundService { binder, activity ->
            activity.moveToState(Lifecycle.State.CREATED)
            withRelay { first, _, _ ->
                binder.start(first); await { binder.snapshot.value.canOpenBrowser }
                val manager = context.getSystemService(NotificationManager::class.java)
                await { manager.activeNotifications.any { it.notification.actions?.isNotEmpty() == true } }
                val cancel = manager.activeNotifications.first { it.notification.actions?.isNotEmpty() == true }.notification.actions.last().actionIntent
                binder.cancel(); await { binder.snapshot.value.phase == PhoneAuthPhase.FINISHED }
                withRelay { replacement, _, _ ->
                    // A new foreground attempt starts while the helper is visible.
                    activity.moveToState(Lifecycle.State.RESUMED)
                    activity.moveToState(Lifecycle.State.CREATED)
                    binder.start(replacement)
                    await { binder.snapshot.value.canOpenBrowser }
                    runCatching { cancel.send() }; Thread.sleep(300)
                    assertEquals(PhoneAuthPhase.READY_FOR_BROWSER, binder.snapshot.value.phase)
                }
            }
        }
    }
    @Test fun notificationCancelClosesTheRealLoopbackListener() {
        permission("android.permission.POST_NOTIFICATIONS", true)
        withBoundService { binder, activity -> withRelay { link, _, _ ->
            activity.moveToState(Lifecycle.State.CREATED); binder.start(link)
            await { binder.snapshot.value.canOpenBrowser }
            val manager = context.getSystemService(NotificationManager::class.java)
            await { manager.activeNotifications.any { it.notification.actions?.isNotEmpty() == true } }
            manager.activeNotifications.first { it.notification.actions?.isNotEmpty() == true }.notification.actions.last().actionIntent.send()
            await { binder.snapshot.value.phase == PhoneAuthPhase.FINISHED }
            await { runCatching { Socket("127.0.0.1", 1455).use { } }.isFailure }
        } }
    }
    @Test fun taskRemovalHandlerCancelsTheActualServiceSession() {
        withBoundService { binder, activity -> withRelay { link, _, _ ->
            activity.moveToState(Lifecycle.State.CREATED); binder.start(link)
            await { binder.snapshot.value.canOpenBrowser }
            // Invoke the real lifecycle handler without adding a production-only test hook.
            val owner = binder.javaClass.declaredFields.first { it.type == PhoneAuthService::class.java }.apply { isAccessible = true }.get(binder) as PhoneAuthService
            InstrumentationRegistry.getInstrumentation().runOnMainSync { owner.onTaskRemoved(null) }
            await { binder.snapshot.value.phase == PhoneAuthPhase.FINISHED }
            await { runCatching { Socket("127.0.0.1", 1455).use { } }.isFailure }
        } }
    }
    @Test fun disabledServiceShowsSafeFailureWithoutClaimingTheCode() {
        permission("android.permission.POST_NOTIFICATIONS", true)
        if (Build.VERSION.SDK_INT >= 37) permission("android.permission.ACCESS_LOCAL_NETWORK", true)
        val component = ComponentName(context, PhoneAuthService::class.java)
        context.packageManager.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        try {
            withRelay { link, _, _ ->
                ActivityScenario.launch<PhoneAuthActivity>(Intent(context, PhoneAuthActivity::class.java).apply { action = Intent.ACTION_VIEW; data = Uri.parse(link.encode()) }).use {
                    compose.onNodeWithText("Continue with ChatGPT").performScrollTo().performClick()
                    compose.onNodeWithText("The phone helper could not start. Reopen AI Chat and try a new TV code.").performScrollTo().assertIsDisplayed()
                    // A fresh claim proves that the failed helper never consumed this TV code.
                    PhoneRelayClient(link).use { assertTrue(it.claim().expiresInMs > 0) }
                }
            }
        } finally { context.packageManager.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP) }
    }
    @Test fun deniedLocalNetworkPermissionPreventsForegroundLanWorkOnApi37() {
        assumeTrue("ACCESS_LOCAL_NETWORK runtime permission begins on API 37", Build.VERSION.SDK_INT >= 37)
        permission("android.permission.ACCESS_LOCAL_NETWORK", false)
        fun flags(operation: String) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm $operation ${context.packageName} android.permission.ACCESS_LOCAL_NETWORK user-fixed").use {
                java.io.FileInputStream(it.fileDescriptor).readBytes()
            }
        }
        flags("set-permission-flags")
        try {
            val link = PhonePairingLink("192.168.1.2", 8443, "ab".repeat(32), "a".repeat(43))
            ActivityScenario.launch<PhoneAuthActivity>(Intent(context, PhoneAuthActivity::class.java).apply { action = Intent.ACTION_VIEW; data = Uri.parse(link.encode()) }).use {
                compose.onNodeWithText("Continue with ChatGPT").performScrollTo().performClick()
                compose.onNodeWithText("Local network access is needed to reach your TV. Select Continue to try again.").performScrollTo().assertIsDisplayed()
                assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.isEmpty())
            }
        } finally { flags("clear-permission-flags"); permission("android.permission.ACCESS_LOCAL_NETWORK", true) }
    }
    @Test fun freshServiceRequiresFreshQrInsteadOfRestoringSecrets() {
        withBoundService { binder, activity ->
            activity.moveToState(Lifecycle.State.CREATED)
            assertEquals(PhoneAuthPhase.IDLE, binder.snapshot.value.phase)
            assertNull(binder.takeBrowserLaunch())
        }
    }
}
