package com.amine.tvclock;

import android.content.Context;

final class ClockPreferences {
    static boolean isEnabled(Context context) {
        return context.getSharedPreferences("clock", Context.MODE_PRIVATE).getBoolean("enabled", false);
    }

    static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences("clock", Context.MODE_PRIVATE).edit()
                .putBoolean("enabled", enabled).apply();
    }

    static int position(Context context) {
        return ClockLayout.position(context.getSharedPreferences("clock", Context.MODE_PRIVATE)
                .getInt("position", 0));
    }

    static int size(Context context) {
        return ClockLayout.size(context.getSharedPreferences("clock", Context.MODE_PRIVATE)
                .getInt("size", 22));
    }

    static void setPosition(Context context, int value) {
        context.getSharedPreferences("clock", Context.MODE_PRIVATE).edit()
                .putInt("position", ClockLayout.position(value)).apply();
    }

    static void setSize(Context context, int value) {
        context.getSharedPreferences("clock", Context.MODE_PRIVATE).edit()
                .putInt("size", ClockLayout.size(value)).apply();
    }

    static void resetLayout(Context context) {
        context.getSharedPreferences("clock", Context.MODE_PRIVATE).edit()
                .remove("position").remove("size").apply();
    }
}
