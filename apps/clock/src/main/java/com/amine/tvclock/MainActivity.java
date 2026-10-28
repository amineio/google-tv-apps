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
import android.widget.Switch;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

public final class MainActivity extends Activity {
    private TextView summary;
    private SettingRow duration, start, bedtimeTime, skip, limitAction, access, appearance;
    private Switch bedtimeToggle;
    private int minutes;
    private boolean updating;
    private final BroadcastReceiver changes = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) { refresh(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        minutes = getPreferences(MODE_PRIVATE).getInt("minutes", 60);
        TvUi.SettingsPage page = new TvUi.SettingsPage(this, "Time limits", "");
        page.help.setText("↑ ↓  Choose\nOK  Open\nBack  Return");
        summary = page.summary; summary.setId(R.id.limits_summary);
        appearance = new SettingRow(this, "Clock appearance", "", R.drawable.ic_clock, R.id.clock_settings,
                () -> startActivity(new Intent(this, ClockSettingsActivity.class)));
        appearance.setValue("›"); page.list.addView(appearance);
        duration = new SettingRow(this, "Timer duration", "", R.drawable.ic_timer, R.id.timer_duration,
                this::chooseDuration);
        duration.setTag("timer-duration"); duration.setValue("›"); page.list.addView(duration);
        start = new SettingRow(this, "Start timer", "Counts time in standby", R.drawable.ic_play, R.id.timer_start, () -> {
            TimeLimits s = LimitsController.read(this);
            if (s.timerElapsed > 0 || (s.extensionElapsed > 0 && !s.extensionBedtime)) {
                LimitsController.change(this, TimeLimits::cancelTimer);
            } else if (needAccess()) {
                LimitsController.startTimer(this, minutes * 60_000L);
            }
            refresh();
        });
        page.list.addView(start);
        bedtimeToggle = TvUi.switchRow(this, "Daily bedtime", "", R.drawable.ic_bedtime, R.id.bedtime_toggle);
        bedtimeToggle.setOnCheckedChangeListener((button, enabled) -> {
            if (updating) return;
            TimeLimits s = LimitsController.read(this);
            if (!enabled || needAccess()) LimitsController.setBedtime(this, enabled, s.bedtimeMinute);
            refresh();
        });
        page.list.addView(bedtimeToggle, new LinearLayout.LayoutParams(-1, -2));
        bedtimeTime = new SettingRow(this, "Bedtime", "", R.drawable.ic_schedule, R.id.bedtime_time, this::editTime);
        bedtimeTime.setValue("›"); page.list.addView(bedtimeTime);
        skip = new SettingRow(this, "Skip next bedtime", "The daily schedule continues after that", R.drawable.ic_skip,
                R.id.bedtime_skip, this::skipBedtime);
        page.list.addView(skip);
        limitAction = new SettingRow(this, "At the limit", "", R.drawable.ic_reminder, R.id.limit_action,
                this::chooseAction);
        limitAction.setTag("limit-action"); limitAction.setValue("›"); page.list.addView(limitAction);
        access = new SettingRow(this, "Enable reminders", "Allow reminders over other apps", R.drawable.ic_access,
                R.id.limits_access, this::requestAccess);
        access.setValue("›"); page.list.addView(access);
        TvUi.gap(page.list, 12);
        TextView note = TvUi.text(this, "5-minute warning · One 5-minute extension", 14, true);
        note.setPadding(TvUi.dp(this, 16), 0, TvUi.dp(this, 16), TvUi.dp(this, 16));
        page.list.addView(note);
        setContentView(page); appearance.requestFocus();
    }

    private void chooseDuration() {
        int[] values = {30, 60, 90};
        int selected = minutes == 30 ? 0 : minutes == 90 ? 2 : 1;
        TvUi.choiceDialog(this, "Timer duration", new String[]{"30 minutes", "60 minutes", "90 minutes"},
                selected, duration, which -> {
                    minutes = values[which];
                    getPreferences(MODE_PRIVATE).edit().putInt("minutes", minutes).apply();
                    refresh();
                });
    }

    private void chooseAction() {
        TvUi.choiceDialog(this, "At the limit", new String[]{"Remind me", "Stop screen"},
                LimitsController.read(this).stopScreen ? 1 : 0, limitAction, which -> {
                    LimitsController.change(this, s -> s.stopScreen = which == 1);
                    refresh();
                });
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
        appearance.setDetail(ClockPreferences.isEnabled(this) ? "On · Position and text size" : "Off · Position and text size");
        duration.setDetail(minutes + " minutes");
        boolean timerActive = s.timerElapsed > 0 || (s.extensionElapsed > 0 && !s.extensionBedtime);
        start.setTitle(timerActive ? "Cancel timer" : "Start timer");
        long timerEnd = s.timerElapsed > 0 ? wall + s.timerElapsed - elapsed : wall + s.extensionElapsed - elapsed;
        start.setDetail(timerActive ? "Ends at " + LimitsController.time(timerEnd) : "Counts time in standby");
        bedtimeTime.setDetail(String.format(Locale.ROOT, "%02d:%02d", s.bedtimeMinute / 60, s.bedtimeMinute % 60));
        updating = true;
        bedtimeToggle.setChecked(s.bedtimeEnabled);
        updating = false;
        skip.setVisibility(s.bedtimeEnabled ? View.VISIBLE : View.GONE);
        boolean tomorrow = s.bedtimeEnabled && !Instant.ofEpochMilli(s.bedtimeWall).atZone(ZoneId.systemDefault())
                .toLocalDate().equals(Instant.ofEpochMilli(wall).atZone(ZoneId.systemDefault()).toLocalDate());
        TvUi.switchLabel(bedtimeToggle, "Daily bedtime", !s.bedtimeEnabled ? "Off · Repeats every day when enabled"
                : "Next: " + (tomorrow ? "tomorrow at " : "today at ") + LimitsController.time(s.bedtimeWall));
        limitAction.setDetail(s.stopScreen ? "Stop screen · Full-screen reminder" : "Remind me · Brief message");
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
