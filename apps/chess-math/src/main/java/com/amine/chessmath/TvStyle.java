package com.amine.chessmath;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
final class TvStyle {
    static final int FOREST=0xff0c2b1d, IVORY=0xfffff1d0, MUTED=0xffb5ceb0, MINT=0xff6be0bd, AMBER=0xffffcf70;
    static final int LEGAL_TEAL=0xff95c4b5, SELECTED_SAGE_GOLD=0xffc9c58a;
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
            p.setColor(focus?0xff3e6046:checked?0xff34543d:0x451c492e);
            c.drawRoundRect(left,top,right,bottom,10*d,10*d,p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth((focus?2.5f:1.4f)*d);
            p.setColor(focus?AMBER:checked?MUTED:0xaafff1d0);
            c.drawRoundRect(left,top,right,bottom,10*d,10*d,p);
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
        private static android.graphics.Bitmap artwork;
        private static boolean attempted;
        private final android.graphics.RectF destination=new android.graphics.RectF();
        private android.graphics.RadialGradient gradient;
        Backdrop(Context c){
            super(c);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            synchronized(Backdrop.class){
                if(!attempted){
                    attempted=true;
                    android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();
                    options.inScaled=false;
                    artwork=android.graphics.BitmapFactory.decodeResource(c.getResources(),R.drawable.forest_backdrop_v2,options);
                }
            }
            p.setFilterBitmap(true);
        }
        @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){
            super.onSizeChanged(width,height,oldWidth,oldHeight);
            if(width>0&&height>0){
                gradient=new android.graphics.RadialGradient(width*.3f,height*.3f,width*.85f,
                    new int[]{0xff194831,FOREST,0xff071a11},null,android.graphics.Shader.TileMode.CLAMP);
                if(artwork!=null){
                    float scale=Math.max((float)width/artwork.getWidth(),(float)height/artwork.getHeight());
                    float w=artwork.getWidth()*scale,h=artwork.getHeight()*scale;
                    destination.set((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2);
                }
            }
        }
        @Override protected void onDraw(Canvas c){
            c.drawColor(FOREST);
            if(artwork!=null){
                c.drawBitmap(artwork,null,destination,p);
            }else{
                p.setShader(gradient);
                c.drawRect(0,0,getWidth(),getHeight(),p);
                p.setShader(null);
            }
        }
    }
}
