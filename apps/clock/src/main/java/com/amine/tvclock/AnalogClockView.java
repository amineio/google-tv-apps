package com.amine.tvclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import java.util.Calendar;
import java.util.Locale;

/** A clock face driven by the TV's current time and time zone. */
final class AnalogClockView extends View {
    private static final int BLUE = Color.rgb(107, 167, 250);
    private static final int LIGHT_BLUE = Color.rgb(183, 212, 255);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Calendar time = Calendar.getInstance();
    private Runnable onTimeChanged;
    private final BroadcastReceiver changes = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { update(); }
    };

    AnalogClockView(Context context) {
        super(context);
        setTag("live-clock");
        update();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        getContext().registerReceiver(changes, filter, Context.RECEIVER_EXPORTED);
        update();
    }

    @Override protected void onDetachedFromWindow() {
        getContext().unregisterReceiver(changes);
        super.onDetachedFromWindow();
    }

    void setOnTimeChanged(Runnable callback) { onTimeChanged = callback; }

    private void update() {
        time = Calendar.getInstance();
        setContentDescription(String.format(Locale.ROOT, "Time %02d:%02d",
                time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE)));
        invalidate();
        if (onTimeChanged != null) onTimeChanged.run();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float side = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f, cy = getHeight() / 2f, radius = side * 0.46f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.BUTT);
        for (int i = 0; i < 60; i++) {
            double angle = Math.toRadians(i * 6 - 90);
            boolean major = i % 5 == 0;
            paint.setColor(major ? TvUi.TEXT : TvUi.MUTED);
            paint.setStrokeWidth(side * (major ? 0.008f : 0.003f));
            float inner = radius * (major ? 0.90f : 0.96f);
            canvas.drawLine(cx + (float) Math.cos(angle) * inner, cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(TvUi.TEXT);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
        paint.setTextSize(side * 0.074f);
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 1; i <= 12; i++) {
            double angle = Math.toRadians(i * 30 - 90);
            float x = cx + (float) Math.cos(angle) * radius * 0.79f;
            float y = cy + (float) Math.sin(angle) * radius * 0.79f;
            canvas.drawText(String.valueOf(i), x, y - (paint.ascent() + paint.descent()) / 2, paint);
        }
        float minute = time.get(Calendar.MINUTE);
        hand(canvas, cx, cy, radius * 0.53f, (time.get(Calendar.HOUR) + minute / 60f) * 30f,
                TvUi.TEXT, side * 0.026f);
        hand(canvas, cx, cy, radius * 0.72f, minute * 6f, LIGHT_BLUE, side * 0.016f);
        paint.setStyle(Paint.Style.FILL); paint.setColor(BLUE);
        canvas.drawCircle(cx, cy, side * 0.021f, paint);
        paint.setColor(LIGHT_BLUE); canvas.drawCircle(cx, cy, side * 0.014f, paint);
    }

    private void hand(Canvas canvas, float cx, float cy, float length, float degrees, int color, float width) {
        double angle = Math.toRadians(degrees - 90);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.BUTT); paint.setColor(color); paint.setStrokeWidth(width);
        canvas.drawLine(cx, cy, cx + (float) Math.cos(angle) * length,
                cy + (float) Math.sin(angle) * length, paint);
    }
}
