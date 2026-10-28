package com.amine.tvclock;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ClockTestRecoveryTest {
    @Test public void restoreInterruptedTestBackup() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        if (!ClockTestBackup.exists(context)) return;
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ClockTestBackup.restore(context);
            LimitsController.check(context);
            ClockTestBackup.clear(context);
        });
    }

    @Test public void backupRestoresPreferenceValuesAndTypes() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        java.util.Map<String, ?> limits = new java.util.HashMap<>(context.getSharedPreferences("limits", Context.MODE_PRIVATE).getAll());
        java.util.Map<String, ?> clock = new java.util.HashMap<>(context.getSharedPreferences("clock", Context.MODE_PRIVATE).getAll());
        java.util.Map<String, ?> main = new java.util.HashMap<>(context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getAll());
        ClockTestBackup.save(context);
        try {
            context.getSharedPreferences("limits", Context.MODE_PRIVATE).edit().putLong("test-only", 4L).commit();
            context.getSharedPreferences("clock", Context.MODE_PRIVATE).edit().putBoolean("test-only", true).commit();
            context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).edit().putInt("test-only", 5).commit();
            try {
                ClockTestBackup.save(context);
                org.junit.Assert.fail("An existing backup must not be replaced");
            } catch (AssertionError expected) {
                org.junit.Assert.assertEquals("Recover the interrupted TV test backup first", expected.getMessage());
            }
            ClockTestBackup.restore(context);
            org.junit.Assert.assertEquals(limits, context.getSharedPreferences("limits", Context.MODE_PRIVATE).getAll());
            org.junit.Assert.assertEquals(clock, context.getSharedPreferences("clock", Context.MODE_PRIVATE).getAll());
            org.junit.Assert.assertEquals(main, context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getAll());
        } finally {
            ClockTestBackup.restore(context);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> LimitsController.check(context));
            ClockTestBackup.clear(context);
        }
    }

    @Test public void failedSetupDoesNotDeleteAnotherTestsBackup() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ClockTestBackup.save(context);
        java.io.File original = new java.io.File(context.getExternalFilesDir(null), "clock-test-backup.json");
        java.io.File safety = new java.io.File(context.getExternalFilesDir(null), "clock-test-safety.json");
        java.nio.file.Files.copy(original.toPath(), safety.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        try {
            LimitsDeviceTest fixture = new LimitsDeviceTest();
            try {
                fixture.prepare();
                org.junit.Assert.fail("Existing backup must prevent test setup");
            } catch (AssertionError expected) {
                org.junit.Assert.assertEquals("Recover the interrupted TV test backup first", expected.getMessage());
            } finally {
                fixture.restore();
            }
            org.junit.Assert.assertTrue("Failed setup must preserve the existing backup", ClockTestBackup.exists(context));
        } finally {
            if (!ClockTestBackup.exists(context)) java.nio.file.Files.copy(safety.toPath(), original.toPath());
            ClockTestBackup.restore(context);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> LimitsController.check(context));
            ClockTestBackup.clear(context);
            org.junit.Assert.assertTrue(safety.delete());
        }
    }

    @Test public void interruptedPermissionTestRecoversOverlayAccess() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ClockTestBackup.save(context);
        android.app.UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        try {
            try (android.os.ParcelFileDescriptor fd = automation.executeShellCommand(
                    "appops set --user 0 com.amine.tvclock SYSTEM_ALERT_WINDOW ignore");
                 java.io.FileInputStream stream = new java.io.FileInputStream(fd.getFileDescriptor())) { stream.readAllBytes(); }
            org.junit.Assert.assertFalse(android.provider.Settings.canDrawOverlays(context));
            ClockTestBackup.restore(context);
            org.junit.Assert.assertTrue("Recovery must restore overlay access", android.provider.Settings.canDrawOverlays(context));
        } finally {
            try (android.os.ParcelFileDescriptor fd = automation.executeShellCommand(
                    "appops set --user 0 com.amine.tvclock SYSTEM_ALERT_WINDOW allow");
                 java.io.FileInputStream stream = new java.io.FileInputStream(fd.getFileDescriptor())) { stream.readAllBytes(); }
            ClockTestBackup.restore(context);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> LimitsController.check(context));
            ClockTestBackup.clear(context);
        }
    }
}
