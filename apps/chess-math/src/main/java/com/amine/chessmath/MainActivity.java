package com.amine.chessmath;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
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
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;
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
    private static EffectPreferences sharedEffects;
    private EffectPreferences effects;
    private EffectPlayer player;
    private final PresentationEvent.Consumption consumed=new PresentationEvent.Consumption();
    private PresentationEvent presenting;
    private void interruptEffects(){
        presenting=null;
        if(board!=null)board.cancelMotion();
        if(player!=null)player.stop();
    }
    private void present(PresentationEvent event){
        if(!alive||!foreground||!hasWindowFocus()||!consumed.take(event)){
            controller.completePresentation(event.id);return;
        }
        if(event.kind==PresentationEvent.Kind.MATH_SUCCESS){
            if(effects.sound())player.play(event.cue);
            return;
        }
        presenting=event;render();
        if(board==null){presenting=null;controller.completePresentation(event.id);return;}
        board.slide(event,effects.motion(),()->{
            if(presenting!=event)return;
            presenting=null;
            if(foreground&&hasWindowFocus()&&effects.sound())player.play(event.cue);
            controller.completePresentation(event.id);render();
        },()->{
            if(presenting==event){presenting=null;controller.completePresentation(event.id);}
        });
    }
    private void changeEffect(boolean sound){
        boolean saved=sound?effects.setSound(!effects.sound()):effects.setMotion(!effects.motion());
        if(!saved)new AlertDialog.Builder(this).setTitle(R.string.error_title)
                .setMessage(R.string.preference_error).setPositiveButton(R.string.ok,null).show();
        if(!effects.sound())player.stop();
        render();
    }
    private LinearLayout content;
    private boolean started, alive, foreground;
    private String menu="", screen="";
    private ChessBoardView board;
    private TextView chessHeading,chessHelp,chessDetail,savingText,breakCountdown;
    private long displayedBreakSecond=-1;
    private MathConfig draft;
    private static LanguagePreferences sharedLanguage;
    private LanguagePreferences language;
    private Context languageContext;
    @Override protected void attachBaseContext(Context base){
        if(sharedLanguage==null)sharedLanguage=new LanguagePreferences(base);
        language=sharedLanguage;
        languageContext=localizedContext(base,language.language());
        super.attachBaseContext(languageContext);
    }
    private static Context localizedContext(Context base,String language){
        Configuration configuration=new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(language));
        return base.createConfigurationContext(configuration);
    }
    @Override public Resources getResources(){
        return languageContext==null?super.getResources():languageContext.getResources();
    }
    private String localized(String text){return LocalizedText.message(text,language.language());}
    private void changeLanguage(String value){
        if(!language.setLanguage(value)){
            new AlertDialog.Builder(this).setMessage(R.string.preference_error).setPositiveButton(R.string.ok,null).show();
            return;
        }
        languageContext=localizedContext(getBaseContext(),value);
        screen="";render();
    }
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
                updateBreakCountdown();
                handler.postDelayed(this,250);
            }
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
        if(saved!=null){
            started=saved.getBoolean("started",false);menu=saved.getString("menu","");
            lastPad=saved.getInt("pad",4);
            if(saved.containsKey("draft-limit")){
                draft=controller.state().config.copy();
                draft.limit=saved.getInt("draft-limit");
                draft.addition=saved.getBoolean("draft-add");draft.subtraction=saved.getBoolean("draft-sub");
                draft.multiplication=saved.getBoolean("draft-mul");draft.crossingTen=saved.getBoolean("draft-cross");
                boolean[] tables=saved.getBooleanArray("draft-tables");
                if(tables!=null&&tables.length==draft.tables.length)System.arraycopy(tables,0,draft.tables,0,tables.length);
            }
        }
        if(sharedEffects==null)sharedEffects=new EffectPreferences(getApplicationContext());
        effects=sharedEffects;player=new EffectPlayer(this);
        controller.setListener(this::render);
        alive=true;
        controller.setPresenter(this::present);
        if(controller.busy()){shell();title(R.string.app_name);body(s(R.string.saving));}
        render();
        handler.post(tick);
    }
    @Override protected void onSaveInstanceState(Bundle saved){
        saved.putBoolean("started",started);saved.putString("menu",menu);saved.putInt("pad",lastPad);
        if(draft!=null){
            saved.putInt("draft-limit",draft.limit);saved.putBoolean("draft-add",draft.addition);
            saved.putBoolean("draft-sub",draft.subtraction);saved.putBoolean("draft-mul",draft.multiplication);
            saved.putBoolean("draft-cross",draft.crossingTen);saved.putBooleanArray("draft-tables",draft.tables.clone());
        }
        super.onSaveInstanceState(saved);
    }
    @Override protected void onResume(){
        super.onResume();
        foreground=true;
        if(controller!=null)controller.foreground(hasWindowFocus());
    }
    @Override protected void onPause(){
        foreground=false;
        interruptEffects();
        if(controller!=null)controller.foreground(false);
        super.onPause();
    }
    @Override public void onWindowFocusChanged(boolean focus){
        super.onWindowFocusChanged(focus);
        if(controller!=null)controller.foreground(focus&&foreground);
        if(!focus){interruptEffects();}
    }
    @Override protected void onDestroy(){
        alive=false;
        handler.removeCallbacksAndMessages(null);
        if(controller!=null){controller.setPresenter(null);controller.setListener(null);}
        interruptEffects();
        if(player!=null)player.close();
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
        if(presenting!=null&&controller.presentation()!=presenting){
            presenting=null;if(board!=null)board.cancelMotion();
        }
        if(controller.busy()){updateSaving();return;}
        if(hasError()){
            shell();
            screen="error";
            title(R.string.error_title);
            body(controller.corruptSave()?s(R.string.load_error):localized(controller.error()));
            if(!controller.corruptSave())action(R.string.retry,()->controller.retrySave());
            action(R.string.reset,()->confirmReset());
            action(R.string.exit,this::finish);
            focusFirst();
            return;
        }
        String target=presenting!=null?"CHESS":!started?"start":!menu.isEmpty()?menu:(state.phase==Session.Phase.TO_MATH||state.phase==Session.Phase.TO_CHESS)?state.phase.name():state.paused?"pause":state.phase.name();
        if(target.equals("CHESS")&&screen.equals("CHESS")&&state.promotionTo<0&&state.intendedMove==null&&(state.result.isEmpty()||presenting!=null)&&board!=null){
            bindBoard();
            return;
        }
        if(target.equals("BREAK")&&screen.equals("BREAK")&&breakCountdown!=null){
            updateBreakCountdown();return;
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
            case "BREAK":breakTime();
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
            savingText.setText(hasError()?localized(controller.error()):s(R.string.saving));
            savingText.setVisibility(controller.busy()?View.VISIBLE:View.GONE);
        }
    }
    private void shell(){
        if(board!=null)board.cancelMotion();
        board=null;
        breakCountdown=null;displayedBreakSecond=-1;
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
        action(R.string.parents,()->{menu="parents";render();});
        Button mute=button(getString(effects.sound()?R.string.sound_on:R.string.sound_off),"pause-sound",()->changeEffect(true));
        content.addView(mute,new LinearLayout.LayoutParams(dp(350),dp(49)));
        space(content,6);
        action(R.string.exit,this::finish);
        verticalClamp(content);
    }
    private void parents(){
        title(R.string.parent_title);
        TextView version=TvStyle.text(this,getString(R.string.app_version,BuildConfig.VERSION_NAME),14,false);
        version.setTextColor(TvStyle.MUTED);
        version.setTag("app-version");
        content.addView(version,new LinearLayout.LayoutParams(-1,-2));
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
        Button sound=button(getString(R.string.effect_value,s(R.string.sound_effects),s(effects.sound()?R.string.on:R.string.off)),"sound-effects",()->changeEffect(true));
        list.addView(sound,new LinearLayout.LayoutParams(dp(350),dp(49)));
        Button motion=button(getString(R.string.effect_value,s(R.string.piece_motion),s(effects.motion()?R.string.on:R.string.off)),"piece-motion",()->changeEffect(false));
        list.addView(motion,new LinearLayout.LayoutParams(dp(350),dp(49)));
        Button languageChoice=button(getString(R.string.effect_value,s(R.string.language),s("en".equals(language.language())?R.string.english:R.string.dutch)),"language",()->{
            new AlertDialog.Builder(this).setTitle(R.string.language)
                .setSingleChoiceItems(new String[]{s(R.string.dutch),s(R.string.english)},"en".equals(language.language())?1:0,
                    (dialog,which)->{dialog.dismiss();changeLanguage(which==1?"en":"nl");}).show();
        });
        list.addView(languageChoice,new LinearLayout.LayoutParams(dp(350),dp(49)));
        action(R.string.to_math,()->confirmSkip(true));
        action(R.string.to_chess,()->confirmSkip(false));
        action(R.string.restart_chess,()->{menu="";controller.restartGame();render();});
        action(R.string.reset,()->confirmReset());
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
    private void confirmReset(){
        new AlertDialog.Builder(this).setTitle(R.string.confirm_reset).setMessage(R.string.reset_help)
            .setNegativeButton(R.string.cancel,(d,w)->{})
            .setPositiveButton(R.string.ok,(d,w)->{
                menu="";controller.resetSession();started=true;render();
            }).show().getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
    }
    private void chess(){
        Session st=controller.state();
        if(st.promotionTo>=0&&presenting==null){
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
                Button choice=button(names[i],"promotion"+i,()->controller.promote(piece));
                choice.setContentDescription(names[i]);
                choice.setSoundEffectsEnabled(false);
                char image=controller.game().whiteToMove()?Character.toUpperCase(piece):piece;
                choice.setCompoundDrawables(null,new PromotionPiece(image),null,null);
                choice.setCompoundDrawablePadding(dp(10));
                LinearLayout.LayoutParams bounds=new LinearLayout.LayoutParams(0,dp(182),1);
                bounds.setMargins(dp(4),dp(20),dp(4),0);
                row.addView(choice,bounds);
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
        if(!st.result.isEmpty()&&presenting==null){
            TextView result=title(R.string.app_name);
            result.setText(localized(st.result));
            action(R.string.math_action,()->controller.resume());
            return;
        }
        // Equal space on both sides keeps the board at the center of the TV.
        content.removeAllViews();
        FrameLayout row=new FrameLayout(this){
            @Override protected void onMeasure(int widthSpec,int heightSpec){
                int width=View.MeasureSpec.getSize(widthSpec);
                int height=View.MeasureSpec.getSize(heightSpec);
                int size=Math.min(dp(462),Math.min(height,width));
                FrameLayout.LayoutParams boardBounds=(FrameLayout.LayoutParams)getChildAt(0).getLayoutParams();
                boardBounds.width=size;
                boardBounds.height=size;
                FrameLayout.LayoutParams statusBounds=(FrameLayout.LayoutParams)getChildAt(1).getLayoutParams();
                statusBounds.width=Math.max(0,(width-size)/2-dp(20));
                super.onMeasure(widthSpec,heightSpec);
            }
        };
        content.addView(row,new LinearLayout.LayoutParams(-1,-1));
        board=new ChessBoardView(this);
        board.setId(View.generateViewId());
        board.setTag("board");
        row.addView(board,new FrameLayout.LayoutParams(0,0,Gravity.CENTER));
        ScrollView status=new ScrollView(this);
        status.setTag("chess-status");
        status.setFocusable(false);
        status.setFillViewport(true);
        row.addView(status,new FrameLayout.LayoutParams(0,-1,Gravity.RIGHT|Gravity.CENTER_VERTICAL));
        LinearLayout side=new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setPadding(0,dp(20),0,dp(20));
        side.setGravity(Gravity.CENTER_VERTICAL);
        status.addView(side,new ScrollView.LayoutParams(-1,-2));
        chessHeading=TvStyle.text(this,"",24,true);
        chessHeading.setTag("chess-turn");
        side.addView(chessHeading);
        space(side,12);
        chessHelp=TvStyle.text(this,"",18,false);
        chessHelp.setTag("chess-prompt");
        chessHelp.setTextColor(TvStyle.MUTED);
        side.addView(chessHelp);
        chessDetail=TvStyle.text(this,"",16,false);
        chessDetail.setTag("chess-feedback");
        chessDetail.setPadding(0,dp(12),0,0);
        side.addView(chessDetail);
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
        boolean white=presenting==null?game.whiteToMove():presenting.beforeWhiteToMove;
        chessHeading.setText(s(white?R.string.your_turn:R.string.computer_turn));
        chessHelp.setText((presenting==null?game.checkSquare():presenting.beforeCheck)>=0?s(R.string.check):st.chessPending?s(R.string.finish_move):s(st.selected>=0?R.string.choose_target:R.string.choose_piece));
        updateBoardDetail(st.focus);
    }
    private void updateBoardDetail(int square){
        if(board==null||chessDetail==null)return;
        Session st=controller.state();
        chessDetail.setText(localized(st.feedback));
        chessDetail.setVisibility(st.feedback.isEmpty()?View.GONE:View.VISIBLE);
    }
    /** Uses the same artwork and color as the pawn that is being promoted. */
    private final class PromotionPiece extends android.graphics.drawable.Drawable {
        private final PiecePainter painter=new PiecePainter(MainActivity.this);
        private final char piece;
        PromotionPiece(char piece){
            this.piece=piece;
            setBounds(0,0,dp(94),dp(94));
        }
        @Override public void draw(android.graphics.Canvas canvas){
            android.graphics.Rect bounds=getBounds();
            painter.draw(canvas,piece,bounds.left,bounds.top,Math.min(bounds.width(),bounds.height()));
        }
        @Override public void setAlpha(int alpha){}
        @Override public void setColorFilter(android.graphics.ColorFilter filter){}
        @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }
    private void math(){
        Session st=controller.state();
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(row,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView reading=new ScrollView(this);
        reading.setId(View.generateViewId());
        reading.setTag("math-reading");
        reading.setFocusable(false);
        reading.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        reading.setFillViewport(true);
        GradientDrawable readingOutline=new GradientDrawable();
        readingOutline.setColor(android.graphics.Color.TRANSPARENT);
        readingOutline.setStroke(dp(2),TvStyle.IVORY);
        readingOutline.setCornerRadius(dp(12));
        android.graphics.drawable.StateListDrawable readingFocus=new android.graphics.drawable.StateListDrawable();
        readingFocus.addState(new int[]{android.R.attr.state_focused},readingOutline);
        reading.setForeground(readingFocus);
        reading.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->
                reading.setFocusable(reading.canScrollVertically(-1)||reading.canScrollVertically(1)));
        reading.setOnKeyListener((v,k,event)->{
            if(k==KeyEvent.KEYCODE_DPAD_RIGHT){
                if(event.getAction()==KeyEvent.ACTION_DOWN){
                    View target=findViewById(reading.getNextFocusRightId());
                    if(target!=null)target.requestFocus();
                }
                return true;
            }
            if(k==KeyEvent.KEYCODE_DPAD_UP||k==KeyEvent.KEYCODE_DPAD_DOWN){
                reading.executeKeyEvent(event);
                return true;
            }
            return k==KeyEvent.KEYCODE_DPAD_LEFT;
        });
        reading.setClipToPadding(false);
        row.addView(reading,new LinearLayout.LayoutParams(0,-1,1));
        LinearLayout left=new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.TOP);
        left.setPadding(0,dp(26),dp(34),dp(16));
        reading.addView(left,new ScrollView.LayoutParams(-1,-2));
        TextView heading=TvStyle.text(this,s(R.string.math_title),30,true);
        heading.setTag("math-title");
        left.addView(heading);
        space(left,8);
        TextView count=TvStyle.text(this,getResources().getQuantityString(R.plurals.math_count,st.correct,st.correct),17,false);
        count.setTag("math-count");
        count.setTextColor(TvStyle.MUTED);
        left.addView(count);
        space(left,28);
        LinearLayout equation=new LinearLayout(this){
            @Override protected void onMeasure(int widthSpec,int heightSpec){
                TextView expression=(TextView)getChildAt(0);
                android.graphics.Paint paint=new android.graphics.Paint(expression.getPaint());
                paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,58,getResources().getDisplayMetrics()));
                int desired=(int)Math.ceil(paint.measureText(expression.getText().toString()))
                        +expression.getCompoundPaddingLeft()+expression.getCompoundPaddingRight();
                int available=View.MeasureSpec.getSize(widthSpec)-getPaddingLeft()-getPaddingRight()-dp(124);
                expression.getLayoutParams().width=Math.max(0,Math.min(desired,available));
                super.onMeasure(widthSpec,heightSpec);
            }
        };
        equation.setTag("math-equation");
        equation.setGravity(Gravity.CENTER);
        left.addView(equation,new LinearLayout.LayoutParams(-1,-2));
        String expression=st.exercise==null?"":st.exercise.expression();
        TextView problem=TvStyle.text(this,expression+" =",58,true);
        problem.setTag("problem");
        problem.setMaxLines(1);
        problem.setGravity(Gravity.CENTER_VERTICAL);
        int equationHeight=dp(80*Math.max(1,getResources().getConfiguration().fontScale));
        problem.setMinHeight(equationHeight);
        problem.setAutoSizeTextTypeUniformWithConfiguration(32,58,1,TypedValue.COMPLEX_UNIT_SP);
        equation.addView(problem,new LinearLayout.LayoutParams(-2,equationHeight));
        TextView answer=TvStyle.text(this,st.input.isEmpty()?"…":st.input,48,true);
        answer.setTag("answer");
        answer.setGravity(Gravity.CENTER);
        answer.setMaxLines(1);
        answer.setPadding(dp(8),dp(8),dp(8),dp(8));
        answer.setAutoSizeTextTypeUniformWithConfiguration(18,48,1,TypedValue.COMPLEX_UNIT_SP);
        GradientDrawable well=new GradientDrawable();
        well.setColor(0x181C492E);
        well.setStroke(dp(2),TvStyle.IVORY);
        well.setCornerRadius(dp(12));
        answer.setBackground(well);
        LinearLayout.LayoutParams answerBounds=new LinearLayout.LayoutParams(dp(112),equationHeight);
        answerBounds.setMargins(dp(12),0,0,0);
        equation.addView(answer,answerBounds);
        space(left,10);
        TextView feedback=TvStyle.text(this,st.solved?s(R.string.math_success):localized(st.feedback),28,true);
        feedback.setTag("math-feedback");
        feedback.setTextColor(TvStyle.MINT);
        feedback.setVisibility(st.solved||!st.feedback.isEmpty()?View.VISIBLE:View.INVISIBLE);
        left.addView(feedback);
        if(st.hintLevel>0&&st.exercise!=null&&!st.solved){
            space(left,12);
            TextView explanation=TvStyle.text(this,st.exercise.hint(Math.min(2,st.hintLevel),language.language()),20,false);
            explanation.setTextColor(TvStyle.MUTED);
            explanation.setTag("math-hint");
            left.addView(explanation);
        }
        LinearLayout pad=new LinearLayout(this);
        pad.setTag("math-keypad");
        pad.setOrientation(LinearLayout.VERTICAL);
        row.addView(pad,new LinearLayout.LayoutParams(dp(240),-1));
        pad.setGravity(Gravity.CENTER);
        if(st.solved)return;
        Button hint=button(s(R.string.hint),"hint",()->controller.hint());
        pad.addView(hint,new LinearLayout.LayoutParams(-1,dp(48)));
        space(pad,8);
        Button[] keys=new Button[11];
        String[] labels={"1","2","3","4","5","6","7","8","9",s(R.string.delete),"0"};
        for(int r=0;r<4;r++){
            LinearLayout line=new LinearLayout(this);
            pad.addView(line,new LinearLayout.LayoutParams(-1,dp(56)));
            for(int col=0;col<3;col++){
                int index=r*3+col;
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);
                lp.setMargins(dp(2),dp(2),dp(2),dp(2));
                if(index==11){line.addView(new View(this),lp);continue;}
                Button b=button(labels[index],"pad"+index,()->{
                    if(index==9)controller.deleteDigit();else controller.digit(index==10?0:index+1);
                });
                if(index!=9)b.setSoundEffectsEnabled(!controller.previewSolvedDigit(index==10?0:index+1));
                b.setTextSize(index==9?18:27);
                b.setPadding(dp(6),0,dp(6),0);
                b.setOnFocusChangeListener((v,f)->{if(f)lastPad=index;});
                keys[index]=b;
                line.addView(b,lp);
            }
        }
        for(int i=0;i<keys.length;i++){
            Button b=keys[i];
            int col=i%3,r=i/3;
            b.setNextFocusLeftId(keys[col==0?i:i-1].getId());
            if(col==0)b.setOnKeyListener((v,k,event)->enterMathReading(reading,v,k,event));
            b.setNextFocusRightId(keys[col==2||i==10?i:i+1].getId());
            b.setNextFocusDownId(keys[r==3?i:Math.min(10,i+3)].getId());
            b.setNextFocusUpId(r==0?hint.getId():keys[i-3].getId());
        }
        hint.setNextFocusUpId(hint.getId());
        hint.setNextFocusLeftId(hint.getId());
        hint.setNextFocusRightId(hint.getId());
        hint.setOnKeyListener((v,k,event)->{
            if(enterMathReading(reading,v,k,event))return true;
            if(k==KeyEvent.KEYCODE_DPAD_DOWN&&event.getAction()==KeyEvent.ACTION_DOWN){
                keys[lastPad].requestFocus();return true;
            }
            return false;
        });
        keys[lastPad].requestFocus();
    }
    private boolean enterMathReading(ScrollView reading,View origin,int key,KeyEvent event){
        if(key!=KeyEvent.KEYCODE_DPAD_LEFT||!reading.isFocusable())return false;
        if(event.getAction()==KeyEvent.ACTION_DOWN){
            reading.setNextFocusRightId(origin.getId());
            reading.requestFocus();
        }
        return true;
    }
    private void breakTime(){
        LinearLayout center=new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        content.addView(center,new LinearLayout.LayoutParams(-1,0,1));
        TextView heading=TvStyle.text(this,s(R.string.break_title),38,true);
        heading.setTag("break-title");heading.setGravity(Gravity.CENTER);
        center.addView(heading,new LinearLayout.LayoutParams(-1,-2));
        space(center,18);
        breakCountdown=TvStyle.text(this,"",76,true);
        breakCountdown.setTag("break-countdown");breakCountdown.setGravity(Gravity.CENTER);
        center.addView(breakCountdown,new LinearLayout.LayoutParams(-1,-2));
        space(center,18);
        TextView help=TvStyle.text(this,s(R.string.break_help),22,false);
        help.setTextColor(TvStyle.MUTED);help.setGravity(Gravity.CENTER);
        center.addView(help,new LinearLayout.LayoutParams(-1,-2));
        updateBreakCountdown();
    }
    private void updateBreakCountdown(){
        if(breakCountdown==null||!"BREAK".equals(screen))return;
        long seconds=(controller.breakRemainingMs()+999)/1000;
        if(seconds==displayedBreakSecond)return;
        displayedBreakSecond=seconds;
        breakCountdown.setText(String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60));
        breakCountdown.setContentDescription(getString(R.string.break_remaining,seconds));
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
                new AlertDialog.Builder(this).setMessage(localized(e.getMessage())).setPositiveButton(R.string.ok,null).show();
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
        if(presenting!=null){
            interruptEffects();menu="pause";controller.pause();render();return;
        }
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
        if(st.phase==Session.Phase.TO_MATH||st.phase==Session.Phase.TO_CHESS){interruptEffects();menu="pause";render();return;}
        if(st.paused){
            controller.resume();
            return;
        }
        if(st.phase==Session.Phase.CHESS&&(st.selected>=0||st.promotionTo>=0||st.intendedMove!=null)){
            controller.cancelSelection();
            return;
        }
        interruptEffects();
        menu="pause";
        controller.pause();
        render();
    }
}
