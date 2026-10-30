package com.amine.tvclock;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Native dashboard geometry; state and actions remain in MainActivity. */
final class ClockDashboard extends LinearLayout {
    final AnalogClockView clock;
    final TextView summary, timerValue, timerDetail, bedtimeValue, bedtimeDetail;
    final LinearLayout timerActions, bedtimeActions, bedtimeHeading, bottom, permissions;
    private final TextView digital, date;

    ClockDashboard(Context c) {
        super(c);
        setOrientation(VERTICAL);
        setBackgroundColor(TvUi.BACKGROUND);
        setPadding(TvUi.dp(c, 24), TvUi.dp(c, 14), TvUi.dp(c, 24), TvUi.dp(c, 14));
        LinearLayout heading = TvUi.row(c);
        heading.addView(TvUi.text(c, "Clock", 26, false), new LayoutParams(0, -2, 1));
        summary = TvUi.text(c, "", 14, true);
        summary.setGravity(Gravity.END);
        heading.addView(summary, new LayoutParams(0, -2, 2));
        addView(heading); TvUi.gap(this, 10);
        LinearLayout content = TvUi.row(c);
        content.setGravity(Gravity.FILL_VERTICAL);
        LinearLayout face = card(c);
        face.setGravity(Gravity.CENTER_HORIZONTAL);
        clock = new AnalogClockView(c);
        face.addView(clock, new LayoutParams(-1, 0, 1));
        digital = TvUi.text(c, "", 62, false);
        digital.setTag("dashboard-time");
        digital.setGravity(Gravity.CENTER);
        face.addView(digital, new LayoutParams(-1, -2));
        date = TvUi.text(c, "", 16, true);
        date.setGravity(Gravity.CENTER);
        face.addView(date, new LayoutParams(-1, -2));
        LayoutParams faceParams = new LayoutParams(0, -1, 0.46f);
        faceParams.setMarginEnd(TvUi.dp(c, 12));
        content.addView(face, faceParams);
        LinearLayout right = TvUi.column(c);
        LinearLayout timer = card(c);
        timer.addView(TvUi.text(c, "Session timer", 20, false));
        timerValue = TvUi.text(c, "", 54, false);
        timerValue.setGravity(Gravity.CENTER_VERTICAL);
        timerValue.setSingleLine(true);
        timerValue.setAutoSizeTextTypeUniformWithConfiguration(24, 54, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        timer.addView(timerValue, new LayoutParams(-1, 0, 1));
        timerDetail = TvUi.text(c, "", 13, true);
        timerDetail.setSingleLine(true);
        timerDetail.setEllipsize(android.text.TextUtils.TruncateAt.END);
        timer.addView(timerDetail);
        TvUi.gap(timer, 8);
        timerActions = TvUi.row(c); timer.addView(timerActions);
        LayoutParams timerParams = new LayoutParams(-1, 0, 1);
        timerParams.bottomMargin = TvUi.dp(c, 10);
        right.addView(timer, timerParams);
        LinearLayout bedtime = card(c);
        bedtimeHeading = TvUi.row(c);
        bedtimeHeading.addView(TvUi.text(c, "Bedtime", 20, false), new LayoutParams(0, -2, 1));
        bedtime.addView(bedtimeHeading);
        bedtimeValue = TvUi.text(c, "", 50, false);
        bedtimeValue.setGravity(Gravity.CENTER_VERTICAL);
        bedtimeValue.setSingleLine(true);
        bedtimeValue.setAutoSizeTextTypeUniformWithConfiguration(24, 50, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        bedtime.addView(bedtimeValue, new LayoutParams(-1, 0, 1));
        bedtimeDetail = TvUi.text(c, "Daily bedtime", 13, true);
        bedtimeDetail.setSingleLine(true);
        bedtime.addView(bedtimeDetail);
        TvUi.gap(bedtime, 8);
        bedtimeActions = TvUi.row(c); bedtime.addView(bedtimeActions);
        right.addView(bedtime, new LayoutParams(-1, 0, 1));
        content.addView(right, new LayoutParams(0, -1, 0.54f));
        addView(content, new LayoutParams(-1, 0, 1));
        TvUi.gap(this, 10);
        bottom = TvUi.row(c); addView(bottom, new LayoutParams(-1, TvUi.dp(c, 48)));
        permissions = TvUi.column(c); addView(permissions);
        updateTime();
    }

    private LinearLayout card(Context c) {
        LinearLayout card = TvUi.column(c);
        card.setPadding(TvUi.dp(c, 20), TvUi.dp(c, 14), TvUi.dp(c, 20), TvUi.dp(c, 14));
        card.setBackground(TvUi.surface(c, TvUi.CARD, TvUi.BORDER));
        return card;
    }

    void addAction(LinearLayout row, SettingRow action) {
        action.dashboardAction();
        LayoutParams params = new LayoutParams(0, -1, 1);
        if (row.getChildCount() > 0) params.setMarginStart(TvUi.dp(getContext(), 10));
        row.addView(action, params);
        row.getLayoutParams().height = TvUi.dp(getContext(), 46);
    }

    void updateTime() {
        ZonedDateTime now = ZonedDateTime.now();
        digital.setText(now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())));
        date.setText(now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())));
    }
}
