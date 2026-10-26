package com.amine.chessmath;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
/** Remote-only TV shell. All game mutations go through SessionController. */
public final class MainActivity extends Activity {
    private static final Handler MAIN_HANDLER=new Handler(Looper.getMainLooper());
    private static SessionController sharedController;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SessionController controller;
    private LinearLayout content;
    private boolean started, alive, foreground;
    private String menu="", screen="";
    private ChessBoardView board;
    private TextView chessHeading,chessHelp,chessDetail,savingText;
    private MathConfig draft;
    private Button parentButton;
    private ProgressBar holdProgress;
    private long holdStart;
    private int lastPad=4;
    private final int[] settingsMemory={
        -1,-1,-1,-1,-1,-1
    }
    ;
    private final Runnable tick=new Runnable(){
        public void run(){
            if(alive&&controller!=null){
                controller.tick();
                updateSaving();
                handler.postDelayed(this,250);
            }
        }
    }
    ;
    private final Runnable hold=new Runnable(){
        public void run(){
            if(holdStart==0||parentButton==null||!parentButton.hasFocus())return;
            long duration=SystemClock.elapsedRealtime()-holdStart;
            holdProgress.setProgress((int)Math.min(100,duration/30));
            if(duration>=3000){
                holdStart=0;
                menu="parents";
                render();
            }
            else handler.postDelayed(this,40);
        }
    }
    ;
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if(sharedController==null){
            sharedController=new SessionController(
                    new SessionStore(getApplicationContext().getFilesDir()),
                    SystemClock::elapsedRealtime,MAIN_HANDLER::post,null);
        }
        controller=sharedController;
        controller.setListener(this::render);
        alive=true;
        if(controller.busy()){shell();title(R.string.app_name);body(s(R.string.saving));}
        render();
        handler.post(tick);
    }
    @Override protected void onResume(){
        super.onResume();
        foreground=true;
        if(controller!=null)controller.foreground(hasWindowFocus());
    }
    @Override protected void onPause(){
        foreground=false;
        cancelHold();
        if(controller!=null)controller.foreground(false);
        super.onPause();
    }
    @Override public void onWindowFocusChanged(boolean focus){
        super.onWindowFocusChanged(focus);
        if(controller!=null)controller.foreground(focus&&foreground);
        if(!focus)cancelHold();
    }
    @Override protected void onDestroy(){
        alive=false;
        handler.removeCallbacksAndMessages(null);
        if(controller!=null)controller.setListener(null);
        super.onDestroy();
    }
    private String s(int id){
        return getString(id);
    }
    private int dp(float n){
        return TvStyle.dp(this,n);
    }
    private boolean hasError(){
        return controller.error()!=null&&!controller.error().isEmpty();
    }
    private void act(Runnable action){
        if(!controller.busy()){
            action.run();
        }
    }
    private void render(){
        if(controller==null||isFinishing())return;
        Session state=controller.state();
        if(controller.busy()){updateSaving();return;}
        if(hasError()){
            shell();
            screen="error";
            title(R.string.error_title);
            body(controller.corruptSave()?s(R.string.load_error):controller.error());
            if(!controller.corruptSave())action(R.string.retry,()->controller.retrySave());
            action(R.string.reset,()->confirm(true));
            action(R.string.exit,this::finish);
            focusFirst();
            return;
        }
        String target=!started?"start":!menu.isEmpty()?menu:(state.phase==Session.Phase.TO_MATH||state.phase==Session.Phase.TO_CHESS)?state.phase.name():state.paused?"pause":state.phase.name();
        if(target.equals("CHESS")&&screen.equals("CHESS")&&state.promotionTo<0&&state.intendedMove==null&&state.result.isEmpty()&&board!=null){
            bindBoard();
            return;
        }
        Object focusTag=getCurrentFocus()==null?null:getCurrentFocus().getTag();
        shell();
        screen=target;
        switch(target){
            case "start":start();
            break;
            case "pause":pause();
            break;
            case "parents":parents();
            break;
            case "settings":settings();
            break;
            case "help":parentHelp();
            break;
            case "licenses":licenses();
            break;
            case "CHESS":chess();
            break;
            case "MATH":math();
            break;
            case "TO_MATH":handoff(true);
            break;
            case "TO_CHESS":handoff(false);
            break;
            default:start();
        }
        if(focusTag!=null){
            View restore=content.findViewWithTag(focusTag);
            if(restore!=null&&restore.isFocusable()){
                restore.requestFocus();
                return;
            }
        }
        if(content.findFocus()==null)focusFirst();
    }
    private void updateSaving(){
        if(savingText!=null){
            savingText.setText(hasError()?controller.error():s(R.string.saving));
            savingText.setVisibility(controller.busy()?View.VISIBLE:View.GONE);
        }
    }
    private void shell(){
        board=null;
        parentButton=null;
        holdProgress=null;
        FrameLayout frame=new FrameLayout(this);
        frame.addView(new TvStyle.Backdrop(this),new FrameLayout.LayoutParams(-1,-1));
        content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(42),dp(28),dp(42),dp(24));
        frame.addView(content,new FrameLayout.LayoutParams(-1,-1));
        savingText=TvStyle.text(this,s(R.string.saving),15,false);
        savingText.setBackgroundColor(TvStyle.FOREST);
        savingText.setPadding(dp(12),dp(6),dp(12),dp(6));
        savingText.setVisibility(View.GONE);
        FrameLayout.LayoutParams status=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.END);
        status.setMargins(0,0,dp(30),dp(14));
        frame.addView(savingText,status);
        setContentView(frame);
        TextView brand=TvStyle.text(this,s(R.string.brand),12,true);
        brand.setTextColor(TvStyle.MUTED);
        content.addView(brand);
        space(content,14);
    }
    private TextView title(int resource){
        TextView text=TvStyle.text(this,s(resource),38,true);
        content.addView(text);
        space(content,10);
        return text;
    }
    private TextView body(String value){
        TextView text=TvStyle.text(this,value,20,false);
        text.setTextColor(TvStyle.MUTED);
        text.setLineSpacing(dp(5),1);
        content.addView(text);
        space(content,14);
        return text;
    }
    private void space(LinearLayout parent,int size){
        View v=new View(this);
        parent.addView(v,new LinearLayout.LayoutParams(1,dp(size)));
    }
    private Button button(String label,String tag,Runnable action){
        Button b=TvStyle.button(this,label);
        b.setTag(tag);
        b.setOnClickListener(v->act(action));
        return b;
    }
    private Button action(int label,Runnable action){
        Button b=button(s(label),s(label),action);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(350),dp(49));
        p.bottomMargin=dp(6);
        content.addView(b,p);
        return b;
    }
    private void focusFirst(){
        for(View v:focusables(content))if(v.isFocusable()){
            v.requestFocus();
            return;
        }
    }
    private List<View> focusables(ViewGroup group){
        List<View> result=new ArrayList<>();
        for(int i=0;i<group.getChildCount();i++){
            View v=group.getChildAt(i);
            if(v instanceof ViewGroup)result.addAll(focusables((ViewGroup)v));
            else if(v.isFocusable())result.add(v);
        }
        return result;
    }
    private void verticalClamp(ViewGroup parent){
        List<View> views=focusables(parent);
        for(int i=0;i<views.size();i++){
            View v=views.get(i);
            v.setNextFocusUpId(views.get(Math.max(0,i-1)).getId());
            v.setNextFocusDownId(views.get(Math.min(views.size()-1,i+1)).getId());
            v.setNextFocusLeftId(v.getId());
            v.setNextFocusRightId(v.getId());
        }
    }
    private void start(){
        title(R.string.app_name);
        body(s(R.string.intro));
        space(content,14);
        action(controller.state().revision==0?R.string.start:R.string.continue_game,()->{
            started=true;menu="";controller.resume();render();
        }
        );
        action(R.string.exit,this::finish);
        verticalClamp(content);
    }
    private void pause(){
        title(R.string.pause);
        action(R.string.resume,()->{
            menu="";controller.resume();render();
        }
        );
        if(controller.state().phase==Session.Phase.CHESS&&controller.game().claimableDraw())action(R.string.claim_draw,()->controller.claimDraw());
        parentButton=action(R.string.parents,()->{
        }
        );
        parentButton.setOnKeyListener((v,key,event)->{
            if(key!=KeyEvent.KEYCODE_DPAD_CENTER&&key!=KeyEvent.KEYCODE_ENTER)return false;if(event.getAction()==KeyEvent.ACTION_DOWN){
                if(event.getRepeatCount()==0){
                    holdStart=SystemClock.elapsedRealtime();handler.post(hold);
                }
                return true;
            }
            cancelHold();return true;
        }
        );
        parentButton.setOnFocusChangeListener((v,focus)->{
            if(!focus)cancelHold();
        }
        );
        holdProgress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        holdProgress.setMax(100);
        holdProgress.setProgressTintList(android.content.res.ColorStateList.valueOf(TvStyle.AMBER));
        content.addView(holdProgress,new LinearLayout.LayoutParams(dp(350),dp(7)));
        body(s(R.string.hold_parents));
        action(R.string.exit,this::finish);
        verticalClamp(content);
    }
    private void cancelHold(){
        holdStart=0;
        handler.removeCallbacks(hold);
        if(holdProgress!=null)holdProgress.setProgress(0);
    }
    private void parents(){
        title(R.string.parent_title);
        ScrollView scroll=new ScrollView(this);
        LinearLayout list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout previous=content;
        content=list;
        action(R.string.settings,()->{
            draft=controller.state().config.copy();java.util.Arrays.fill(settingsMemory,-1);menu="settings";render();
        }
        );
        action(R.string.to_math,()->confirmSkip(true));
        action(R.string.to_chess,()->confirmSkip(false));
        action(R.string.restart_chess,()->confirm(false));
        action(R.string.reset,()->confirm(true));
        action(R.string.about_game,()->{menu="help";render();});
        action(R.string.licenses,()->{
            menu="licenses";render();
        }
        );
        action(R.string.exit,this::finish);
        verticalClamp(list);
        content=previous;
    }
    private void confirmSkip(boolean math){
        new AlertDialog.Builder(this).setTitle(math?R.string.to_math:R.string.to_chess).setMessage(R.string.skip_help).setNegativeButton(R.string.cancel,(d,w)->{
        }
        ).setPositiveButton(R.string.ok,(d,w)->{
            menu="";if(math)controller.skipToMath();else controller.skipToChess();render();
        }
        ).show().getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }
    private void confirm(boolean all){
        new AlertDialog.Builder(this).setTitle(R.string.confirm_reset).setMessage(all?R.string.reset_help:R.string.restart_help).setNegativeButton(R.string.cancel,(d,w)->{
        }
        ).setPositiveButton(R.string.ok,(d,w)->{
            menu="";if(all){
                controller.resetSession();started=true;
            }
            else controller.restartGame();render();
        }
        ).show().getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }
    private void chess(){
        Session st=controller.state();
        if(st.promotionTo>=0){
            title(R.string.promotion);
            LinearLayout row=new LinearLayout(this);
            content.addView(row);
            String[] names={
                s(R.string.queen),s(R.string.rook),s(R.string.bishop),s(R.string.knight)
            }
            ;
            char[] values={
                'q','r','b','n'
            }
            ;
            for(int i=0;i<4;i++){
                final char piece=values[i];
                row.addView(button(names[i],"promotion"+i,()->controller.promote(piece)),new LinearLayout.LayoutParams(0,dp(86),1));
            }
            horizontalClamp(row);
            return;
        }
        if(st.intendedMove!=null){
            title(R.string.claim_title);
            action(R.string.play_move,()->controller.playIntendedMove());
            action(R.string.claim_draw,()->controller.claimDraw());
            return;
        }
        if(!st.result.isEmpty()){
            TextView result=title(R.string.app_name);
            result.setText(st.result);
            action(st.chessPending?R.string.math_action:R.string.restart_chess,()->{
                if(st.chessPending)controller.resume();else controller.restartGame();
            }
            );
            return;
        }
        // Chess owns the largest square area, with a quiet instruction panel to its right.
        content.removeAllViews();
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(row,new LinearLayout.LayoutParams(-1,-1));
        board=new ChessBoardView(this);
        board.setId(View.generateViewId());
        board.setTag("board");
        row.addView(board,new LinearLayout.LayoutParams(0,-1,1.12f));
        LinearLayout side=new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setPadding(dp(30),dp(20),0,dp(20));
        side.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(side,new LinearLayout.LayoutParams(0,-1,1));
        TextView brand=TvStyle.text(this,s(R.string.brand),12,true);
        brand.setTextColor(TvStyle.MUTED);
        side.addView(brand);
        space(side,30);
        chessHeading=TvStyle.text(this,"",35,true);
        side.addView(chessHeading);
        space(side,18);
        chessHelp=TvStyle.text(this,"",24,false);
        chessHelp.setTextColor(TvStyle.MUTED);
        side.addView(chessHelp);
        space(side,18);
        chessDetail=TvStyle.text(this,"",18,false);
        side.addView(chessDetail);
        space(side,40);
        side.addView(TvStyle.text(this,s(R.string.board_help),17,false));
        space(side,12);
        side.addView(TvStyle.text(this,s(R.string.back_pause),16,false));
        board.setListener(new ChessBoardView.Listener(){
            public void focus(int square){
                controller.focusSquare(square);
                updateBoardDetail(square);
            }
            public void activate(int square){
                act(()->controller.chooseSquare(square));
            }
        }
        );
        bindBoard();
        board.requestFocus();
    }
    private void bindBoard(){
        Session st=controller.state();
        ChessGame game=controller.game();
        board.bind(game,st.focus,st.selected,st.moves.isEmpty()?"":st.moves.get(st.moves.size()-1));
        chessHeading.setText(s(game.whiteToMove()?R.string.your_turn:R.string.computer_turn));
        chessHelp.setText(game.checkSquare()>=0?s(R.string.check):st.chessPending?s(R.string.finish_move):s(st.selected>=0?R.string.choose_target:R.string.choose_piece));
        updateBoardDetail(st.focus);
    }
    private void updateBoardDetail(int square){
        if(board==null||chessDetail==null)return;
        Session st=controller.state();
        ChessGame game=controller.game();
        String detail=getString(R.string.square_description,ChessBoardView.coordinate(square),board.pieceName(game.pieceAt(square)));
        if(!st.feedback.isEmpty())detail+="\n"+st.feedback;
        chessDetail.setText(detail);
    }
    private void math(){
        Session st=controller.state();
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(row,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout left=new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_VERTICAL);
        left.setPadding(0,0,dp(24),0);
        row.addView(left,new LinearLayout.LayoutParams(0,-1,1.6f));
        left.addView(TvStyle.text(this,s(R.string.math_title),42,true));
        space(left,12);
        TextView count=TvStyle.text(this,getResources().getQuantityString(R.plurals.math_count,st.correct,st.correct),21,false);
        count.setTextColor(TvStyle.MUTED);
        left.addView(count);
        space(left,30);
        String expression=st.exercise==null?"":st.exercise.expression();
        TextView problem=TvStyle.text(this,expression+" = "+(st.input.isEmpty()?"…":st.input),46,true);
        problem.setTag("problem");
        left.addView(problem);
        space(left,22);
        TextView feedback=TvStyle.text(this,st.feedback,22,true);
        feedback.setTextColor(TvStyle.MINT);
        left.addView(feedback);
        if(st.hintLevel>0&&st.exercise!=null){
            space(left,8);
            TextView hint=TvStyle.text(this,st.exercise.hint(st.hintLevel),20,false);
            hint.setTextColor(TvStyle.MUTED);
            left.addView(hint);
        }
        space(left,22);
        TextView paper=TvStyle.text(this,s(R.string.paper),19,false);
        paper.setTextColor(TvStyle.MUTED);
        left.addView(paper);
        LinearLayout pad=new LinearLayout(this);
        pad.setOrientation(LinearLayout.VERTICAL);
        row.addView(pad,new LinearLayout.LayoutParams(0,-1,1));
        pad.setGravity(Gravity.CENTER_VERTICAL);
        if(st.solved){
            Button next=button(s(R.string.next),"next",()->controller.nextExercise());
            pad.addView(next,new LinearLayout.LayoutParams(-1,dp(70)));
            next.requestFocus();
            return;
        }
        Button hint=button(s(R.string.hint),"hint",()->controller.hint());
        pad.addView(hint,new LinearLayout.LayoutParams(-1,dp(48)));
        space(pad,6);
        Button[] keys=new Button[12];
        String[] labels={
            "1","2","3","4","5","6","7","8","9",s(R.string.delete),"0",s(R.string.ok)
        }
        ;
        for(int r=0;r<4;r++){
            LinearLayout line=new LinearLayout(this);
            pad.addView(line,new LinearLayout.LayoutParams(-1,0,1));
            for(int col=0;col<3;col++){
                int index=r*3+col;
                Button b=button(labels[index],"pad"+index,()->{
                    if(index==9)controller.deleteDigit();else if(index==11)controller.submitAnswer();else controller.digit(index==10?0:index+1);
                }
                );
                b.setTextSize(index==9||index==11?23:34);
                b.setOnFocusChangeListener((v,f)->{
                    if(f)lastPad=index;
                }
                );
                keys[index]=b;
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);
                lp.setMargins(dp(2),dp(2),dp(2),dp(2));
                line.addView(b,lp);
            }
        }
        for(int i=0;i<12;i++){
            Button b=keys[i];
            int col=i%3,r=i/3;
            b.setNextFocusLeftId(keys[col==0?i:i-1].getId());
            b.setNextFocusRightId(keys[col==2?i:i+1].getId());
            b.setNextFocusDownId(keys[r==3?i:i+3].getId());
            b.setNextFocusUpId(r==0?hint.getId():keys[i-3].getId());
        }
        hint.setNextFocusUpId(hint.getId());
        hint.setNextFocusLeftId(hint.getId());
        hint.setNextFocusRightId(hint.getId());
        hint.setOnKeyListener((v,k,event)->{
            if(k==KeyEvent.KEYCODE_DPAD_DOWN&&event.getAction()==KeyEvent.ACTION_DOWN){
                keys[lastPad].requestFocus();return true;
            }
            return false;
        }
        );
        keys[lastPad].requestFocus();
        TextView remote=TvStyle.text(this,s(R.string.math_remote)+" · "+s(R.string.back_pause),14,false);
        remote.setTextColor(TvStyle.MUTED);
        content.addView(remote);
    }
    private void handoff(boolean math){
        title(math?R.string.handoff_math:R.string.handoff_chess);
        body(s(R.string.handoff_help));
        space(content,20);
        action(math?R.string.math_action:R.string.chess_action,()->controller.resume());
    }
    private void horizontalClamp(LinearLayout row){
        for(int i=0;i<row.getChildCount();i++){
            View v=row.getChildAt(i);
            v.setNextFocusLeftId(row.getChildAt(Math.max(0,i-1)).getId());
            v.setNextFocusRightId(row.getChildAt(Math.min(row.getChildCount()-1,i+1)).getId());
            v.setNextFocusUpId(v.getId());
            v.setNextFocusDownId(v.getId());
        }
    }
    private void settings(){
        title(R.string.settings);
        TextView note=TvStyle.text(this,s(R.string.settings_next),16,false);
        note.setTextColor(TvStyle.MUTED);
        content.addView(note);
        ScrollView scroll=new ScrollView(this);
        LinearLayout form=new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(form);
        content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        List<List<Button>> rows=new ArrayList<>();
        List<Button> families=new ArrayList<>();
        families.add(toggle(s(R.string.addition),draft.addition,()->draft.addition=!draft.addition));
        families.add(toggle(s(R.string.subtraction),draft.subtraction,()->draft.subtraction=!draft.subtraction));
        families.add(toggle(s(R.string.multiplication),draft.multiplication,()->draft.multiplication=!draft.multiplication));
        addRow(form,rows,families);
        TextView limit=TvStyle.text(this,s(R.string.number_limit),15,false);
        form.addView(limit);
        List<Button> limits=new ArrayList<>();
        for(int value:new int[]{
            20,50,100
        }
        )limits.add(toggle(String.valueOf(value),draft.limit==value,()->draft.limit=value));
        addRow(form,rows,limits);
        addRow(form,rows,java.util.Collections.singletonList(toggle(s(R.string.crossing),draft.crossingTen,()->draft.crossingTen=!draft.crossingTen)));
        TextView help=TvStyle.text(this,s(R.string.crossing_help),14,false);
        help.setTextColor(TvStyle.MUTED);
        form.addView(help);
        form.addView(TvStyle.text(this,s(R.string.tables),15,false));
        TextView tableHelp=TvStyle.text(this,s(R.string.table_help),14,false);
        tableHelp.setTextColor(TvStyle.MUTED);
        form.addView(tableHelp);
        for(int r=0;r<2;r++){
            List<Button> tables=new ArrayList<>();
            for(int i=1+r*5;i<=5+r*5;i++){
                final int table=i;
                tables.add(toggle(String.valueOf(i),draft.tables[i],()->draft.tables[table]=!draft.tables[table]));
            }
            addRow(form,rows,tables);
        }
        List<Button> actions=new ArrayList<>();
        actions.add(button(s(R.string.save),"save",()->{
            try{
                draft.validate();controller.saveConfig(draft);menu="parents";render();
            }
            catch(IllegalArgumentException e){
                new AlertDialog.Builder(this).setMessage(e.getMessage()).setPositiveButton(R.string.ok,null).show();
            }
        }
        ));
        actions.add(button(s(R.string.cancel),"cancel",()->{
            menu="parents";render();
        }
        ));
        addRow(form,rows,actions);
        final int[] remembered=settingsMemory;
        for(int r=0;r<rows.size();r++)for(int c=0;c<rows.get(r).size();c++){
            final int rr=r,cc=c;
            Button b=rows.get(r).get(c);
            b.setOnFocusChangeListener((v,f)->{
                if(f){
                    remembered[rr]=cc;v.getParent().requestChildFocus(v,v);v.requestRectangleOnScreen(new android.graphics.Rect(0,0,v.getWidth(),v.getHeight()),false);
                }
            }
            );
            b.setOnKeyListener((v,k,e)->{
                if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;if(k==KeyEvent.KEYCODE_DPAD_UP||k==KeyEvent.KEYCODE_DPAD_DOWN){
                    int next=Math.max(0,Math.min(rows.size()-1,rr+(k==KeyEvent.KEYCODE_DPAD_UP?-1:1)));int chosen=remembered[next];if(chosen<0){
                        float center=(cc+.5f)/rows.get(rr).size();float distance=Float.MAX_VALUE;chosen=0;for(int j=0;j<rows.get(next).size();j++){
                            float delta=Math.abs((j+.5f)/rows.get(next).size()-center);if(delta<distance-.0001f){
                                distance=delta;chosen=j;
                            }
                        }
                    }
                    rows.get(next).get(chosen).requestFocus();return true;
                }
                return false;
            }
            );
        }
    }
    private Button toggle(String label,boolean checked,Runnable change){
        Button b=button(getString(checked?R.string.selected:R.string.unselected,label),label,()->{
            change.run();render();
        }
        );
        b.setSelected(checked);
        b.setTextSize(18);
        return b;
    }
    private void addRow(LinearLayout form,List<List<Button>> rows,List<Button> controls){
        LinearLayout row=new LinearLayout(this);
        row.setPadding(0,dp(3),0,dp(3));
        form.addView(row,new LinearLayout.LayoutParams(-1,dp(53)));
        for(Button b:controls){
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);
            p.setMargins(dp(2),0,dp(2),0);
            row.addView(b,p);
        }
        horizontalClamp(row);
        rows.add(controls);
    }
    private void parentHelp(){
        title(R.string.about_game);
        ScrollView scroll=new ScrollView(this);
        TextView text=TvStyle.text(this,s(R.string.parent_help),20,false);
        text.setTextColor(TvStyle.MUTED);
        text.setLineSpacing(dp(5),1);
        text.setPadding(0,dp(8),dp(20),dp(20));
        scroll.addView(text);
        scroll.setFocusable(true);
        scroll.setId(View.generateViewId());
        content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void licenses(){
        title(R.string.licenses);
        ScrollView scroll=new ScrollView(this);
        TextView text=TvStyle.text(this,licenseText(),14,false);
        text.setPadding(0,dp(8),dp(12),dp(12));
        scroll.addView(text);
        scroll.setFocusable(true);
        scroll.setId(View.generateViewId());
        content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }
    private String licenseText(){
        StringBuilder result=new StringBuilder();
        try{
            appendAssets(result,"licenses");
        }
        catch(Exception e){
            result.append("Apache License 2.0\nchesslib · Commons Lang\n").append(e.getClass().getSimpleName());
        }
        return result.toString();
    }
    private void appendAssets(StringBuilder out,String dir)throws Exception{
        for(String name:getAssets().list(dir)){
            String path=dir+"/"+name;
            String[] children=getAssets().list(path);
            if(children.length>0){
                appendAssets(out,path);
                continue;
            }
            out.append(name).append("\n\n");
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(getAssets().open(path),java.nio.charset.StandardCharsets.UTF_8))){
                String line;
                while((line=reader.readLine())!=null)out.append(line).append('\n');
            }
            out.append("\n\n");
        }
    }
    @Override public void onBackPressed(){
        cancelHold();
        if(controller.busy())return;
        if(!menu.isEmpty()){
            if(menu.equals("pause")){menu="";controller.resume();render();return;}
            menu=menu.equals("settings")||menu.equals("licenses")||menu.equals("help")?"parents":"pause";
            render();
            return;
        }
        if(!started){
            finish();
            return;
        }
        Session st=controller.state();
        if(st.phase==Session.Phase.TO_MATH||st.phase==Session.Phase.TO_CHESS){menu="pause";render();return;}
        if(st.paused){
            controller.resume();
            return;
        }
        if(st.phase==Session.Phase.CHESS&&(st.selected>=0||st.promotionTo>=0||st.intendedMove!=null)){
            controller.cancelSelection();
            return;
        }
        menu="pause";
        controller.pause();
        render();
    }
}
