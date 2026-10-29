package com.amine.familyhome

import android.accessibilityservice.AccessibilityService
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FamilyAccessService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stockWindows = emptySet<String>()
    private var foregroundPackage: String? = null
    private var foregroundWindow: String? = null
    private var forcedRevision: Long? = null
    private val redirects by lazy {
        RedirectDispatcher(SystemClock::elapsedRealtime,
            { wait, task -> scope.launch { delay(wait); task() } }, ::needsRedirect, ::performOpenHome)
    }
    private var warning: TextView? = null
    private var registered = false

    private val screens = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            scope.launch {
                if (intent.action == Intent.ACTION_SCREEN_OFF) family.screenOff()
                else if (ownsHome()) family.wake()
            }
        }
    }

    override fun onServiceConnected() {
        active = true
        family.helperConnected(ownsHome())
        stockWindows = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(HomeRouting.STOCK_PACKAGE), 0
        ).map { it.activityInfo.name }.toSet()
        family.effect = { effect ->
            when (effect) {
                FamilyEffect.PauseMedia -> pauseMedia()
                FamilyEffect.OpenHome -> if (interactive() && ownsHome()) openHome()
                is FamilyEffect.Warning -> if (interactive()) showWarning(effect.text)
            }
        }
        registerReceiver(screens, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF)
        }, RECEIVER_NOT_EXPORTED)
        registered = true
        // Always dispatch output after the controller's durable command releases its mutex.
        scope.launch(Dispatchers.Main) {
            family.voice.collect { announcement -> FamilySpeechService.dispatch(this@FamilyAccessService, announcement) }
        }
        scope.launch {
            if (ownsHome()) family.wake()
            while (isActive) {
                val home = ownsHome()
                family.helperConnected(home)
                if (!home || !interactive()) {
                    if (family.ui.value.state.selected != null) family.screenOff()
                } else family.pulse()
                delay(1_000)
            }
        }
    }

    private fun interactive() = getSystemService(PowerManager::class.java).isInteractive
    private fun ownsHome() = getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !interactive() || !ownsHome()) return
        val pkg = event.packageName?.toString()
        val window = event.className?.toString()
        foregroundPackage = pkg
        foregroundWindow = window
        if (HomeRouting.isStockHome(pkg, window, stockWindows)) { openHome(force = false); return }
        val current = family.ui.value
        val decision = if (current.storageFailed && pkg != packageName) AccessDecision.CHOOSER
            else AccessPolicy.decide(current.state, pkg, window, SystemClock.elapsedRealtime())
        if (decision != AccessDecision.ALLOW) {
            pauseMedia()
            android.util.Log.i("FamilyHome.Access", "Redirect package=$pkg window=$window decision=$decision")
            openHome(force = false)
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!ownsHome()) return false
        if (event.keyCode == KeyEvent.KEYCODE_SETTINGS) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
                if (family.ui.value.state.selected?.isAdult == true) {
                    startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } else openHome()
            }
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
                if (HomeRouting.isShortPress(event.downTime, event.eventTime, ViewConfiguration.getLongPressTimeout().toLong()) ||
                    family.ui.value.state.selected?.isAdult != true) openHome()
                else startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_ALL_APPS) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) openHome()
            return true
        }
        return false
    }

    private fun openHome(force: Boolean = true) {
        if (force) forcedRevision = family.ui.value.state.revision
        redirects.request()
    }

    private fun needsRedirect(): Boolean {
        if (!interactive() || !ownsHome()) return false
        val current = family.ui.value
        if (forcedRevision == current.state.revision) return true
        if (foregroundPackage == packageName) return false
        return HomeRouting.isStockHome(foregroundPackage, foregroundWindow, stockWindows) || current.storageFailed ||
            AccessPolicy.decide(current.state, foregroundPackage, foregroundWindow, SystemClock.elapsedRealtime()) != AccessDecision.ALLOW
    }

    private fun performOpenHome() {
        forcedRevision = null
        startActivity(Intent(this, MainActivity::class.java)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION))
    }

    private fun pauseMedia() {
        val audio = getSystemService(AudioManager::class.java)
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
    }

    private fun showWarning(text: String) {
        removeWarning()
        val view = TextView(this).apply {
            this.text = text; textSize = 25f; setTextColor(Color.WHITE)
            setBackgroundColor(0xEE142B35.toInt()); setPadding(32, 20, 32, 20)
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val layout = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = 50 }
        getSystemService(WindowManager::class.java).addView(view, layout)
        warning = view
        scope.launch { delay(5_000); if (warning === view) removeWarning() }
    }

    private fun removeWarning() {
        warning?.let { getSystemService(WindowManager::class.java).removeView(it) }
        warning = null
    }

    override fun onInterrupt() {
        family.cancelVoice()
    }
    override fun onDestroy() {
        active = false
        family.helperConnected(false)
        family.effect = {}
        (application as FamilyApplication).scope.launch { family.screenOff() }
        if (registered) unregisterReceiver(screens)
        removeWarning()
        scope.cancel()
        super.onDestroy()
    }

    companion object { var active = false; private set }
}
