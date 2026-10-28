package com.amine.tvclock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LimitsDeviceTest {
    private Context context;
    private boolean backupOwned;

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
        assertTrue("Overlay and exact alarm access are required", LimitsController.access(context));
        ClockTestBackup.save(context);
        backupOwned = true;
        main(() -> {
            context.stopService(new Intent(context, ClockService.class));
            ClockPreferences.setEnabled(context, false);
            LimitsStore.save(context, new TimeLimits());
            LimitsController.check(context);
        });
    }

    @After public void restore() {
        if (!backupOwned) return;
        main(() -> {
            for (Activity a : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) a.finish();
            ClockTestBackup.restore(context);
            LimitsController.check(context);
        });
        home();
        ClockTestBackup.clear(context);
        backupOwned = false;
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

    private boolean dialogHas(String text) {
        AccessibilityNodeInfo root = InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
        return root != null && !root.findAccessibilityNodeInfosByText(text).isEmpty();
    }

    @Test public void customDurationSavesWithoutStartingAndBackCancelsEdits() {
        context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().putInt("minutes", 60).commit();
        Activity page = openMain();
        AtomicReference<View> duration = new AtomicReference<>();
        main(() -> {
            duration.set(page.findViewById(android.R.id.content).findViewWithTag("timer-duration"));
            duration.get().requestFocus(); duration.get().performClick();
        });
        waitUntil(() -> dialogHas("Hours") && dialogHas("Minutes"));
        shell("input keyevent KEYCODE_DPAD_UP KEYCODE_DPAD_RIGHT " +
                "KEYCODE_DPAD_UP KEYCODE_DPAD_UP KEYCODE_DPAD_UP KEYCODE_DPAD_CENTER");
        waitUntil(() -> context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getInt("minutes", 60) == 123);
        assertEquals(0, LimitsController.read(context).timerElapsed);
        main(() -> assertTrue(duration.get().isFocused()));
        main(() -> duration.get().performClick());
        waitUntil(() -> dialogHas("Hours"));
        shell("input keyevent KEYCODE_DPAD_UP KEYCODE_BACK");
        assertEquals(123, context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getInt("minutes", 60));
        main(() -> page.findViewById(R.id.timer_start).performClick());
        long remaining = LimitsController.read(context).timerElapsed - SystemClock.elapsedRealtime();
        assertTrue(remaining > 7_370_000 && remaining <= 7_380_000);
        main(() -> page.findViewById(R.id.timer_start).performClick());
        assertEquals(0, LimitsController.read(context).timerElapsed);
    }

    @Test public void zeroDurationCannotBeSavedOrStartATimer() {
        context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().putInt("minutes", 60).commit();
        Activity page = openMain();
        main(() -> page.findViewById(android.R.id.content).findViewWithTag("timer-duration").performClick());
        waitUntil(() -> dialogHas("Hours") && dialogHas("Minutes"));
        shell("input keyevent KEYCODE_DPAD_DOWN KEYCODE_DPAD_RIGHT KEYCODE_DPAD_CENTER");
        waitUntil(() -> dialogHas("Hours"));
        assertEquals(60, context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getInt("minutes", 60));
        assertEquals(0, LimitsController.read(context).timerElapsed);
        shell("input keyevent KEYCODE_DPAD_UP KEYCODE_DPAD_CENTER");
        waitUntil(() -> context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getInt("minutes", 60) == 1);
    }

    @Test public void analogClockShowsTheCurrentTvTime() {
        Activity page = openMain();
        AtomicReference<CharSequence> description = new AtomicReference<>();
        AtomicReference<View> clockFace = new AtomicReference<>();
        main(() -> {
            View face = page.findViewById(android.R.id.content).findViewWithTag("live-clock");
            assertNotNull("A live analog clock must be visible", face);
            assertTrue(face.isShown());
            clockFace.set(face);
            description.set(face.getContentDescription());
        });
        String current = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.ROOT).format(new java.util.Date());
        assertNotNull(description.get());
        assertTrue(description.get().toString(), description.get().toString().contains(current));
        screenshot("live-clock-main");
        main(() -> {
            TimeLimits changed = new TimeLimits();
            changed.startTimer(3L * 24 * 60 * 60_000, System.currentTimeMillis(),
                    SystemClock.elapsedRealtime(), LimitsController.boot(context));
            LimitsStore.save(context, changed);
        });
        String original = description.get().toString();
        long end = SystemClock.elapsedRealtime() + 70_000;
        while (original.equals(description.get().toString()) && SystemClock.elapsedRealtime() < end) {
            SystemClock.sleep(1_500);
            main(() -> description.set(clockFace.get().getContentDescription()));
        }
        assertNotEquals("The analog clock must update as the minute changes", original, description.get().toString());
        main(() -> assertTrue("The next deadline must refresh on a time tick",
                ((android.widget.TextView) page.findViewById(R.id.limits_summary)).getText().toString().startsWith("Next:")));
        current = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.ROOT).format(new java.util.Date());
        assertTrue(description.get().toString(), description.get().toString().contains(current));
    }

    @Test public void timerFieldsSupportAccessibilityAdjustment() {
        context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().putInt("minutes", 60).commit();
        Activity page = openMain();
        main(() -> page.findViewById(android.R.id.content).findViewWithTag("timer-duration").performClick());
        waitUntil(() -> dialogHas("Hours"));
        AccessibilityNodeInfo root = InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
        java.util.List<AccessibilityNodeInfo> hours = root.findAccessibilityNodeInfosByViewId("com.amine.tvclock:id/duration_hour");
        assertFalse(hours.isEmpty());
        assertTrue("The screen reader must be able to increase the hours", hours.get(0).performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD));
        waitUntil(() -> dialogHas("Hours: 2"));
        assertEquals(60, context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getInt("minutes", 60));
        shell("input keyevent KEYCODE_DPAD_CENTER");
        waitUntil(() -> context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getInt("minutes", 60) == 120);
    }

    @Test public void skippedBedtimeShowsTheNewDay() {
        main(() -> LimitsController.setBedtime(context, true, 22 * 60 + 30));
        Activity page = openMain();
        AtomicReference<String> before = new AtomicReference<>(), after = new AtomicReference<>();
        main(() -> {
            before.set(page.findViewById(R.id.bedtime_time).getContentDescription().toString());
            page.findViewById(R.id.bedtime_skip).performClick();
        });
        clickDialog("Skip next");
        main(() -> after.set(page.findViewById(R.id.bedtime_time).getContentDescription().toString()));
        assertTrue("The next bedtime day must be visible", before.get().contains("Next:"));
        assertNotEquals("Skipping must change the displayed day", before.get(), after.get());
    }

    @Test public void longTimerShowsItsEndDate() {
        main(() -> LimitsController.startTimer(context, 3 * 24 * 60 * 60_000L));
        Activity page = openMain();
        long wall = LimitsController.read(context).timerWall;
        String date = java.time.Instant.ofEpochMilli(wall).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                .format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d", java.util.Locale.getDefault()));
        AtomicReference<String> label = new AtomicReference<>();
        main(() -> label.set(page.findViewById(R.id.timer_start).getContentDescription().toString()));
        assertTrue(label.get(), label.get().contains(date));
    }

    @Test public void missingPermissionShowsThatLimitsAreInactive() {
        shell("appops set --user 0 com.amine.tvclock SYSTEM_ALERT_WINDOW ignore");
        try {
            waitUntil(() -> !android.provider.Settings.canDrawOverlays(context));
            Activity page = openMain();
            AtomicReference<String> message = new AtomicReference<>();
            main(() -> message.set(((android.widget.TextView) page.findViewById(R.id.limits_summary)).getText().toString()));
            assertTrue(message.get(), message.get().toLowerCase(java.util.Locale.ROOT).contains("limits inactive"));
        } finally {
            shell("appops set --user 0 com.amine.tvclock SYSTEM_ALERT_WINDOW allow");
            waitUntil(() -> LimitsController.access(context));
        }
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
        context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().putInt("minutes", 60).commit();
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
