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
    static android.widget.Switch switchControl(Context context,String label){
        android.widget.Switch control=new android.widget.Switch(context);
        control.setId(View.generateViewId());control.setText(label);
        control.setTextColor(IVORY);control.setTextSize(21);control.setSingleLine(false);control.setMaxLines(3);
        control.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        control.setMinHeight(dp(context,52));control.setMinimumHeight(dp(context,52));
        control.setPadding(dp(context,16),dp(context,8),dp(context,16),dp(context,8));
        control.setSwitchMinWidth(dp(context,48));control.setSwitchPadding(dp(context,12));
        control.setShowText(false);control.setSplitTrack(false);
        control.setThumbTintList(new android.content.res.ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{AMBER,IVORY}));
        control.setTrackTintList(new android.content.res.ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff6b9277,0xff49614f}));
        control.setBackground(new FocusSurface(context));control.setStateListAnimator(null);
        return control;
    }
    static void preferenceRow(TextView row){
        Context context=row.getContext();
        row.setTextSize(20);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL|android.view.Gravity.START);
        row.setMinHeight(dp(context,56));row.setMinimumHeight(dp(context,56));
        row.setPadding(dp(context,16),dp(context,12),dp(context,16),dp(context,12));
        row.setBackground(new PreferenceSurface(context));
    }
    static void settingIcon(TextView row,int resource){
        android.graphics.drawable.Drawable icon=row.getContext().getDrawable(resource).mutate();
        int size=dp(row.getContext(),24);
        icon.setBounds(0,0,size,size);
        icon.setTintList(new android.content.res.ColorStateList(
                new int[][]{new int[]{android.R.attr.state_focused},new int[]{}},new int[]{AMBER,IVORY}));
        row.setCompoundDrawablesRelative(icon,null,null,null);
        row.setCompoundDrawablePadding(dp(row.getContext(),14));
    }
    private static final class PreferenceSurface extends android.graphics.drawable.Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float density;
        PreferenceSurface(Context context){density=context.getResources().getDisplayMetrics().density;}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] states){invalidateSelf();return true;}
        @Override public void draw(Canvas canvas){
            boolean focused=false;
            for(int state:getState())focused|=state==android.R.attr.state_focused;
            float left=getBounds().left+2*density,top=getBounds().top+2*density;
            float right=getBounds().right-2*density,bottom=getBounds().bottom-2*density;
            paint.setStyle(Paint.Style.FILL);paint.setColor(focused?0xff3e6046:0x350c2b1d);
            canvas.drawRoundRect(left,top,right,bottom,6*density,6*density,paint);
            if(focused){
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*density);paint.setColor(AMBER);
                canvas.drawRoundRect(left,top,right,bottom,6*density,6*density,paint);
            }else{
                paint.setStrokeWidth(density);paint.setColor(0x25b5ceb0);
                canvas.drawLine(left+14*density,bottom,right-14*density,bottom,paint);
            }
        }
        @Override public void setAlpha(int alpha){}
        @Override public void setColorFilter(android.graphics.ColorFilter filter){}
        @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
    }
    static Button titleButton(Context context,String label,boolean primary,boolean quiet){
        Button button=button(context,label);
        button.setTextSize(quiet?20:22);
        button.setMinHeight(dp(context,quiet?48:56));
        button.setMinimumHeight(dp(context,quiet?48:56));
        button.setPadding(dp(context,20),dp(context,12),dp(context,20),dp(context,12));
        button.setBackground(new FocusSurface(context,primary,quiet));
        return button;
    }
    static final class TitleArtwork extends View {
        private final PiecePainter pieces;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        TitleArtwork(Context context){
            super(context);pieces=new PiecePainter(context);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            setFocusable(false);
        }
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            float size=Math.min(Math.min(getWidth(),getHeight()),dp(getContext(),340));
            float left=(getWidth()-size)/2,top=(getHeight()-size)/2;
            paint.setColor(0x22000000);
            canvas.drawOval(left+size*.06f,top+size*.69f,left+size*.96f,top+size*.80f,paint);
            pieces.draw(canvas,'n',left+size*.51f,top+size*.24f,size*.48f);
            pieces.draw(canvas,'K',left+size*.03f,top+size*.02f,size*.75f);
        }
    }
    private static final class FocusSurface extends android.graphics.drawable.Drawable {
        private final Paint p=new Paint(3);
        private final float d;
        private final boolean primary,quiet;
        FocusSurface(Context c){this(c,false,false);}
        FocusSurface(Context c,boolean primary,boolean quiet){
            this.primary=primary;this.quiet=quiet;
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
            p.setColor(focus?0xff3e6046:quiet?0x00000000:primary||checked?0xff34543d:0x451c492e);
            c.drawRoundRect(left,top,right,bottom,10*d,10*d,p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth((focus?2.5f:1.4f)*d);
            p.setColor(focus?AMBER:quiet?0x00000000:primary||checked?MUTED:0xaafff1d0);
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
