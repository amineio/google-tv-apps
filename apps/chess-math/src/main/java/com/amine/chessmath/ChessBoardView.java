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
    private final PiecePainter pieces = new PiecePainter();
    private ChessGame game;
    private int square = 52, selected = -1;
    private List<String> legal = Collections.emptyList();
    private String lastMove = "";
    private Listener listener;
    public ChessBoardView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE,null);
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
        selected=source;
        lastMove=last==null?"":last;
        legal=game.legalMoves();
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
            if(event.getRepeatCount()==0&&listener!=null)listener.activate(square);
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
        setContentDescription(getContext().getString(R.string.square_description,coordinate(square),game==null?"":pieceName(game.pieceAt(square))));
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
        for(int i=0;i<64;i++) {
            float x=margin+i%8*cell,y=margin+i/8*cell;
            paint.setColor((i/8+i%8)%2==0?0xffeee3c8:0xff486a4d);
            paint.setStyle(Paint.Style.FILL);
            c.drawRect(x,y,x+cell,y+cell,paint);
            if(lastMove.length()>=4&&(coordinate(i).equals(lastMove.substring(0,2))||coordinate(i).equals(lastMove.substring(2,4)))) {
                c.save();
                c.clipRect(x,y,x+cell,y+cell);
                paint.setColor(0x465bbca3);
                paint.setStrokeWidth(2);
                for(float d=-cell;d<cell*2;d+=10)c.drawLine(x+d,y,x+d+cell,y+cell,paint);
                c.restore();
            }
            char piece=game==null?'.':game.pieceAt(i);
            pieces.draw(c,piece,x,y,cell);
            if(selected>=0)for(String move:legal)if(move.startsWith(coordinate(selected)+coordinate(i))) {
                paint.setColor(TvStyle.MINT);
                paint.setStrokeWidth(cell*.055f);
                if(piece=='.'){
                    paint.setStyle(Paint.Style.FILL);
                    c.drawCircle(x+cell/2,y+cell/2,cell*.11f,paint);
                }
                else corners(c,x+cell*.12f,y+cell*.12f,cell*.76f,cell*.18f);
                break;
            }
            if(i==selected){
                paint.setColor(TvStyle.AMBER);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(cell*.055f);
                c.drawRect(x+cell*.1f,y+cell*.1f,x+cell*.9f,y+cell*.9f,paint);
                paint.setStyle(Paint.Style.FILL);
                c.drawCircle(x+cell*.18f,y+cell*.18f,cell*.06f,paint);
            }
            if(game!=null&&game.checkSquare()==i){
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
                paint.setColor(0xff183729);
                paint.setStrokeWidth(cell*.1f);
                corners(c,x+3,y+3,cell-6,cell*.22f);
                paint.setColor(TvStyle.IVORY);
                paint.setStrokeWidth(cell*.055f);
                corners(c,x+3,y+3,cell-6,cell*.22f);
            }
        }
    }
    private void corners(Canvas c,float x,float y,float size,float length) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for(int n=0;n<4;n++){
            float xx=x+(n%2)*size,yy=y+(n/2)*size;
            c.drawLine(xx,yy,xx+(n%2==0?length:-length),yy,paint);
            c.drawLine(xx,yy,xx,yy+(n/2==0?length:-length),paint);
        }
    }
}
