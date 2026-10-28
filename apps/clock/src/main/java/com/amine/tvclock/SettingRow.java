package com.amine.tvclock;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One remote focus target for a setting and its current value. */
@android.annotation.SuppressLint("ViewConstructor")
final class SettingRow extends LinearLayout {
    private final TextView title, detail, value;
    private final ImageView icon;
    private Runnable less, more;

    SettingRow(Context context, String label, String description, int iconId, int id, Runnable action) {
        super(context);
        setId(id);
        setGravity(Gravity.CENTER_VERTICAL);
        setFocusable(true);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setMinimumHeight(TvUi.dp(context, 64));
        setPadding(TvUi.dp(context, 16), TvUi.dp(context, 12), TvUi.dp(context, 16), TvUi.dp(context, 12));
        setBackground(TvUi.preferenceBackground(context));
        icon = new ImageView(context);
        Drawable drawable = context.getDrawable(iconId).mutate();
        icon.setImageDrawable(drawable);
        LayoutParams iconLayout = new LayoutParams(TvUi.dp(context, 24), TvUi.dp(context, 24));
        iconLayout.setMarginEnd(TvUi.dp(context, 16));
        addView(icon, iconLayout);
        LinearLayout labels = TvUi.column(context);
        title = TvUi.text(context, label, 20, false);
        detail = TvUi.text(context, description, 14, true);
        labels.addView(title);
        LayoutParams descriptionLayout = new LayoutParams(-1, -2);
        descriptionLayout.topMargin = TvUi.dp(context, 4);
        labels.addView(detail, descriptionLayout);
        addView(labels, new LayoutParams(0, -2, 1));
        value = TvUi.text(context, "", 18, false);
        value.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        LayoutParams valueLayout = new LayoutParams(-2, -2);
        valueLayout.setMarginStart(TvUi.dp(context, 16));
        addView(value, valueLayout);
        for (android.view.View child : new android.view.View[]{icon, title, detail, value}) {
            child.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        setOnFocusChangeListener((v, focused) -> colors());
        if (action != null) setOnClickListener(v -> action.run());
        setDetail(description);
        colors();
    }

    void setTitle(String label) { title.setText(label); description(); }
    void setDetail(String text) {
        detail.setText(text);
        detail.setVisibility(text.isEmpty() ? GONE : VISIBLE);
        description();
    }
    void setValue(String text) {
        value.setText(less == null ? text : "‹   " + text + "   ›");
        value.setVisibility(text.isEmpty() ? GONE : VISIBLE);
        String spoken = text.replace("›", "").trim();
        setStateDescription(spoken.isEmpty() ? null : spoken);
    }
    void setValueId(int id) { value.setId(id); }
    void setAdjustment(Runnable decrease, Runnable increase) { less = decrease; more = increase; }
    private void description() {
        setContentDescription(title.getText() + (detail.getText().length() == 0 ? "" : ". " + detail.getText()));
    }
    private void colors() {
        int foreground = isFocused() ? TvUi.BACKGROUND : TvUi.TEXT;
        title.setTextColor(foreground);
        value.setTextColor(foreground);
        detail.setTextColor(isFocused() ? TvUi.FOCUSED_DETAIL : TvUi.MUTED);
        icon.setColorFilter(foreground);
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (less != null && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? less : more).run();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (less != null && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) return true;
        return super.onKeyUp(keyCode, event);
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(less == null ? "android.widget.Button" : "android.widget.SeekBar");
        if (less != null) {
            info.setScrollable(true);
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        }
    }
    @Override public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (less != null && action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) { less.run(); return true; }
        if (more != null && action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) { more.run(); return true; }
        return super.performAccessibilityAction(action, arguments);
    }
}
