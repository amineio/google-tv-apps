package com.amine.tvclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class LimitsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean timeChanged = Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action);
        LimitsController.restore(context, timeChanged);
    }
}
