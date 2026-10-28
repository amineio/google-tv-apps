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
import android.widget.ScrollView;
import android.widget.Switch;

final class TvUi {
    static final int BACKGROUND = Color.rgb(32, 36, 43), TEXT = Color.rgb(237, 239, 243);
    static final int MUTED = Color.rgb(177, 184, 196), ACCENT = Color.rgb(183, 212, 255);
    static final int FOCUSED_DETAIL = Color.rgb(75, 82, 94);
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

    static StateListDrawable preferenceBackground(Context c) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, surface(c, TEXT, 0));
        states.addState(new int[]{android.R.attr.state_pressed}, surface(c, ACCENT, 0));
        states.addState(new int[]{}, surface(c, Color.TRANSPARENT, 0));
        return states;
    }

    @android.annotation.SuppressLint("ViewConstructor")
    static final class SettingsPage extends LinearLayout {
        final LinearLayout list;
        final TextView summary;
        final AnalogClockView clock;
        SettingsPage(Context c, String heading, String description) {
            super(c);
            setBackgroundColor(BACKGROUND);
            setPadding(dp(c, 48), dp(c, 28), dp(c, 48), dp(c, 28));
            LinearLayout brand = column(c);
            brand.setPadding(0, dp(c, 36), 0, 0);
            brand.setGravity(Gravity.CENTER_HORIZONTAL);
            clock = new AnalogClockView(c);
            brand.addView(clock, new LinearLayout.LayoutParams(dp(c, 160), dp(c, 160)));
            gap(brand, 24);
            brand.addView(text(c, "TV Clock", 32, false));
            gap(brand, 12);
            summary = text(c, description, 16, true);
            summary.setGravity(Gravity.CENTER);
            summary.setLineSpacing(dp(c, 4), 1);
            brand.addView(summary);
            LinearLayout.LayoutParams brandLayout = new LinearLayout.LayoutParams(dp(c, 272), -1);
            brandLayout.setMarginEnd(dp(c, 40));
            addView(brand, brandLayout);
            LinearLayout settings = column(c);
            TextView title = text(c, heading, 22, true);
            title.setPadding(dp(c, 16), dp(c, 8), 0, dp(c, 16));
            settings.addView(title);
            ScrollView scroll = new ScrollView(c);
            scroll.setVerticalScrollBarEnabled(true);
            scroll.setClipToPadding(false);
            list = column(c);
            scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
            settings.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            addView(settings, new LinearLayout.LayoutParams(0, -1, 1));
        }
    }

    static Switch switchRow(Context c, String title, String detail, int icon, int id) {
        Switch view = new Switch(c);
        view.setId(id);
        view.setTextSize(20);
        view.setShowText(false);
        view.setSwitchPadding(dp(c, 20));
        view.setMinimumHeight(dp(c, 64));
        view.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12));
        view.setBackground(preferenceBackground(c));
        view.setTextColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                new int[]{BACKGROUND, TEXT}));
        android.graphics.drawable.Drawable glyph = c.getDrawable(icon).mutate();
        glyph.setBounds(0, 0, dp(c, 24), dp(c, 24));
        glyph.setTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
                new int[]{BACKGROUND, TEXT}));
        view.setCompoundDrawablesRelative(glyph, null, null, null);
        view.setCompoundDrawablePadding(dp(c, 16));
        view.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},
                new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{BACKGROUND, TEXT, MUTED}));
        view.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{ACCENT, MUTED}));
        switchLabel(view, title, detail);
        return view;
    }

    static void switchLabel(Switch view, String title, String detail) {
        if (detail.isEmpty()) {
            view.setText(title);
            view.setOnFocusChangeListener(null);
            return;
        }
        android.text.SpannableString text = new android.text.SpannableString(title + "\n" + detail);
        text.setSpan(new android.text.style.AbsoluteSizeSpan(14, true), title.length() + 1, text.length(), 0);
        text.setSpan(new android.text.style.ForegroundColorSpan(view.isFocused() ? FOCUSED_DETAIL : MUTED),
                title.length() + 1, text.length(), 0);
        view.setText(text);
        view.setOnFocusChangeListener((v, focused) -> switchLabel(view, title, detail));
    }

    static void choiceDialog(android.app.Activity activity, String title, String[] choices, int selected,
            View returnFocus, java.util.function.IntConsumer choose) {
        ColorStateList colors = new ColorStateList(new int[][]{
                new int[]{android.R.attr.state_selected}, new int[]{android.R.attr.state_focused}, new int[]{}},
                new int[]{BACKGROUND, BACKGROUND, TEXT});
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(activity,
                android.R.layout.simple_list_item_single_choice, choices) {
            @Override public View getView(int position, View convert, android.view.ViewGroup parent) {
                android.widget.CheckedTextView row = (android.widget.CheckedTextView) super.getView(position, convert, parent);
                row.setTextSize(20); row.setTextColor(colors); row.setCheckMarkTintList(colors);
                row.setMinimumHeight(dp(activity, 60));
                row.setPadding(dp(activity, 24), dp(activity, 12), dp(activity, 24), dp(activity, 12));
                return row;
            }
        };
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(activity).setTitle(title)
                .setSingleChoiceItems(adapter, selected, (d, which) -> { d.dismiss(); choose.accept(which); })
                .setNegativeButton("Cancel", (d, which) -> {}).create();
        dialog.setOnDismissListener(d -> returnFocus.requestFocus());
        dialog.setOnShowListener(d -> {
            dialog.getWindow().setBackgroundDrawable(surface(activity, BACKGROUND, 0));
            dialog.getWindow().setLayout(dp(activity, 560), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            styleButton(activity, dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE));
            dialog.getListView().setSelector(surface(activity, TEXT, 0));
            dialog.getListView().setSelection(selected);
            dialog.getListView().requestFocus();
        });
        dialog.show();
    }
}
