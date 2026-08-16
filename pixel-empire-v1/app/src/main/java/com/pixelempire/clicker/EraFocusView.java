package com.pixelempire.clicker;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

import java.lang.reflect.Field;

/** Premium focal monument overlay shown only on the WORLD screen. */
public final class EraFocusView extends View implements Runnable {
    private static final float W=720f;
    private final ReleaseGameState s;
    private final UltraGameView ultra;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap castleArt;
    private float scale=1f,logicalH=1500f;
    private boolean running;
    private Field tabField;

    public EraFocusView(Context c,ReleaseGameState state,UltraGameView ultra){
        super(c);s=state;this.ultra=ultra;setWillNotDraw(false);setBackgroundColor(Color.TRANSPARENT);
        castleArt=BitmapFactory.decodeResource(getResources(),R.drawable.castle_focus);
        try{tabField=PremiumGameView.class.getDeclaredField("tab");tabField.setAccessible(true);}catch(Throwable ignored){}
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();running=true;post(this);}
    @Override protected void onDetachedFromWindow(){running=false;removeCallbacks(this);super.onDetachedFromWindow();}
    @Override public void run(){if(!running)return;invalidate();postDelayed(this,s.lowPower?120:50);}
    @Override public boolean onTouchEvent(MotionEvent e){return false;}

    private boolean worldScreen(){try{View v=ultra.getChildAt(0);return !(v instanceof PremiumGameView)||tabField==null||tabField.getInt(v)==0;}catch(Throwable ignored){return true;}}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);if(!worldScreen())return;scale=getWidth()/W;if(scale<=0)scale=1;logicalH=getHeight()/scale;c.save();c.scale(scale,scale);
        float dashTop=Math.min(790f,Math.max(690f,logicalH*.51f)),ground=dashTop-35;
        if(s.stage>=10&&s.stage<=15)drawCastleArt(c,dashTop);
        else if(s.stage>=16&&s.stage<=20)industry(c,360,ground,.88f+(s.stage-16)*.025f);
        else if(s.stage>=21&&s.stage<=26)metropolis(c,360,ground,.88f+(s.stage-21)*.025f,false);
        else if(s.stage>=27&&s.stage<=32)metropolis(c,360,ground,.9f+(s.stage-27)*.025f,true);
        else if(s.stage>=33)infinity(c,360,ground,.83f+(s.stage-33)*.018f);
        c.restore();
    }

    private void drawCastleArt(Canvas c,float dashTop){
        if(castleArt==null)return;
        float h=Math.min(440,dashTop-210),w=h*(castleArt.getWidth()/(float)castleArt.getHeight());
        float left=360-w/2,top=dashTop-h-18;
        p.setColor(Color.argb(75,0,0,0));c.drawOval(new RectF(160,dashTop-60,560,dashTop-12),p);
        p.setAlpha(255);p.setFilterBitmap(true);c.drawBitmap(castleArt,null,new RectF(left,top,left+w,top+h),p);p.setFilterBitmap(false);p.setAlpha(255);
        glow(c,360,dashTop-h*.42f,125,Color.rgb(255,180,61),28);
    }

    private void industry(Canvas c,float x,float y,float g){
        shadow(c,x,y,205*g,32*g);int brick=Color.rgb(132,75,50),dark=Color.rgb(78,52,45),metal=Color.rgb(58,70,78),orange=Color.rgb(255,151,37);
        block(c,x,y-70*g,270*g,140*g,brick,Color.rgb(99,59,47),dark);
        // detailed brick rows
        for(int r=0;r<6;r++)for(int k=-5;k<=5;k++){float bx=x+k*23*g+(r%2)*11*g,by=y-128*g+r*19*g;p.setColor((r+k)%2==0?Color.rgb(151,84,54):Color.rgb(120,67,48));c.drawRect(bx-10*g,by,bx+9*g,by+5*g,p);}
        // windows
        for(int k=-4;k<=4;k+=2){float wx=x+k*27*g;p.setColor(orange);c.drawRoundRect(new RectF(wx-12*g,y-98*g,wx+12*g,y-65*g),3,3,p);glow(c,wx,y-82*g,22*g,orange,45);}
        // chimneys and smoke
        for(int i=-1;i<=1;i++){float sx=x+i*91*g,extra=i==0?30*g:0;p.setColor(metal);c.drawRect(sx-17*g,y-270*g-extra,sx+17*g,y-112*g,p);p.setColor(Color.rgb(36,44,49));c.drawRect(sx-21*g,y-278*g-extra,sx+21*g,y-258*g-extra,p);smoke(c,sx,y-292*g-extra,g,i);}
        // pipes and tanks
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(9*g);p.setColor(Color.rgb(99,127,134));Path pipe=new Path();pipe.moveTo(x-160*g,y-34*g);pipe.lineTo(x-160*g,y-180*g);pipe.lineTo(x-108*g,y-180*g);c.drawPath(pipe,p);p.setStyle(Paint.Style.FILL);
        for(int i=0;i<3;i++){float tx=x+105*g+i*29*g;p.setColor(Color.rgb(84,102,108));c.drawRoundRect(new RectF(tx-13*g,y-100*g,tx+13*g,y-25*g),12*g,12*g,p);}
        p.setColor(Color.rgb(38,210,236));c.drawRect(x-135*g,y-143*g,x+135*g,y-136*g,p);
    }

    private void metropolis(Canvas c,float x,float y,float g,boolean orbital){
        shadow(c,x,y,220*g,34*g);int[] hs={165,245,205,330,230,280,185};
        for(int i=0;i<7;i++){float xx=x+(i-3)*56*g,h=hs[i]*g+(orbital?45*g:0),w=(45+(i%2)*11)*g;int front=i%2==0?Color.rgb(42,65,91):Color.rgb(55,80,105);block(c,xx,y-h/2,w,h,front,Color.rgb(29,49,70),Color.rgb(24,42,64));
            for(float yy=y-h+19*g;yy<y-17*g;yy+=23*g){int col=(((int)(yy/20)+i)&1)==0?Color.rgb(43,221,244):Color.rgb(190,88,255);p.setColor(col);c.drawRoundRect(new RectF(xx-w*.29f,yy,xx+w*.29f,yy+6*g),3,3,p);}}
        if(orbital){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6*g);for(int i=0;i<2;i++){p.setColor(i==0?Color.rgb(59,224,249):Color.rgb(190,82,255));c.drawOval(new RectF(x-(180+i*40)*g,y-285*g-i*10*g,x+(180+i*40)*g,y-155*g+i*10*g),p);}p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);c.drawCircle(x,y-385*g,10*g,p);glow(c,x,y-385*g,55*g,Color.rgb(52,219,247),40);}
        long t=System.currentTimeMillis();for(int i=0;i<4;i++){float xx=100+((t/9+i*190)%540),yy=y-310*g+i*37*g;p.setColor(i%2==0?Color.rgb(255,204,54):Color.rgb(47,220,243));c.drawRoundRect(new RectF(xx-18*g,yy-5*g,xx+18*g,yy+5*g),5,5,p);}
    }

    private void infinity(Canvas c,float x,float y,float g){
        shadow(c,x,y,220*g,36*g);float h=500*g,w=176*g;Path q=new Path();q.moveTo(x-w/2,y);q.lineTo(x-w*.41f,y-h*.55f);q.lineTo(x-w*.17f,y-h);q.lineTo(x,y-h-120*g);q.lineTo(x+w*.17f,y-h);q.lineTo(x+w*.41f,y-h*.55f);q.lineTo(x+w/2,y);q.close();p.setShader(new LinearGradient(x-w/2,y-h,x+w/2,y,Color.rgb(13,25,52),Color.rgb(70,34,105),Shader.TileMode.CLAMP));c.drawPath(q,p);p.setShader(null);
        for(int i=0;i<16;i++){float yy=y-h+18*g+i*h/17;int col=i%2==0?Color.rgb(49,227,250):Color.rgb(198,82,255);p.setColor(col);c.drawRoundRect(new RectF(x-w*.31f,yy,x+w*.31f,yy+7*g),3,3,p);glow(c,x,yy,42*g,col,18);}
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6*g);for(int i=0;i<4;i++){float rx=(125+i*35)*g,ry=(40+i*9)*g;p.setColor(i%2==0?Color.rgb(54,225,250):Color.rgb(194,78,255));c.drawOval(new RectF(x-rx,y-h-45*g-ry,x+rx,y-h-45*g+ry),p);}p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE);c.drawCircle(x,y-h-124*g,12*g,p);p.setColor(Color.rgb(49,224,252));c.drawCircle(x,y-h-124*g,7*g,p);glow(c,x,y-h-124*g,65*g,Color.rgb(50,225,252),55);
        for(int i=-1;i<=1;i++){float px=x+i*175*g,py=y-115*g-Math.abs(i)*34*g;p.setColor(Color.argb(205,52,210,239));c.drawOval(new RectF(px-57*g,py-10*g,px+57*g,py+10*g),p);p.setColor(Color.rgb(22,32,55));c.drawRoundRect(new RectF(px-31*g,py-46*g,px+31*g,py-6*g),5,5,p);p.setColor(i==0?Color.rgb(195,83,255):Color.rgb(51,222,246));c.drawRect(px-24*g,py-38*g,px+24*g,py-33*g,p);}
    }

    private void block(Canvas c,float cx,float cy,float w,float h,int front,int left,int right){float x1=cx-w/2,x2=cx+w/2,y1=cy-h/2,y2=cy+h/2;p.setColor(front);c.drawRoundRect(new RectF(x1,y1,x2,y2),4,4,p);Path l=new Path();l.moveTo(x1,y1);l.lineTo(x1-18,y1-10);l.lineTo(x1-18,y2-10);l.lineTo(x1,y2);l.close();p.setColor(left);c.drawPath(l,p);Path r=new Path();r.moveTo(x2,y1);r.lineTo(x2+18,y1-10);r.lineTo(x2+18,y2-10);r.lineTo(x2,y2);r.close();p.setColor(right);c.drawPath(r,p);}
    private void smoke(Canvas c,float x,float y,float g,int seed){long t=System.currentTimeMillis();for(int i=0;i<5;i++){float drift=(float)Math.sin(t*.001+i+seed)*13*g;p.setColor(Color.argb(100-i*13,224,228,230));c.drawCircle(x+drift,y-i*29*g-(t%1400)/1400f*20*g,14*g+i*5*g,p);}}
    private void glow(Canvas c,float x,float y,float r,int col,int alpha){p.setShader(new RadialGradient(x,y,r,Color.argb(alpha,Color.red(col),Color.green(col),Color.blue(col)),Color.TRANSPARENT,Shader.TileMode.CLAMP));c.drawCircle(x,y,r,p);p.setShader(null);}
    private void shadow(Canvas c,float x,float y,float rx,float ry){p.setColor(Color.argb(105,0,0,0));c.drawOval(new RectF(x-rx,y-ry*.35f,x+rx,y+ry*.7f),p);}
}
