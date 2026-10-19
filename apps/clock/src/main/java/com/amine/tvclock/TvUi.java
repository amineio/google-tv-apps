package com.amine.tvclock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class TvUi {
    static final int BACKGROUND = Color.rgb(13, 17, 23), TEXT = Color.rgb(241, 245, 249);
    static final int MUTED = Color.rgb(171, 182, 196), ACCENT = Color.rgb(166, 205, 255);
    static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    static LinearLayout page(Context c) {
        LinearLayout page = column(c); page.setBackgroundColor(BACKGROUND);
        page.setPadding(dp(c, 48), dp(c, 28), dp(c, 48), dp(c, 28));
        page.setGravity(Gravity.CENTER_VERTICAL); return page;
    }
    static LinearLayout column(Context c) {
        LinearLayout view = new LinearLayout(c); view.setOrientation(LinearLayout.VERTICAL); return view;
    }
    static LinearLayout row(Context c) {
        LinearLayout view = new LinearLayout(c); view.setGravity(Gravity.CENTER_VERTICAL); return view;
    }
    static TextView text(Context c, String text, int size, boolean muted) {
        TextView view = new TextView(c); view.setText(text); view.setTextSize(size);
        view.setTextColor(muted ? MUTED : TEXT); view.setIncludeFontPadding(false);
        return view;
    }
    static GradientDrawable surface(Context c, int color, int border) {
        GradientDrawable shape = new GradientDrawable(); shape.setColor(color);
        shape.setCornerRadius(dp(c, 12)); if (border != 0) shape.setStroke(dp(c, 2), border);
        return shape;
    }
    static Button button(Context c, String label, int id, Runnable click) {
        Button button = new Button(c); button.setId(id); button.setText(label);
        styleButton(c, button);
        button.setOnClickListener(v -> click.run()); return button;
    }
    static void styleButton(Context c, Button button) {
        button.setTextSize(17);
        button.setAllCaps(false); button.setMinWidth(0); button.setMinimumWidth(0);
        button.setMinHeight(0); button.setMinimumHeight(0);
        button.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8));
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused}, surface(c, TEXT, ACCENT));
        background.addState(new int[]{android.R.attr.state_pressed}, surface(c, ACCENT, 0));
        background.addState(new int[]{android.R.attr.state_selected}, surface(c, Color.rgb(38, 61, 85), 0));
        background.addState(new int[]{}, surface(c, Color.rgb(30, 38, 49), 0));
        button.setBackground(background);
        button.setTextColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                new int[]{BACKGROUND, TEXT}));
    }
    static void styleConfirmation(android.app.AlertDialog dialog) {
        Context c = dialog.getContext();
        dialog.getWindow().setBackgroundDrawable(surface(c, Color.rgb(30, 38, 49), 0));
        for (int which : new int[]{android.app.AlertDialog.BUTTON_NEGATIVE, android.app.AlertDialog.BUTTON_POSITIVE}) {
            Button button = dialog.getButton(which);
            styleButton(c, button); button.setMinimumHeight(dp(c, 48));
        }
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }
    static void addButton(LinearLayout row, Button button, int width) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(row.getContext(), width), dp(row.getContext(), 48));
        params.setMarginEnd(dp(row.getContext(), 10)); row.addView(button, params);
    }
    static void gap(LinearLayout page, int height) {
        View space = new View(page.getContext()); page.addView(space, new LinearLayout.LayoutParams(1, dp(page.getContext(), height)));
    }
}
