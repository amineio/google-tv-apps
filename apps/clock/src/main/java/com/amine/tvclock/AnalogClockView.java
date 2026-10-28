package com.amine.tvclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import java.util.Calendar;
import java.util.Locale;

/** A clock face driven by the TV's current time and time zone. */
final class AnalogClockView extends View {
    private static final int BLUE = Color.rgb(107, 167, 250);
    private static final int LIGHT_BLUE = Color.rgb(183, 212, 255);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rim = new RectF();
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
        float cx = getWidth() / 2f, cy = getHeight() / 2f, radius = side * 0.43f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(BLUE);
        paint.setStrokeWidth(side * 0.035f);
        rim.set(cx - radius, cy - radius, cx + radius, cy + radius);
        for (int i = 0; i < 4; i++) canvas.drawArc(rim, i * 90f - 87f, 84f, false, paint);
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30 - 90);
            boolean major = i % 3 == 0;
            paint.setColor(major ? TvUi.TEXT : LIGHT_BLUE);
            paint.setStrokeWidth(side * (major ? 0.022f : 0.012f));
            float inner = radius * (major ? 0.70f : 0.79f), outer = radius * 0.88f;
            canvas.drawLine(cx + (float) Math.cos(angle) * inner, cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * outer, cy + (float) Math.sin(angle) * outer, paint);
        }
        float minute = time.get(Calendar.MINUTE);
        hand(canvas, cx, cy, radius * 0.53f, (time.get(Calendar.HOUR) + minute / 60f) * 30f,
                TvUi.TEXT, side * 0.042f);
        hand(canvas, cx, cy, radius * 0.72f, minute * 6f, LIGHT_BLUE, side * 0.027f);
        paint.setStyle(Paint.Style.FILL); paint.setColor(BLUE);
        canvas.drawCircle(cx, cy, side * 0.037f, paint);
        paint.setColor(LIGHT_BLUE); canvas.drawCircle(cx, cy, side * 0.018f, paint);
    }

    private void hand(Canvas canvas, float cx, float cy, float length, float degrees, int color, float width) {
        double angle = Math.toRadians(degrees - 90);
        paint.setStyle(Paint.Style.STROKE); paint.setColor(color); paint.setStrokeWidth(width);
        canvas.drawLine(cx, cy, cx + (float) Math.cos(angle) * length,
                cy + (float) Math.sin(angle) * length, paint);
    }
}
