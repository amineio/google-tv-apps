package com.amine.chessmath;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
final class TvStyle {
    static final int FOREST=0xff0c2b1d, IVORY=0xfffff1d0, MUTED=0xffb5ceb0, MINT=0xff6be0bd, AMBER=0xffffcf70;
    static int dp(Context c,float v){
        return Math.round(v*c.getResources().getDisplayMetrics().density);
    }
    static TextView text(Context c,String text,float size,boolean bold){
        TextView v=new TextView(c);
        v.setText(text);
        v.setTextColor(IVORY);
        v.setTextSize(size);
        v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        v.setIncludeFontPadding(false);
        return v;
    }
    static Button button(Context c,String title){
        Button b=new Button(c);
        b.setId(View.generateViewId());
        b.setText(title);
        b.setTextSize(21);
        b.setTextColor(IVORY);
        b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setMinHeight(dp(c,48));
        b.setMinimumHeight(dp(c,48));
        b.setPadding(dp(c,18),dp(c,8),dp(c,18),dp(c,8));
        b.setBackground(new FocusSurface(c));
        b.setStateListAnimator(null);
        return b;
    }
    private static final class FocusSurface extends android.graphics.drawable.Drawable {
        private final Paint p=new Paint(3);
        private final float d;
        FocusSurface(Context c){
            d=c.getResources().getDisplayMetrics().density;
        }
        @Override public boolean isStateful(){
            return true;
        }
        @Override protected boolean onStateChange(int[] states){
            invalidateSelf();
            return true;
        }
        @Override public void draw(Canvas c){
            boolean focus=false,checked=false;
            for(int s:getState()){
                focus|=s==android.R.attr.state_focused;
                checked|=s==android.R.attr.state_selected;
            }
            float left=getBounds().left+3*d,top=getBounds().top+3*d,right=getBounds().right-3*d,bottom=getBounds().bottom-3*d;
            p.setStyle(Paint.Style.FILL);
            p.setColor(focus?0xff52734c:checked?0xff345c40:0x451c492e);
            c.drawRoundRect(left,top,right,bottom,10*d,10*d,p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth((focus?3:1.4f)*d);
            p.setColor(focus?IVORY:checked?AMBER:0xff80977c);
            c.drawRoundRect(left,top,right,bottom,10*d,10*d,p);
            if(focus){
                p.setStrokeWidth(4*d);
                float len=11*d;
                for(int n=0;n<4;n++){
                    float x=n%2==0?left:right,y=n/2==0?top:bottom;
                    c.drawLine(x,y,x+(n%2==0?len:-len),y,p);
                    c.drawLine(x,y,x,y+(n/2==0?len:-len),p);
                }
            }
        }
        @Override public void setAlpha(int a){
        }
        @Override public void setColorFilter(android.graphics.ColorFilter f){
        }
        @Override public int getOpacity(){
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }
    static final class Backdrop extends View {
        private final Paint p=new Paint(3);
        private final Path leaf=new Path();
        private android.graphics.RadialGradient gradient;
        Backdrop(Context c){
            super(c);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
        @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){
            super.onSizeChanged(width,height,oldWidth,oldHeight);
            if(width>0&&height>0)gradient=new android.graphics.RadialGradient(width*.3f,height*.3f,width*.85f,
                    new int[]{0xff194831,FOREST,0xff071a11},null,android.graphics.Shader.TileMode.CLAMP);
        }
        @Override protected void onDraw(Canvas c){
            c.drawColor(FOREST);
            p.setShader(gradient);
            c.drawRect(0,0,getWidth(),getHeight(),p);
            p.setShader(null);
            p.setColor(0x205d8850);
            for(int corner=0;corner<2;corner++){
                c.save();
                if(corner==1){
                    c.translate(getWidth(),getHeight());
                    c.rotate(180);
                }
                float unit=getHeight()/540f;
                c.scale(unit,unit);
                for(int n=0;n<9;n++){
                    float x=10+n*9,y=520-n*26;
                    leaf.reset();
                    leaf.moveTo(x,y);
                    leaf.cubicTo(x-42,y-32,x-28,y-59,x,y-71);
                    leaf.cubicTo(x+12,y-43,x+15,y-22,x,y);
                    c.drawPath(leaf,p);
                }
                c.restore();
            }
        }
    }
}
