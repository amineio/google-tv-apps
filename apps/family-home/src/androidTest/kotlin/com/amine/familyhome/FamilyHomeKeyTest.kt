package com.amine.familyhome

import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import android.view.ViewConfiguration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Calls the real key filter without starting or binding the accessibility service. */
@RunWith(AndroidJUnit4::class)
class FamilyHomeKeyTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val controller get() = context.family
    private val launches = mutableListOf<Intent>()
    private var service: FamilyAccessService? = null
    private var originalRoles: List<String>? = null
    private var originalState: FamilyState? = null
    private var user = ""
    private val longPressMs get() = ViewConfiguration.getLongPressTimeout().toLong()

    @Before fun createKeyFilter() {
        // The default UiAutomation connection suppresses real accessibility services.
        assertEquals("This fixture changes the Home role only on an emulator", "1", shell("getprop ro.kernel.qemu"))
        await { !FamilyAccessService.active && controller.ui.value.ready }
        assertTrue(context.getSystemService(PowerManager::class.java).isInteractive)
        user = shell("am get-current-user")
        check(user.matches(Regex("[0-9]+")))
        originalRoles = homeRoles()
        shell("cmd package set-home-activity --user $user ${context.packageName}/.MainActivity")
        await { ownsHome() }
        runBlocking {
            val repository = DataStoreFamilyRepository(context)
            originalState = repository.load()
            repository.save(FamilyState())
            assertTrue(controller.load())
            controller.helperConnected(true)
            assertTrue(controller.select(Viewer.AMINE))
        }
        instrumentation.runOnMainSync {
            val capturingContext = object : ContextWrapper(context) {
                override fun startActivity(intent: Intent) { launches.add(Intent(intent)) }
            }
            service = FamilyAccessService().also {
                ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java)
                    .apply { isAccessible = true }.invoke(it, capturingContext)
            }
        }
    }

    @After fun restoreFixture() {
        try {
            service?.let { fixture ->
                instrumentation.runOnMainSync {
                    (FamilyAccessService::class.java.getDeclaredField("scope")
                        .apply { isAccessible = true }.get(fixture) as CoroutineScope).cancel()
                }
            }
            originalState?.let { saved ->
                runBlocking {
                    DataStoreFamilyRepository(context).save(saved)
                    controller.load()
                }
                controller.helperConnected(false)
            }
        } finally {
            originalRoles?.let { saved ->
                if (homeRoles() != saved) {
                    shell("cmd role clear-role-holders --user $user android.app.role.HOME")
                    saved.forEach { pkg ->
                        check(pkg.matches(Regex("[A-Za-z0-9_.]+")))
                        shell("cmd role add-role-holder --user $user android.app.role.HOME $pkg")
                    }
                    await { homeRoles() == saved }
                }
            }
        }
    }

    @Test fun shortHomeConsumesDownRepeatAndUpAndOpensFamilyHomeOnce() {
        val before = controller.ui.value.state
        val consumed = listOf(
            send(key(KeyEvent.ACTION_DOWN)),
            send(key(KeyEvent.ACTION_DOWN, duration = 1, repeat = 1)),
            send(key(KeyEvent.ACTION_UP, duration = 2)),
        )
        assertHomeLaunch()
        assertEquals(before, controller.ui.value.state)
        assertEquals(listOf(true, true, true), consumed)
    }

    @Test fun canceledHomeReleasesAreConsumedWithoutOpeningAnything() {
        val before = controller.ui.value.state
        val consumed = listOf(2L, longPressMs + 1).flatMap { duration ->
            listOf(send(key(KeyEvent.ACTION_DOWN)),
                send(key(KeyEvent.ACTION_UP, duration, flags = KeyEvent.FLAG_CANCELED)))
        }
        assertTrue(launches.isEmpty())
        assertEquals(before, controller.ui.value.state)
        assertEquals(listOf(true, true, true, true), consumed)
    }

    @Test fun parentHomeHoldOpensSettingsAtReleaseWithoutChangingProfile() {
        val before = controller.ui.value.state
        val down = send(key(KeyEvent.ACTION_DOWN))
        val repeat = send(key(KeyEvent.ACTION_DOWN, longPressMs, repeat = 1, flags = KeyEvent.FLAG_LONG_PRESS))
        assertTrue("Holding Home must wait for release", launches.isEmpty())
        val up = send(key(KeyEvent.ACTION_UP, longPressMs))
        assertEquals(Settings.ACTION_SETTINGS, launches.single().action)
        assertEquals(before, controller.ui.value.state)
        assertEquals(listOf(true, true, true), listOf(down, repeat, up))
    }

    @Test fun secondAdultHasTheSameSettingsKeyAndHeldHomeActions() {
        runBlocking { assertTrue(controller.select(Viewer("HASNAA"))) }
        val before = controller.ui.value.state
        pressHome(longPressMs + 1)
        assertEquals(Settings.ACTION_SETTINGS, launches.single().action)
        launches.clear()
        send(key(KeyEvent.ACTION_DOWN, code = KeyEvent.KEYCODE_SETTINGS))
        send(key(KeyEvent.ACTION_UP, 2, code = KeyEvent.KEYCODE_SETTINGS))
        assertEquals(Settings.ACTION_SETTINGS, launches.single().action)
        assertEquals(before, controller.ui.value.state)
    }

    @Test fun childHomeHoldReturnsHomeWithoutChangingSession() {
        runBlocking { assertTrue(controller.start(Viewer.ELIAS, 20)) }
        val before = controller.ui.value.state
        val consumed = pressHome(longPressMs + 1)
        assertHomeLaunch()
        assertEquals(before, controller.ui.value.state)
        assertEquals(listOf(true, true), consumed)
    }

    @Test fun chooserHomeHoldReturnsHomeWithoutSelectingAProfile() {
        runBlocking { assertTrue(controller.chooseProfiles()) }
        val before = controller.ui.value.state
        val consumed = pressHome(longPressMs + 1)
        assertHomeLaunch()
        assertEquals(before, controller.ui.value.state)
        assertEquals(listOf(true, true), consumed)
    }

    @Test fun losingHomeRolePassesHomeSettingsAndAllAppsThrough() {
        shell("cmd role clear-role-holders --user $user android.app.role.HOME")
        await { !ownsHome() }
        val before = controller.ui.value.state
        val consumed = listOf(KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_SETTINGS, KeyEvent.KEYCODE_ALL_APPS).flatMap { code ->
            listOf(send(key(KeyEvent.ACTION_DOWN, code = code)), send(key(KeyEvent.ACTION_UP, 2, code = code)))
        }
        assertEquals(List(6) { false }, consumed)
        assertTrue(launches.isEmpty())
        assertEquals(before, controller.ui.value.state)
    }

    @Test fun parentSettingsKeyStillOpensSettings() {
        val down = send(key(KeyEvent.ACTION_DOWN, code = KeyEvent.KEYCODE_SETTINGS))
        val up = send(key(KeyEvent.ACTION_UP, 2, code = KeyEvent.KEYCODE_SETTINGS))
        assertEquals(Settings.ACTION_SETTINGS, launches.single().action)
        assertEquals(listOf(true, true), listOf(down, up))
    }

    @Test fun childSettingsKeyStillReturnsHome() {
        runBlocking { assertTrue(controller.start(Viewer.ELIAS, 20)) }
        val before = controller.ui.value.state
        val down = send(key(KeyEvent.ACTION_DOWN, code = KeyEvent.KEYCODE_SETTINGS))
        val up = send(key(KeyEvent.ACTION_UP, 2, code = KeyEvent.KEYCODE_SETTINGS))
        assertHomeLaunch()
        assertEquals(before, controller.ui.value.state)
        assertEquals(listOf(true, true), listOf(down, up))
    }

    @Test fun directionalKeysPassThroughWithoutOpeningAnActivity() {
        val before = controller.ui.value.state
        assertFalse(send(key(KeyEvent.ACTION_DOWN, code = KeyEvent.KEYCODE_DPAD_RIGHT)))
        assertFalse(send(key(KeyEvent.ACTION_UP, 2, code = KeyEvent.KEYCODE_DPAD_RIGHT)))
        assertTrue(launches.isEmpty())
        assertEquals(before, controller.ui.value.state)
    }

    private fun pressHome(duration: Long) = listOf(send(key(KeyEvent.ACTION_DOWN)), send(key(KeyEvent.ACTION_UP, duration)))

    private fun key(action: Int, duration: Long = 0, repeat: Int = 0, flags: Int = 0, code: Int = KeyEvent.KEYCODE_HOME) =
        KeyEvent(1_000, 1_000 + duration, action, code, repeat, 0, 0, 0, flags)

    private fun send(event: KeyEvent): Boolean {
        var consumed = false
        instrumentation.runOnMainSync {
            consumed = FamilyAccessService::class.java.getDeclaredMethod("onKeyEvent", KeyEvent::class.java)
                .apply { isAccessible = true }.invoke(requireNotNull(service), event) as Boolean
        }
        instrumentation.waitForIdleSync()
        return consumed
    }

    private fun assertHomeLaunch() {
        val intent = launches.single()
        assertEquals(context.packageName, intent.component?.packageName)
        assertEquals("${context.packageName}.MainActivity", intent.component?.className)
        assertTrue(intent.hasCategory(Intent.CATEGORY_HOME))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    private fun ownsHome() = context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
    private fun homeRoles() = shell("cmd role get-role-holders --user $user android.app.role.HOME").lines().filter(String::isNotBlank)

    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }

    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        while (!condition()) {
            if (SystemClock.elapsedRealtime() >= deadline) fail("Timed out waiting for the key filter fixture")
            SystemClock.sleep(10)
        }
    }
}
