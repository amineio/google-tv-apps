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
import java.time.ZoneId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        summary = page.summary; summary.setId(R.id.limits_summary);
        appearance = new SettingRow(this, "Clock appearance", "", R.drawable.ic_clock, R.id.clock_settings,
                () -> startActivity(new Intent(this, ClockSettingsActivity.class)));
        appearance.setValue("›"); page.list.addView(appearance);
        duration = new SettingRow(this, "Timer duration", "", R.drawable.ic_timer, R.id.timer_duration,
                this::chooseDuration);
        duration.setTag("timer-duration"); duration.setValue("›"); page.list.addView(duration);
        start = new SettingRow(this, "Start timer", "", R.drawable.ic_play, R.id.timer_start, () -> {
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
        skip = new SettingRow(this, "Skip next bedtime", "", R.drawable.ic_skip,
                R.id.bedtime_skip, this::skipBedtime);
        page.list.addView(skip);
        limitAction = new SettingRow(this, "At the limit", "", R.drawable.ic_reminder, R.id.limit_action,
                this::chooseAction);
        limitAction.setTag("limit-action"); limitAction.setValue("›"); page.list.addView(limitAction);
        access = new SettingRow(this, "Enable reminders", "", R.drawable.ic_access,
                R.id.limits_access, this::requestAccess);
        access.setValue("›"); page.list.addView(access);
        setContentView(page);
        page.clock.setOnTimeChanged(this::refresh);
        appearance.requestFocus();
    }

    private void chooseDuration() {
        int[] time = {minutes / 60, minutes % 60};
        Dialog dialog = new Dialog(this);
        LinearLayout page = TvUi.page(this);
        page.setBackground(TvUi.surface(this, TvUi.BACKGROUND, 0));
        page.addView(TvUi.text(this, "Timer duration", 28, false)); TvUi.gap(page, 24);
        LinearLayout fields = TvUi.row(this);
        Runnable save = () -> {
            long selected = time[0] * 60L + time[1];
            if (selected == 0 || selected > Integer.MAX_VALUE) {
                Toast.makeText(this, selected == 0 ? "Set at least 1 minute" : "Duration is too long", Toast.LENGTH_SHORT).show();
                return;
            }
            minutes = (int) selected;
            getPreferences(MODE_PRIVATE).edit().putInt("minutes", minutes).apply();
            dialog.dismiss(); refresh();
        };
        Button hour = TvUi.button(this, String.valueOf(time[0]), R.id.duration_hour, save);
        Button minute = TvUi.button(this, String.format(Locale.ROOT, "%02d", time[1]), R.id.duration_minute, save);
        Button[] buttons = {hour, minute};
        String[] labels = {"Hours", "Minutes"};
        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            LinearLayout column = TvUi.column(this);
            TextView label = TvUi.text(this, labels[i], 16, true);
            label.setGravity(android.view.Gravity.CENTER);
            column.addView(label, new LinearLayout.LayoutParams(-1, -2)); TvUi.gap(column, 8);
            buttons[i].setTextSize(32);
            buttons[i].setContentDescription(labels[i] + ": " + buttons[i].getText());
            int max = index == 0 ? Integer.MAX_VALUE / 60 : 59;
            java.util.function.IntConsumer change = value -> {
                time[index] = Math.max(0, Math.min(max, value));
                buttons[index].setText(index == 0 ? String.valueOf(time[index]) : String.format(Locale.ROOT, "%02d", time[index]));
                buttons[index].setContentDescription(labels[index] + ": " + buttons[index].getText());
            };
            buttons[i].setAccessibilityDelegate(new View.AccessibilityDelegate() {
                @Override public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName("android.widget.SeekBar");
                    info.setScrollable(true);
                    info.setRangeInfo(android.view.accessibility.AccessibilityNodeInfo.RangeInfo.obtain(
                            android.view.accessibility.AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_INT, 0, max, time[index]));
                    info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
                    info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
                    info.addAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
                }
                @Override public boolean performAccessibilityAction(View host, int action, Bundle arguments) {
                    if (action == android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                            || action == android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
                        change.accept(time[index] + (action == android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD ? 1 : -1));
                        host.announceForAccessibility(host.getContentDescription());
                        return true;
                    }
                    if (action == android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId() && arguments != null
                            && arguments.containsKey(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE)) {
                        change.accept((int) arguments.getFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE));
                        host.announceForAccessibility(host.getContentDescription());
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, arguments);
                }
            });
            buttons[i].setOnKeyListener((v, key, event) -> {
                if (key != KeyEvent.KEYCODE_DPAD_UP && key != KeyEvent.KEYCODE_DPAD_DOWN) return false;
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    change.accept(time[index] + (key == KeyEvent.KEYCODE_DPAD_UP ? 1 : -1));
                }
                return true;
            });
            column.addView(buttons[i], new LinearLayout.LayoutParams(-1, TvUi.dp(this, 72)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(TvUi.dp(this, i == 0 ? 200 : 150), -2);
            if (i == 0) params.setMarginEnd(TvUi.dp(this, 16));
            fields.addView(column, params);
        }
        page.addView(fields); TvUi.gap(page, 20);
        page.addView(TvUi.text(this, "↑ ↓ Change   ·   OK Save", 14, true));
        dialog.setContentView(page);
        dialog.setOnDismissListener(d -> duration.requestFocus());
        dialog.show();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(TvUi.dp(this, 560), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        hour.requestFocus();
    }

    private String durationLabel() {
        int hours = minutes / 60, remainder = minutes % 60;
        if (hours == 0) return remainder + " min";
        return hours + " hr" + (remainder == 0 ? "" : " " + remainder + " min");
    }

    private String dayLabel(long wall) {
        LocalDate day = Instant.ofEpochMilli(wall).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.equals(today)) return "today";
        if (day.equals(today.plusDays(1))) return "tomorrow";
        String pattern = day.getYear() == today.getYear() ? "EEE, MMM d" : "EEE, MMM d, uuuu";
        return day.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()));
    }

    private String deadlineLabel(long wall) { return dayLabel(wall) + " " + LimitsController.time(wall); }

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
        summary.setText(!LimitsController.access(this) ? "Permission required. Limits inactive."
                : next == null ? "No active limits" : "Next: " + deadlineLabel(next.wall));
        appearance.setDetail(ClockPreferences.isEnabled(this) ? "On" : "Off");
        duration.setValue(durationLabel() + "   ›");
        boolean timerActive = s.timerElapsed > 0 || (s.extensionElapsed > 0 && !s.extensionBedtime);
        start.setTitle(timerActive ? "Cancel timer" : "Start timer");
        long timerEnd = s.timerElapsed > 0 ? wall + s.timerElapsed - elapsed : wall + s.extensionElapsed - elapsed;
        start.setDetail(timerActive ? "Ends " + deadlineLabel(timerEnd) : "");
        bedtimeTime.setValue(String.format(Locale.ROOT, "%02d:%02d   ›", s.bedtimeMinute / 60, s.bedtimeMinute % 60));
        bedtimeTime.setDetail(s.bedtimeEnabled ? "Next: " + dayLabel(s.bedtimeWall) : "");
        updating = true;
        bedtimeToggle.setChecked(s.bedtimeEnabled);
        updating = false;
        skip.setVisibility(s.bedtimeEnabled ? View.VISIBLE : View.GONE);
        TvUi.switchLabel(bedtimeToggle, "Daily bedtime", "");
        limitAction.setValue((s.stopScreen ? "Stop screen" : "Remind me") + "   ›");
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
        page.addView(TvUi.text(this, "↑ ↓ Change   ·   OK Save", 14, true));
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
