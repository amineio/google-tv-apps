package com.amine.tvclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.widget.TextView;

final class OutlinedClockView extends TextView {
    private final TextPaint outline = new TextPaint();

    OutlinedClockView(Context context) {
        super(context);
        int padding = (int) Math.ceil(2 * getResources().getDisplayMetrics().density);
        setPadding(padding, padding, padding, padding);
    }

    @Override protected void onDraw(Canvas canvas) {
        outline.set(getPaint());
        outline.setColor(Color.BLACK);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeJoin(Paint.Join.ROUND);
        outline.setStrokeWidth(3 * getResources().getDisplayMetrics().density);
        canvas.drawText(getText().toString(), getCompoundPaddingLeft(), getBaseline(), outline);
        super.onDraw(canvas);
    }
}
