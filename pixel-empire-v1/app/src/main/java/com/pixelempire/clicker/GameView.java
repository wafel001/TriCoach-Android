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

public final class GameView extends View implements Runnable {
    private static final float DESIGN_W=540f;
    private final GameState state;
    private final Paint p=new Paint();
    private final Paint tp=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random=new Random();
    private final List<Hit> hits=new ArrayList<>();
    private final List<Particle> particles=new ArrayList<>();
    private final List<Floating> floats=new ArrayList<>();
    private final Vibrator vibrator;
    private ToneGenerator tone;

    private float scale=1f,logicalH=960f;
    private boolean running=false;
    private long lastNanos=0,lastAutosave=0,lastTapAt=0;
    private double secAcc=0;
    private int tab=0,upgradePage=0,researchPage=0,missionPage=0,goalMode=0,tutorialStep=0,combo=0;
    private float downX,downY;private boolean moved=false;
    private boolean offlinePopup;
    private String toast="";private long toastUntil=0;
    private long resetArmedUntil=0,ascendArmedUntil=0;
    private int lastSeenLevel,lastSeenStage;

    private static final int BG=Color.rgb(6,10,20),PANEL=Color.rgb(15,23,42),PANEL2=Color.rgb(24,35,58),STROKE=Color.rgb(56,72,100);
    private static final int TEXT=Color.rgb(244,247,252),MUTED=Color.rgb(148,163,184),GOLD=Color.rgb(251,191,36),GREEN=Color.rgb(52,211,153);
    private static final int CYAN=Color.rgb(34,211,238),PURPLE=Color.rgb(196,132,252),RED=Color.rgb(248,113,113),BLUE=Color.rgb(96,165,250);

    private static final class Hit{final RectF r;final String type,id;Hit(RectF r,String type,String id){this.r=r;this.type=type;this.id=id;}}
    private static final class Particle{float x,y,vx,vy,life,size;int color;Particle(float x,float y,float vx,float vy,float life,float size,int color){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=life;this.size=size;this.color=color;}}
    private static final class Floating{float x,y,life;final String text;final int color;Floating(float x,float y,String text,int color){this.x=x;this.y=y;this.text=text;this.color=color;this.life=1f;}}

    public GameView(Context context,GameState state){
        super(context);this.state=state;setFocusable(true);setBackgroundColor(BG);
        p.setAntiAlias(false);tp.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        vibrator=(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,30);}catch(Throwable ignored){}
        offlinePopup=state.startupOfflineGain>0.01;lastSeenLevel=state.level;lastSeenStage=state.stage;
        if(state.nextEventAt<=0)state.nextEventAt=System.currentTimeMillis()+45_000;
    }

    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();running=true;lastNanos=System.nanoTime();post(this);}
    @Override protected void onDetachedFromWindow(){running=false;removeCallbacks(this);if(tone!=null){try{tone.release();}catch(Throwable ignored){}tone=null;}super.onDetachedFromWindow();}

    @Override public void run(){
        if(!running)return;long n=System.nanoTime();double dt=Math.min(.25,Math.max(0,(n-lastNanos)/1e9));lastNanos=n;
        state.tick(dt);secAcc+=dt;if(secAcc>=1){long s=(long)secAcc;state.playSeconds+=s;secAcc-=s;state.checkAchievements();}
        updateEvents();updateFx((float)dt);consumeAchievementToast();detectProgress();
        long now=System.currentTimeMillis();if(lastAutosave==0||now-lastAutosave>=5000){state.save(getContext());lastAutosave=now;}
        invalidate();postDelayed(this,state.lowPower?100:33);
    }

    private void detectProgress(){
        if(state.stage>lastSeenStage){lastSeenStage=state.stage;lastSeenLevel=state.level;showToast(L10n.t(state.language,"new_stage")+"  "+L10n.stageName(state.language,state.stage),2600);burst(270,330,CYAN,34);playFeedback(true);}
        else if(state.level>lastSeenLevel){lastSeenLevel=state.level;showToast(L10n.t(state.language,"new_level")+"  "+state.level,1200);burst(270,420,GOLD,18);}
    }

    private void updateEvents(){long now=System.currentTimeMillis();if(state.activeEventType>=0&&now>=state.activeEventUntil){state.activeEventType=-1;state.activeEventUntil=0;state.nextEventAt=now+45_000+random.nextInt(65_000);}else if(state.activeEventType<0&&now>=state.nextEventAt){state.activeEventType=random.nextInt(5);state.activeEventUntil=now+18_000;}}
    private void updateFx(float dt){for(int i=particles.size()-1;i>=0;i--){Particle q=particles.get(i);q.life-=dt;q.x+=q.vx*dt;q.y+=q.vy*dt;q.vy+=85*dt;if(q.life<=0)particles.remove(i);}for(int i=floats.size()-1;i>=0;i--){Floating f=floats.get(i);f.life-=dt*.75f;f.y-=42*dt;if(f.life<=0)floats.remove(i);}}
    private void consumeAchievementToast(){if(!state.justUnlocked.isEmpty()&&System.currentTimeMillis()>toastUntil){GameState.Achievement a=state.justUnlocked.remove(0);showToast(L10n.t(state.language,"achievement")+"  +"+a.reward+" ◆",1800);}}
    private void showToast(String s,long ms){toast=s;toastUntil=System.currentTimeMillis()+ms;}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);scale=getWidth()/DESIGN_W;if(scale<=0)scale=1;logicalH=getHeight()/scale;c.save();c.scale(scale,scale);hits.clear();
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(BG);c.drawRect(0,0,DESIGN_W,logicalH,p);
        float top=82,bottom=logicalH-70;
        if(tab==0)drawWorld(c,top,bottom);else{drawBackdrop(c);if(tab==1)drawUpgrades(c,top,bottom);else if(tab==2)drawGoals(c,top,bottom);else if(tab==3)drawResearch(c,top,bottom);else drawMenu(c,top,bottom);}
        drawTop(c);drawEvent(c);drawNav(c);drawFx(c);drawToast(c);if(offlinePopup)drawOffline(c);if(!state.tutorialSeen)drawTutorial(c);c.restore();
    }

    private void drawBackdrop(Canvas c){p.setShader(new LinearGradient(0,0,0,logicalH,Color.rgb(6,11,23),Color.rgb(10,20,36),Shader.TileMode.CLAMP));c.drawRect(0,0,DESIGN_W,logicalH,p);p.setShader(null);for(int i=0;i<16;i++){p.setColor(Color.argb(40,80,120,180));float x=(i*73)%540,y=100+(i*97)%700;c.drawRect(x,y,x+2,y+2,p);}}

    private void drawTop(Canvas c){
        p.setColor(Color.argb(235,8,14,26));c.drawRect(0,0,DESIGN_W,82,p);p.setColor(Color.rgb(36,50,73));c.drawRect(0,80,DESIGN_W,82,p);
        coin(c,24,28,12);text(c,state.format(state.coins),45,33,20,TEXT,Paint.Align.LEFT);text(c,state.format(state.getCps())+" "+L10n.t(state.language,"per_sec"),46,56,10,MUTED,Paint.Align.LEFT);
        diamond(c,365,27,9,PURPLE);text(c,Integer.toString(state.crystals),382,33,14,TEXT,Paint.Align.LEFT);
        diamond(c,438,27,9,CYAN);text(c,Integer.toString(state.researchPoints),455,33,14,TEXT,Paint.Align.LEFT);
        text(c,"★ "+state.legacyStars,505,33,14,GOLD,Paint.Align.RIGHT);
        text(c,L10n.t(state.language,"level")+" "+state.level,505,58,9,MUTED,Paint.Align.RIGHT);
    }

    private void drawWorld(Canvas c,float top,float bottom){
        RectF world=new RectF(0,top,DESIGN_W,bottom);hits.add(new Hit(world,"tap",""));
        boolean night=LocalTime.now().getHour()<6||LocalTime.now().getHour()>=20;
        int s=state.stage;int skyA,skyB;
        if(s>=20){skyA=Color.rgb(8,8,32);skyB=Color.rgb(40,18,75);night=true;}else if(s>=15){skyA=night?Color.rgb(8,15,38):Color.rgb(45,92,148);skyB=night?Color.rgb(30,20,70):Color.rgb(178,105,180);}else{skyA=night?Color.rgb(8,18,44):Color.rgb(61,135,203);skyB=night?Color.rgb(25,43,83):Color.rgb(164,221,240);}
        p.setShader(new LinearGradient(0,top,0,bottom,skyA,skyB,Shader.TileMode.CLAMP));c.drawRect(world,p);p.setShader(null);
        drawSkyObjects(c,top,bottom,night,s);drawParallax(c,top,bottom,s);float ground=bottom-125;drawGround(c,ground,bottom,s);drawAmbient(c,ground,s);drawStructure(c,270,ground,s,state.levelProgress());
        drawWorldHud(c,top,bottom);
    }

    private void drawSkyObjects(Canvas c,float top,float bottom,boolean night,int s){
        if(night){for(int i=0;i<34;i++){float x=(i*83+17)%530,y=top+18+(i*47)%230;pixel(c,x,y,(i%4==0?4:2),Color.rgb(225,235,255));}p.setColor(Color.rgb(240,238,199));c.drawRect(452,top+38,478,top+64,p);p.setColor(Color.rgb(30,35,70));c.drawRect(462,top+34,482,top+56,p);}else{p.setColor(Color.rgb(255,223,92));c.drawRect(454,top+34,482,top+62,p);c.drawRect(461,top+27,475,top+69,p);}
        float t=(System.currentTimeMillis()%20000)/20000f;cloud(c,55+t*55,top+70,1f);cloud(c,345-t*45,top+120,.75f);
        if(s>=18){p.setColor(Color.argb(190,180,220,255));c.drawRect(55,top+62,122,top+66,p);c.drawRect(68,top+57,110,top+70,p);}
    }

    private void drawParallax(Canvas c,float top,float bottom,int s){float base=bottom-118;p.setColor(s>=15?Color.rgb(34,45,76):Color.rgb(71,102,118));Path path=new Path();path.moveTo(0,base);for(int x=0;x<=540;x+=60){float y=base-75-(float)(28*Math.sin((x+s*13)*.028));path.lineTo(x,y);}path.lineTo(540,base);path.close();c.drawPath(path,p);p.setColor(s>=15?Color.rgb(24,34,60):Color.rgb(48,78,78));Path path2=new Path();path2.moveTo(0,base+22);for(int x=0;x<=540;x+=45){float y=base-32-(float)(20*Math.sin((x+80)*.037));path2.lineTo(x,y);}path2.lineTo(540,base+22);path2.close();c.drawPath(path2,p);}

    private void drawGround(Canvas c,float ground,float bottom,int s){if(s<12){p.setColor(Color.rgb(50,93,62));c.drawRect(0,ground,540,bottom,p);p.setColor(Color.rgb(84,123,71));c.drawRect(0,ground,540,ground+12,p);p.setColor(Color.rgb(58,47,38));c.drawRect(0,bottom-24,540,bottom,p);}else if(s<18){p.setColor(Color.rgb(67,70,77));c.drawRect(0,ground,540,bottom,p);p.setColor(Color.rgb(120,111,84));c.drawRect(0,ground,540,ground+10,p);for(int x=0;x<540;x+=32){p.setColor(Color.rgb(47,50,57));c.drawRect(x,bottom-22,x+20,bottom-18,p);}}else{p.setColor(Color.rgb(24,33,52));c.drawRect(0,ground,540,bottom,p);p.setColor(CYAN);for(int x=0;x<540;x+=64)c.drawRect(x,ground+7,x+38,ground+10,p);p.setColor(Color.rgb(11,19,32));c.drawRect(0,bottom-22,540,bottom,p);}}

    private void drawAmbient(Canvas c,float ground,int s){long now=System.currentTimeMillis();float walk=(now%9000)/9000f;if(s<12){tree(c,35,ground-5,1f);tree(c,495,ground-4,.9f);if(s>=3)tree(c,75,ground-3,.65f);}if(s>=12&&s<18){for(int i=0;i<3;i++){float x=45+i*180;p.setColor(Color.rgb(95,109,119));c.drawRect(x,ground-34,x+8,ground,p);p.setColor(Color.rgb(245,158,11));c.drawRect(x+1,ground-39,x+7,ground-33,p);}}for(int i=0;i<Math.min(5,1+s/4);i++){float x=((walk*600+i*130)%620)-40;worker(c,x,ground-18,i,s);}}

    private void worker(Canvas c,float x,float y,int i,int s){int body=s>=15?CYAN:(i%2==0?Color.rgb(52,97,160):Color.rgb(158,88,63));p.setColor(Color.rgb(237,200,166));c.drawRect(x,y-13,x+6,y-7,p);p.setColor(body);c.drawRect(x-1,y-7,x+7,y+5,p);p.setColor(Color.rgb(20,25,35));c.drawRect(x,y+5,x+2,y+12,p);c.drawRect(x+5,y+5,x+7,y+12,p);if(s>=15){p.setColor(PURPLE);c.drawRect(x+1,y-11,x+5,y-9,p);}}
    private void tree(Canvas c,float x,float y,float z){p.setColor(Color.rgb(87,57,39));c.drawRect(x-3*z,y-28*z,x+3*z,y,p);p.setColor(Color.rgb(35,111,61));c.drawRect(x-14*z,y-46*z,x+14*z,y-22*z,p);c.drawRect(x-9*z,y-56*z,x+9*z,y-36*z,p);}

    private void drawStructure(Canvas c,float cx,float ground,int s,float progress){float g=.78f+.22f*progress;if(s==0)leanTo(c,cx,ground,g);else if(s==1)hut(c,cx,ground,g);else if(s==2)cabins(c,cx,ground,g,1);else if(s==3)farm(c,cx,ground,g);else if(s==4)cabins(c,cx,ground,g,3);else if(s==5)village(c,cx,ground,g);else if(s<=11)castle(c,cx,ground,g,s);else if(s<=15)industrial(c,cx,ground,g,s);else future(c,cx,ground,g,s);}
    private void leanTo(Canvas c,float cx,float ground,float g){float w=135*g,h=78*g;p.setColor(Color.rgb(84,54,35));c.drawRect(cx-w/2,ground-h*.12f,cx+w/2,ground,p);for(int i=0;i<7;i++){p.setColor(i%2==0?Color.rgb(127,84,47):Color.rgb(108,69,40));float x=cx-w/2+i*w/7;c.drawRect(x,ground-h,x+w/7+2,ground-h*.08f,p);}p.setColor(Color.rgb(62,40,28));c.drawRect(cx-8,ground-h*.75f,cx+8,ground,p);}
    private void hut(Canvas c,float cx,float ground,float g){float w=150*g,h=92*g;p.setColor(Color.rgb(130,82,48));c.drawRect(cx-w/2,ground-h*.65f,cx+w/2,ground,p);p.setColor(Color.rgb(82,50,34));c.drawRect(cx-w*.58f,ground-h*.78f,cx+w*.58f,ground-h*.62f,p);c.drawRect(cx-w*.42f,ground-h*.92f,cx+w*.42f,ground-h*.76f,p);p.setColor(Color.rgb(249,210,91));c.drawRect(cx-w*.28f,ground-h*.44f,cx-w*.10f,ground-h*.25f,p);p.setColor(Color.rgb(56,39,30));c.drawRect(cx+w*.14f,ground-h*.45f,cx+w*.33f,ground,p);}
    private void cabins(Canvas c,float cx,float ground,float g,int count){for(int i=0;i<count;i++){float x=cx+(i-(count-1)/2f)*125*g;hut(c,x,ground-(i%2)*5,g*(count>1?.72f:1f));}}
    private void farm(Canvas c,float cx,float ground,float g){hut(c,cx-55*g,ground,g*.9f);p.setColor(Color.rgb(136,45,39));c.drawRect(cx+35*g,ground-72*g,cx+120*g,ground,p);p.setColor(Color.rgb(91,39,35));c.drawRect(cx+27*g,ground-78*g,cx+128*g,ground-66*g,p);c.drawRect(cx+42*g,ground-88*g,cx+113*g,ground-76*g,p);p.setColor(Color.rgb(255,235,150));c.drawRect(cx+55*g,ground-50*g,cx+72*g,ground-32*g,p);c.drawRect(cx+87*g,ground-50*g,cx+104*g,ground-32*g,p);}
    private void village(Canvas c,float cx,float ground,float g){cabins(c,cx,ground,g*.82f,3);p.setColor(Color.rgb(118,105,84));c.drawRect(cx-38,ground-116*g,cx+38,ground,p);p.setColor(Color.rgb(69,67,63));c.drawRect(cx-48,ground-124*g,cx+48,ground-112*g,p);p.setColor(GOLD);c.drawRect(cx-8,ground-80*g,cx+8,ground-63*g,p);}

    private void castle(Canvas c,float cx,float ground,float g,int s){int towers=2+(s>=9?2:0);float bodyW=(190+(s-6)*13)*g,bodyH=(105+(s-6)*12)*g;int stone=s>=10?Color.rgb(187,181,171):Color.rgb(119,126,132);p.setColor(stone);c.drawRect(cx-bodyW/2,ground-bodyH,cx+bodyW/2,ground,p);for(int i=0;i<towers;i++){float tx=cx-bodyW/2+i*(bodyW/(towers-1));float tw=42*g,th=bodyH+42*g+(i%2)*12;p.setColor(stone);c.drawRect(tx-tw/2,ground-th,tx+tw/2,ground,p);battlement(c,tx-tw/2,ground-th,tw,stone);}battlement(c,cx-bodyW/2,ground-bodyH,bodyW,stone);p.setColor(Color.rgb(63,49,44));c.drawRect(cx-18*g,ground-48*g,cx+18*g,ground,p);for(int x=-70;x<=70;x+=35){p.setColor(Color.rgb(254,220,112));c.drawRect(cx+x*g-5,ground-bodyH+30*g,cx+x*g+5,ground-bodyH+43*g,p);}if(s>=10){p.setColor(PURPLE);c.drawRect(cx-3,ground-bodyH-80*g,cx+3,ground-bodyH,p);c.drawRect(cx+3,ground-bodyH-79*g,cx+38,ground-bodyH-68*g,p);}}
    private void battlement(Canvas c,float x,float y,float w,int col){p.setColor(col);for(float xx=x;xx<x+w;xx+=18){c.drawRect(xx,y-12,Math.min(xx+11,x+w),y+3,p);}}

    private void industrial(Canvas c,float cx,float ground,float g,int s){float w=(210+(s-12)*22)*g,h=(115+(s-12)*20)*g;p.setColor(Color.rgb(71,78,88));c.drawRect(cx-w/2,ground-h,cx+w/2,ground,p);for(int i=0;i<3+(s-12);i++){float x=cx-w/2+28+i*45;p.setColor(Color.rgb(250,190,60));c.drawRect(x,ground-h+26,x+18,ground-h+45,p);}p.setColor(Color.rgb(48,55,65));c.drawRect(cx-w/2+22,ground-h-95*g,cx-w/2+47,ground-h,p);c.drawRect(cx+w/2-56,ground-h-68*g,cx+w/2-34,ground-h,p);float smoke=(System.currentTimeMillis()%4000)/4000f;for(int i=0;i<3;i++){p.setColor(Color.argb(120-i*25,190,198,205));float yy=ground-h-112*g-i*20-smoke*18;c.drawRect(cx-w/2+16-i*7,yy,cx-w/2+53+i*7,yy+14,p);}if(s>=13){p.setColor(CYAN);for(int y=0;y<4;y++)c.drawRect(cx-w/2+12,ground-h+18+y*24,cx+w/2-12,ground-h+20+y*24,p);}if(s>=15){p.setColor(PURPLE);c.drawRect(cx-w/2-8,ground-h-9,cx+w/2+8,ground-h-3,p);text(c,"PIXEL",cx,ground-h-17,14,CYAN,Paint.Align.CENTER);}}

    private void future(Canvas c,float cx,float ground,float g,int s){float baseW=(185+(s-16)*10)*g;float h=(150+(s-16)*35)*g;int dark=Color.rgb(26,37,58),metal=Color.rgb(56,75,102);p.setColor(dark);c.drawRect(cx-baseW/2,ground-h*.72f,cx+baseW/2,ground,p);p.setColor(metal);c.drawRect(cx-baseW*.34f,ground-h,cx+baseW*.34f,ground,p);for(int i=0;i<7;i++){float y=ground-h+22+i*(h-38)/7;p.setColor(i%2==0?CYAN:PURPLE);c.drawRect(cx-baseW*.26f,y,cx+baseW*.26f,y+5,p);}p.setColor(Color.rgb(10,18,32));c.drawRect(cx-17,ground-64,cx+17,ground,p);if(s>=17){p.setColor(Color.argb(100,34,211,238));c.drawRect(cx-baseW*.48f,ground-h*.68f,cx+baseW*.48f,ground-h*.64f,p);}if(s>=18){floatingPlatform(c,cx-150,ground-h*.55f,.75f);floatingPlatform(c,cx+150,ground-h*.42f,.65f);}if(s>=19){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setColor(Color.argb(190,150,220,255));c.drawOval(new RectF(cx-150,ground-h-78,cx+150,ground-h+5),p);p.setStyle(Paint.Style.FILL);}if(s>=20){p.setColor(Color.rgb(180,190,205));c.drawRect(cx-120,ground-38,cx-80,ground,p);c.drawRect(cx+80,ground-50,cx+125,ground,p);}if(s>=21){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(9);p.setColor(PURPLE);c.drawOval(new RectF(cx-210,ground-h*.73f,cx+210,ground-h*.34f),p);p.setStyle(Paint.Style.FILL);}if(s>=22){for(int i=0;i<5;i++){p.setColor(Color.argb(80+i*24,100,220,255));float off=i*12;c.drawRect(cx-baseW*.34f-off,ground-h-off,cx+baseW*.34f+off,ground-h+4-off,p);}}if(s>=23){p.setColor(Color.rgb(245,245,255));c.drawRect(cx-7,ground-h-118,cx+7,ground-h+5,p);p.setColor(CYAN);c.drawRect(cx-2,ground-h-180,cx+2,ground-h-118,p);for(int i=0;i<4;i++){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(i%2==0?CYAN:PURPLE);float r=65+i*22;c.drawCircle(cx,ground-h-40,r,p);}p.setStyle(Paint.Style.FILL);}}
    private void floatingPlatform(Canvas c,float x,float y,float g){p.setColor(Color.rgb(30,43,64));c.drawRect(x-50*g,y-10*g,x+50*g,y+10*g,p);p.setColor(CYAN);c.drawRect(x-37*g,y+11*g,x+37*g,y+15*g,p);p.setColor(Color.argb(80,80,220,255));c.drawRect(x-25*g,y+15*g,x+25*g,y+35*g,p);}

    private void drawWorldHud(Canvas c,float top,float bottom){
        float cardY=top+12;panel(c,12,cardY,355,cardY+74,Color.argb(210,10,18,31),Color.argb(160,100,140,190),10);
        text(c,L10n.stageName(state.language,state.stage),28,cardY+27,16,TEXT,Paint.Align.LEFT);text(c,L10n.t(state.language,"stage")+" "+(state.stage+1)+"/24  •  "+L10n.t(state.language,"level")+" "+state.level+"/288",28,cardY+49,9,MUTED,Paint.Align.LEFT);
        progress(c,28,cardY+59,330,cardY+67,(float)state.levelProgress(),state.stage>=18?CYAN:GOLD);
        if(state.canClaimDaily()){button(c,370,cardY,528,cardY+74,L10n.t(state.language,"daily"),GOLD,BG,true);hits.add(new Hit(new RectF(370,cardY,528,cardY+74),"daily",""));}
        float hintY=bottom-91;panel(c,75,hintY,465,hintY+47,Color.argb(185,8,15,27),Color.argb(150,255,255,255),12);text(c,L10n.t(state.language,"tap_anywhere"),270,hintY+20,9,TEXT,Paint.Align.CENTER);text(c,"+"+state.format(state.getTapPower())+"  •  "+L10n.t(state.language,"combo")+" x"+Math.max(1,combo),270,hintY+38,10,GOLD,Paint.Align.CENTER);
    }

    private void drawUpgrades(Canvas c,float top,float bottom){header(c,L10n.t(state.language,"upgrades"),"18 × "+L10n.t(state.language,"upgrades"),top);List<GameState.Upgrade> all=new ArrayList<>(state.upgrades.values());int per=6,pages=(all.size()+per-1)/per;upgradePage=Math.max(0,Math.min(pages-1,upgradePage));float y=top+58;int start=upgradePage*per,end=Math.min(all.size(),start+per);float row=Math.min(88,(bottom-y-54-(end-start-1)*7)/(end-start));for(int i=start;i<end;i++){GameState.Upgrade u=all.get(i);float y2=y+row;boolean unlocked=state.stage>=u.def.unlockStage,can=state.canBuyUpgrade(u.def.id);panel(c,12,y,528,y2,PANEL,unlocked?(u.def.tap?GOLD:GREEN):Color.rgb(44,52,66),10);p.setColor(u.def.tap?GOLD:GREEN);c.drawRect(13,y+1,18,y2-1,p);text(c,L10n.upgradeName(state.language,u.def.id),31,y+25,13,unlocked?TEXT:MUTED,Paint.Align.LEFT);String desc=unlocked?L10n.upgradeDesc(state.language,u.def.tap,u.def.value)+"  •  "+L10n.t(state.language,"lvl")+" "+u.level:L10n.t(state.language,"locked")+" • "+L10n.t(state.language,"stage")+" "+(u.def.unlockStage+1);text(c,desc,31,y+48,9,MUTED,Paint.Align.LEFT);if(unlocked){text(c,state.format(u.cost()),500,y+29,13,can?GOLD:MUTED,Paint.Align.RIGHT);text(c,L10n.t(state.language,"buy"),500,y+51,9,can?GREEN:MUTED,Paint.Align.RIGHT);hits.add(new Hit(new RectF(12,y,528,y2),"upgrade",u.def.id));}y=y2+7;}pager(c,bottom,pages,upgradePage,"up_prev","up_next");}

    private void drawGoals(Canvas c,float top,float bottom){header(c,goalMode==0?L10n.t(state.language,"missions"):L10n.t(state.language,"achievement"),goalMode==0?state.missionsClaimed+"/"+state.missions.size():state.unlockedAchievementCount()+"/"+state.achievements.size(),top);float toggleY=top+47;button(c,12,toggleY,260,toggleY+38,L10n.t(state.language,"missions"),goalMode==0?GOLD:PANEL2,goalMode==0?BG:TEXT,true);button(c,280,toggleY,528,toggleY+38,L10n.t(state.language,"achievement"),goalMode==1?PURPLE:PANEL2,goalMode==1?BG:TEXT,true);hits.add(new Hit(new RectF(12,toggleY,260,toggleY+38),"goal_mode","0"));hits.add(new Hit(new RectF(280,toggleY,528,toggleY+38),"goal_mode","1"));float y=toggleY+50;if(goalMode==0)drawMissions(c,y,bottom);else drawAchievements(c,y,bottom);}
    private void drawMissions(Canvas c,float y,float bottom){int per=5,pages=(state.missions.size()+per-1)/per;missionPage=Math.max(0,Math.min(pages-1,missionPage));int start=missionPage*per,end=Math.min(state.missions.size(),start+per);float row=Math.min(105,(bottom-y-53-(end-start-1)*8)/(end-start));for(int i=start;i<end;i++){GameState.Mission m=state.missions.get(i);float y2=y+row;boolean ready=state.missionReady(m);int border=m.claimed?GREEN:ready?GOLD:STROKE;panel(c,12,y,528,y2,PANEL,border,10);text(c,L10n.t(state.language,"mission")+" #"+(i+1),29,y+24,10,m.claimed?GREEN:GOLD,Paint.Align.LEFT);text(c,state.goalText(m.type,m.target),29,y+49,12,m.claimed?MUTED:TEXT,Paint.Align.LEFT);double val=state.goalValue(m.type);progress(c,29,y+62,390,y+70,(float)Math.min(1,val/Math.max(1,m.target)),m.claimed?GREEN:GOLD);text(c,"+"+m.researchReward+" ◇  +"+m.crystalReward+" ◆",498,y+28,10,CYAN,Paint.Align.RIGHT);if(m.claimed)text(c,L10n.t(state.language,"completed"),498,y+58,9,GREEN,Paint.Align.RIGHT);else if(ready){text(c,L10n.t(state.language,"claim"),498,y+58,10,GOLD,Paint.Align.RIGHT);hits.add(new Hit(new RectF(12,y,528,y2),"mission",Integer.toString(i)));}y=y2+8;}pager(c,bottom,pages,missionPage,"mission_prev","mission_next");}
    private void drawAchievements(Canvas c,float y,float bottom){int per=6,pages=(state.achievements.size()+per-1)/per;missionPage=Math.max(0,Math.min(pages-1,missionPage));int start=missionPage*per,end=Math.min(state.achievements.size(),start+per);float row=Math.min(83,(bottom-y-53-(end-start-1)*7)/(end-start));for(int i=start;i<end;i++){GameState.Achievement a=state.achievements.get(i);float y2=y+row;panel(c,12,y,528,y2,PANEL,a.unlocked?GREEN:STROKE,10);diamond(c,35,y+row/2,9,a.unlocked?GREEN:Color.rgb(70,83,104));text(c,L10n.t(state.language,"achievement")+" #"+(i+1),57,y+25,10,a.unlocked?GREEN:MUTED,Paint.Align.LEFT);text(c,state.goalText(a.type,a.target),57,y+49,11,a.unlocked?TEXT:MUTED,Paint.Align.LEFT);text(c,"+"+a.reward+" ◆",500,y+40,10,a.unlocked?PURPLE:MUTED,Paint.Align.RIGHT);y=y2+7;}pager(c,bottom,pages,missionPage,"mission_prev","mission_next");}

    private void drawResearch(Canvas c,float top,float bottom){header(c,L10n.t(state.language,"research"),L10n.t(state.language,"research_points")+": "+state.researchPoints,top);List<GameContent.ResearchDef> all=GameContent.research();int per=6,pages=(all.size()+per-1)/per;researchPage=Math.max(0,Math.min(pages-1,researchPage));float y=top+58;int start=researchPage*per,end=Math.min(all.size(),start+per);float row=Math.min(88,(bottom-y-54-(end-start-1)*7)/(end-start));for(int i=start;i<end;i++){GameContent.ResearchDef r=all.get(i);float y2=y+row;boolean owned=state.research.contains(r.id),unlocked=state.stage>=r.unlockStage,can=state.canResearch(r.id);panel(c,12,y,528,y2,PANEL,owned?CYAN:unlocked?PURPLE:STROKE,10);diamond(c,36,y+row/2,10,owned?CYAN:unlocked?PURPLE:Color.rgb(62,73,92));text(c,L10n.researchName(state.language,r.id),60,y+27,12,owned?TEXT:unlocked?TEXT:MUTED,Paint.Align.LEFT);text(c,L10n.researchDesc(state.language,r.kind,r.bonus),60,y+51,9,MUTED,Paint.Align.LEFT);if(owned)text(c,L10n.t(state.language,"owned"),500,y+39,9,CYAN,Paint.Align.RIGHT);else if(unlocked){text(c,r.cost+" ◇",500,y+28,11,can?PURPLE:MUTED,Paint.Align.RIGHT);text(c,L10n.t(state.language,"unlock"),500,y+51,9,can?GREEN:MUTED,Paint.Align.RIGHT);hits.add(new Hit(new RectF(12,y,528,y2),"research",r.id));}else text(c,L10n.t(state.language,"stage")+" "+(r.unlockStage+1),500,y+39,9,MUTED,Paint.Align.RIGHT);y=y2+7;}pager(c,bottom,pages,researchPage,"res_prev","res_next");}

    private void drawMenu(Canvas c,float top,float bottom){header(c,L10n.t(state.language,"menu"),"Pixel Empire 2.0",top);float y=top+54;panel(c,12,y,528,y+116,PANEL,state.availableLegacyStars()>0?GOLD:STROKE,12);text(c,L10n.t(state.language,"prestige"),29,y+28,15,TEXT,Paint.Align.LEFT);text(c,L10n.t(state.language,"prestige_desc"),29,y+51,9,MUTED,Paint.Align.LEFT);int gain=state.availableLegacyStars();text(c,gain>0?"+"+gain+" ★":L10n.t(state.language,"prestige_need"),500,y+31,10,gain>0?GOLD:MUTED,Paint.Align.RIGHT);String asc=System.currentTimeMillis()<ascendArmedUntil?L10n.t(state.language,"prestige_ready")+"?":L10n.t(state.language,"prestige");button(c,29,y+69,511,y+104,asc,gain>0?GOLD:PANEL2,gain>0?BG:MUTED,gain>0);if(gain>0)hits.add(new Hit(new RectF(29,y+69,511,y+104),"ascend",""));y+=130;
        setting(c,y,L10n.t(state.language,"sound"),state.soundEnabled,"sound");y+=58;setting(c,y,L10n.t(state.language,"haptics"),state.hapticsEnabled,"haptic");y+=58;setting(c,y,L10n.t(state.language,"notation"),state.compactNumbers,"notation");y+=58;setting(c,y,L10n.t(state.language,"low_power"),state.lowPower,"power");y+=58;
        panel(c,12,y,528,y+54,PANEL,STROKE,9);text(c,L10n.t(state.language,"language"),29,y+33,11,TEXT,Paint.Align.LEFT);text(c,L10n.languageLabel(state.language)+"  ›",500,y+33,11,CYAN,Paint.Align.RIGHT);hits.add(new Hit(new RectF(12,y,528,y+54),"setting","lang"));y+=66;
        panel(c,12,y,528,Math.min(bottom-68,y+118),PANEL,STROKE,10);stat(c,y+25,L10n.t(state.language,"total_earned"),state.format(state.lifetimeCoins));stat(c,y+48,L10n.t(state.language,"total_taps"),Long.toString(state.totalTaps));stat(c,y+71,L10n.t(state.language,"best_combo"),"x"+state.bestCombo);stat(c,y+94,L10n.t(state.language,"play_time"),GameState.formatDuration(state.playSeconds));
        float ry=bottom-55;String rt=System.currentTimeMillis()<resetArmedUntil?L10n.t(state.language,"reset_confirm"):L10n.t(state.language,"reset");button(c,12,ry,528,bottom-6,rt,Color.rgb(69,28,35),RED,true);hits.add(new Hit(new RectF(12,ry,528,bottom-6),"reset",""));}

    private void setting(Canvas c,float y,String label,boolean on,String id){panel(c,12,y,528,y+52,PANEL,STROKE,9);text(c,label,29,y+32,11,TEXT,Paint.Align.LEFT);p.setColor(on?GREEN:Color.rgb(60,72,92));c.drawRoundRect(new RectF(448,y+14,508,y+38),12,12,p);p.setColor(Color.WHITE);c.drawCircle(on?496:460,y+26,9,p);hits.add(new Hit(new RectF(12,y,528,y+52),"setting",id));}
    private void stat(Canvas c,float y,String l,String v){text(c,l,29,y,9,MUTED,Paint.Align.LEFT);text(c,v,500,y,10,TEXT,Paint.Align.RIGHT);}

    private void header(Canvas c,String title,String sub,float top){text(c,title,18,top+27,19,TEXT,Paint.Align.LEFT);text(c,sub,522,top+27,9,MUTED,Paint.Align.RIGHT);p.setColor(Color.rgb(37,50,70));c.drawRect(18,top+40,522,top+42,p);}
    private void pager(Canvas c,float bottom,int pages,int page,String prev,String next){if(pages<=1)return;float y=bottom-42;button(c,12,y,145,bottom-4,"‹",PANEL2,TEXT,page>0);button(c,395,y,528,bottom-4,"›",PANEL2,TEXT,page<pages-1);text(c,(page+1)+" / "+pages,270,y+25,10,MUTED,Paint.Align.CENTER);hits.add(new Hit(new RectF(12,y,145,bottom-4),prev,""));hits.add(new Hit(new RectF(395,y,528,bottom-4),next,""));}

    private void drawEvent(Canvas c){if(state.activeEventType<0||offlinePopup||!state.tutorialSeen)return;float y=86;int col=state.activeEventType==0?GOLD:state.activeEventType==1?PURPLE:state.activeEventType==2?RED:state.activeEventType==3?GREEN:CYAN;String key=state.activeEventType==0?"event_gold":state.activeEventType==1?"event_crystal":state.activeEventType==2?"event_frenzy":state.activeEventType==3?"event_auto":"event_xp";panel(c,44,y,496,y+42,Color.argb(235,12,20,34),col,12);text(c,L10n.t(state.language,key),62,y+27,10,col,Paint.Align.LEFT);long left=Math.max(0,(state.activeEventUntil-System.currentTimeMillis()+999)/1000);text(c,left+"s",480,y+27,9,TEXT,Paint.Align.RIGHT);hits.add(new Hit(new RectF(44,y,496,y+42),"event",""));}

    private void drawNav(Canvas c){float y=logicalH-70;p.setColor(Color.rgb(7,13,24));c.drawRect(0,y,540,logicalH,p);p.setColor(Color.rgb(38,51,72));c.drawRect(0,y,540,y+2,p);String[] labels={L10n.t(state.language,"world"),L10n.t(state.language,"upgrades"),L10n.t(state.language,"missions"),L10n.t(state.language,"research"),L10n.t(state.language,"menu")};float w=108;for(int i=0;i<5;i++){float x=i*w;if(tab==i){p.setColor(Color.rgb(20,31,51));c.drawRect(x+4,y+4,x+w-4,logicalH-4,p);p.setColor(GOLD);c.drawRect(x+23,y+4,x+w-23,y+7,p);}navIcon(c,i,x+w/2,y+24,tab==i?GOLD:MUTED);text(c,labels[i],x+w/2,y+55,7.5f,tab==i?TEXT:MUTED,Paint.Align.CENTER);hits.add(new Hit(new RectF(x,y,x+w,logicalH),"tab",Integer.toString(i)));}}
    private void navIcon(Canvas c,int i,float x,float y,int col){p.setColor(col);if(i==0){c.drawRect(x-13,y-2,x+13,y+13,p);c.drawRect(x-8,y-12,x+8,y+15,p);}else if(i==1){c.drawRect(x-13,y-10,x+13,y-4,p);c.drawRect(x-10,y,x+10,y+13,p);}else if(i==2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawRect(x-12,y-11,x+12,y+14,p);c.drawLine(x-7,y-3,x+7,y-3,p);c.drawLine(x-7,y+4,x+7,y+4,p);p.setStyle(Paint.Style.FILL);}else if(i==3){diamond(c,x,y,12,col);}else{c.drawCircle(x,y+1,11,p);p.setColor(BG);c.drawCircle(x,y+1,4,p);}}

    private void drawFx(Canvas c){for(Particle q:particles){int a=(int)(255*Math.max(0,Math.min(1,q.life)));p.setColor((q.color&0x00ffffff)|(a<<24));c.drawRect(q.x-q.size/2,q.y-q.size/2,q.x+q.size/2,q.y+q.size/2,p);}for(Floating f:floats){int a=(int)(255*Math.max(0,Math.min(1,f.life)));text(c,f.text,f.x,f.y,14,(f.color&0xffffff)|(a<<24),Paint.Align.CENTER);}}
    private void drawToast(Canvas c){if(toast.isEmpty()||System.currentTimeMillis()>=toastUntil)return;float y=logicalH-128;panel(c,48,y,492,y+44,Color.argb(240,13,22,38),GOLD,11);text(c,toast,270,y+28,10,TEXT,Paint.Align.CENTER);}

    private void drawOffline(Canvas c){p.setColor(Color.argb(220,0,0,0));c.drawRect(0,0,540,logicalH,p);float h=300,y=(logicalH-h)/2;panel(c,40,y,500,y+h,Color.rgb(13,22,38),GOLD,18);coin(c,270,y+55,25);text(c,L10n.t(state.language,"welcome_back"),270,y+103,20,TEXT,Paint.Align.CENTER);text(c,L10n.t(state.language,"away")+": "+GameState.formatDuration(state.startupOfflineSeconds),270,y+136,10,MUTED,Paint.Align.CENTER);text(c,L10n.t(state.language,"offline"),270,y+164,10,MUTED,Paint.Align.CENTER);text(c,"+"+state.format(state.startupOfflineGain),270,y+202,27,GOLD,Paint.Align.CENTER);if(state.startupOfflineLevels>0)text(c,"+"+state.startupOfflineLevels+" "+L10n.t(state.language,"level"),270,y+225,10,CYAN,Paint.Align.CENTER);button(c,85,y+246,455,y+284,L10n.t(state.language,"continue"),GOLD,BG,true);hits.add(new Hit(new RectF(40,y,500,y+h),"offline",""));}

    private void drawTutorial(Canvas c){p.setColor(Color.argb(232,2,7,16));c.drawRect(0,0,540,logicalH,p);float y=logicalH/2-180;panel(c,32,y,508,y+360,Color.rgb(12,21,37),GOLD,18);String body;if(tutorialStep==0)body=L10n.t(state.language,"tutorial_1");else if(tutorialStep==1)body=L10n.t(state.language,"tutorial_2");else if(tutorialStep==2)body=L10n.t(state.language,"tutorial_3");else body=L10n.t(state.language,"tutorial_4");drawMiniLogo(c,270,y+80,tutorialStep);text(c,L10n.t(state.language,"tutorial_title"),270,y+145,23,TEXT,Paint.Align.CENTER);wrapText(c,body,270,y+193,420,12,MUTED);text(c,(tutorialStep+1)+" / 4",270,y+280,10,MUTED,Paint.Align.CENTER);text(c,L10n.t(state.language,"tap_continue"),270,y+326,11,GOLD,Paint.Align.CENTER);}
    private void drawMiniLogo(Canvas c,float x,float y,int step){if(step==0){hut(c,x,y+40,.55f);}else if(step==1){coin(c,x,y,25);diamond(c,x+45,y,13,PURPLE);}else if(step==2){castle(c,x,y+55,.45f,9);}else future(c,x,y+70,.35f,23);}

    private void wrapText(Canvas c,String s,float x,float y,float maxW,float size,int color){String[] words=s.split(" ");String line="";int n=0;for(String w:words){String test=line.isEmpty()?w:line+" "+w;tp.setTextSize(size);if(tp.measureText(test)>maxW&&!line.isEmpty()){text(c,line,x,y+n*20,size,color,Paint.Align.CENTER);n++;line=w;}else line=test;}if(!line.isEmpty())text(c,line,x,y+n*20,size,color,Paint.Align.CENTER);}

    @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX()/scale,y=e.getY()/scale;if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;moved=false;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){if(Math.abs(x-downX)>13||Math.abs(y-downY)>13)moved=true;return true;}if(e.getAction()==MotionEvent.ACTION_UP){performClick();if(moved)return true;if(!state.tutorialSeen){tutorialStep++;if(tutorialStep>=4){state.tutorialSeen=true;state.save(getContext());showToast("Pixel Empire",1300);}invalidate();return true;}if(offlinePopup){offlinePopup=false;playFeedback(false);invalidate();return true;}for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(h.r.contains(x,y)){handleHit(h,x,y);break;}}return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}

    private void handleHit(Hit h,float x,float y){switch(h.type){
        case "tab":tab=Integer.parseInt(h.id);playFeedback(false);break;
        case "tap":doTap(x,y);break;
        case "daily":if(state.canClaimDaily()){int r=state.claimDaily();showToast(L10n.t(state.language,"daily")+" +"+r+" ◆",1800);burst(x,y,GOLD,22);playFeedback(true);}break;
        case "event":int t=state.activeEventType;state.collectEvent(t);showToast(t==1?"+◆":t==4?"XP x4":"BOOST!",1600);burst(x,y,t==1?PURPLE:t==4?CYAN:GOLD,32);playFeedback(true);break;
        case "upgrade":if(state.buyUpgrade(h.id)){showToast(L10n.upgradeName(state.language,h.id)+" +1",850);playFeedback(false);}else{showToast("…",600);playError();}break;
        case "up_prev":if(upgradePage>0)upgradePage--;break;case "up_next":if(upgradePage<2)upgradePage++;break;
        case "goal_mode":goalMode=Integer.parseInt(h.id);missionPage=0;break;
        case "mission":int mi=Integer.parseInt(h.id);if(state.claimMission(mi)){showToast(L10n.t(state.language,"reward")+"!",1000);burst(x,y,CYAN,16);playFeedback(true);}break;
        case "mission_prev":if(missionPage>0)missionPage--;break;case "mission_next":missionPage++;break;
        case "research":if(state.buyResearch(h.id)){showToast(L10n.researchName(state.language,h.id),1100);burst(x,y,PURPLE,18);playFeedback(true);}else playError();break;
        case "res_prev":if(researchPage>0)researchPage--;break;case "res_next":if(researchPage<2)researchPage++;break;
        case "setting":setting(h.id);break;case "ascend":ascend();break;case "reset":reset();break;case "offline":offlinePopup=false;break;
    }state.save(getContext());invalidate();}

    private void doTap(float x,float y){long now=System.currentTimeMillis();if(now-lastTapAt<=780)combo=Math.min(50,combo+1);else combo=1;lastTapAt=now;boolean crit=random.nextDouble()<.06;double amount=state.tap(combo,crit);floats.add(new Floating(x,y-14,(crit?L10n.t(state.language,"critical")+" ":"+")+state.format(amount),crit?PURPLE:GOLD));if(floats.size()>18)floats.remove(0);burst(x,y,crit?PURPLE:GOLD,crit?18:8);playFeedback(crit);}
    private void burst(float x,float y,int color,int n){for(int i=0;i<n;i++){double a=random.nextDouble()*Math.PI*2;float sp=30+random.nextFloat()*105;particles.add(new Particle(x,y,(float)Math.cos(a)*sp,(float)Math.sin(a)*sp-35,.45f+random.nextFloat()*.55f,2+random.nextFloat()*5,color));}while(particles.size()>100)particles.remove(0);}

    private void setting(String id){if("sound".equals(id))state.soundEnabled=!state.soundEnabled;else if("haptic".equals(id))state.hapticsEnabled=!state.hapticsEnabled;else if("notation".equals(id))state.compactNumbers=!state.compactNumbers;else if("power".equals(id))state.lowPower=!state.lowPower;else if("lang".equals(id))state.language=L10n.nextLang(state.language);playFeedback(false);}
    private void ascend(){int gain=state.availableLegacyStars();if(gain<=0)return;long now=System.currentTimeMillis();if(now>ascendArmedUntil){ascendArmedUntil=now+5000;showToast(L10n.t(state.language,"prestige")+"?  +"+gain+" ★",1800);return;}if(state.ascend()){ascendArmedUntil=0;tab=0;showToast("+"+gain+" ★",2200);burst(270,380,GOLD,45);playFeedback(true);lastSeenLevel=state.level;lastSeenStage=state.stage;}}
    private void reset(){long now=System.currentTimeMillis();if(now>resetArmedUntil){resetArmedUntil=now+5000;showToast(L10n.t(state.language,"reset_confirm"),1800);playError();return;}state.hardReset(getContext());postDelayed(()->((Activity)getContext()).recreate(),350);}

    private void playFeedback(boolean strong){if(state.soundEnabled&&tone!=null)try{tone.startTone(strong?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,strong?70:30);}catch(Throwable ignored){}if(state.hapticsEnabled&&vibrator!=null&&vibrator.hasVibrator())try{if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createOneShot(strong?38:14,strong?130:60));else vibrator.vibrate(strong?38:14);}catch(Throwable ignored){}}
    private void playError(){if(state.soundEnabled&&tone!=null)try{tone.startTone(ToneGenerator.TONE_PROP_NACK,55);}catch(Throwable ignored){}if(state.hapticsEnabled&&vibrator!=null&&vibrator.hasVibrator())try{if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createOneShot(28,80));else vibrator.vibrate(28);}catch(Throwable ignored){}}

    private void panel(Canvas c,float x1,float y1,float x2,float y2,int fill,int stroke,float radius){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawRoundRect(new RectF(x1,y1,x2,y2),radius,radius,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(stroke);c.drawRoundRect(new RectF(x1,y1,x2,y2),radius,radius,p);p.setStyle(Paint.Style.FILL);}
    private void button(Canvas c,float x1,float y1,float x2,float y2,String label,int fill,int color,boolean enabled){p.setColor(enabled?fill:Color.rgb(37,46,62));c.drawRoundRect(new RectF(x1,y1,x2,y2),9,9,p);text(c,label,(x1+x2)/2,(y1+y2)/2+4,9.5f,enabled?color:MUTED,Paint.Align.CENTER);}
    private void progress(Canvas c,float x1,float y1,float x2,float y2,float v,int col){p.setColor(Color.rgb(39,50,68));c.drawRoundRect(new RectF(x1,y1,x2,y2),5,5,p);p.setColor(col);c.drawRoundRect(new RectF(x1,y1,x1+(x2-x1)*Math.max(0,Math.min(1,v)),y2),5,5,p);}
    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align){tp.setShader(null);tp.setStyle(Paint.Style.FILL);tp.setColor(color);tp.setTextSize(size);tp.setTextAlign(align);tp.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText(s,x,y,tp);}
    private void pixel(Canvas c,float x,float y,float size,int col){p.setColor(col);c.drawRect(x-size/2,y-size/2,x+size/2,y+size/2,p);}
    private void coin(Canvas c,float x,float y,float r){p.setColor(Color.rgb(180,110,8));c.drawRect(x-r,y-r/2,x+r,y+r/2,p);c.drawRect(x-r/2,y-r,x+r/2,y+r,p);p.setColor(GOLD);c.drawRect(x-r+3,y-r/2+2,x+r-3,y+r/2-2,p);c.drawRect(x-r/2+2,y-r+3,x+r/2-2,y+r-3,p);p.setColor(Color.rgb(255,245,190));c.drawRect(x-r/3,y-r/2,x-1,y-2,p);}
    private void diamond(Canvas c,float x,float y,float r,int col){p.setColor(col);Path d=new Path();d.moveTo(x,y-r);d.lineTo(x+r,y);d.lineTo(x,y+r);d.lineTo(x-r,y);d.close();c.drawPath(d,p);p.setColor(Color.argb(130,255,255,255));c.drawRect(x-r*.28f,y-r*.48f,x,y-r*.18f,p);}
}
