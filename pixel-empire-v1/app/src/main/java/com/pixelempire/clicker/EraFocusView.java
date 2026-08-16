package com.pixelempire.clicker;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;

/** High-impact focal monument layer. Drawn only on the WORLD screen, above the voxel landscape. */
public final class EraFocusView extends View implements Runnable {
    private static final float W=720f;
    private final ReleaseGameState s;
    private final UltraGameView ultra;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private float scale=1f, logicalH=1500f;
    private boolean running;
    private Field tabField;

    public EraFocusView(Context c, ReleaseGameState state, UltraGameView ultra){
        super(c);s=state;this.ultra=ultra;setWillNotDraw(false);setBackgroundColor(Color.TRANSPARENT);
        try{tabField=PremiumGameView.class.getDeclaredField("tab");tabField.setAccessible(true);}catch(Throwable ignored){}
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();running=true;post(this);}
    @Override protected void onDetachedFromWindow(){running=false;removeCallbacks(this);super.onDetachedFromWindow();}
    @Override public void run(){if(!running)return;invalidate();postDelayed(this,s.lowPower?120:50);}
    @Override public boolean onTouchEvent(MotionEvent e){return false;}

    private boolean worldScreen(){
        try{
            if(ultra.getChildCount()==0)return true;
            View v=ultra.getChildAt(0);
            if(v instanceof PremiumGameView&&tabField!=null)return tabField.getInt(v)==0;
        }catch(Throwable ignored){}
        return true;
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);if(!worldScreen())return;scale=getWidth()/W;if(scale<=0)scale=1;logicalH=getHeight()/scale;c.save();c.scale(scale,scale);
        float dashTop=Math.min(790f,Math.max(690f,logicalH*.51f));float ground=dashTop-42f;
        if(s.stage>=10&&s.stage<=15)castle(c,360,ground,0.88f+(s.stage-10)*.025f);
        else if(s.stage>=16&&s.stage<=20)industry(c,360,ground,0.90f+(s.stage-16)*.025f);
        else if(s.stage>=21&&s.stage<=24)city(c,360,ground,0.90f+(s.stage-21)*.03f);
        else if(s.stage>=25&&s.stage<=32)orbital(c,360,ground,0.88f+(s.stage-25)*.025f);
        else if(s.stage>=33)infinity(c,360,ground,0.82f+(s.stage-33)*.02f);
        c.restore();
    }

    private void castle(Canvas c,float x,float y,float g){
        shadow(c,x,y,175*g,30*g);int stone=Color.rgb(176,181,176),dark=Color.rgb(108,121,124),roof=Color.rgb(40,104,171),gold=Color.rgb(255,196,45);
        // rear keep
        block(c,x,y-86*g,165*g,112*g,stone,dark,Color.rgb(91,104,107));
        // towers
        for(int i=-1;i<=1;i+=2){float tx=x+i*132*g;block(c,tx,y-59*g,68*g,150*g,stone,dark,Color.rgb(88,103,107));blueRoof(c,tx,y-145*g,43*g,58*g,roof);crenels(c,tx-34*g,y-137*g,68*g,stone,g);}
        block(c,x,y-89*g,78*g,190*g,Color.rgb(190,193,187),dark,Color.rgb(93,107,109));blueRoof(c,x,y-196*g,49*g,70*g,roof);
        // gate / windows
        p.setColor(Color.rgb(47,34,26));c.drawRoundRect(new RectF(x-25*g,y-70*g,x+25*g,y),20*g,20*g,p);
        for(int row=0;row<3;row++)for(int col=-2;col<=2;col++){float wx=x+col*30*g,wy=y-122*g+row*28*g;p.setColor(gold);c.drawRoundRect(new RectF(wx-5*g,wy-8*g,wx+5*g,wy+8*g),3*g,3*g,p);glow(c,wx,wy,15*g,gold);}
        // bridge stones
        for(int i=-4;i<=4;i++){p.setColor(i%2==0?Color.rgb(170,159,133):Color.rgb(145,139,122));c.drawRoundRect(new RectF(x+i*25*g-12*g,y+4*g,x+i*25*g+12*g,y+18*g),3,3,p);}
        flag(c,x,y-277*g,g);
    }
    private void industry(Canvas c,float x,float y,float g){
        shadow(c,x,y,190*g,30*g);int brick=Color.rgb(126,71,48),metal=Color.rgb(64,74,80),gold=Color.rgb(255,164,43);
        block(c,x,y-65*g,245*g,125*g,brick,Color.rgb(92,55,44),Color.rgb(72,49,43));
        // saw-tooth roof
        Path q=new Path();q.moveTo(x-125*g,y-128*g);for(int i=0;i<5;i++){q.lineTo(x-125*g+i*50*g,y-164*g);q.lineTo(x-100*g+i*50*g,y-128*g);}q.lineTo(x+125*g,y-128*g);q.close();p.setColor(Color.rgb(91,88,78));c.drawPath(q,p);
        for(int i=-1;i<=1;i++){float sx=x+i*85*g;p.setColor(metal);c.drawRect(sx-15*g,y-255*g-(i==0?25*g:0),sx+15*g,y-105*g,p);p.setColor(Color.rgb(33,40,44));c.drawRect(sx-19*g,y-260*g-(i==0?25*g:0),sx+19*g,y-245*g-(i==0?25*g:0),p);smoke(c,sx,y-280*g-(i==0?25*g:0),g,i);}
        for(int i=0;i<5;i++){float wx=x-98*g+i*49*g;p.setColor(gold);c.drawRoundRect(new RectF(wx-12*g,y-97*g,wx+12*g,y-69*g),3,3,p);glow(c,wx,y-83*g,17*g,gold);}
        // pipes
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(10*g);p.setColor(Color.rgb(104,127,131));Path pipe=new Path();pipe.moveTo(x-155*g,y-40*g);pipe.lineTo(x-155*g,y-155*g);pipe.lineTo(x-105*g,y-155*g);c.drawPath(pipe,p);p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(46,202,227));c.drawRect(x-126*g,y-133*g,x+126*g,y-125*g,p);
    }
    private void city(Canvas c,float x,float y,float g){
        shadow(c,x,y,205*g,32*g);int[] heights={160,235,190,290,205,255,175};for(int i=0;i<7;i++){float xx=x+(i-3)*54*g,h=heights[i]*g,w=(43+(i%2)*10)*g;int body=i%2==0?Color.rgb(43,67,91):Color.rgb(57,82,105);block(c,xx,y-h/2,w,h,body,Color.rgb(31,51,71),Color.rgb(27,45,64));for(float yy=y-h+18*g;yy<y-16*g;yy+=24*g){p.setColor(((int)(yy/20)+i)%2==0?Color.rgb(45,215,239):Color.rgb(190,92,255));c.drawRoundRect(new RectF(xx-w*.28f,yy,xx+w*.28f,yy+6*g),3,3,p);}}
        p.setColor(Color.argb(130,50,220,244));c.drawOval(new RectF(x-205*g,y-72*g,x+205*g,y-52*g),p);
        // sky traffic
        long t=System.currentTimeMillis();for(int i=0;i<3;i++){float xx=x-170*g+((t/7+i*180)%440)*g*.75f,yy=y-310*g+i*45*g;p.setColor(i%2==0?Color.rgb(255,202,55):Color.rgb(51,217,240));c.drawRoundRect(new RectF(xx-18*g,yy-6*g,xx+18*g,yy+6*g),5,5,p);}
    }
    private void orbital(Canvas c,float x,float y,float g){
        city(c,x,y,g*.70f);float h=330*g;block(c,x,y-h/2,105*g,h,Color.rgb(29,42,70),Color.rgb(20,31,55),Color.rgb(16,25,48));for(int i=0;i<9;i++){float yy=y-h+25*g+i*34*g;p.setColor(i%2==0?Color.rgb(45,215,241):Color.rgb(178,83,250));c.drawRoundRect(new RectF(x-43*g,yy,x+43*g,yy+6*g),3,3,p);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6*g);for(int i=0;i<2;i++){p.setColor(i==0?Color.rgb(65,224,250):Color.rgb(181,77,255));c.drawOval(new RectF(x-(160+i*38)*g,y-250*g-i*12*g,x+(160+i*38)*g,y-135*g+i*12*g),p);}p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);c.drawCircle(x,y-h-28*g,10*g,p);glow(c,x,y-h-28*g,35*g,Color.rgb(57,218,245));}
    private void infinity(Canvas c,float x,float y,float g){
        shadow(c,x,y,210*g,34*g);float h=500*g,w=172*g;Path q=new Path();q.moveTo(x-w/2,y);q.lineTo(x-w*.40f,y-h*.56f);q.lineTo(x-w*.17f,y-h);q.lineTo(x,y-h-112*g);q.lineTo(x+w*.17f,y-h);q.lineTo(x+w*.40f,y-h*.56f);q.lineTo(x+w/2,y);q.close();p.setShader(new LinearGradient(x-w/2,y-h,x+w/2,y,Color.rgb(15,27,55),Color.rgb(68,35,102),Shader.TileMode.CLAMP));c.drawPath(q,p);p.setShader(null);
        for(int i=0;i<15;i++){float yy=y-h+22*g+i*h/16;p.setColor(i%2==0?Color.rgb(54,225,250):Color.rgb(196,84,255));c.drawRoundRect(new RectF(x-w*.31f,yy,x+w*.31f,yy+7*g),3,3,p);glow(c,x,yy,45*g,i%2==0?Color.rgb(54,225,250):Color.rgb(196,84,255));}
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6*g);for(int i=0;i<4;i++){float rx=(120+i*34)*g,ry=(43+i*8)*g;p.setColor(i%2==0?Color.rgb(55,224,250):Color.rgb(189,76,255));c.drawOval(new RectF(x-rx,y-h-38*g-ry,x+rx,y-h-38*g+ry),p);}p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE);c.drawCircle(x,y-h-116*g,12*g,p);p.setColor(Color.rgb(50,225,255));c.drawCircle(x,y-h-116*g,7*g,p);glow(c,x,y-h-116*g,55*g,Color.rgb(55,220,255));
        // hovering platforms
        for(int i=-1;i<=1;i++){float px=x+i*170*g,py=y-125*g-Math.abs(i)*32*g;p.setColor(Color.argb(190,54,205,235));c.drawOval(new RectF(px-55*g,py-10*g,px+55*g,py+10*g),p);p.setColor(Color.rgb(22,31,53));c.drawRect(px-28*g,py-42*g,px+28*g,py-6*g,p);}
    }

    private void block(Canvas c,float cx,float cy,float w,float h,int front,int left,int right){float x1=cx-w/2,x2=cx+w/2,y1=cy-h/2,y2=cy+h/2;p.setColor(front);c.drawRoundRect(new RectF(x1,y1,x2,y2),4,4,p);Path l=new Path();l.moveTo(x1,y1);l.lineTo(x1-18,y1-10);l.lineTo(x1-18,y2-10);l.lineTo(x1,y2);l.close();p.setColor(left);c.drawPath(l,p);Path r=new Path();r.moveTo(x2,y1);r.lineTo(x2+18,y1-10);r.lineTo(x2+18,y2-10);r.lineTo(x2,y2);r.close();p.setColor(right);c.drawPath(r,p);}
    private void blueRoof(Canvas c,float x,float y,float w,float h,int col){Path r=new Path();r.moveTo(x-w,y);r.lineTo(x,y-h);r.lineTo(x+w,y);r.close();p.setColor(col);c.drawPath(r,p);p.setColor(Color.argb(80,255,255,255));Path hi=new Path();hi.moveTo(x-w*.78f,y-3);hi.lineTo(x,y-h+8);hi.lineTo(x-w*.18f,y-3);hi.close();c.drawPath(hi,p);}
    private void crenels(Canvas c,float x,float y,float w,int col,float g){for(float xx=x;xx<x+w;xx+=18*g){p.setColor(col);c.drawRect(xx,y,Math.min(x+w,xx+11*g),y+13*g,p);}}
    private void flag(Canvas c,float x,float y,float g){p.setColor(Color.rgb(67,50,34));c.drawRect(x-2*g,y,x+2*g,y+65*g,p);p.setColor(Color.rgb(212,51,53));Path f=new Path();f.moveTo(x,y);f.lineTo(x+54*g,y+10*g);f.lineTo(x,y+25*g);f.close();c.drawPath(f,p);}
    private void smoke(Canvas c,float x,float y,float g,int seed){long t=System.currentTimeMillis();for(int i=0;i<5;i++){float drift=(float)Math.sin(t*.001+i+seed)*12*g;p.setColor(Color.argb(95-i*12,221,226,229));c.drawCircle(x+drift,y-i*28*g-(t%1400)/1400f*20*g,14*g+i*5*g,p);}}
    private void glow(Canvas c,float x,float y,float r,int col){p.setShader(new RadialGradient(x,y,r,Color.argb(75,Color.red(col),Color.green(col),Color.blue(col)),Color.TRANSPARENT,Shader.TileMode.CLAMP));c.drawCircle(x,y,r,p);p.setShader(null);}
    private void shadow(Canvas c,float x,float y,float rx,float ry){p.setColor(Color.argb(105,0,0,0));c.drawOval(new RectF(x-rx,y-ry*.35f,x+rx,y+ry*.7f),p);}
}
