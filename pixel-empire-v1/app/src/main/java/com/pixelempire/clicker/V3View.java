package com.pixelempire.clicker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class V3View extends View implements Runnable {
    private static final float W=540f;
    private static final int BG=Color.rgb(8,14,20),INK=Color.rgb(245,247,250),MUTED=Color.rgb(170,182,194);
    private static final int PANEL=Color.rgb(19,31,40),PANEL2=Color.rgb(27,44,56),LINE=Color.rgb(67,91,105);
    private static final int GOLD=Color.rgb(255,193,35),GREEN=Color.rgb(87,219,73),CYAN=Color.rgb(73,204,255),PURPLE=Color.rgb(185,98,255),RED=Color.rgb(245,74,69),ORANGE=Color.rgb(255,139,34);

    private final V3State state;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd=new Random(7);
    private final List<Hit> hits=new ArrayList<>();
    private final List<Particle> particles=new ArrayList<>();
    private final List<FloatText> floatTexts=new ArrayList<>();
    private final Vibrator vibrator;
    private ToneGenerator tone;

    private float scale=1f,logicalH=960f,downX,downY;
    private boolean running,moved,offlinePopup;
    private long lastFrameNs,lastAutosave,lastTapAt,toastUntil,resetArmedUntil,rebirthArmedUntil;
    private double secAcc;
    private int tab=0,buildPage=0,heroPage=0,techPage=0,missionPage=0,tutorialStep=0;
    private int comboClicks=0,lastSeenStage,lastSeenLevel;
    private String toast="";

    private static final class Hit{final RectF r;final String type;final int id;Hit(RectF r,String type,int id){this.r=r;this.type=type;this.id=id;}}
    private static final class Particle{float x,y,vx,vy,life,size;final int color;Particle(float x,float y,float vx,float vy,float life,float size,int color){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;this.size=size;this.color=color;}}
    private static final class FloatText{float x,y,life=1;final String s;final int color;FloatText(float x,float y,String s,int color){this.x=x;this.y=y;this.s=s;this.color=color;}}

    public V3View(Context context,V3State state){
        super(context);this.state=state;setFocusable(true);setBackgroundColor(BG);
        tp.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        vibrator=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,25);}catch(Throwable ignored){}
        offlinePopup=state.startupOfflineGain>0.01;lastSeenStage=state.stage;lastSeenLevel=state.level;
        if(state.nextEventAt<=0)state.nextEventAt=System.currentTimeMillis()+55_000;
    }

    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();running=true;lastFrameNs=System.nanoTime();post(this);}
    @Override protected void onDetachedFromWindow(){running=false;removeCallbacks(this);if(tone!=null){try{tone.release();}catch(Throwable ignored){}tone=null;}super.onDetachedFromWindow();}

    @Override public void run(){
        if(!running)return;long n=System.nanoTime();double dt=Math.min(.25,Math.max(0,(n-lastFrameNs)/1e9));lastFrameNs=n;
        state.tick(dt);secAcc+=dt;if(secAcc>=1){long s=(long)secAcc;state.playSeconds+=s;secAcc-=s;}
        updateFx((float)dt);progressFeedback();
        long now=System.currentTimeMillis();if(comboClicks>0&&now-lastTapAt>4000)comboClicks=0;
        if(lastAutosave==0||now-lastAutosave>5000){state.save(getContext());lastAutosave=now;}
        invalidate();postDelayed(this,state.lowPower?100:33);
    }

    private void updateFx(float dt){
        for(int i=particles.size()-1;i>=0;i--){Particle q=particles.get(i);q.life-=dt;q.x+=q.vx*dt;q.y+=q.vy*dt;q.vy+=75*dt;if(q.life<=0)particles.remove(i);}
        for(int i=floatTexts.size()-1;i>=0;i--){FloatText f=floatTexts.get(i);f.life-=dt*.7f;f.y-=35*dt;if(f.life<=0)floatTexts.remove(i);}
    }
    private void progressFeedback(){if(state.stage>lastSeenStage){lastSeenStage=state.stage;lastSeenLevel=state.level;showToast(V3L10n.t(state.language,"new_era")+"  "+V3L10n.stage(state.language,state.stage),2600);burst(270,350,CYAN,45);feedback(true);}else if(state.level>lastSeenLevel){lastSeenLevel=state.level;showToast(V3L10n.t(state.language,"level")+" "+state.level,1000);burst(270,410,GOLD,12);}}
    private void showToast(String s,long ms){toast=s;toastUntil=System.currentTimeMillis()+ms;}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);scale=getWidth()/W;if(scale<=0)scale=1;logicalH=getHeight()/scale;c.save();c.scale(scale,scale);hits.clear();
        p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(BG);c.drawRect(0,0,W,logicalH,p);
        float top=86,bottom=logicalH-76;
        if(tab==0)drawWorld(c,top,bottom);else{drawMenuBg(c);if(tab==1)drawBuild(c,top,bottom);else if(tab==2)drawHeroes(c,top,bottom);else if(tab==3)drawResearch(c,top,bottom);else if(tab==4)drawMissions(c,top,bottom);else drawEmpire(c,top,bottom);}
        drawTop(c);drawNav(c);drawFx(c);drawToast(c);if(offlinePopup)drawOffline(c);if(!state.tutorialSeen)drawTutorial(c);c.restore();
    }

    private void drawMenuBg(Canvas c){p.setShader(new LinearGradient(0,0,0,logicalH,Color.rgb(11,25,31),Color.rgb(10,16,26),Shader.TileMode.CLAMP));c.drawRect(0,0,W,logicalH,p);p.setShader(null);for(int i=0;i<28;i++){float x=(i*73)%540,y=100+(i*101)%780;voxelSpark(c,x,y,Color.argb(38,95,180,170));}}

    private void drawTop(Canvas c){
        p.setColor(Color.argb(245,11,22,27));c.drawRoundRect(new RectF(7,7,533,81),12,12,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(54,78,84));c.drawRoundRect(new RectF(7,7,533,81),12,12,p);p.setStyle(Paint.Style.FILL);
        resourcePill(c,15,14,175,47,GOLD,"●",state.format(state.coins),state.format(state.getCps())+V3L10n.t(state.language,"persec"));
        resourcePill(c,183,14,290,47,CYAN,"◆",Integer.toString(state.crystals),"");
        resourcePill(c,298,14,405,47,PURPLE,"◇",Integer.toString(state.science),"");
        resourcePill(c,413,14,525,47,GOLD,"★",Integer.toString(state.legacyStars),"");
        text(c,V3L10n.t(state.language,"era")+" "+(state.stage+1)+" • "+V3L10n.stage(state.language,state.stage),18,67,9,INK,Paint.Align.LEFT);
        text(c,V3L10n.t(state.language,"level")+" "+state.level+" / "+V3Content.MAX_LEVEL,522,67,9,MUTED,Paint.Align.RIGHT);
        progress(c,18,72,522,78,(float)state.levelProgress(),state.stage>=25?PURPLE:GOLD);
    }
    private void resourcePill(Canvas c,float x1,float y1,float x2,float y2,int col,String icon,String value,String sub){p.setColor(Color.rgb(25,39,46));c.drawRoundRect(new RectF(x1,y1,x2,y2),9,9,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(Color.rgb(74,96,102));c.drawRoundRect(new RectF(x1,y1,x2,y2),9,9,p);p.setStyle(Paint.Style.FILL);text(c,icon,x1+14,y1+21,15,col,Paint.Align.CENTER);text(c,value,x1+27,y1+19,12,INK,Paint.Align.LEFT);if(!sub.isEmpty())text(c,sub,x1+27,y1+30,7,GREEN,Paint.Align.LEFT);}

    private void drawWorld(Canvas c,float top,float bottom){
        hits.add(new Hit(new RectF(0,top,W,bottom),"tap",0));
        drawVoxelScene(c,top,bottom);
        drawWorldMissions(c,top+12);
        drawWorldEvents(c,top+12);
        drawBossHud(c,top+113);
        drawComboHud(c,bottom-72);
    }

    private void drawVoxelScene(Canvas c,float top,float bottom){
        boolean night=LocalTime.now().getHour()<6||LocalTime.now().getHour()>=20;
        int sky1=night?Color.rgb(15,33,55):Color.rgb(82,183,226),sky2=night?Color.rgb(47,45,86):Color.rgb(210,239,221);
        if(state.stage>=30){sky1=night?Color.rgb(9,16,46):Color.rgb(72,102,174);sky2=night?Color.rgb(45,20,78):Color.rgb(183,145,208);}
        p.setShader(new LinearGradient(0,top,0,bottom,sky1,sky2,Shader.TileMode.CLAMP));c.drawRect(0,top,W,bottom,p);p.setShader(null);
        drawSunAndClouds(c,top,night);drawDistantVoxelHills(c,top,bottom);
        float baseY=top+250;drawIsland(c,baseY,bottom);drawRiver(c,baseY,bottom);drawFoliage(c,baseY);
        drawEraSettlement(c,270,baseY+125,state.stage);
        drawVillagers(c,baseY+190);
        drawWaterfall(c,245,baseY+155,bottom-25);
        p.setShader(new LinearGradient(0,bottom-120,0,bottom,Color.argb(0,5,12,16),Color.argb(120,4,9,12),Shader.TileMode.CLAMP));c.drawRect(0,bottom-120,W,bottom,p);p.setShader(null);
    }

    private void drawSunAndClouds(Canvas c,float top,boolean night){if(night){p.setColor(Color.rgb(245,240,201));c.drawCircle(455,top+70,25,p);p.setColor(Color.rgb(45,49,84));c.drawCircle(465,top+62,24,p);for(int i=0;i<25;i++){p.setColor(Color.argb(170,235,245,255));c.drawRect((i*83)%520,top+22+(i*47)%170,(i*83)%520+2,top+24+(i*47)%170,p);}}else{p.setColor(Color.rgb(255,231,130));c.drawCircle(455,top+66,28,p);}float t=(System.currentTimeMillis()%24000L)/24000f;cloud(c,35+t*75,top+93,.8f);cloud(c,350-t*65,top+145,.65f);}
    private void cloud(Canvas c,float x,float y,float s){p.setColor(Color.argb(195,255,255,255));c.drawRoundRect(new RectF(x,y,x+72*s,y+20*s),9*s,9*s,p);c.drawCircle(x+22*s,y,s*14,p);c.drawCircle(x+46*s,y+2*s,s*17,p);}
    private void drawDistantVoxelHills(Canvas c,float top,float bottom){for(int layer=0;layer<3;layer++){int col=layer==0?Color.rgb(66,123,91):layer==1?Color.rgb(48,98,78):Color.rgb(35,74,66);float y=top+180+layer*45;Path h=new Path();h.moveTo(0,bottom);for(int x=0;x<=540;x+=45){float yy=y+(float)(Math.sin((x+layer*37)*.035)*25);h.lineTo(x,yy);}h.lineTo(540,bottom);h.close();p.setColor(col);c.drawPath(h,p);}}

    private void drawIsland(Canvas c,float baseY,float bottom){
        for(int r=0;r<10;r++)for(int col=0;col<12;col++){
            float x=270+(col-r)*24;float y=baseY+(col+r)*11;
            if(x<-40||x>580)continue;
            boolean river=Math.abs((col-r)-1)<=1;
            int top=river?Color.rgb(86,177,193):((r+col)%4==0?Color.rgb(93,177,68):Color.rgb(76,160,61));
            isoBlock(c,x,y,24,10,top,river?Color.rgb(55,126,151):Color.rgb(80,102,56),river?Color.rgb(45,106,130):Color.rgb(62,82,43));
        }
        p.setColor(Color.rgb(68,57,43));c.drawRect(0,bottom-26,W,bottom,p);
    }
    private void isoBlock(Canvas c,float cx,float cy,float w,float h,int top,int left,int right){Path a=new Path();a.moveTo(cx,cy-w*.28f);a.lineTo(cx+w,cy);a.lineTo(cx,cy+w*.28f);a.lineTo(cx-w,cy);a.close();p.setColor(top);c.drawPath(a,p);Path l=new Path();l.moveTo(cx-w,cy);l.lineTo(cx,cy+w*.28f);l.lineTo(cx,cy+w*.28f+h);l.lineTo(cx-w,cy+h);l.close();p.setColor(left);c.drawPath(l,p);Path r=new Path();r.moveTo(cx+w,cy);r.lineTo(cx,cy+w*.28f);r.lineTo(cx,cy+w*.28f+h);r.lineTo(cx+w,cy+h);r.close();p.setColor(right);c.drawPath(r,p);}
    private void drawRiver(Canvas c,float baseY,float bottom){p.setColor(Color.argb(95,210,250,255));for(int i=0;i<12;i++){float y=baseY+15+i*23;c.drawRoundRect(new RectF(215+i*2,y,330+i*2,y+5),3,3,p);}}
    private void drawWaterfall(Canvas c,float x,float y,float bottom){p.setShader(new LinearGradient(x,y,x,bottom,Color.rgb(120,225,242),Color.rgb(57,142,180),Shader.TileMode.CLAMP));c.drawRect(x-25,y,x+25,bottom,p);p.setShader(null);for(int i=0;i<5;i++){p.setColor(Color.argb(120,235,255,255));float xx=x-20+i*10;c.drawRect(xx,y,xx+4,bottom-12-(i%2)*15,p);}p.setColor(Color.argb(180,220,250,255));c.drawOval(new RectF(x-50,bottom-18,x+55,bottom+7),p);}

    private void drawFoliage(Canvas c,float baseY){int[] xs={60,112,410,470,145,390,70,485};int[] ys={170,240,205,290,330,350,385,420};for(int i=0;i<xs.length;i++)voxelTree(c,xs[i],baseY-115+ys[i]*.55f,.72f+(i%3)*.08f,i%4==3?Color.rgb(178,102,53):Color.rgb(67,151,59));for(int i=0;i<24;i++){float x=35+(i*91)%470,y=baseY+40+(i*47)%170;p.setColor(i%3==0?Color.rgb(255,205,78):i%3==1?Color.rgb(224,99,154):Color.rgb(242,244,236));c.drawCircle(x,y,2.5f,p);}}
    private void voxelTree(Canvas c,float x,float y,float s,int leaf){p.setColor(Color.rgb(91,61,40));c.drawRect(x-4*s,y,x+4*s,y+44*s,p);cube(c,x,y-5*s,24*s,leaf);cube(c,x-14*s,y+8*s,17*s,darker(leaf,.87f));cube(c,x+15*s,y+7*s,18*s,lighter(leaf,1.07f));}
    private void cube(Canvas c,float x,float y,float s,int col){isoBlock(c,x,y,s*.55f,s*.55f,col,darker(col,.78f),darker(col,.65f));}

    private void drawEraSettlement(Canvas c,float x,float ground,int stage){
        if(stage<5){drawHut3d(c,x,ground,1f+stage*.05f);if(stage>=3)drawHut3d(c,x-105,ground+22,.6f);}
        else if(stage<9){for(int i=-2;i<=2;i++)drawHut3d(c,x+i*80,ground+Math.abs(i)*13,.52f+(i==0?.18f:0));drawTownHall(c,x,ground-8,.75f);}
        else if(stage<15){drawCastle3d(c,x,ground,1f+(stage-9)*.035f);for(int i=0;i<Math.min(4,stage-9);i++)drawHut3d(c,75+i*115,ground+45,.42f);}
        else if(stage<21){drawFactory3d(c,x,ground,1f+(stage-15)*.03f);for(int i=0;i<3;i++)drawFactory3d(c,95+i*170,ground+55,.35f);}
        else if(stage<27){drawCity3d(c,x,ground,1f+(stage-21)*.035f);}
        else{drawFuture3d(c,x,ground,1f+(stage-27)*.025f,stage);}
    }
    private void drawHut3d(Canvas c,float x,float y,float s){shadow(c,x,y,70*s,22*s);p.setColor(Color.rgb(138,88,48));c.drawRect(x-42*s,y-54*s,x+42*s,y,p);p.setColor(Color.rgb(91,55,34));Path roof=new Path();roof.moveTo(x-53*s,y-54*s);roof.lineTo(x,y-92*s);roof.lineTo(x+53*s,y-54*s);roof.close();c.drawPath(roof,p);p.setColor(Color.rgb(246,205,96));c.drawRect(x-28*s,y-38*s,x-10*s,y-20*s,p);p.setColor(Color.rgb(59,39,29));c.drawRect(x+14*s,y-40*s,x+30*s,y,p);p.setColor(Color.rgb(169,112,59));for(int i=0;i<5;i++)c.drawRect(x-38*s+i*16*s,y-51*s,x-32*s+i*16*s,y-4*s,p);}
    private void drawTownHall(Canvas c,float x,float y,float s){shadow(c,x,y,86*s,25*s);p.setColor(Color.rgb(172,158,120));c.drawRect(x-55*s,y-88*s,x+55*s,y,p);p.setColor(Color.rgb(91,76,62));c.drawRect(x-66*s,y-99*s,x+66*s,y-84*s,p);p.setColor(Color.rgb(226,184,67));c.drawRect(x-8*s,y-58*s,x+8*s,y,p);p.setColor(Color.rgb(181,55,55));c.drawRect(x,y-130*s,x+40*s,y-116*s,p);c.drawRect(x-3*s,y-131*s,x+3*s,y-78*s,p);}
    private void drawCastle3d(Canvas c,float x,float y,float s){shadow(c,x,y,125*s,34*s);int stone=Color.rgb(177,180,171);p.setColor(stone);c.drawRect(x-95*s,y-112*s,x+95*s,y,p);for(int i=-1;i<=1;i+=2){float tx=x+i*88*s;p.setColor(darker(stone,.85f));c.drawRect(tx-28*s,y-162*s,tx+28*s,y,p);crenel(c,tx-28*s,y-173*s,56*s,stone,s);}crenel(c,x-95*s,y-123*s,190*s,stone,s);p.setColor(Color.rgb(62,49,42));c.drawRoundRect(new RectF(x-18*s,y-52*s,x+18*s,y),15*s,15*s,p);p.setColor(GOLD);for(int i=-2;i<=2;i++)c.drawRect(x+i*31*s-5*s,y-90*s,x+i*31*s+5*s,y-74*s,p);}
    private void crenel(Canvas c,float x,float y,float w,int col,float s){p.setColor(col);for(float xx=x;xx<x+w;xx+=18*s)c.drawRect(xx,y,Math.min(xx+11*s,x+w),y+13*s,p);}
    private void drawFactory3d(Canvas c,float x,float y,float s){shadow(c,x,y,120*s,30*s);p.setColor(Color.rgb(77,84,91));c.drawRect(x-105*s,y-105*s,x+105*s,y,p);p.setColor(Color.rgb(47,54,61));c.drawRect(x-80*s,y-170*s,x-50*s,y,p);c.drawRect(x+55*s,y-145*s,x+80*s,y,p);p.setColor(Color.rgb(255,192,67));for(int i=0;i<5;i++)c.drawRect(x-78*s+i*36*s,y-80*s,x-58*s+i*36*s,y-57*s,p);float smoke=(System.currentTimeMillis()%5000L)/5000f;for(int i=0;i<4;i++){p.setColor(Color.argb(95-i*13,225,228,230));c.drawCircle(x-65*s+i*8,y-185*s-i*20-smoke*15,14+i*4,p);}}
    private void drawCity3d(Canvas c,float x,float y,float s){shadow(c,x,y,145*s,34*s);int[] hs={115,175,135,215,150};for(int i=0;i<5;i++){float xx=x+(i-2)*48*s,h=hs[i]*s;p.setColor(i%2==0?Color.rgb(68,93,111):Color.rgb(85,110,128));c.drawRect(xx-19*s,y-h,xx+19*s,y,p);for(int yy=0;yy<h-20;yy+=24*s){p.setColor(Color.rgb(113,216,225));c.drawRect(xx-11*s,y-h+12*s+yy,xx+11*s,y-h+17*s+yy,p);}}}
    private void drawFuture3d(Canvas c,float x,float y,float s,int stage){shadow(c,x,y,150*s,36*s);float h=(180+(stage-27)*13)*s;p.setColor(Color.rgb(34,50,68));c.drawRect(x-55*s,y-h,x+55*s,y,p);p.setColor(Color.rgb(77,105,128));c.drawRect(x-28*s,y-h-65*s,x+28*s,y,p);for(int i=0;i<9;i++){p.setColor(i%2==0?CYAN:PURPLE);float yy=y-h+18*s+i*25*s;c.drawRect(x-43*s,yy,x+43*s,yy+5*s,p);}if(stage>=32){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5*s);p.setColor(Color.argb(190,118,218,255));c.drawOval(new RectF(x-130*s,y-h*.78f,x+130*s,y-h*.38f),p);p.setStyle(Paint.Style.FILL);}if(stage>=35){for(int i=0;i<3;i++){float xx=x+(i-1)*120*s;p.setColor(Color.argb(100,130,220,255));c.drawOval(new RectF(xx-50*s,y-105*s,xx+50*s,y-88*s),p);}}if(stage>=39){p.setColor(Color.rgb(238,245,255));c.drawRect(x-6*s,y-h-145*s,x+6*s,y-h,p);p.setColor(CYAN);c.drawRect(x-2*s,y-h-220*s,x+2*s,y-h-145*s,p);for(int i=0;i<4;i++){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(i%2==0?CYAN:PURPLE);c.drawCircle(x,y-h-35*s,72*s+i*24*s,p);}p.setStyle(Paint.Style.FILL);}}
    private void shadow(Canvas c,float x,float y,float rx,float ry){p.setColor(Color.argb(75,0,0,0));c.drawOval(new RectF(x-rx,y-ry*.35f,x+rx,y+ry*.7f),p);}
    private void drawVillagers(Canvas c,float ground){int count=Math.min(8,2+state.stage/5);float t=(System.currentTimeMillis()%10000L)/10000f;for(int i=0;i<count;i++){float x=((t*620+i*89)%620)-40,y=ground+(i%3)*17;villager(c,x,y,i);}}
    private void villager(Canvas c,float x,float y,int i){p.setColor(Color.rgb(235,196,155));c.drawRect(x-3,y-14,x+3,y-8,p);p.setColor(i%3==0?Color.rgb(58,108,183):i%3==1?Color.rgb(189,78,75):Color.rgb(67,145,89));c.drawRect(x-5,y-8,x+5,y+3,p);p.setColor(Color.rgb(39,42,44));c.drawRect(x-4,y+3,x-1,y+10,p);c.drawRect(x+1,y+3,x+4,y+10,p);}

    private void drawWorldMissions(Canvas c,float y){for(int k=0;k<2;k++){int idx=nextMission(k);if(idx<0)continue;float yy=y+k*54;panel(c,10,yy,188,yy+47,Color.argb(220,16,31,38),Color.rgb(75,101,106),8);text(c,V3L10n.t(state.language,"mission")+" #"+(idx+1),20,yy+16,7,GOLD,Paint.Align.LEFT);text(c,shorten(state.missionText(idx),24),20,yy+30,7.5f,INK,Paint.Align.LEFT);progress(c,20,yy+36,172,yy+42,(float)Math.min(1,state.missionValue(idx)/state.missionTarget(idx)),GREEN);}}
    private int nextMission(int offset){int found=0;for(int i=0;i<state.missionCount();i++)if(!state.missionClaimed[i]){if(found==offset)return i;found++;}return -1;}
    private String shorten(String s,int n){return s.length()<=n?s:s.substring(0,n-1)+"…";}

    private void drawWorldEvents(Canvas c,float y){float x1=386,x2=530;if(state.canClaimDaily()){panel(c,x1,y,x2,y+43,Color.argb(228,32,43,35),GOLD,9);text(c,"🎁  "+V3L10n.t(state.language,"daily"),458,y+18,8,GOLD,Paint.Align.CENTER);text(c,V3L10n.t(state.language,"claim"),458,y+34,8,GREEN,Paint.Align.CENTER);hits.add(new Hit(new RectF(x1,y,x2,y+43),"daily",0));y+=50;}if(state.activeEvent>=0){int col=state.activeEvent==0?GOLD:state.activeEvent==1?PURPLE:state.activeEvent==2?CYAN:ORANGE;String key=state.activeEvent==0?"gold_rush":state.activeEvent==1?"crystal_rain":state.activeEvent==2?"build_fever":"xp_boost";panel(c,x1,y,x2,y+50,Color.argb(232,19,31,42),col,9);text(c,V3L10n.t(state.language,key),458,y+18,8,col,Paint.Align.CENTER);long sec=Math.max(0,(state.activeEventUntil-System.currentTimeMillis()+999)/1000);text(c,sec+"s • TAP",458,y+37,8,INK,Paint.Align.CENTER);hits.add(new Hit(new RectF(x1,y,x2,y+50),"event",0));}}

    private void drawBossHud(Canvas c,float y){if(state.stage<2)return;if(state.bossActive){panel(c,120,y,420,y+50,Color.argb(228,36,24,30),RED,9);text(c,V3L10n.t(state.language,"boss")+" • "+V3L10n.stage(state.language,state.bossStage),270,y+16,8,INK,Paint.Align.CENTER);progress(c,138,y+25,402,y+34,(float)(state.bossHp/Math.max(1,state.bossMaxHp)),RED);long sec=Math.max(0,(state.bossEndAt-System.currentTimeMillis()+999)/1000);text(c,state.format(state.bossHp)+" HP • "+sec+"s",270,y+45,7,MUTED,Paint.Align.CENTER);}else{button(c,205,y,335,y+36,V3L10n.t(state.language,"fight_boss"),Color.rgb(114,49,42),INK,true);hits.add(new Hit(new RectF(205,y,335,y+36),"boss",0));}}

    private double comboMult(){return Math.min(5.0,1.0+Math.floor(comboClicks/50.0)*0.1);}
    private void drawComboHud(Canvas c,float y){double m=comboMult();int base=(comboClicks/50)*50;int next=base+50;float prog=(comboClicks-base)/50f;panel(c,54,y,486,y+58,Color.argb(225,12,24,30),Color.rgb(92,115,118),10);text(c,V3L10n.t(state.language,"combo")+" x"+String.format(Locale.US,"%.1f",m),72,y+22,15,GOLD,Paint.Align.LEFT);text(c,comboClicks+" / "+next+" "+V3L10n.t(state.language,"clicks"),470,y+20,8,INK,Paint.Align.RIGHT);progress(c,72,y+32,470,y+40,prog,GOLD);text(c,"x"+String.format(Locale.US,"%.1f",Math.min(5,m+.1))+" "+V3L10n.t(state.language,"next_combo"),270,y+53,7,MUTED,Paint.Align.CENTER);}

    private void drawBuild(Canvas c,float top,float bottom){title(c,V3L10n.t(state.language,"build"),V3L10n.t(state.language,"income")+" "+state.format(state.getCps())+V3L10n.t(state.language,"persec"),top);drawBuyModes(c,top+43);List<V3Content.BuildingDef> b=V3Content.buildings();int per=5,pages=(b.size()+per-1)/per;buildPage=clamp(buildPage,0,pages-1);float y=top+91;for(int j=0;j<per;j++){int i=buildPage*per+j;if(i>=b.size())break;V3Content.BuildingDef d=b.get(i);float y2=y+100;boolean unlock=state.stage>=d.unlockStage;int n=state.selectedBuyCount(i);double cost=n>0?state.buildingCost(i,n):Double.POSITIVE_INFINITY;panel(c,12,y,528,y2,PANEL,unlock?GREEN:LINE,11);buildingIcon(c,48,y+50,i,unlock?GREEN:MUTED);text(c,V3L10n.building(state.language,d.id),82,y+28,13,unlock?INK:MUTED,Paint.Align.LEFT);text(c,V3L10n.t(state.language,"lvl")+" "+state.buildings[i]+" • "+state.format(d.baseCps*Math.max(1,state.buildings[i]))+V3L10n.t(state.language,"persec"),82,y+50,8,MUTED,Paint.Align.LEFT);progress(c,82,y+62,338,y+70,(float)Math.min(1,state.coins/Math.max(1,cost)),GREEN);String buy=n<=0?V3L10n.t(state.language,"buy"):V3L10n.t(state.language,"buy")+" x"+n;button(c,355,y+24,510,y+73,buy,unlock&&state.coins>=cost?Color.rgb(52,165,47):Color.rgb(53,63,67),INK,unlock);text(c,unlock?state.format(cost):V3L10n.t(state.language,"locked"),432,y+91,8,unlock?GOLD:MUTED,Paint.Align.CENTER);if(unlock)hits.add(new Hit(new RectF(345,y+12,520,y+92),"building",i));y=y2+7;}pager(c,bottom,pages,buildPage,"build_prev","build_next");}
    private void drawBuyModes(Canvas c,float y){int[] modes={1,10,25,100,-1};String[] s={"x1","x10","x25","x100","MAX"};float w=96;for(int i=0;i<modes.length;i++){float x=18+i*101;boolean on=state.buyMode==modes[i];button(c,x,y,x+w,y+36,s[i],on?Color.rgb(199,122,34):PANEL2,on?INK:MUTED,true);hits.add(new Hit(new RectF(x,y,x+w,y+36),"buy_mode",modes[i]));}}
    private void buildingIcon(Canvas c,float x,float y,int i,int col){p.setColor(Color.rgb(35,51,58));c.drawCircle(x,y,28,p);p.setColor(col);if(i<3){c.drawRect(x-16,y-5,x+16,y+14,p);Path r=new Path();r.moveTo(x-20,y-5);r.lineTo(x,y-23);r.lineTo(x+20,y-5);r.close();c.drawPath(r,p);}else if(i<8){c.drawRect(x-17,y-17,x+17,y+16,p);for(int k=-1;k<=1;k++)c.drawRect(x+k*12-3,y-26,x+k*12+3,y-17,p);}else{c.drawRect(x-18,y-22,x+18,y+18,p);p.setColor(CYAN);for(int yy=-14;yy<14;yy+=10)c.drawRect(x-12,y+yy,x+12,y+yy+3,p);}}

    private void drawHeroes(Canvas c,float top,float bottom){title(c,V3L10n.t(state.language,"heroes"),state.totalHeroLevels()+" "+V3L10n.t(state.language,"lvl"),top);List<V3Content.HeroDef> list=V3Content.heroes();int per=4,pages=(list.size()+per-1)/per;heroPage=clamp(heroPage,0,pages-1);float y=top+52;for(int j=0;j<per;j++){int i=heroPage*per+j;if(i>=list.size())break;V3Content.HeroDef h=list.get(i);float y2=y+125;boolean unlock=state.stage>=h.unlockStage;int rarity=rarityColor(h.rarity);panel(c,12,y,528,y2,PANEL,unlock?rarity:LINE,12);heroPortrait(c,62,y+61,i,rarity,unlock);text(c,heroName(i),110,y+27,14,unlock?INK:MUTED,Paint.Align.LEFT);text(c,h.role+" • "+rarityName(h.rarity),110,y+49,8,rarity,Paint.Align.LEFT);text(c,V3L10n.t(state.language,"lvl")+" "+state.heroes[i],110,y+73,10,INK,Paint.Align.LEFT);double power=h.basePower*Math.max(1,state.heroes[i])*Math.pow(1.055,Math.max(0,state.heroes[i]-1));text(c,V3L10n.t(state.language,"dps")+" "+state.format(power)+V3L10n.t(state.language,"persec"),110,y+96,9,GREEN,Paint.Align.LEFT);double cost=state.heroCost(i);button(c,360,y+35,505,y+85,V3L10n.t(state.language,"upgrade"),unlock&&state.coins>=cost?Color.rgb(52,165,47):Color.rgb(53,63,67),INK,unlock);text(c,unlock?state.format(cost):V3L10n.t(state.language,"locked"),432,y+107,8,unlock?GOLD:MUTED,Paint.Align.CENTER);if(unlock)hits.add(new Hit(new RectF(350,y+20,515,y+112),"hero",i));y=y2+9;}pager(c,bottom,pages,heroPage,"hero_prev","hero_next");}
    private String heroName(int i){String[] n={"Mira","Borin","Luna","Roland","Ada","Marco","Ignis","Tink","Nova","Aion","Astra","Omega"};return n[i];}
    private int rarityColor(int r){return r==0?Color.rgb(180,190,195):r==1?GREEN:r==2?CYAN:r==3?PURPLE:r==4?GOLD:Color.rgb(255,106,196);}
    private String rarityName(int r){String[] x={"COMMON","RARE","EPIC","LEGENDARY","MYTHIC","INFINITY"};return x[Math.max(0,Math.min(x.length-1,r))];}
    private void heroPortrait(Canvas c,float x,float y,int i,int col,boolean unlock){p.setColor(Color.rgb(35,45,52));c.drawCircle(x,y,39,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(unlock?col:LINE);c.drawCircle(x,y,39,p);p.setStyle(Paint.Style.FILL);p.setColor(unlock?Color.rgb(235,196,155):Color.rgb(85,92,96));c.drawCircle(x,y-7,14,p);p.setColor(unlock?col:Color.rgb(72,78,82));c.drawRect(x-21,y+7,x+21,y+31,p);p.setColor(Color.rgb(30,34,36));c.drawRect(x-13,y-24,x+13,y-17,p);}

    private void drawResearch(Canvas c,float top,float bottom){title(c,V3L10n.t(state.language,"tech_tree"),V3L10n.t(state.language,"science")+" "+state.science,top);List<V3Content.TechDef> tech=V3Content.techs();int per=10,pages=(tech.size()+per-1)/per;techPage=clamp(techPage,0,pages-1);float areaTop=top+56,areaBottom=bottom-52;panel(c,10,areaTop,530,areaBottom,Color.rgb(13,28,37),Color.rgb(50,83,92),12);int start=techPage*per,end=Math.min(tech.size(),start+per);float[] xs={95,270,445,145,360,95,270,445,145,360};float[] ys={areaTop+75,areaTop+75,areaTop+75,areaTop+185,areaTop+185,areaTop+300,areaTop+300,areaTop+300,areaTop+415,areaTop+415};for(int i=start;i<end;i++){int local=i-start;V3Content.TechDef d=tech.get(i);if(d.parent>=start&&d.parent<end){int pl=d.parent-start;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(state.techLevels[d.parent]>0?GREEN:Color.rgb(66,82,87));c.drawLine(xs[pl],ys[pl]+31,xs[local],ys[local]-31,p);p.setStyle(Paint.Style.FILL);}}
        for(int i=start;i<end;i++){int local=i-start;drawTechNode(c,i,xs[local],ys[local]);}
        pager(c,bottom,pages,techPage,"tech_prev","tech_next");}
    private void drawTechNode(Canvas c,int i,float x,float y){V3Content.TechDef d=V3Content.techs().get(i);boolean unlock=state.techUnlocked(i),max=state.techLevels[i]>=d.maxLevel;int col=max?GREEN:unlock?ORANGE:Color.rgb(93,98,101);p.setColor(Color.rgb(31,38,40));c.drawCircle(x,y,36,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(col);c.drawCircle(x,y,36,p);p.setStyle(Paint.Style.FILL);techIcon(c,x,y-4,d.kind,col);text(c,state.techLevels[i]+"/"+d.maxLevel,x,y+48,8,max?GREEN:INK,Paint.Align.CENTER);text(c,techName(i),x,y+63,7.3f,unlock?INK:MUTED,Paint.Align.CENTER);if(unlock&&!max){text(c,state.techCost(i)+"◇",x,y+78,7,PURPLE,Paint.Align.CENTER);hits.add(new Hit(new RectF(x-44,y-44,x+44,y+84),"tech",i));}}
    private void techIcon(Canvas c,float x,float y,int kind,int col){p.setColor(col);if(kind==0){c.drawRect(x-16,y-3,x+16,y+3,p);c.drawRect(x+5,y-17,x+11,y+16,p);}else if(kind==1){c.drawRect(x-16,y-15,x+16,y+15,p);p.setColor(Color.rgb(31,38,40));c.drawRect(x-8,y-8,x+8,y+8,p);}else if(kind==2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(col);c.drawCircle(x,y,15,p);c.drawLine(x-20,y,x+20,y,p);c.drawLine(x,y-20,x,y+20,p);p.setStyle(Paint.Style.FILL);}else if(kind==3){Path a=new Path();a.moveTo(x,y-18);a.lineTo(x+17,y+15);a.lineTo(x-17,y+15);a.close();c.drawPath(a,p);}else{diamond(c,x,y,16,col);}}
    private String techName(int i){String[] n={"Narzędzia","Zbieractwo","Planowanie","Handel","Metalurgia","Architektura","Gildie","Para","Elektryczność","Logistyka","Komputery","Automatyzacja","Fuzja","Nanity","AI","Orbita","Kwanty","Czas","Antymateria","Singularność"};return n[i];}

    private void drawMissions(Canvas c,float top,float bottom){title(c,V3L10n.t(state.language,"missions"),state.missionsClaimed+"/"+state.missionCount()+" • 🏆 "+state.achievementCount()+"/16",top);int per=5,pages=(state.missionCount()+per-1)/per;missionPage=clamp(missionPage,0,pages-1);float y=top+55;for(int j=0;j<per;j++){int i=missionPage*per+j;if(i>=state.missionCount())break;float y2=y+105;boolean ready=state.missionReady(i),done=state.missionClaimed[i];panel(c,12,y,528,y2,PANEL,done?GREEN:ready?GOLD:LINE,11);text(c,"#"+(i+1)+"  "+state.missionText(i),28,y+26,11,done?MUTED:INK,Paint.Align.LEFT);float val=(float)Math.min(1,state.missionValue(i)/state.missionTarget(i));progress(c,28,y+43,375,y+52,val,done?GREEN:GOLD);text(c,state.format(state.missionValue(i))+" / "+state.format(state.missionTarget(i)),28,y+70,8,MUTED,Paint.Align.LEFT);text(c,"+"+state.missionCrystalReward(i)+" ◆  +"+state.missionScienceReward(i)+" ◇",500,y+31,9,CYAN,Paint.Align.RIGHT);if(done)text(c,V3L10n.t(state.language,"complete"),500,y+72,9,GREEN,Paint.Align.RIGHT);else if(ready){button(c,393,y+57,510,y+92,V3L10n.t(state.language,"claim"),Color.rgb(52,165,47),INK,true);hits.add(new Hit(new RectF(385,y+50,518,y+99),"mission",i));}y=y2+8;}pager(c,bottom,pages,missionPage,"mission_prev","mission_next");}

    private void drawEmpire(Canvas c,float top,float bottom){title(c,V3L10n.t(state.language,"empire"),"Pixel Empire 3.0",top);float y=top+52;int gain=state.availableLegacyStars();panel(c,12,y,528,y+116,PANEL,gain>0?PURPLE:LINE,12);text(c,V3L10n.t(state.language,"prestige"),28,y+28,17,PURPLE,Paint.Align.LEFT);text(c,V3L10n.t(state.language,"prestige_desc"),28,y+50,8,MUTED,Paint.Align.LEFT);text(c,"+"+gain+" ★",500,y+32,12,gain>0?GOLD:MUTED,Paint.Align.RIGHT);button(c,28,y+70,512,y+104,System.currentTimeMillis()<rebirthArmedUntil?V3L10n.t(state.language,"prestige")+"?":V3L10n.t(state.language,"prestige"),gain>0?Color.rgb(122,57,164):Color.rgb(55,61,66),INK,gain>0);if(gain>0)hits.add(new Hit(new RectF(28,y+68,512,y+106),"rebirth",0));y+=129;
        panel(c,12,y,528,y+73,PANEL,CYAN,10);text(c,V3L10n.t(state.language,"offline"),28,y+25,12,CYAN,Paint.Align.LEFT);text(c,V3L10n.t(state.language,"offline_rule"),28,y+48,8,INK,Paint.Align.LEFT);text(c,"0.2× = "+state.format(state.getCps(false)*.2)+V3L10n.t(state.language,"persec"),500,y+64,8,GREEN,Paint.Align.RIGHT);y+=86;
        setting(c,y,V3L10n.t(state.language,"sound"),state.soundEnabled,"sound");y+=54;setting(c,y,V3L10n.t(state.language,"haptics"),state.hapticsEnabled,"haptics");y+=54;setting(c,y,V3L10n.t(state.language,"power"),state.lowPower,"power");y+=54;
        panel(c,12,y,528,y+50,PANEL,LINE,9);text(c,V3L10n.t(state.language,"language"),28,y+31,10,INK,Paint.Align.LEFT);text(c,V3L10n.languageName(state.language)+" ›",500,y+31,10,CYAN,Paint.Align.RIGHT);hits.add(new Hit(new RectF(12,y,528,y+50),"lang",0));y+=61;
        float sh=Math.min(124,bottom-y-60);panel(c,12,y,528,y+sh,PANEL,LINE,10);stat(c,y+25,V3L10n.t(state.language,"total_earned"),state.format(state.lifetimeCoins));stat(c,y+48,V3L10n.t(state.language,"total_taps"),Long.toString(state.totalTaps));stat(c,y+71,V3L10n.t(state.language,"best_combo"),"x"+String.format(Locale.US,"%.1f",state.bestComboMultiplier));stat(c,y+94,V3L10n.t(state.language,"max_level"),Integer.toString(state.bestLevel));
        button(c,15,bottom-47,525,bottom-8,System.currentTimeMillis()<resetArmedUntil?"POTWIERDŹ RESET":V3L10n.t(state.language,"reset"),Color.rgb(88,38,42),RED,true);hits.add(new Hit(new RectF(15,bottom-48,525,bottom-7),"reset",0));}
    private void setting(Canvas c,float y,String label,boolean on,String id){panel(c,12,y,528,y+44,PANEL,LINE,8);text(c,label,28,y+28,9,INK,Paint.Align.LEFT);p.setColor(on?GREEN:Color.rgb(74,81,84));c.drawRoundRect(new RectF(455,y+11,510,y+34),12,12,p);p.setColor(Color.WHITE);c.drawCircle(on?498:467,y+22.5f,8,p);hits.add(new Hit(new RectF(12,y,528,y+44),id,0));}
    private void stat(Canvas c,float y,String k,String v){text(c,k,28,y,8,MUTED,Paint.Align.LEFT);text(c,v,500,y,9,INK,Paint.Align.RIGHT);}

    private void drawOffline(Canvas c){p.setColor(Color.argb(225,0,0,0));c.drawRect(0,0,W,logicalH,p);float h=315,y=(logicalH-h)/2;panel(c,38,y,502,y+h,Color.rgb(18,28,36),PURPLE,18);drawChest(c,270,y+70);text(c,V3L10n.t(state.language,"welcome_back"),270,y+127,19,INK,Paint.Align.CENTER);text(c,V3L10n.t(state.language,"away")+": "+V3State.formatDuration(state.startupOfflineSeconds),270,y+153,9,MUTED,Paint.Align.CENTER);text(c,V3L10n.t(state.language,"offline_rule"),270,y+178,8,CYAN,Paint.Align.CENTER);text(c,"+"+state.format(state.startupOfflineGain),270,y+221,27,GOLD,Paint.Align.CENTER);button(c,95,y+251,445,y+292,V3L10n.t(state.language,"claim"),Color.rgb(53,174,48),INK,true);hits.add(new Hit(new RectF(38,y,502,y+h),"offline",0));}
    private void drawChest(Canvas c,float x,float y){p.setColor(Color.rgb(126,72,33));c.drawRoundRect(new RectF(x-48,y-28,x+48,y+35),9,9,p);p.setColor(GOLD);c.drawRect(x-48,y-4,x+48,y+8,p);c.drawRect(x-7,y-28,x+7,y+35,p);p.setColor(Color.rgb(255,227,116));c.drawRect(x-8,y-4,x+8,y+10,p);}

    private void drawTutorial(Canvas c){p.setColor(Color.argb(235,4,9,12));c.drawRect(0,0,W,logicalH,p);float y=logicalH/2-190;panel(c,28,y,512,y+380,Color.rgb(17,30,37),GOLD,18);if(tutorialStep==0)drawHut3d(c,270,y+150,.72f);else if(tutorialStep==1){drawChest(c,270,y+95);text(c,"x1.1  →  x1.2  →  x1.3",270,y+165,18,GOLD,Paint.Align.CENTER);}else drawFuture3d(c,270,y+175,.52f,39);String body=tutorialStep==0?V3L10n.t(state.language,"tap_anywhere")+". Każde dotknięcie rozwija świat.":tutorialStep==1?"Combo rośnie o x0.1 dopiero co 50 kolejnych kliknięć.":"Buduj od szałasu aż do Wieży Nieskończoności.";text(c,"PIXEL EMPIRE 3.0",270,y+225,22,INK,Paint.Align.CENTER);wrap(c,body,270,y+260,420,11,MUTED);text(c,(tutorialStep+1)+" / 3",270,y+315,9,MUTED,Paint.Align.CENTER);text(c,"TAP • "+V3L10n.t(state.language,"skip"),270,y+350,10,GOLD,Paint.Align.CENTER);}

    private void drawNav(Canvas c){float y=logicalH-76;p.setColor(Color.rgb(10,20,25));c.drawRect(0,y,W,logicalH,p);p.setColor(Color.rgb(61,84,88));c.drawRect(0,y,W,y+2,p);String[] l={V3L10n.t(state.language,"world"),V3L10n.t(state.language,"build"),V3L10n.t(state.language,"heroes"),V3L10n.t(state.language,"research"),V3L10n.t(state.language,"missions"),V3L10n.t(state.language,"empire")};float ww=W/6;for(int i=0;i<6;i++){float x=i*ww;if(tab==i){p.setColor(Color.rgb(21,50,63));c.drawRoundRect(new RectF(x+3,y+4,x+ww-3,logicalH-4),7,7,p);p.setColor(CYAN);c.drawRect(x+18,y+4,x+ww-18,y+7,p);}navIcon(c,i,x+ww/2,y+27,tab==i?CYAN:MUTED);text(c,l[i],x+ww/2,y+62,6.6f,tab==i?INK:MUTED,Paint.Align.CENTER);hits.add(new Hit(new RectF(x,y,x+ww,logicalH),"tab",i));}}
    private void navIcon(Canvas c,int i,float x,float y,int col){p.setColor(col);if(i==0){cube(c,x,y,16,col);}else if(i==1){c.drawRect(x-14,y-6,x+14,y+5,p);c.rotate(-35,x,y);c.drawRect(x-4,y-17,x+4,y+17,p);c.rotate(35,x,y);}else if(i==2){c.drawCircle(x,y-5,9,p);c.drawRoundRect(new RectF(x-13,y+4,x+13,y+16),6,6,p);}else if(i==3)diamond(c,x,y,13,col);else if(i==4){c.drawRoundRect(new RectF(x-12,y-15,x+12,y+15),3,3,p);p.setColor(BG);c.drawRect(x-7,y-7,x+7,y-4,p);c.drawRect(x-7,y,x+7,y+3,p);c.drawRect(x-7,y+7,x+7,y+10,p);}else{p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(col);c.drawCircle(x,y,13,p);c.drawLine(x,y-18,x,y+18,p);c.drawLine(x-18,y,x+18,y,p);p.setStyle(Paint.Style.FILL);}}

    private void drawFx(Canvas c){for(Particle q:particles){int a=(int)(255*Math.max(0,Math.min(1,q.life)));p.setColor((q.color&0xffffff)|(a<<24));c.drawCircle(q.x,q.y,q.size,p);}for(FloatText f:floatTexts){int a=(int)(255*Math.max(0,Math.min(1,f.life)));text(c,f.s,f.x,f.y,14,(f.color&0xffffff)|(a<<24),Paint.Align.CENTER);}}
    private void drawToast(Canvas c){if(toast.isEmpty()||System.currentTimeMillis()>=toastUntil)return;float y=logicalH-137;panel(c,68,y,472,y+43,Color.argb(240,15,27,34),GOLD,10);text(c,toast,270,y+27,9.5f,INK,Paint.Align.CENTER);}

    @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX()/scale,y=e.getY()/scale;if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;moved=false;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){if(Math.abs(x-downX)>14||Math.abs(y-downY)>14)moved=true;return true;}if(e.getAction()==MotionEvent.ACTION_UP){performClick();if(moved)return true;if(!state.tutorialSeen){tutorialStep++;if(tutorialStep>=3){state.tutorialSeen=true;state.save(getContext());}invalidate();return true;}if(offlinePopup){offlinePopup=false;feedback(false);invalidate();return true;}for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(h.r.contains(x,y)){handle(h,x,y);break;}}return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}

    private void handle(Hit h,float x,float y){switch(h.type){
        case"tab":tab=h.id;feedback(false);break;
        case"tap":doTap(x,y);break;
        case"daily":int r=state.claimDaily();if(r>0){showToast("+"+r+" ◆",1200);burst(x,y,GOLD,20);feedback(true);}break;
        case"event":state.collectEvent();showToast("BOOST!",1200);burst(x,y,PURPLE,24);feedback(true);break;
        case"boss":if(state.startBoss()){showToast(V3L10n.t(state.language,"fight_boss"),900);feedback(true);}break;
        case"buy_mode":state.buyMode=h.id;break;
        case"building":if(state.buyBuilding(h.id)){feedback(false);}else error();break;
        case"build_prev":buildPage--;break;case"build_next":buildPage++;break;
        case"hero":if(state.buyHero(h.id)){feedback(false);}else error();break;
        case"hero_prev":heroPage--;break;case"hero_next":heroPage++;break;
        case"tech":if(state.buyTech(h.id)){burst(x,y,PURPLE,15);feedback(true);}else error();break;
        case"tech_prev":techPage--;break;case"tech_next":techPage++;break;
        case"mission":if(state.claimMission(h.id)){showToast(V3L10n.t(state.language,"reward")+"!",900);burst(x,y,CYAN,15);feedback(true);}break;
        case"mission_prev":missionPage--;break;case"mission_next":missionPage++;break;
        case"sound":state.soundEnabled=!state.soundEnabled;break;case"haptics":state.hapticsEnabled=!state.hapticsEnabled;break;case"power":state.lowPower=!state.lowPower;break;
        case"lang":state.language=V3L10n.next(state.language);break;
        case"rebirth":rebirth();break;case"reset":reset();break;case"offline":offlinePopup=false;break;
    }state.save(getContext());invalidate();}

    private void doTap(float x,float y){long now=System.currentTimeMillis();if(now-lastTapAt>4000)comboClicks=0;comboClicks++;lastTapAt=now;double mult=comboMult();boolean crit=rnd.nextDouble()<state.getCritChance();double amount=state.tap(mult,crit);String s=(crit?V3L10n.t(state.language,"critical")+" ":"+")+state.format(amount);floatTexts.add(new FloatText(x,y-12,s,crit?PURPLE:GOLD));while(floatTexts.size()>20)floatTexts.remove(0);burst(x,y,crit?PURPLE:GOLD,crit?18:7);feedback(crit);}
    private void rebirth(){int gain=state.availableLegacyStars();if(gain<=0)return;long now=System.currentTimeMillis();if(now>rebirthArmedUntil){rebirthArmedUntil=now+5000;showToast(V3L10n.t(state.language,"prestige")+" +"+gain+" ★ ?",1500);return;}if(state.rebirth()){rebirthArmedUntil=0;comboClicks=0;tab=0;lastSeenStage=0;lastSeenLevel=1;showToast("+"+gain+" ★",1700);burst(270,360,PURPLE,40);feedback(true);}}
    private void reset(){long now=System.currentTimeMillis();if(now>resetArmedUntil){resetArmedUntil=now+5000;showToast("Kliknij RESET ponownie",1400);error();return;}state.hardReset(getContext());postDelayed(()->((Activity)getContext()).recreate(),300);}

    private void feedback(boolean strong){if(state.soundEnabled&&tone!=null)try{tone.startTone(strong?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,strong?70:25);}catch(Throwable ignored){}if(state.hapticsEnabled&&vibrator!=null&&vibrator.hasVibrator())try{if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createOneShot(strong?35:11,strong?125:55));else vibrator.vibrate(strong?35:11);}catch(Throwable ignored){}}
    private void error(){if(state.soundEnabled&&tone!=null)try{tone.startTone(ToneGenerator.TONE_PROP_NACK,45);}catch(Throwable ignored){}if(state.hapticsEnabled&&vibrator!=null&&vibrator.hasVibrator())try{if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createOneShot(22,70));else vibrator.vibrate(22);}catch(Throwable ignored){}}
    private void burst(float x,float y,int col,int n){for(int i=0;i<n;i++){double a=rnd.nextDouble()*Math.PI*2;float v=25+rnd.nextFloat()*85;particles.add(new Particle(x,y,(float)Math.cos(a)*v,(float)Math.sin(a)*v-25,.45f+rnd.nextFloat()*.45f,2+rnd.nextFloat()*4,col));}while(particles.size()>110)particles.remove(0);}

    private void title(Canvas c,String a,String b,float top){text(c,a,18,top+28,20,INK,Paint.Align.LEFT);text(c,b,522,top+27,8.5f,MUTED,Paint.Align.RIGHT);p.setColor(Color.rgb(54,76,81));c.drawRect(18,top+39,522,top+41,p);}
    private void panel(Canvas c,float x1,float y1,float x2,float y2,int fill,int stroke,float r){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawRoundRect(new RectF(x1,y1,x2,y2),r,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(stroke);c.drawRoundRect(new RectF(x1,y1,x2,y2),r,r,p);p.setStyle(Paint.Style.FILL);}
    private void button(Canvas c,float x1,float y1,float x2,float y2,String s,int fill,int col,boolean enabled){p.setColor(enabled?fill:Color.rgb(49,57,60));c.drawRoundRect(new RectF(x1,y1,x2,y2),8,8,p);p.setColor(enabled?lighter(fill,1.13f):Color.rgb(67,74,77));c.drawRect(x1+5,y1+4,x2-5,y1+7,p);text(c,s,(x1+x2)/2,(y1+y2)/2+4,8.5f,enabled?col:MUTED,Paint.Align.CENTER);}
    private void progress(Canvas c,float x1,float y1,float x2,float y2,float v,int col){v=Math.max(0,Math.min(1,v));p.setColor(Color.rgb(35,47,51));c.drawRoundRect(new RectF(x1,y1,x2,y2),5,5,p);p.setColor(col);c.drawRoundRect(new RectF(x1,y1,x1+(x2-x1)*v,y2),5,5,p);}
    private void pager(Canvas c,float bottom,int pages,int page,String prev,String next){if(pages<=1)return;float y=bottom-43;button(c,15,y,145,bottom-6,"‹",PANEL2,INK,page>0);button(c,395,y,525,bottom-6,"›",PANEL2,INK,page<pages-1);text(c,(page+1)+" / "+pages,270,y+25,9,MUTED,Paint.Align.CENTER);if(page>0)hits.add(new Hit(new RectF(15,y,145,bottom-6),prev,0));if(page<pages-1)hits.add(new Hit(new RectF(395,y,525,bottom-6),next,0));}
    private void wrap(Canvas c,String s,float x,float y,float max,float size,int col){String[] w=s.split(" ");String line="";int row=0;tp.setTextSize(size);for(String a:w){String n=line.isEmpty()?a:line+" "+a;if(tp.measureText(n)>max&&!line.isEmpty()){text(c,line,x,y+row*19,size,col,Paint.Align.CENTER);row++;line=a;}else line=n;}if(!line.isEmpty())text(c,line,x,y+row*19,size,col,Paint.Align.CENTER);}
    private void text(Canvas c,String s,float x,float y,float size,int col,Paint.Align align){tp.setShader(null);tp.setStyle(Paint.Style.FILL);tp.setTextSize(size);tp.setTextAlign(align);tp.setColor(col);tp.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText(s,x,y,tp);}
    private void diamond(Canvas c,float x,float y,float r,int col){p.setColor(col);Path d=new Path();d.moveTo(x,y-r);d.lineTo(x+r,y);d.lineTo(x,y+r);d.lineTo(x-r,y);d.close();c.drawPath(d,p);p.setColor(Color.argb(105,255,255,255));c.drawRect(x-r*.25f,y-r*.55f,x,y-r*.2f,p);}
    private void voxelSpark(Canvas c,float x,float y,int col){p.setColor(col);c.drawRect(x-2,y-7,x+2,y+7,p);c.drawRect(x-7,y-2,x+7,y+2,p);}
    private int darker(int color,float f){return Color.rgb(clamp((int)(Color.red(color)*f),0,255),clamp((int)(Color.green(color)*f),0,255),clamp((int)(Color.blue(color)*f),0,255));}
    private int lighter(int color,float f){return Color.rgb(clamp((int)(Color.red(color)*f),0,255),clamp((int)(Color.green(color)*f),0,255),clamp((int)(Color.blue(color)*f),0,255));}
    private int clamp(int v,int a,int b){return Math.max(a,Math.min(b,v));}
}
