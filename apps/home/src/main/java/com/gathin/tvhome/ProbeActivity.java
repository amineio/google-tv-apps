package com.gathin.tvhome;

import android.app.Activity;
import android.os.Bundle;

/** Upstream #1121: refresh the foreground window event once at boot without opening Home blindly. */
public class ProbeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().post(() -> {
            finish();
            overridePendingTransition(0, 0);
        });
    }
}
