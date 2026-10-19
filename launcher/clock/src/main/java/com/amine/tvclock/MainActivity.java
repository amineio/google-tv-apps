package com.amine.tvclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

public final class MainActivity extends Activity {
    private TextView summary, timerDetail, bedtimeDetail;
    private Button start, bedtimeTime, bedtimeToggle, skip, reminder, stopScreen, access;
    private Button[] presets;
    private int minutes;
    private final BroadcastReceiver changes = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) { refresh(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        minutes = getPreferences(MODE_PRIVATE).getInt("minutes", 60);
        LinearLayout page = TvUi.page(this);
        LinearLayout header = TvUi.row(this);
        header.addView(TvUi.text(this, "Time limits", 32, false), new LinearLayout.LayoutParams(0, -2, 1));
        TvUi.addButton(header, TvUi.button(this, "Clock appearance", R.id.clock_settings,
                () -> startActivity(new Intent(this, ClockSettingsActivity.class))), 200);
        page.addView(header); TvUi.gap(page, 12);
        summary = TvUi.text(this, "", 16, true); summary.setId(R.id.limits_summary);
        page.addView(summary); TvUi.gap(page, 28);

        LinearLayout timer = TvUi.row(this);
        label(timer, "Timer");
        int[] values = {30, 60, 90};
        int[] ids = {R.id.timer_30, R.id.timer_60, R.id.timer_90};
        presets = new Button[values.length];
        for (int i = 0; i < values.length; i++) {
            final int value = values[i];
            presets[i] = TvUi.button(this, value + " min", ids[i], () -> {
                minutes = value; getPreferences(MODE_PRIVATE).edit().putInt("minutes", minutes).apply(); refresh();
            });
            TvUi.addButton(timer, presets[i], 110);
        }
        start = TvUi.button(this, "Start timer", R.id.timer_start, () -> {
            TimeLimits s = LimitsController.read(this);
            if (s.timerElapsed > 0 || (s.extensionElapsed > 0 && !s.extensionBedtime)) {
                LimitsController.change(this, TimeLimits::cancelTimer);
            } else if (needAccess()) {
                LimitsController.startTimer(this, minutes * 60_000L);
            }
            refresh();
        });
        TvUi.addButton(timer, start, 164); page.addView(timer); TvUi.gap(page, 8);
        timerDetail = TvUi.text(this, "", 15, true); timerDetail.setId(R.id.timer_detail);
        page.addView(timerDetail); TvUi.gap(page, 24);

        LinearLayout bed = TvUi.row(this); label(bed, "Bedtime");
        bedtimeTime = TvUi.button(this, "22:30", R.id.bedtime_time, this::editTime);
        TvUi.addButton(bed, bedtimeTime, 128);
        bedtimeToggle = TvUi.button(this, "Off", R.id.bedtime_toggle, () -> {
            TimeLimits s = LimitsController.read(this);
            if (s.bedtimeEnabled || needAccess()) LimitsController.setBedtime(this, !s.bedtimeEnabled, s.bedtimeMinute);
            refresh();
        });
        TvUi.addButton(bed, bedtimeToggle, 100);
        skip = TvUi.button(this, "Skip next", R.id.bedtime_skip, this::skipBedtime);
        TvUi.addButton(bed, skip, 138); page.addView(bed); TvUi.gap(page, 8);
        bedtimeDetail = TvUi.text(this, "", 15, true); bedtimeDetail.setId(R.id.bedtime_detail);
        page.addView(bedtimeDetail); TvUi.gap(page, 24);

        LinearLayout action = TvUi.row(this); label(action, "At the limit");
        reminder = TvUi.button(this, "Remind me", R.id.action_remind, () -> LimitsController.change(this, s -> s.stopScreen = false));
        stopScreen = TvUi.button(this, "Stop screen", R.id.action_stop, () -> LimitsController.change(this, s -> s.stopScreen = true));
        TvUi.addButton(action, reminder, 170); TvUi.addButton(action, stopScreen, 170);
        page.addView(action); TvUi.gap(page, 16);
        access = TvUi.button(this, "Enable reminders", R.id.limits_access, this::requestAccess);
        page.addView(access, new LinearLayout.LayoutParams(TvUi.dp(this, 300), TvUi.dp(this, 48)));
        page.addView(TvUi.text(this, "5-minute warning · One 5-minute extension · Shared by everyone", 14, true));
        setContentView(page); start.requestFocus();
    }

    private void label(LinearLayout row, String text) {
        row.addView(TvUi.text(this, text, 20, false), new LinearLayout.LayoutParams(TvUi.dp(this, 140), -2));
    }

    private boolean needAccess() {
        if (LimitsController.access(this)) return true;
        requestAccess(); return false;
    }

    private void requestAccess() {
        String action = Settings.canDrawOverlays(this)
                ? Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM : Settings.ACTION_MANAGE_OVERLAY_PERMISSION;
        try { startActivity(new Intent(action, Uri.parse("package:" + getPackageName()))); }
        catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Allow TV Clock to show reminders in Settings > Apps.", Toast.LENGTH_LONG).show();
        }
    }

    private void refresh() {
        TimeLimits s = LimitsController.read(this);
        long wall = System.currentTimeMillis(), elapsed = SystemClock.elapsedRealtime();
        TimeLimits.Deadline next = s.next(wall, elapsed, LimitsController.boot(this));
        summary.setText(!LimitsController.access(this) ? "Reminders need permission. Limits are inactive."
                : next == null ? "No limits active. Start a timer or set a daily bedtime."
                : "Next stop at " + LimitsController.time(next.wall)
                    + (ClockPreferences.isEnabled(this) ? " · The earlier limit applies." : " · Clock hidden; limits stay active."));
        int[] values = {30, 60, 90};
        for (int i = 0; i < presets.length; i++) {
            presets[i].setSelected(values[i] == minutes);
            presets[i].setText((values[i] == minutes ? "✓ " : "") + values[i] + " min");
        }
        boolean timerActive = s.timerElapsed > 0 || (s.extensionElapsed > 0 && !s.extensionBedtime);
        start.setText(timerActive ? "Cancel timer" : "Start timer");
        long timerEnd = s.timerElapsed > 0 ? wall + s.timerElapsed - elapsed : wall + s.extensionElapsed - elapsed;
        timerDetail.setText(timerActive ? "Timer ends at " + LimitsController.time(timerEnd)
                : "Counts from Start timer, including standby.");
        bedtimeTime.setText(String.format(Locale.ROOT, "%02d:%02d", s.bedtimeMinute / 60, s.bedtimeMinute % 60));
        bedtimeToggle.setText(s.bedtimeEnabled ? "On" : "Off"); bedtimeToggle.setSelected(s.bedtimeEnabled);
        skip.setVisibility(s.bedtimeEnabled ? View.VISIBLE : View.GONE);
        boolean tomorrow = s.bedtimeEnabled && !Instant.ofEpochMilli(s.bedtimeWall).atZone(ZoneId.systemDefault())
                .toLocalDate().equals(Instant.ofEpochMilli(wall).atZone(ZoneId.systemDefault()).toLocalDate());
        bedtimeDetail.setText(!s.bedtimeEnabled ? "Every day · Off until you turn it on."
                : "Every day · Next reminder: " + (tomorrow ? "tomorrow at " : "today at ") + LimitsController.time(s.bedtimeWall));
        reminder.setSelected(!s.stopScreen); stopScreen.setSelected(s.stopScreen);
        reminder.setText(s.stopScreen ? "Remind me" : "✓ Remind me");
        stopScreen.setText(s.stopScreen ? "✓ Stop screen" : "Stop screen");
        access.setVisibility(LimitsController.access(this) ? View.GONE : View.VISIBLE);
    }

    private void skipBedtime() {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Skip the next bedtime?")
                .setMessage("The daily schedule will continue after that.")
                .setNegativeButton("Keep bedtime", (d, which) -> {})
                .setPositiveButton("Skip next", (d, which) -> {
                    LimitsController.change(this, s -> s.skipBedtime(System.currentTimeMillis(), ZoneId.systemDefault()));
                    refresh();
                }).create();
        dialog.setOnShowListener(d -> TvUi.styleConfirmation(dialog)); dialog.show();
    }

    private void editTime() {
        TimeLimits s = LimitsController.read(this);
        int[] time = {s.bedtimeMinute / 60, s.bedtimeMinute % 60};
        Dialog dialog = new Dialog(this);
        LinearLayout page = TvUi.page(this);
        page.setBackground(TvUi.surface(this, TvUi.BACKGROUND, 0));
        page.addView(TvUi.text(this, "Daily bedtime", 28, false)); TvUi.gap(page, 24);
        LinearLayout row = TvUi.row(this);
        Runnable save = () -> {
            LimitsController.setBedtime(this, s.bedtimeEnabled, time[0] * 60 + time[1]);
            dialog.dismiss(); refresh();
        };
        Button hour = TvUi.button(this, String.format(Locale.ROOT, "%02d", time[0]), R.id.bedtime_hour, save);
        Button minute = TvUi.button(this, String.format(Locale.ROOT, "%02d", time[1]), R.id.bedtime_minute, save);
        Button[] fields = {hour, minute};
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            if (i == 1) row.addView(TvUi.text(this, ":  ", 32, false));
            fields[i].setTextSize(32);
            fields[i].setOnKeyListener((v, key, event) -> {
                if (key != KeyEvent.KEYCODE_DPAD_UP && key != KeyEvent.KEYCODE_DPAD_DOWN) return false;
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    time[index] = Math.floorMod(time[index] + (key == KeyEvent.KEYCODE_DPAD_UP ? 1 : -1), index == 0 ? 24 : 60);
                    fields[index].setText(String.format(Locale.ROOT, "%02d", time[index]));
                }
                return true;
            });
            TvUi.addButton(row, fields[i], 150);
            fields[i].getLayoutParams().height = TvUi.dp(this, 72);
        }
        page.addView(row); TvUi.gap(page, 24);
        page.addView(TvUi.text(this, "Left / Right: hours or minutes\nUp / Down: change · OK: save · Back: cancel", 16, true));
        dialog.setContentView(page); dialog.show();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(TvUi.dp(this, 560), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        hour.requestFocus();
    }

    @Override protected void onStart() {
        super.onStart(); registerReceiver(changes, new IntentFilter(ClockService.STATE_CHANGED), RECEIVER_NOT_EXPORTED);
    }
    @Override protected void onResume() { super.onResume(); LimitsController.check(this); refresh(); }
    @Override protected void onStop() { unregisterReceiver(changes); super.onStop(); }
}
