package com.amine.tvclock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.LinearLayout;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ClockService extends Service {
    static final String STATE_CHANGED = "com.amine.tvclock.STATE_CHANGED";
    private static final String STOP = "com.amine.tvclock.STOP";
    private static final String CHANNEL = "clock";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private WindowManager windows;
    private TextView clock;
    private boolean receiverRegistered;
    private LinearLayout warning;
    private final Runnable hideWarning = () -> {
        if (warning != null) windows.removeView(warning);
        warning = null;
        if (clock == null && !LimitsController.read(this).active()) stopSelf();
    };

    static void sync(Context context) {
        context.startForegroundService(new Intent(context, ClockService.class));
    }

    static void warn(Context context, String title, String detail) {
        context.startForegroundService(new Intent(context, ClockService.class)
                .putExtra("warningTitle", title).putExtra("warningDetail", detail));
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (clock == null) return;
            clock.setText(format.format(ZonedDateTime.now()));
            if (getSystemService(PowerManager.class).isInteractive()) {
                handler.postDelayed(this, 60_000 - Math.floorMod(System.currentTimeMillis(), 60_000));
            }
        }
    };
    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            handler.removeCallbacks(tick);
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) LimitsController.screenOn(ClockService.this);
            if (Intent.ACTION_TIME_CHANGED.equals(intent.getAction()) || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                LimitsController.restore(ClockService.this, true);
            }
            if (!Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) tick.run();
        }
    };

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) {
            ClockPreferences.setEnabled(this, false);
        }
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Clock", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, ClockService.class).setAction(STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.clock_icon).setContentTitle("TV Clock")
                .setContentText(LimitsController.read(this).active() ? "Time limits are active." : "The clock is on.").setContentIntent(open)
                .setOngoing(true).setShowWhen(false)
                .addAction(new Notification.Action.Builder(null, "Hide clock", stop).build()).build();
        startForeground(1, notification);
        if (!Settings.canDrawOverlays(this)) {
            ClockPreferences.setEnabled(this, false);
            stopSelf();
            notifyState();
            return START_NOT_STICKY;
        }
        windows = getSystemService(WindowManager.class);
        if (!receiverRegistered) registerTimeReceiver();
        if (!ClockPreferences.isEnabled(this) && clock != null) {
            windows.removeView(clock); clock = null;
        }
        if (ClockPreferences.isEnabled(this) && clock == null) {
            try {
                showClock();
            } catch (RuntimeException e) {
                ClockPreferences.setEnabled(this, false);
                stopSelf();
                notifyState();
                return START_NOT_STICKY;
            }
        }
        if (clock != null) updateLayout();
        if (intent != null && intent.hasExtra("warningTitle")) {
            showWarning(intent.getStringExtra("warningTitle"), intent.getStringExtra("warningDetail"));
        }
        if (LimitsController.read(this).stopPending && warning != null) {
            handler.removeCallbacks(hideWarning);
            windows.removeView(warning); warning = null;
        }
        handler.removeCallbacks(tick);
        tick.run();
        notifyState();
        if (clock == null && warning == null && !LimitsController.read(this).active()) {
            stopSelf(); return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    // The requested position is the physical top-right corner in every locale.
    @android.annotation.SuppressLint("RtlHardcoded")
    private void showClock() {
        windows = getSystemService(WindowManager.class);
        TextView view = new OutlinedClockView(this);
        view.setTextColor(Color.WHITE);
        view.setTextSize(ClockPreferences.size(this));
        view.setFontFeatureSettings("tnum");
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        WindowManager.LayoutParams position = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        position.gravity = Gravity.TOP | Gravity.RIGHT;
        float density = getResources().getDisplayMetrics().density;
        position.x = Math.round(24 * density);
        position.y = Math.round(18 * density);
        position.setTitle("TV Clock overlay");
        windows.addView(view, position);
        clock = view;
    }

    private void registerTimeReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(timeReceiver, filter, RECEIVER_NOT_EXPORTED);
        receiverRegistered = true;
    }

    private void showWarning(String title, String detail) {
        handler.removeCallbacks(hideWarning);
        if (warning != null) windows.removeView(warning);
        warning = TvUi.column(this);
        warning.setPadding(TvUi.dp(this, 20), TvUi.dp(this, 16), TvUi.dp(this, 20), TvUi.dp(this, 16));
        warning.setBackground(TvUi.surface(this, TvUi.BACKGROUND, 0));
        warning.addView(TvUi.text(this, title, 20, false));
        TvUi.gap(warning, 8);
        warning.addView(TvUi.text(this, detail, 16, true));
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(TvUi.dp(this, 330),
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.END; p.x = TvUi.dp(this, 24); p.y = TvUi.dp(this, 24);
        if (clock != null && ClockPreferences.position(this) <= 5) {
            p.y = ((WindowManager.LayoutParams) clock.getLayoutParams()).y + clock.getMeasuredHeight() + TvUi.dp(this, 12);
        }
        p.setTitle("TV Clock warning");
        windows.addView(warning, p);
        handler.postDelayed(hideWarning, 8_000);
    }

    private void updateLayout() {
        clock.setTextSize(ClockPreferences.size(this));
        clock.setText(format.format(ZonedDateTime.now()));
        clock.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        WindowManager.LayoutParams position = (WindowManager.LayoutParams) clock.getLayoutParams();
        int height = windows.getMaximumWindowMetrics().getBounds().height();
        int inset = Math.round(18 * getResources().getDisplayMetrics().density);
        position.y = ClockLayout.verticalOffset(height, clock.getMeasuredHeight(), inset,
                ClockPreferences.position(this));
        windows.updateViewLayout(clock, position);
    }

    private void notifyState() {
        sendBroadcast(new Intent(STATE_CHANGED).setPackage(getPackageName()));
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (receiverRegistered) unregisterReceiver(timeReceiver);
        if (clock != null) windows.removeView(clock);
        if (warning != null) windows.removeView(warning);
        clock = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
