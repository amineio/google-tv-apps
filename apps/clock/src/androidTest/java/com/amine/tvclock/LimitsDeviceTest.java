package com.amine.tvclock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LimitsDeviceTest {
    private Context context;
    private Map<String, ?> limits, clock, mainPreferences;

    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private void waitUntil(BooleanSupplier condition) {
        long end = SystemClock.elapsedRealtime() + 12_000;
        while (SystemClock.elapsedRealtime() < end) {
            if (condition.getAsBoolean()) return;
            SystemClock.sleep(150);
        }
        fail("TV did not reach the expected state");
    }
    private Activity activity() {
        AtomicReference<Activity> current = new AtomicReference<>();
        main(() -> {
            for (Activity a : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) current.set(a);
        });
        return current.get();
    }
    private void home() {
        main(() -> context.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        waitUntil(() -> shell("dumpsys activity activities").matches("(?s).*topResumedActivity=.*com\\.gathin\\.tvhome/\\.MainActivity[^\\n]*.*"));
        shell("input keyevent KEYCODE_DPAD_UP");
        SystemClock.sleep(300);
    }
    private String shell(String command) {
        try (android.os.ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
             java.io.FileInputStream stream = new java.io.FileInputStream(fd.getFileDescriptor())) {
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) { throw new AssertionError(e); }
    }
    private void screenshot(String name) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(700);
        try {
            File output = new File(context.getExternalFilesDir(null), name + ".png");
            try (FileOutputStream stream = new FileOutputStream(output)) {
                InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot()
                        .compress(Bitmap.CompressFormat.PNG, 100, stream);
            }
        } catch (Exception e) { throw new AssertionError(e); }
    }
    private void clickDialog(String text) {
        waitUntil(() -> {
            AccessibilityNodeInfo root = InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
            return root != null && !root.findAccessibilityNodeInfosByText(text).isEmpty();
        });
        AccessibilityNodeInfo root = InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
        for (AccessibilityNodeInfo node : root.findAccessibilityNodeInfosByText(text)) {
            if (node.getText() != null && text.equalsIgnoreCase(node.getText().toString())) {
                assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK)); return;
            }
        }
        fail("Missing confirmation action: " + text);
    }

    @Before public void prepare() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        limits = new HashMap<>(context.getSharedPreferences("limits", Context.MODE_PRIVATE).getAll());
        clock = new HashMap<>(context.getSharedPreferences("clock", Context.MODE_PRIVATE).getAll());
        mainPreferences = new HashMap<>(context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getAll());
        assertTrue("Overlay and exact alarm access are required", LimitsController.access(context));
        main(() -> {
            context.stopService(new Intent(context, ClockService.class));
            ClockPreferences.setEnabled(context, false);
            LimitsStore.save(context, new TimeLimits());
            LimitsController.check(context);
        });
    }

    @After public void restore() {
        main(() -> {
            for (Activity a : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) a.finish();
            restorePreferences("limits", limits); restorePreferences("clock", clock);
            restorePreferences("MainActivity", mainPreferences);
            LimitsController.check(context);
        });
        home();
    }
    private void restorePreferences(String name, Map<String, ?> data) {
        if (data == null) return;
        SharedPreferences.Editor edit = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear();
        for (Map.Entry<String, ?> entry : data.entrySet()) {
            Object value = entry.getValue(); String key = entry.getKey();
            if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
            else if (value instanceof Long) edit.putLong(key, (Long) value);
            else if (value instanceof Integer) edit.putInt(key, (Integer) value);
            else if (value instanceof String) edit.putString(key, (String) value);
        }
        assertTrue(edit.commit());
    }

    private Activity openMain() {
        main(() -> context.startActivity(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        waitUntil(() -> activity() instanceof MainActivity);
        return activity();
    }

    @Test public void flatAppearanceRowsAdjustWithTheRemoteWithoutEnablingAHiddenClock() {
        main(() -> {
            ClockPreferences.setPosition(context, 20);
            ClockPreferences.setSize(context, 28);
        });
        Activity page = openMain();
        shell("input keyevent KEYCODE_DPAD_CENTER");
        waitUntil(() -> activity() instanceof ClockSettingsActivity);
        Activity settings = activity();
        main(() -> {
            View position = settings.findViewById(android.R.id.content).findViewWithTag("clock-position");
            assertNotNull("Position needs one remote-adjustable row", position);
            position.requestFocus();
        });
        shell("input keyevent KEYCODE_DPAD_LEFT");
        waitUntil(() -> ClockPreferences.position(context) == 19);
        shell("input keyevent KEYCODE_DPAD_RIGHT KEYCODE_DPAD_RIGHT");
        assertEquals(20, ClockPreferences.position(context));
        shell("input keyevent KEYCODE_DPAD_DOWN KEYCODE_DPAD_LEFT");
        waitUntil(() -> ClockPreferences.size(context) == 26);
        shell("input keyevent KEYCODE_DPAD_RIGHT");
        waitUntil(() -> ClockPreferences.size(context) == 28);
        assertFalse(ClockPreferences.isEnabled(context));
        assertFalse(shell("dumpsys window windows").contains("TV Clock overlay"));
        shell("input keyevent KEYCODE_BACK");
        waitUntil(() -> activity() instanceof MainActivity);
        main(() -> page.findViewById(R.id.clock_settings).performClick());
        waitUntil(() -> activity() instanceof ClockSettingsActivity);
        assertEquals(20, ClockPreferences.position(context));
        assertEquals(28, ClockPreferences.size(context));
        screenshot("settings-appearance");
    }

    @Test public void durationChoiceReturnsToTheRowAndDoesNotStartATimer() {
        Activity page = openMain();
        AtomicReference<View> duration = new AtomicReference<>();
        main(() -> {
            duration.set(page.findViewById(android.R.id.content).findViewWithTag("timer-duration"));
            assertNotNull("Timer duration needs a settings row", duration.get());
            duration.get().requestFocus();
        });
        shell("input keyevent KEYCODE_DPAD_CENTER");
        clickDialog("30 minutes");
        waitUntil(() -> context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getInt("minutes", 60) == 30);
        assertEquals(0, LimitsController.read(context).timerElapsed);
        main(() -> assertTrue(duration.get().isFocused()));
        main(() -> duration.get().performClick());
        shell("input keyevent KEYCODE_BACK");
        assertEquals(30, context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getInt("minutes", 60));
        main(() -> page.findViewById(R.id.timer_start).performClick());
        long remaining = LimitsController.read(context).timerElapsed - SystemClock.elapsedRealtime();
        assertTrue(remaining > 1_790_000 && remaining <= 1_800_000);
        main(() -> page.findViewById(R.id.timer_start).performClick());
        assertEquals(0, LimitsController.read(context).timerElapsed);
    }

    @Test public void reminderChoiceSavesAndBackKeepsTheSelectedAction() {
        Activity page = openMain();
        AtomicReference<View> action = new AtomicReference<>();
        main(() -> {
            action.set(page.findViewById(android.R.id.content).findViewWithTag("limit-action"));
            assertNotNull("Reminder action needs a settings row", action.get());
            action.get().performClick();
        });
        clickDialog("Remind me");
        waitUntil(() -> !LimitsController.read(context).stopScreen);
        main(() -> action.get().performClick());
        shell("input keyevent KEYCODE_BACK");
        assertFalse(LimitsController.read(context).stopScreen);
        assertEquals(0, LimitsController.read(context).timerElapsed);
    }

    @Test public void warningUsesARealAlarmWithoutClockOrFocusCapture() {
        home();
        main(() -> LimitsController.startTimer(context, TimeLimits.FIVE_MINUTES + 3_000));
        waitUntil(() -> shell("dumpsys window windows").contains("TV Clock warning"));
        String windows = shell("dumpsys window windows");
        assertFalse(windows.contains("TV Clock overlay"));
        assertTrue(shell("dumpsys activity activities").matches("(?s).*topResumedActivity=.*com\\.gathin\\.tvhome/\\.MainActivity[^\\n]*.*"));
        assertFalse(LimitsController.read(context).warnedToken.isEmpty());
        screenshot("limits-warning");
    }

    @Test public void stopFromBackgroundConfirmsAnExtensionAndCannotExtendTwice() {
        home(); shell("input keyevent KEYCODE_DPAD_UP");
        main(() -> {
            TimeLimits s = LimitsController.read(context);
            s.startTimer(3_000, System.currentTimeMillis(), SystemClock.elapsedRealtime(), LimitsController.boot(context));
            s.warnedToken = s.next(System.currentTimeMillis(), SystemClock.elapsedRealtime(), LimitsController.boot(context)).token;
            LimitsStore.save(context, s); LimitsController.check(context);
        });
        assertFalse(shell("dumpsys window windows").contains("TV Clock warning"));
        waitUntil(() -> activity() instanceof StopActivity);
        Activity stop = activity();
        waitUntil(() -> {
            java.util.concurrent.atomic.AtomicBoolean focused = new java.util.concurrent.atomic.AtomicBoolean();
            main(() -> focused.set(stop.findViewById(R.id.stop_home).isFocused()));
            return focused.get();
        });
        main(() -> {
            assertTrue(stop.findViewById(R.id.stop_home).isFocused());
            stop.findViewById(R.id.stop_extend).performClick();
        });
        assertEquals(0, LimitsController.read(context).extensionElapsed);
        screenshot("limits-extension-confirmation");
        clickDialog("Keep stop time");
        assertTrue(LimitsController.read(context).stopPending);
        main(() -> stop.findViewById(R.id.stop_extend).performClick());
        clickDialog("Add 5 minutes");
        waitUntil(() -> !LimitsController.read(context).stopPending);
        long remaining = LimitsController.read(context).extensionElapsed - SystemClock.elapsedRealtime();
        assertTrue(remaining > 295_000 && remaining <= 300_000);
        main(() -> {
            TimeLimits s = LimitsController.read(context);
            s.extensionElapsed = SystemClock.elapsedRealtime() + 2_000;
            s.extensionWall = System.currentTimeMillis() + 2_000;
            LimitsStore.save(context, s); LimitsController.check(context);
        });
        home(); waitUntil(() -> activity() instanceof StopActivity);
        Activity secondStop = activity();
        main(() -> assertNull(secondStop.findViewById(R.id.stop_extend)));
        screenshot("limits-stop-after-extension");
        shell("input keyevent KEYCODE_BACK");
        waitUntil(() -> !LimitsController.read(context).stopPending);
        main(() -> LimitsController.check(context)); SystemClock.sleep(500);
        assertFalse(activity() instanceof StopActivity);
    }

    @Test public void bedtimeAlarmAdvancesTheDailySchedule() {
        home();
        main(() -> {
            LimitsController.setBedtime(context, true, 22 * 60 + 30);
            TimeLimits s = LimitsController.read(context);
            s.bedtimeWall = System.currentTimeMillis() + 3_000;
            LimitsStore.save(context, s); LimitsController.check(context);
        });
        waitUntil(() -> activity() instanceof StopActivity);
        TimeLimits s = LimitsController.read(context);
        assertTrue(s.stopBedtime); assertTrue(s.bedtimeEnabled);
        assertTrue(s.bedtimeWall > System.currentTimeMillis());
        waitUntil(() -> !shell("dumpsys window windows").contains("TV Clock warning"));
        screenshot("limits-bedtime-stop");
    }

    @Test public void aNewDeadlineClosesTheOldExtensionConfirmation() {
        home(); main(() -> LimitsController.startTimer(context, 2_000));
        waitUntil(() -> activity() instanceof StopActivity);
        Activity stop = activity();
        main(() -> stop.findViewById(R.id.stop_extend).performClick());
        long first = LimitsController.read(context).stopVersion;
        main(() -> {
            TimeLimits s = LimitsController.read(context);
            s.bedtimeEnabled = true; s.bedtimeWall = System.currentTimeMillis() + 2_000;
            LimitsStore.save(context, s); LimitsController.check(context);
        });
        waitUntil(() -> LimitsController.read(context).stopVersion > first);
        waitUntil(() -> InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow() != null);
        AccessibilityNodeInfo root = InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
        assertNotNull(root);
        assertTrue("A new stop must close the previous occurrence's confirmation",
                root.findAccessibilityNodeInfosByText("Add 5 minutes").isEmpty());
        assertEquals(0, LimitsController.read(context).extensionElapsed);
    }

    @Test public void remoteControlsCanStartCancelAndEditWithoutArmingBedtime() {
        home();
        main(() -> context.startActivity(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        waitUntil(() -> activity() instanceof MainActivity);
        Activity page = activity();
        main(() -> page.findViewById(R.id.timer_start).performClick());
        long remaining = LimitsController.read(context).timerElapsed - SystemClock.elapsedRealtime();
        assertTrue(remaining > 1_790_000 && remaining <= 5_400_000);
        main(() -> page.findViewById(R.id.timer_start).performClick());
        assertEquals(0, LimitsController.read(context).timerElapsed);
        main(() -> page.findViewById(R.id.bedtime_time).performClick());
        waitUntil(() -> {
            AccessibilityNodeInfo root = InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
            if (root == null) return false;
            java.util.List<AccessibilityNodeInfo> fields = root.findAccessibilityNodeInfosByViewId("com.amine.tvclock:id/bedtime_hour");
            if (fields.isEmpty()) return false;
            android.graphics.Rect bounds = new android.graphics.Rect(); fields.get(0).getBoundsInScreen(bounds);
            return bounds.width() > 100;
        });
        int originalMinute = LimitsController.read(context).bedtimeMinute;
        shell("input keyevent KEYCODE_DPAD_UP KEYCODE_DPAD_RIGHT KEYCODE_DPAD_DOWN KEYCODE_DPAD_CENTER");
        int expected = ((originalMinute / 60 + 1) % 24) * 60 + Math.floorMod(originalMinute % 60 - 1, 60);
        waitUntil(() -> LimitsController.read(context).bedtimeMinute == expected);
        assertFalse(LimitsController.read(context).bedtimeEnabled);
        main(() -> page.findViewById(R.id.bedtime_time).performClick());
        screenshot("limits-time-picker");
        shell("input keyevent KEYCODE_BACK");
        assertFalse(LimitsController.read(context).bedtimeEnabled);
        screenshot("limits-main");
    }
}
