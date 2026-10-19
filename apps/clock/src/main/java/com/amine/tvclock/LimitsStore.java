package com.amine.tvclock;

import android.content.Context;
import android.content.SharedPreferences;

final class LimitsStore {
    static TimeLimits load(Context context) {
        SharedPreferences p = context.getSharedPreferences("limits", Context.MODE_PRIVATE);
        TimeLimits s = new TimeLimits();
        s.timerWall = p.getLong("timerWall", 0); s.timerElapsed = p.getLong("timerElapsed", 0);
        s.timerBoot = p.getInt("timerBoot", 0);
        s.stateBoot = p.getInt("stateBoot", 0);
        s.bedtimeEnabled = p.getBoolean("bedtimeEnabled", false);
        s.bedtimeMinute = p.getInt("bedtimeMinute", 1350); s.bedtimeWall = p.getLong("bedtimeWall", 0);
        s.lastBedtimeDay = p.getLong("lastBedtimeDay", Long.MIN_VALUE);
        s.extensionWall = p.getLong("extensionWall", 0); s.extensionElapsed = p.getLong("extensionElapsed", 0);
        s.extensionBoot = p.getInt("extensionBoot", 0); s.extensionBedtime = p.getBoolean("extensionBedtime", false);
        s.warnedToken = p.getString("warnedToken", "");
        s.stopPending = p.getBoolean("stopPending", false); s.stopBedtime = p.getBoolean("stopBedtime", false);
        s.stopExtended = p.getBoolean("stopExtended", false); s.stopVersion = p.getLong("stopVersion", 0);
        s.lastScreenOnWall = p.getLong("lastScreenOnWall", 0);
        s.lastScreenOnElapsed = p.getLong("lastScreenOnElapsed", 0);
        s.stopScreen = p.getBoolean("stopScreen", true);
        return s;
    }

    static void save(Context context, TimeLimits s) {
        boolean saved = context.getSharedPreferences("limits", Context.MODE_PRIVATE).edit()
                .putLong("timerWall", s.timerWall).putLong("timerElapsed", s.timerElapsed).putInt("timerBoot", s.timerBoot)
                .putInt("stateBoot", s.stateBoot)
                .putBoolean("bedtimeEnabled", s.bedtimeEnabled).putInt("bedtimeMinute", s.bedtimeMinute)
                .putLong("bedtimeWall", s.bedtimeWall).putLong("lastBedtimeDay", s.lastBedtimeDay)
                .putLong("extensionWall", s.extensionWall).putLong("extensionElapsed", s.extensionElapsed)
                .putInt("extensionBoot", s.extensionBoot).putBoolean("extensionBedtime", s.extensionBedtime)
                .putString("warnedToken", s.warnedToken).putBoolean("stopPending", s.stopPending)
                .putBoolean("stopBedtime", s.stopBedtime).putBoolean("stopExtended", s.stopExtended)
                .putLong("stopVersion", s.stopVersion).putLong("lastScreenOnWall", s.lastScreenOnWall)
                .putLong("lastScreenOnElapsed", s.lastScreenOnElapsed)
                .putBoolean("stopScreen", s.stopScreen).commit();
        if (!saved) throw new IllegalStateException("The TV could not save its time limits");
    }
}
