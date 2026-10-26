package com.amine.chessmath;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.KeyEvent;
import android.view.View;
import java.util.Collections;
import java.util.List;
public final class ChessBoardView extends View {
    public interface Listener {
        void focus(int square);
        void activate(int square);
    }
    private final Paint paint = new Paint(3);
    private final PiecePainter pieces;
    private ChessGame game;
    private int square = 52, selected = -1;
    private List<String> legal = Collections.emptyList();
    private int lastFrom=-1,lastTo=-1,beforeLastFrom=-1,beforeLastTo=-1;
    private Listener listener;
    private android.animation.ValueAnimator animator;
    private PresentationEvent transition;
    private float progress;
    private Runnable interrupted;
    public boolean moving(){return transition!=null;}
    public void slide(PresentationEvent event,boolean enabled,Runnable landing,Runnable cancellation){
        cancelMotion();
        if(!enabled||!android.animation.ValueAnimator.areAnimatorsEnabled()) {landing.run();return;}
        transition=event;interrupted=cancellation;selected=-1;legal=Collections.emptyList();progress=0;
        beforeLastFrom=moveSquare(event.beforeLastMove,0);beforeLastTo=moveSquare(event.beforeLastMove,2);describe();
        animator=android.animation.ValueAnimator.ofFloat(0,1);
        animator.setDuration(event.durationMs);
        animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        animator.addUpdateListener(a->{progress=(float)a.getAnimatedValue();invalidate();});
        animator.addListener(new android.animation.AnimatorListenerAdapter(){
            @Override public void onAnimationEnd(android.animation.Animator a){
                if(transition!=event)return;
                transition=null;animator=null;interrupted=null;describe();invalidate();landing.run();
            }
        });
        animator.start();invalidate();
    }
    public void cancelMotion(){
        transition=null;interrupted=null;
        android.animation.ValueAnimator previous=animator;animator=null;
        if(previous!=null)previous.cancel();
        describe();invalidate();
    }
    @Override protected void onDetachedFromWindow(){
        Runnable cancellation=interrupted;cancelMotion();
        if(cancellation!=null)cancellation.run();
        super.onDetachedFromWindow();
    }
    private static int moveSquare(String move,int offset){
        return move==null||move.length()<4?-1:(8-(move.charAt(offset+1)-'0'))*8+move.charAt(offset)-'a';
    }
    public ChessBoardView(Context context) {
        super(context);
        pieces = new PiecePainter(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }
    public void setListener(Listener value) {
        listener=value;
    }
    public int square() {
        return square;
    }
    public void setSquare(int value) {
        square=Math.max(0,Math.min(63,value));
        invalidate();
    }
    public void bind(ChessGame value,int focus,int source,String last) {
        game=value;
        setSquare(focus);
        selected=transition==null?source:-1;
        lastFrom=moveSquare(last,0);lastTo=moveSquare(last,2);
        legal=transition==null?game.legalMoves():Collections.emptyList();
        describe();
        invalidate();
    }
    @Override protected void onMeasure(int w,int h) {
        int size=Math.min(MeasureSpec.getSize(w),MeasureSpec.getSize(h));
        setMeasuredDimension(size,size);
    }
    @Override public boolean onKeyDown(int key,KeyEvent event) {
        int row=square/8,col=square%8;
        if(key==KeyEvent.KEYCODE_DPAD_LEFT) col=Math.max(0,col-1);
        else if(key==KeyEvent.KEYCODE_DPAD_RIGHT) col=Math.min(7,col+1);
        else if(key==KeyEvent.KEYCODE_DPAD_UP) row=Math.max(0,row-1);
        else if(key==KeyEvent.KEYCODE_DPAD_DOWN) row=Math.min(7,row+1);
        else if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER) {
            if(transition==null&&event.getRepeatCount()==0&&listener!=null)listener.activate(square);
            return true;
        }
        else return super.onKeyDown(key,event);
        square=row*8+col;
        describe();
        invalidate();
        if(listener!=null)listener.focus(square);
        return true;
    }
    private void describe(){
        setContentDescription(getContext().getString(R.string.square_description,coordinate(square),game==null?"":pieceName(transition==null?game.pieceAt(square):transition.beforePiece(square))));
    }
    public static String coordinate(int square){
        return ""+(char)('a'+square%8)+(8-square/8);
    }
    public String pieceName(char p) {
        String[] names=getResources().getStringArray(R.array.piece_names);
        int at="pnbrqk".indexOf(Character.toLowerCase(p));
        return at<0?getContext().getString(R.string.empty_square):getContext().getString(Character.isUpperCase(p)?R.string.white_piece:R.string.black_piece,names[at]);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float margin=5,cell=(Math.min(getWidth(),getHeight())-margin*2)/8f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(TvStyle.IVORY);
        c.drawRoundRect(0,0,getWidth(),getHeight(),7,7,paint);
        int checkSquare = transition!=null?transition.beforeCheck:game == null ? -1 : game.checkSquare();
        int shownFrom=transition==null?lastFrom:beforeLastFrom,shownTo=transition==null?lastTo:beforeLastTo;
        for(int i=0;i<64;i++) {
            float x=margin+i%8*cell,y=margin+i/8*cell;
            paint.setColor((i/8+i%8)%2==0?0xffeee3c8:0xff486a4d);
            paint.setStyle(Paint.Style.FILL);
            c.drawRect(x,y,x+cell,y+cell,paint);
            if(i==shownFrom||i==shownTo) {
                paint.setColor(0x26b5ceb0);
                c.drawRect(x,y,x+cell,y+cell,paint);
            }
            if(i==selected){
                paint.setColor(0x40c9c58a);
                c.drawRect(x,y,x+cell,y+cell,paint);
            }
            char piece=transition!=null?transition.beforePiece(i):game==null?'.':game.pieceAt(i);
            if(transition!=null&&(i==transition.from||i==transition.rookFrom))piece='.';
            pieces.draw(c,piece,x,y,cell);
            if(selected>=0)for(String move:legal)if(move.startsWith(coordinate(selected)+coordinate(i))) {
                if(piece=='.'){
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(TvStyle.FOREST);
                    c.drawCircle(x+cell/2,y+cell/2,cell*.14f,paint);
                    paint.setColor(TvStyle.LEGAL_TEAL);
                    c.drawCircle(x+cell/2,y+cell/2,cell*.11f,paint);
                } else {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(TvStyle.FOREST);
                    paint.setStrokeWidth(cell*.1f);
                    c.drawCircle(x+cell/2,y+cell/2,cell*.42f,paint);
                    paint.setColor(TvStyle.LEGAL_TEAL);
                    paint.setStrokeWidth(cell*.055f);
                    c.drawCircle(x+cell/2,y+cell/2,cell*.42f,paint);
                }
                break;
            }
            if(i==selected){
                paint.setColor(TvStyle.FOREST);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(cell*.08f);
                c.drawRect(x+cell*.14f,y+cell*.14f,x+cell*.86f,y+cell*.86f,paint);
                paint.setColor(TvStyle.SELECTED_SAGE_GOLD);
                paint.setStrokeWidth(cell*.04f);
                c.drawRect(x+cell*.14f,y+cell*.14f,x+cell*.86f,y+cell*.86f,paint);
            }
            if(checkSquare==i){
                paint.setColor(0xffff817b);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(3);
                c.drawRect(x+4,y+4,x+cell-4,y+cell-4,paint);
                c.drawRect(x+8,y+8,x+cell-8,y+cell-8,paint);
                paint.setStyle(Paint.Style.FILL);
                c.drawCircle(x+cell*.8f,y+cell*.2f,cell*.15f,paint);
                paint.setColor(TvStyle.FOREST);
                paint.setTextSize(cell*.25f);
                paint.setTextAlign(Paint.Align.CENTER);
                c.drawText("!",x+cell*.8f,y+cell*.29f,paint);
            }
            if(i==square&&hasFocus()){
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(TvStyle.FOREST);
                paint.setStrokeWidth(cell*.1f);
                c.drawRect(x+cell*.045f,y+cell*.045f,x+cell*.955f,y+cell*.955f,paint);
                paint.setColor(TvStyle.AMBER);
                paint.setStrokeWidth(cell*.055f);
                c.drawRect(x+cell*.045f,y+cell*.045f,x+cell*.955f,y+cell*.955f,paint);
            }
        }
        if(transition!=null){
            if(transition.rookFrom>=0)drawMover(c,transition.rookPiece,transition.rookFrom,transition.rookTo,margin,cell);
            drawMover(c,transition.movingPiece,transition.from,transition.to,margin,cell);
        }
    }
    private void drawMover(Canvas c,char piece,int from,int to,float margin,float cell){
        float x=margin+(from%8+(to%8-from%8)*progress)*cell;
        float y=margin+(from/8+(to/8-from/8)*progress)*cell;
        pieces.draw(c,piece,x,y,cell);
    }
}
