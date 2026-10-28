package com.amine.chessmath;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Nonfocusable score and thinking feedback beside the board. */
@android.annotation.SuppressLint("ViewConstructor") // Created directly with an explicit player side, never inflated from XML.
final class ChessScoreView extends LinearLayout {
    private final TextView points,delta,status,help;
    private final ProgressBar progress;
    private final boolean player;
    private final Runnable hideFeedback=this::clearFeedback;

    ChessScoreView(Context context,boolean player){
        super(context);this.player=player;
        setOrientation(VERTICAL);setGravity(Gravity.CENTER_HORIZONTAL);
        setFocusable(false);setTag(player?"player-score":"computer-score");
        setPadding(dp(4),0,dp(4),0);
        TextView label=text(context.getString(player?R.string.score_you:R.string.score_computer),18,true);
        label.setMinLines(2);label.setGravity(Gravity.CENTER);addView(label,params());
        points=text("",46,true);points.setTag(player?"player-points":"computer-points");
        addView(points,params());
        TextView unit=text(context.getString(R.string.score_points),14,false);
        unit.setTextColor(TvStyle.MUTED);addView(unit,params());
        delta=text("",20,true);delta.setTag(player?"player-delta":"computer-delta");
        delta.setMinLines(1);delta.setVisibility(INVISIBLE);
        LayoutParams change=params();change.topMargin=dp(8);addView(delta,change);
        status=text("",16,true);status.setMinLines(2);
        if(player)status.setTag("thinking-status");
        LayoutParams state=params();state.topMargin=dp(12);addView(status,state);
        help=text(context.getString(R.string.thinking_prompt),14,false);
        help.setTextColor(TvStyle.MUTED);help.setMinLines(2);
        if(player)help.setTag("thinking-help");addView(help,params());
        progress=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(false);progress.setMax(60000);
        progress.setProgressTintList(ColorStateList.valueOf(TvStyle.AMBER));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xff3e6046));
        if(player)progress.setTag("thinking-progress");
        LayoutParams line=new LayoutParams(-1,dp(4));line.topMargin=dp(8);line.leftMargin=dp(6);line.rightMargin=dp(6);
        addView(progress,line);
        updateThinking(0,0,"");
    }
    private int dp(int value){return TvStyle.dp(getContext(),value);}
    private LayoutParams params(){return new LayoutParams(-1,-2);}
    private TextView text(String value,int size,boolean bold){
        TextView view=TvStyle.text(getContext(),value,size,bold);
        view.setGravity(Gravity.CENTER);view.setFocusable(false);return view;
    }
    void setPoints(int value){
        String label=Integer.toString(value);
        if(!label.contentEquals(points.getText()))points.setText(label);
    }
    void updateThinking(long remaining,long total,String turn){
        boolean waiting=remaining>0;
        String label=waiting?getContext().getString(R.string.thinking_countdown,(remaining+999)/1000):turn;
        if(!label.contentEquals(status.getText()))status.setText(label);
        status.setTextColor(waiting?TvStyle.AMBER:TvStyle.MUTED);
        status.setVisibility(player?VISIBLE:INVISIBLE);
        // Reserve the same space on both sides to keep the score totals aligned.
        help.setVisibility(player&&waiting?VISIBLE:INVISIBLE);
        progress.setVisibility(player&&waiting?VISIBLE:INVISIBLE);
        progress.setMax((int)Math.max(1,total));progress.setProgress((int)remaining);
    }
    void feedback(int change,boolean motion){
        if(change==0)return;
        clearFeedback();
        delta.setText(getContext().getString(change>0?R.string.score_gain:R.string.score_loss,Math.abs(change)));
        delta.setTextColor(change>0?0xff9fdbad:0xffffa394);
        delta.setVisibility(VISIBLE);
        if(motion&&android.animation.ValueAnimator.areAnimatorsEnabled()){
            delta.animate().translationY(dp(change>0?-12:12)).alpha(0)
                    .setStartDelay(250).setDuration(1000).withEndAction(hideFeedback).start();
        }else postDelayed(hideFeedback,1250);
    }
    void clearFeedback(){
        removeCallbacks(hideFeedback);delta.animate().cancel();
        delta.setVisibility(INVISIBLE);delta.setText("");delta.setAlpha(1);delta.setTranslationY(0);
    }
    @Override protected void onDetachedFromWindow(){clearFeedback();super.onDetachedFromWindow();}
}
