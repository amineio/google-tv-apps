package com.amine.tvclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

final class LimitsController {
    static int boot(Context c) { return Settings.Global.getInt(c.getContentResolver(), Settings.Global.BOOT_COUNT, 0); }
    static boolean access(Context c) {
        return c.getSystemService(AlarmManager.class).canScheduleExactAlarms() && Settings.canDrawOverlays(c);
    }
    static String time(long wall) {
        return DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
                .format(Instant.ofEpochMilli(wall).atZone(ZoneId.systemDefault()));
    }
    static synchronized TimeLimits read(Context c) {
        TimeLimits s = LimitsStore.load(c);
        s.restoreBoot(System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(c));
        return s;
    }
    static synchronized void change(Context c, Consumer<TimeLimits> action) {
        TimeLimits s = read(c); action.accept(s); LimitsStore.save(c, s); check(c);
    }
    static void startTimer(Context c, long duration) {
        change(c, s -> s.startTimer(duration, System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(c)));
    }
    static void setBedtime(Context c, boolean enabled, int minute) {
        change(c, s -> s.setBedtime(enabled, minute, System.currentTimeMillis(), ZoneId.systemDefault()));
    }
    static synchronized void check(Context c) {
        long wall = System.currentTimeMillis(), elapsed = SystemClock.elapsedRealtime();
        TimeLimits s = read(c);
        boolean visible = c.getSystemService(PowerManager.class).isInteractive() && access(c);
        TimeLimits.Event event = s.evaluate(wall, elapsed, boot(c), ZoneId.systemDefault(), visible);
        if (event.kind == TimeLimits.STOP && !s.stopScreen) s.dismiss(event.version);
        LimitsStore.save(c, s);
        arm(c, s);
        if (event.kind == TimeLimits.WARNING || (event.kind == TimeLimits.STOP && !s.stopScreen)) {
            String title = event.kind == TimeLimits.STOP ? "Time to stop"
                    : event.wall - wall >= 290_000 ? (event.bedtime ? "Bedtime in 5 minutes" : "5 minutes left")
                    : (event.bedtime ? "Bedtime soon" : "Your timer ends soon");
            ClockService.warn(c, title, "Time to stop at " + time(event.wall));
        } else if (event.kind == TimeLimits.STOP) {
            Intent stop = new Intent(c, StopActivity.class).putExtra("version", event.version)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            c.startActivity(stop);
        }
        ClockService.sync(c);
        c.sendBroadcast(new Intent(ClockService.STATE_CHANGED).setPackage(c.getPackageName()));
    }
    static synchronized void restore(Context c, boolean timeChanged) {
        TimeLimits s = read(c);
        if (timeChanged) s.clockChanged(System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(c), ZoneId.systemDefault());
        LimitsStore.save(c, s); check(c);
    }
    static synchronized boolean extend(Context c, long version) {
        TimeLimits s = read(c);
        if (s.stopVersion != version || !s.extend(System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(c))) return false;
        LimitsStore.save(c, s); check(c); return true;
    }
    static synchronized void dismiss(Context c, long version) {
        TimeLimits s = read(c); s.dismiss(version); LimitsStore.save(c, s); arm(c, s);
    }
    static synchronized void screenOn(Context c) {
        TimeLimits s = read(c);
        s.lastScreenOnWall = System.currentTimeMillis();
        s.lastScreenOnElapsed = SystemClock.elapsedRealtime();
        LimitsStore.save(c, s); check(c);
    }
    private static void arm(Context c, TimeLimits s) {
        AlarmManager alarms = c.getSystemService(AlarmManager.class);
        PendingIntent pending = PendingIntent.getBroadcast(c, 20, new Intent(c, LimitsReceiver.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        alarms.cancel(pending);
        if (!access(c)) return;
        long next = s.checkpoint(System.currentTimeMillis(), SystemClock.elapsedRealtime(), boot(c));
        if (next > 0) alarms.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, next, pending);
    }
}
