package com.amine.tvclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

public final class StopActivity extends Activity {
    private long version, shownAt;
    private boolean ready;
    private AlertDialog confirmation;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); render();
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); render();
    }
    @Override protected void onResume() {
        super.onResume();
        TimeLimits s = LimitsController.read(this);
        if (!s.stopPending || s.stopVersion != version) finish();
    }
    private void render() {
        if (confirmation != null) { confirmation.dismiss(); confirmation = null; }
        version = getIntent().getLongExtra("version", -1);
        TimeLimits s = LimitsController.read(this);
        if (!s.stopPending || s.stopVersion != version) { finish(); return; }
        shownAt = SystemClock.uptimeMillis(); ready = false;
        getWindow().setTitle("TV Clock stop");
        LinearLayout page = TvUi.page(this);
        page.addView(TvUi.text(this, "TV Clock", 16, true)); TvUi.gap(page, 44);
        page.addView(TvUi.text(this, s.stopBedtime ? "Time for bed" : "Time to stop", 44, false));
        TvUi.gap(page, 12); page.addView(TvUi.text(this, LimitsController.time(System.currentTimeMillis()), 32, false));
        TvUi.gap(page, 16); page.addView(TvUi.text(this, s.stopBedtime ? "Your bedtime has arrived." : "Your timer has ended.", 20, true));
        TvUi.gap(page, 32);
        LinearLayout actions = TvUi.row(this);
        Button home = TvUi.button(this, "Go to Home", R.id.stop_home, () -> {
            LimitsController.dismiss(this, version);
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });
        TvUi.addButton(actions, home, 240);
        if (s.canExtend(System.currentTimeMillis(), SystemClock.elapsedRealtime(), LimitsController.boot(this))) {
            TvUi.addButton(actions, TvUi.button(this, "5 more minutes", R.id.stop_extend, this::confirmExtension), 240);
        }
        page.addView(actions); TvUi.gap(page, 28);
        page.addView(TvUi.text(this, "Use the power button to turn off the TV.", 16, true));
        setContentView(page); home.requestFocus();
    }
    private void confirmExtension() {
        final long confirmedVersion = version;
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Add five minutes?")
                .setMessage("This is the only extension for this limit.")
                .setNegativeButton("Keep stop time", (d, which) -> {})
                .setPositiveButton("Add 5 minutes", (d, which) -> {
                    if (LimitsController.extend(this, confirmedVersion)) finish();
                    else { Toast.makeText(this, "Another limit is due first.", Toast.LENGTH_LONG).show(); render(); }
                }).create();
        dialog.setOnShowListener(d -> TvUi.styleConfirmation(dialog));
        confirmation = dialog;
        dialog.show();
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (!ready && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            if (event.getDownTime() < shownAt) {
                if (event.getAction() == KeyEvent.ACTION_UP) ready = true;
                return true;
            }
            ready = true;
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed() { LimitsController.dismiss(this, version); super.onBackPressed(); }
    @Override protected void onStop() {
        if (confirmation != null) { confirmation.dismiss(); confirmation = null; }
        if (!isChangingConfigurations()) LimitsController.dismiss(this, version);
        super.onStop();
    }
}
