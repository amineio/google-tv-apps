package com.amine.tvclock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class ClockSettingsActivity extends Activity {
    private Switch clockSwitch;
    private TextView status;
    private SettingRow positionRow, sizeRow;
    private boolean updating;
    private boolean awaitingPermission;
    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refresh(); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        awaitingPermission = state != null && state.getBoolean("awaitingPermission");
        TvUi.SettingsPage page = new TvUi.SettingsPage(this, "Clock appearance",
                "Keep the time in view.\nChanges stay saved.");
        status = page.summary;
        clockSwitch = TvUi.switchRow(this, getString(R.string.show_clock),
                "24-hour time · Transparent background", R.drawable.ic_clock, R.id.clock_switch);
        page.list.addView(clockSwitch, new android.widget.LinearLayout.LayoutParams(-1, -2));
        positionRow = new SettingRow(this, getString(R.string.position), "Along the right edge", R.drawable.ic_position,
                R.id.position_control, null);
        positionRow.setTag("clock-position");
        positionRow.setValueId(R.id.position_value);
        positionRow.setAdjustment(() -> changePosition(-1), () -> changePosition(1));
        page.list.addView(positionRow);
        sizeRow = new SettingRow(this, getString(R.string.size), "Use Left or Right to adjust", R.drawable.ic_text_size,
                R.id.size_control, null);
        sizeRow.setValueId(R.id.size_value);
        sizeRow.setAdjustment(() -> changeSize(-2), () -> changeSize(2));
        page.list.addView(sizeRow);
        page.list.addView(new SettingRow(this, getString(R.string.reset_layout), "Restore the original layout",
                R.drawable.ic_reset, R.id.reset_layout, () -> {
                    ClockPreferences.resetLayout(this);
                    applyLayout();
                }));
        SettingRow limits = new SettingRow(this, "Time limits", "Timer and daily bedtime", R.drawable.ic_timer,
                R.id.time_limits, this::finish);
        limits.setValue("›"); page.list.addView(limits);
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
        status.setText(enabled ? "The clock is on.\nChanges appear immediately and stay saved."
                : "The clock is off.\nChoose its position and size before you turn it on.");
        int position = ClockPreferences.position(this);
        positionRow.setValue(position == 0 ? getString(R.string.top)
                : position == 20 ? getString(R.string.bottom) : getString(R.string.position_percent, position * 5));
        sizeRow.setValue(getString(R.string.size_number, ClockPreferences.size(this)));
        updating = false;
    }

    private void applyLayout() {
        if (ClockPreferences.isEnabled(this)) setClock(Settings.canDrawOverlays(this));
        else refresh();
    }

    private void changePosition(int step) {
        ClockPreferences.setPosition(this, ClockPreferences.position(this) + step);
        applyLayout();
    }

    private void changeSize(int step) {
        ClockPreferences.setSize(this, ClockPreferences.size(this) + step);
        applyLayout();
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

}
