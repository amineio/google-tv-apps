package com.amine.tvclock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class ClockSettingsActivity extends Activity {
    private Switch clockSwitch;
    private TextView status;
    private TextView positionValue;
    private TextView sizeValue;
    private boolean updating;
    private boolean awaitingPermission;
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        awaitingPermission = state != null && state.getBoolean("awaitingPermission");
        LinearLayout page = new LinearLayout(this);
        page.setBackgroundColor(TvUi.BACKGROUND);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_VERTICAL);
        page.setPadding(dp(64), dp(40), dp(64), dp(40));
        page.addView(text("Clock appearance", 30));
        clockSwitch = new Switch(this);
        clockSwitch.setId(R.id.clock_switch);
        clockSwitch.setText(R.string.show_clock);
        clockSwitch.setTextSize(22);
        clockSwitch.setTextColor(Color.WHITE);
        clockSwitch.setShowText(false);
        clockSwitch.setPadding(dp(16), 0, dp(16), 0);
        clockSwitch.setBackgroundResource(android.R.drawable.list_selector_background);
        LinearLayout.LayoutParams row = new LinearLayout.LayoutParams(dp(530), dp(64));
        row.topMargin = dp(12);
        page.addView(clockSwitch, row);
        status = text("", 18);
        page.addView(status);
        positionValue = adjustmentRow(page, R.string.position, R.id.position_value,
                R.string.higher, R.id.position_up, () -> {
                    ClockPreferences.setPosition(this, ClockPreferences.position(this) - 1);
                    applyLayout();
                }, R.string.lower, R.id.position_down, () -> {
                    ClockPreferences.setPosition(this, ClockPreferences.position(this) + 1);
                    applyLayout();
                });
        sizeValue = adjustmentRow(page, R.string.size, R.id.size_value,
                R.string.smaller, R.id.size_down, () -> {
                    ClockPreferences.setSize(this, ClockPreferences.size(this) - 2);
                    applyLayout();
                }, R.string.larger, R.id.size_up, () -> {
                    ClockPreferences.setSize(this, ClockPreferences.size(this) + 2);
                    applyLayout();
                });
        Button reset = button(R.string.reset_layout, R.id.reset_layout, () -> {
            ClockPreferences.resetLayout(this);
            applyLayout();
        });
        page.addView(reset, new LinearLayout.LayoutParams(dp(530), dp(48)));
        page.addView(text("Changes appear immediately and stay saved.", 16));
        setContentView(page);
        clockSwitch.requestFocus();
        clockSwitch.setOnCheckedChangeListener((button, enabled) -> {
            if (updating) return;
            if (enabled && !Settings.canDrawOverlays(this)) {
                awaitingPermission = true;
                refresh();
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                } catch (android.content.ActivityNotFoundException e) {
                    awaitingPermission = false;
                    Toast.makeText(this, "Allow display over other apps in Settings > Apps.",
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
            setClock(enabled);
        });
    }

    private void setClock(boolean enabled) {
        ClockPreferences.setEnabled(this, enabled);
        Intent service = new Intent(this, ClockService.class);
        if (enabled) {
            try {
                startForegroundService(service);
            } catch (RuntimeException e) {
                ClockPreferences.setEnabled(this, false);
                Toast.makeText(this, "The clock could not start. Turn it on again.", Toast.LENGTH_LONG).show();
            }
        } else {
            ClockService.sync(this);
        }
        refresh();
    }

    private void refresh() {
        updating = true;
        boolean enabled = ClockPreferences.isEnabled(this) && Settings.canDrawOverlays(this);
        clockSwitch.setChecked(enabled);
        status.setText(enabled ? "On · 24-hour time · Transparent background" : "Off");
        int position = ClockPreferences.position(this);
        positionValue.setText(position == 0 ? getString(R.string.top)
                : position == 20 ? getString(R.string.bottom) : getString(R.string.position_percent, position * 5));
        sizeValue.setText(getString(R.string.size_number, ClockPreferences.size(this)));
        updating = false;
    }

    private void applyLayout() {
        if (ClockPreferences.isEnabled(this)) setClock(Settings.canDrawOverlays(this));
        else refresh();
    }

    private TextView adjustmentRow(LinearLayout page, int label, int valueId,
            int lessLabel, int lessId, Runnable less, int moreLabel, int moreId, Runnable more) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(getString(label), 20), new LinearLayout.LayoutParams(dp(180), dp(56)));
        row.addView(button(lessLabel, lessId, less), new LinearLayout.LayoutParams(dp(120), dp(48)));
        TextView value = text("", 18);
        value.setId(valueId);
        value.setGravity(Gravity.CENTER);
        row.addView(value, new LinearLayout.LayoutParams(dp(110), dp(56)));
        row.addView(button(moreLabel, moreId, more), new LinearLayout.LayoutParams(dp(120), dp(48)));
        page.addView(row, new LinearLayout.LayoutParams(dp(530), dp(64)));
        return value;
    }

    private Button button(int label, int id, Runnable action) {
        return TvUi.button(this, getString(label), id, action);
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ClockService.STATE_CHANGED);
        registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED);
    }

    @Override protected void onResume() {
        super.onResume();
        if (awaitingPermission) {
            awaitingPermission = false;
            if (Settings.canDrawOverlays(this)) setClock(true);
        }
        if (ClockPreferences.isEnabled(this)) {
            setClock(Settings.canDrawOverlays(this));
        }
        refresh();
    }

    @Override protected void onStop() {
        unregisterReceiver(stateReceiver);
        super.onStop();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("awaitingPermission", awaitingPermission);
        super.onSaveInstanceState(state);
    }

    private TextView text(String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(Color.WHITE);
        view.setPadding(0, dp(8), 0, dp(8));
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
