package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;

import java.util.HashSet;
import java.util.Set;

/** Home-key helper. Optional stock_home_redirect keeps Google's Cast dependencies enabled. */
public class HomeKeyService extends AccessibilityService {
    private static final String TAG = "TvHome.HomeKey";
    private int[] settingsScancodes = new int[0];
    private String stockPackage = "";
    private final Set<String> stockActivities = new HashSet<>();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        settingsScancodes = getResources().getIntArray(R.array.settings_key_scancodes);
        stockPackage = Config.load(this).stockHomeRedirect;
        stockActivities.clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !stockPackage.isEmpty()) {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(stockPackage);
            for (ResolveInfo info : getPackageManager().queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY)) {
                stockActivities.add(info.activityInfo.name);
            }
        }
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            // Only package/class window metadata, never screen content.
            info.eventTypes = stockActivities.isEmpty() ? 0 : AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
            setServiceInfo(info);
        }
        if (redirectEnabled()) probeAtBoot();
        Log.i(TAG, "Connected; stock HOME redirect=" + stockPackage);
    }

    private boolean redirectEnabled() {
        if (stockActivities.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;
        RoleManager roles = getSystemService(RoleManager.class);
        return roles != null && roles.isRoleHeld(RoleManager.ROLE_HOME);
    }

    private void probeAtBoot() {
        int boot = Settings.Global.getInt(getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        SharedPreferences prefs = getSharedPreferences("home_redirect", MODE_PRIVATE);
        if (!StockHomePolicy.probe(boot, prefs.getInt("probe_boot", -1), SystemClock.elapsedRealtime())) return;
        prefs.edit().putInt("probe_boot", boot).apply();
        try {
            startActivity(new Intent(this, ProbeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION));
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Cannot refresh the foreground window", e);
        }
    }

    private boolean isSettingsKey(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) return true;
        for (int code : settingsScancodes) if (event.getScanCode() == code) return true;
        return false;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (isSettingsKey(event)) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return true;
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_HOME && redirectEnabled()) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()
                    && StockHomePolicy.shortPress(event.getDownTime(), event.getEventTime(),
                    ViewConfiguration.getLongPressTimeout())) openHome();
            // Let Android retain its long-press action. A subsequent stock HOME window is redirected below.
            return false;
        }
        if (event.getKeyCode() != KeyEvent.KEYCODE_ALL_APPS) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) openHome();
        return true;
    }

    private void openHome() {
        // Without ACTION_MAIN this remains an ordinary task. Google Home keeps its background-launch
        // exemption for the remote's YouTube/Netflix buttons (upstream #1274).
        Intent home = redirectEnabled() ? new Intent(this, MainActivity.class)
                : new Intent(Intent.ACTION_MAIN).setPackage(getPackageName());
        try {
            startActivity(home.addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Cannot open Home", e);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && StockHomePolicy.redirect(redirectEnabled(), stockPackage, stockActivities,
                event.getPackageName(), event.getClassName())) {
            Log.i(TAG, "Redirecting stock HOME window");
            openHome();
        }
    }

    @Override
    public void onInterrupt() {}
}
