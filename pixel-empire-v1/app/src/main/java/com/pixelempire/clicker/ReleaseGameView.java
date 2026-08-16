package com.pixelempire.clicker;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

import java.util.*;

/** Polished single-view game renderer. All UI is dynamic; the rich world image is an art layer only. */
public final class ReleaseGameView extends View implements Runnable {
    private static final float W=720f;
    private static final int BG=Color.rgb(5,12,18), PANEL=Color.rgb(13,27,38), PANEL2=Color.rgb(20,40,56), STROKE=Color.rgb(57,91,110);
    private static final int TEXT=Color.rgb(244,248,252), MUTED=Color.rgb(164,183,194), GOLD=Color.rgb(255,190,31), GREEN=Color.rgb(91,211,66), CYAN=Color.rgb(45,209,235), PURPLE=Color.rgb(173,88,255), RED=Color.rgb(244,71,78), ORANGE=Color.rgb(255,131,30);

    private final ReleaseGameState s;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tp=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rng=new Random();
    private final List<Hit> hits=new ArrayList<>();
    private final List<FloatFx> fx=new ArrayList<>();
    private final Bitmap world;
    private final Vibrator vib;
    private ToneGenerator tone;

    private float scale=1, logicalH=1500;
    private boolean running=false;
    private long lastNs, lastAutosave, lastTapAt, resetArmedUntil, rebirthArmedUntil;
    private double secAcc;
    private int tab=0, comboTaps=0, buildPage=0, heroPage=0, missionPage=0, missionMode=0, buyMode=0, tutorialStep=0;
    private float downX,downY;private boolean moved;
    private boolean offlinePopup;
    private String toast="";private long toastUntil;

    private static final class Hit{final RectF r;final String t;final int id;Hit(RectF r,String t,int id){this.r=r;this.t=t;this.id=id;}}
    private static final class FloatFx{float x,y,life;final String text;final int color;FloatFx(float x,float y,String t,int c){this.x=x;this.y=y;this.text=t;this.color=c;this.life=1f;}}

    public ReleaseGameView(Context c,ReleaseGameState state){
        super(c);s=state;world=ReleaseArt.world();
        tp.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        vib=(Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
        try{tone=new ToneGenerator(AudioManager.STREAM_MUSIC,26);}catch(Throwable ignored){}
        offlinePopup=s.startupOfflineGain>0.01;
        setFocusable(true);setBackgroundColor(BG);
    }

    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();running=true;lastNs=System.nanoTime();post(this);}
    @Override protected void onDetachedFromWindow(){running=false;removeCallbacks(this);if(tone!=null)try{tone.release();}catch(Throwable ignored){}super.onDetachedFromWindow();}

    @Override public void run(){
        if(!running)return;long n=System.nanoTime();double dt=Math.min(.2,Math.max(0,(n-lastNs)/1e9));lastNs=n;
        if(comboTaps>0&&System.currentTimeMillis()-lastTapAt>1800)comboTaps=0;
        s.tick(dt);secAcc+=dt;if(secAcc>=1){long q=(long)secAcc;s.playSeconds+=q;secAcc-=q;}
        updateFx((float)dt);
        long now=System.currentTimeMillis();if(lastAutosave==0||now-lastAutosave>5000){s.save(getContext());lastAutosave=now;}
        invalidate();postDelayed(this,s.lowPower?100:33);
    }

    private void updateFx(float dt){for(int i=fx.size()-1;i>=0;i--){FloatFx f=fx.get(i);f.life-=dt*.7f;f.y-=38*dt;if(f.life<=0)fx.remove(i);}}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);scale=getWidth()/W;if(scale<=0)scale=1;logicalH=getHeight()/scale;c.save();c.scale(scale,scale);hits.clear();
        p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(BG);c.drawRect(0,0,W,logicalH,p);
        if(tab==0)drawWorld(c);else drawSectionBackground(c);
        drawTop(c);
        if(tab==1)drawBuild(c);else if(tab==2)drawHeroes(c);else if(tab==3)drawResearch(c);else if(tab==4)drawMissions(c);else if(tab==5)drawShop(c);else if(tab==6)drawEmpire(c);
        drawBottomNav(c);drawFx(c);drawToast(c);
        if(offlinePopup)drawOffline(c);if(!s.tutorialSeen)drawTutorial(c);
        c.restore();
    }

    private float navY(){return logicalH-104f;}
    private float worldBottom(){return navY()-122f;}

    private void drawTop(Canvas c){
        panel(c,8,8,712,106,Color.argb(248,6,17,24),Color.rgb(76,112,125),18);
        resourceChip(c,18,17,172,65,GOLD,"●",s.format(s.coins),s.format(s.getCps())+"/s");
        resourceChip(c,182,17,328,65,CYAN,"◆",Integer.toString(s.crystals),"");
        resourceChip(c,338,17,494,65,PURPLE,"◇",Integer.toString(s.researchPoints),"");
        resourceChip(c,504,17,652,65,GOLD,"★",Integer.toString(s.legacyStars),"");
        panel(c,660,17,702,65,PANEL2,STROKE,10);text(c,"☰",681,50,24,TEXT,Paint.Align.CENTER);hits.add(new Hit(new RectF(658,15,706,69),"tab",6));
        text(c,ReleaseGameState.tr(s.language,"Epoka ","Era ","Era ","Éra ","Эра ","时代 ")+(s.stage+1)+" • "+ReleaseContent.stageName(s.language,s.stage),24,82,13,TEXT,Paint.Align.LEFT);
        text(c,ReleaseGameState.tr(s.language,"Poziom ","Level ","Nivel ","Úroveň ","Уровень ","等级 ")+s.level+" / "+ReleaseContent.MAX_LEVEL,690,82,11,MUTED,Paint.Align.RIGHT);
        progress(c,24,88,696,101,(float)s.levelProgress(),GOLD);
        p.setColor(Color.rgb(1,7,11));c.drawRect(0,106,W,114,p);
    }

    private void resourceChip(Canvas c,float x1,float y1,float x2,float y2,int col,String icon,String val,String sub){
        panel(c,x1,y1,x2,y2,Color.rgb(21,40,49),Color.rgb(62,88,99),12);text(c,icon,x1+17,y1+31,15,col,Paint.Align.CENTER);text(c,val,x1+34,y1+27,16,TEXT,Paint.Align.LEFT);if(!sub.isEmpty())text(c,sub,x1+34,y1+43,8,GREEN,Paint.Align.LEFT);
    }

    private void drawWorld(Canvas c){
        float y1=114,y2=worldBottom();
        if(world!=null){p.setFilterBitmap(true);c.drawBitmap(world,null,new RectF(0,y1,W,y2),p);p.setFilterBitmap(false);}else{p.setColor(Color.rgb(40,108,73));c.drawRect(0,y1,W,y2,p);}
        int tint=Color.TRANSPARENT;if(s.stage>=32)tint=Color.argb(80,54,15,112);else if(s.stage>=24)tint=Color.argb(55,0,120,150);else if(s.stage>=16)tint=Color.argb(55,70,65,52);else if(s.stage<4)tint=Color.argb(28,255,183,70);
        if(tint!=Color.TRANSPARENT){p.setColor(tint);c.drawRect(0,y1,W,y2,p);}
        drawEraStructure(c,360,y2-165,s.stage,(float)s.levelProgress());
        hits.add(new Hit(new RectF(0,y1,W,y2),"tap",0));
        drawWorldLeftRail(c,y1+80);drawWorldRightRail(c,y1+60);drawBoss(c,y1,y2);drawCombo(c,y2);drawPowerTap(c,y2);
    }

    private void drawEraStructure(Canvas c,float cx,float ground,int stage,float progress){
        if(stage<8)return;
        float grow=.82f+.18f*progress;
        if(stage<16)drawCastle(c,cx,ground,grow,stage);
        else if(stage<24)drawFactory(c,cx,ground,grow,stage);
        else if(stage<32)drawMetropolis(c,cx,ground,grow,stage);
        else drawInfinity(c,cx,ground,grow,stage);
    }
    private void drawCastle(Canvas c,float x,float y,float g,int st){float w=(170+(st-8)*10)*g,h=(120+(st-8)*8)*g;int stone=Color.rgb(196,193,181);p.setColor(stone);c.drawRoundRect(new RectF(x-w/2,y-h,x+w/2,y),5,5,p);for(int k=-1;k<=1;k++){float tx=x+k*w*.42f,th=h+(k==0?55:32)*g;p.setColor(stone);c.drawRect(tx-27*g,y-th,tx+27*g,y,p);p.setColor(Color.rgb(58,68,75));c.drawRect(tx-33*g,y-th-17*g,tx+33*g,y-th,p);}p.setColor(Color.rgb(51,39,29));c.drawRoundRect(new RectF(x-20*g,y-52*g,x+20*g,y),12,12,p);for(int k=-2;k<=2;k++){p.setColor(GOLD);c.drawRect(x+k*32*g-4,y-h+28*g,x+k*32*g+4,y-h+41*g,p);}}
    private void drawFactory(Canvas c,float x,float y,float g,int st){float w=230*g,h=(135+(st-16)*12)*g;p.setColor(Color.rgb(68,76,83));c.drawRect(x-w/2,y-h,x+w/2,y,p);p.setColor(Color.rgb(40,47,53));c.drawRect(x-w*.40f,y-h-100*g,x-w*.28f,y,p);c.drawRect(x+w*.23f,y-h-75*g,x+w*.35f,y,p);for(int i=0;i<4;i++){p.setColor(ORANGE);c.drawRect(x-w/2+25+i*52,y-h+30,x-w/2+47+i*52,y-h+53,p);}p.setColor(CYAN);c.drawRect(x-w/2,y-h-5,x+w/2,y-h,p);}
    private void drawMetropolis(Canvas c,float x,float y,float g,int st){for(int i=-2;i<=2;i++){float ww=(48+(i%2==0?15:0))*g,hh=(130+(i+2)*27+(st-24)*10)*g,xx=x+i*60*g;p.setColor(i%2==0?Color.rgb(38,60,84):Color.rgb(50,73,95));c.drawRoundRect(new RectF(xx-ww/2,y-hh,xx+ww/2,y),8,8,p);for(float yy=y-hh+18;yy<y-15;yy+=22){p.setColor(((int)yy)%44==0?PURPLE:CYAN);c.drawRect(xx-ww*.28f,yy,xx+ww*.28f,yy+5,p);}}}
    private void drawInfinity(Canvas c,float x,float y,float g,int st){float h=(250+(st-32)*32)*g,w=(120+(st-32)*5)*g;p.setColor(Color.rgb(20,29,55));Path q=new Path();q.moveTo(x-w/2,y);q.lineTo(x-w*.22f,y-h);q.lineTo(x,y-h-90*g);q.lineTo(x+w*.22f,y-h);q.lineTo(x+w/2,y);q.close();c.drawPath(q,p);for(int i=0;i<10;i++){float yy=y-h+20+i*h/11;p.setColor(i%2==0?CYAN:PURPLE);c.drawRoundRect(new RectF(x-w*.28f,yy,x+w*.28f,yy+6),3,3,p);}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);for(int i=0;i<3;i++){p.setColor(i%2==0?CYAN:PURPLE);c.drawOval(new RectF(x-90-i*25,y-h-40-i*9,x+90+i*25,y-h+20+i*9),p);}p.setStyle(Paint.Style.FILL);}

    private void drawWorldLeftRail(Canvas c,float y){
        sideButton(c,12,y,112,y+72,GOLD,"▣",tr("DZIENNE","DAILY"),s.canClaimDaily()?1:0,"daily");y+=82;
        sideButton(c,12,y,112,y+72,RED,"◎",tr("MISJE","MISSIONS"),countReadyMissions(),"missions");y+=82;
        sideButton(c,12,y,112,y+72,GOLD,"♛",tr("OSIĄG.","ACHIEV."),0,"achievements");y+=82;
        sideButton(c,12,y,112,y+72,PURPLE,"◆",tr("EVENT","EVENT"),s.eventType>=0?1:0,"event");
    }
    private void sideButton(Canvas c,float x1,float y1,float x2,float y2,int col,String icon,String label,int badge,String type){panel(c,x1,y1,x2,y2,Color.argb(235,10,24,32),Color.argb(220,74,100,113),12);text(c,icon,(x1+x2)/2,y1+31,24,col,Paint.Align.CENTER);text(c,label,(x1+x2)/2,y1+58,9,TEXT,Paint.Align.CENTER);if(badge>0){p.setColor(RED);c.drawCircle(x2-8,y1+9,12,p);text(c,Integer.toString(badge),x2-8,y1+13,9,Color.WHITE,Paint.Align.CENTER);}hits.add(new Hit(new RectF(x1,y1,x2,y2),type,0));}

    private void drawWorldRightRail(Canvas c,float y){
        float x1=545,x2=708;panel(c,x1,y-12,x2,y+260,Color.argb(225,7,18,27),Color.argb(210,82,104,113),14);text(c,tr("AKTYWNE EVENTY","ACTIVE EVENTS"),(x1+x2)/2,y+10,10,TEXT,Paint.Align.CENTER);
        String[] n={tr("ZŁOTA GORĄCZKA","GOLD RUSH"),tr("KOMETA KRYSZTAŁU","CRYSTAL COMET"),tr("SZAŁ BUDOWY","BUILD FRENZY"),tr("ARCHITEKT","ARCHITECT"),tr("POWER TAP","POWER TAP")};
        int[] cols={GOLD,PURPLE,ORANGE,GREEN,CYAN};
        for(int i=0;i<3;i++){float yy=y+25+i*69;int type=s.eventType>=0?(s.eventType+i)%5:i;panel(c,x1+8,yy,x2-8,yy+60,Color.argb(240,16,34,44),cols[type],8);text(c,type==0?"●":type==1?"◆":type==2?"⚒":type==3?"▰":"⚡",x1+29,yy+35,20,cols[type],Paint.Align.CENTER);text(c,n[type],x1+49,yy+24,9,TEXT,Paint.Align.LEFT);String time=eventTimeText(type);text(c,time,x1+49,yy+43,8,cols[type],Paint.Align.LEFT);if(s.eventType==type){hits.add(new Hit(new RectF(x1+8,yy,x2-8,yy+60),"event",type));}}
        button(c,x1+8,y+236,x2-8,y+270,tr("BOOSTERY","BOOSTERS"),Color.rgb(19,94,161),TEXT,true);hits.add(new Hit(new RectF(x1+8,y+232,x2-8,y+274),"tab",5));
    }
    private String eventTimeText(int type){long now=System.currentTimeMillis();if(s.eventType==type&&s.eventUntil>now)return ReleaseGameState.duration((s.eventUntil-now)/1000);if(type==0&&s.goldBoostUntil>now)return "x2 • "+ReleaseGameState.duration((s.goldBoostUntil-now)/1000);if(type==2&&s.buildBoostUntil>now)return "x4 XP • "+ReleaseGameState.duration((s.buildBoostUntil-now)/1000);if(type==4&&s.tapBoostUntil>now)return "x3 TAP • "+ReleaseGameState.duration((s.tapBoostUntil-now)/1000);return tr("gotowe","ready");}

    private void drawCombo(Canvas c,float y2){float y=y2+12;panel(c,12,y,548,y+98,Color.argb(245,8,20,27),Color.argb(230,72,104,116),14);double mult=ReleaseGameState.comboMultiplier(comboTaps);int next=ReleaseGameState.comboNextThreshold(comboTaps);text(c,"COMBO x"+String.format(Locale.US,"%.1f",mult),32,y+33,20,GOLD,Paint.Align.LEFT);text(c,comboTaps+" / "+next+" "+tr("kliknięć","taps"),525,y+31,10,TEXT,Paint.Align.RIGHT);progress(c,32,y+48,525,y+60,Math.min(1f,comboTaps/(float)next),GOLD);int step=Math.max(1,next/50-2);for(int i=0;i<5;i++){int th=(step+i)*50;float xx=55+i*108;text(c,Integer.toString(th),xx,y+78,8,MUTED,Paint.Align.CENTER);text(c,"x"+String.format(Locale.US,"%.1f",ReleaseGameState.comboMultiplier(th)),xx,y+92,9,th<=comboTaps?GOLD:MUTED,Paint.Align.CENTER);}}
    private void drawPowerTap(Canvas c,float y2){float y=y2+12;boolean on=System.currentTimeMillis()<s.tapBoostUntil;panel(c,558,y,708,y+98,on?Color.rgb(40,121,24):Color.rgb(20,87,27),GREEN,14);text(c,"⚡",582,y+47,27,GOLD,Paint.Align.CENTER);text(c,on?"POWER x3":"POWER TAP",635,y+34,13,TEXT,Paint.Align.CENTER);text(c,on?ReleaseGameState.duration((s.tapBoostUntil-System.currentTimeMillis())/1000):s.powerTapCharges+" / 3",635,y+61,11,TEXT,Paint.Align.CENTER);text(c,on?tr("AKTYWNY","ACTIVE"):tr("AKTYWUJ","ACTIVATE"),635,y+83,8,GREEN,Paint.Align.CENTER);hits.add(new Hit(new RectF(558,y,708,y+98),"power",0));}

    private void drawBoss(Canvas c,float y1,float y2){if(!s.bossActive)return;float cy=(y1+y2)/2+20;panel(c,170,y1+35,550,y1+112,Color.argb(235,22,11,29),PURPLE,15);text(c,tr("BOSS • STRAŻNIK EPOKI","BOSS • ERA GUARDIAN"),360,y1+64,14,TEXT,Paint.Align.CENTER);progress(c,195,y1+76,525,y1+91,(float)(s.bossHp/Math.max(1,s.bossMaxHp)),RED);text(c,s.format(s.bossHp)+" / "+s.format(s.bossMaxHp)+" HP",360,y1+107,9,MUTED,Paint.Align.CENTER);p.setColor(Color.argb(210,30,18,45));c.drawCircle(360,cy,86,p);p.setColor(PURPLE);c.drawCircle(360,cy-15,54,p);p.setColor(Color.rgb(17,14,26));c.drawRect(333,cy-32,348,cy-12,p);c.drawRect(372,cy-32,387,cy-12,p);p.setColor(RED);c.drawRect(338,cy-27,345,cy-20,p);c.drawRect(377,cy-27,384,cy-20,p);text(c,ReleaseGameState.duration(Math.max(0,(s.bossUntil-System.currentTimeMillis())/1000)),360,cy+115,14,GOLD,Paint.Align.CENTER);}

    private void drawSectionBackground(Canvas c){p.setShader(new LinearGradient(0,110,0,logicalH,BG,Color.rgb(7,23,35),Shader.TileMode.CLAMP));c.drawRect(0,110,W,logicalH,p);p.setShader(null);for(int i=0;i<30;i++){p.setColor(Color.argb(30,75,140,160));c.drawCircle((i*113)%720,150+(i*79)%1100,2+(i%3),p);}}

    private void drawBuild(Canvas c){float top=125,bottom=navY()-10;header(c,tr("BUDOWA I PRODUKCJA","BUILD & PRODUCTION"),tr("Kupuj x1 / x10 / x25 / x100 / MAX","Buy x1 / x10 / x25 / x100 / MAX"),top);drawBuyModes(c,top+52);int per=6,pages=(ReleaseContent.BUILDINGS.length+per-1)/per;buildPage=clamp(buildPage,0,pages-1);int start=buildPage*per,end=Math.min(ReleaseContent.BUILDINGS.length,start+per);float y=top+105;float row=Math.min(122,(bottom-y-54)/Math.max(1,end-start));for(int i=start;i<end;i++){ReleaseContent.BuildingDef d=ReleaseContent.BUILDINGS[i];float y2=y+row-8;boolean unlocked=s.stage>=d.unlockStage;panel(c,18,y,702,y2,PANEL,unlocked?Color.rgb(151,43,54):STROKE,14);drawBuildingIcon(c,55,(y+y2)/2,i,unlocked);text(c,ReleaseContent.buildingName(s.language,i),96,y+30,14,unlocked?TEXT:MUTED,Paint.Align.LEFT);text(c,tr("Poziom ","Level ")+s.buildings[i]+" • "+tr("Produkcja ","Production ")+s.format(d.baseCps*Math.max(1,s.buildings[i]))+"/s",96,y+53,9,MUTED,Paint.Align.LEFT);int cnt=selectedBuyCount(i);double cost=cnt<=0?Double.POSITIVE_INFINITY:s.buildingCost(i,cnt);String count=buyMode==4?"MAX":"x"+buyModeCount();boolean can=unlocked&&cnt>0&&s.coins>=cost;progress(c,96,y+68,480,y+80,s.buildings[i]==0?0:Math.min(1,s.buildings[i]/100f),GREEN);button(c,500,y+22,684,y+75,tr("KUP ","BUY ")+count+"\n"+s.format(cost),can?Color.rgb(43,154,36):PANEL2,can?Color.WHITE:MUTED,can);if(unlocked)hits.add(new Hit(new RectF(490,y+15,690,y+84),"buybuilding",i));if(!unlocked)text(c,tr("Odblokuj epokę ","Unlock era ")+(d.unlockStage+1),500,y+95,9,MUTED,Paint.Align.RIGHT);y+=row;}pager(c,bottom-5,pages,buildPage,"buildpage");}
    private int buyModeCount(){return buyMode==0?1:buyMode==1?10:buyMode==2?25:buyMode==3?100:0;}
    private int selectedBuyCount(int i){return buyMode==4?s.maxAffordableBuildingCount(i):buyModeCount();}
    private void drawBuyModes(Canvas c,float y){String[] a={"x1","x10","x25","x100","MAX"};for(int i=0;i<5;i++){float x=18+i*137;button(c,x,y,x+125,y+39,a[i],buyMode==i?GOLD:PANEL2,buyMode==i?BG:TEXT,true);hits.add(new Hit(new RectF(x,y,x+125,y+42),"buymode",i));}}
    private void drawBuildingIcon(Canvas c,float x,float y,int i,boolean on){int col=on?(i<4?Color.rgb(182,109,49):i<9?Color.rgb(121,129,135):i<12?CYAN:PURPLE):Color.rgb(60,70,75);p.setColor(Color.rgb(40,24,22));c.drawCircle(x,y,30,p);p.setColor(col);float h=20+(i%4)*5;c.drawRect(x-20,y+h/2-25,x+20,y+20,p);p.setColor(on?GOLD:MUTED);c.drawRect(x-8,y+2,x+8,y+20,p);}

    private void drawHeroes(Canvas c){float top=125,bottom=navY()-10;header(c,tr("BOHATEROWIE","HEROES"),tr("Bohaterowie zwiększają produkcję i walczą z bossami","Heroes boost production and fight bosses"),top);int per=5,pages=(ReleaseContent.HEROES.length+per-1)/per;heroPage=clamp(heroPage,0,pages-1);int start=heroPage*per,end=Math.min(ReleaseContent.HEROES.length,start+per);float y=top+62,row=Math.min(145,(bottom-y-105)/Math.max(1,end-start));for(int i=start;i<end;i++){float y2=y+row-8;ReleaseContent.HeroDef d=ReleaseContent.HEROES[i];boolean unlocked=s.stage>=d.unlockStage;panel(c,18,y,702,y2,Color.rgb(18,31,43),unlocked?d.color:STROKE,14);heroPortrait(c,65,(y+y2)/2,i,unlocked);text(c,d.name,116,y+30,15,unlocked?TEXT:MUTED,Paint.Align.LEFT);text(c,d.rarity,116,y+50,9,unlocked?d.color:MUTED,Paint.Align.LEFT);text(c,"LVL "+s.heroes[i]+" • DPS "+s.format(d.baseDps*Math.max(1,s.heroes[i])),116,y+74,10,CYAN,Paint.Align.LEFT);double cost=s.heroCost(i);boolean can=unlocked&&s.coins>=cost;button(c,514,y+27,684,y+76,tr("ULEPSZ","UPGRADE")+"\n"+s.format(cost),can?Color.rgb(55,158,39):PANEL2,can?Color.WHITE:MUTED,can);if(unlocked)hits.add(new Hit(new RectF(505,y+18,692,y+85),"hero",i));if(!unlocked)text(c,tr("Epoka ","Era ")+(d.unlockStage+1),680,y+100,9,MUTED,Paint.Align.RIGHT);y+=row;}float sy=bottom-88;panel(c,18,sy,702,sy+70,PANEL,PURPLE,12);text(c,tr("AKTYWNE UMIEJĘTNOŚCI","ACTIVE SKILLS"),34,sy+23,10,GOLD,Paint.Align.LEFT);for(int i=0;i<5;i++){float x=260+i*78;p.setColor(i%2==0?ORANGE:PURPLE);c.drawCircle(x,sy+40,22,p);text(c,i%2==0?"⚔":"✦",x,sy+47,17,Color.WHITE,Paint.Align.CENTER);}pager(c,bottom,pages,heroPage,"heropage");}
    private void heroPortrait(Canvas c,float x,float y,int i,boolean on){int col=on?ReleaseContent.HEROES[i].color:Color.rgb(65,70,78);p.setColor(Color.rgb(28,20,24));c.drawRoundRect(new RectF(x-35,y-35,x+35,y+35),12,12,p);p.setColor(col);c.drawCircle(x,y-10,20,p);p.setColor(Color.rgb(244,190,144));c.drawCircle(x,y-14,12,p);p.setColor(col);c.drawRect(x-20,y+7,x+20,y+28,p);p.setColor(Color.rgb(30,30,35));c.drawRect(x-6,y-16,x-2,y-12,p);c.drawRect(x+4,y-16,x+8,y-12,p);}

    private void drawResearch(Canvas c){float top=125,bottom=navY()-10;header(c,tr("DRZEWKO TECHNOLOGII","TECH TREE"),tr("Punkty badań: ","Research points: ")+s.researchPoints,top);float x0=75,y0=225;int cols=4;float dx=180,dy=Math.min(185,(bottom-y0-40)/5f);for(int i=0;i<ReleaseContent.TECH.length;i++){int r=i/cols,col=i%cols;float x=x0+col*dx,y=y0+r*dy;if(ReleaseContent.TECH[i].prereq>=0){int pr=ReleaseContent.TECH[i].prereq;int rr=pr/cols,cc=pr%cols;float px=x0+cc*dx,py=y0+rr*dy;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(s.tech[pr]>0?GREEN:Color.rgb(66,77,83));c.drawLine(px,py+34,x,y-34,p);p.setStyle(Paint.Style.FILL);}drawTechNode(c,x,y,i);} }
    private void drawTechNode(Canvas c,float x,float y,int i){ReleaseContent.TechDef d=ReleaseContent.TECH[i];boolean unlocked=s.techUnlocked(i),max=s.tech[i]>=d.max;int col=max?CYAN:unlocked?GREEN:Color.rgb(83,88,90);p.setColor(Color.argb(240,15,28,38));c.drawCircle(x,y,43,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(col);c.drawCircle(x,y,43,p);p.setStyle(Paint.Style.FILL);techIcon(c,x,y-4,i,col);text(c,ReleaseContent.techName(s.language,i),x,y+62,8,unlocked||max?TEXT:MUTED,Paint.Align.CENTER);text(c,s.tech[i]+"/"+d.max,x,y+78,9,col,Paint.Align.CENTER);if(unlocked&&!max){text(c,s.techCost(i)+"◇",x,y+95,8,PURPLE,Paint.Align.CENTER);hits.add(new Hit(new RectF(x-50,y-50,x+50,y+105),"tech",i));}}
    private void techIcon(Canvas c,float x,float y,int i,int col){p.setColor(col);if(i%4==0){c.drawRect(x-17,y-5,x+17,y+5,p);c.drawRect(x-4,y-20,x+4,y+20,p);}else if(i%4==1){c.drawRect(x-18,y-15,x+18,y+15,p);p.setColor(BG);c.drawRect(x-8,y-7,x+8,y+7,p);}else if(i%4==2){Path d=new Path();d.moveTo(x,y-22);d.lineTo(x+20,y);d.lineTo(x,y+22);d.lineTo(x-20,y);d.close();c.drawPath(d,p);}else{p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);c.drawCircle(x,y,18,p);c.drawLine(x-25,y,x+25,y,p);p.setStyle(Paint.Style.FILL);}}

    private void drawMissions(Canvas c){float top=125,bottom=navY()-10;header(c,tr("MISJE I OSIĄGNIĘCIA","MISSIONS & ACHIEVEMENTS"),missionMode==0?s.missionsClaimed+"/"+s.missionClaimed.length:s.achievementsUnlocked+"/"+s.achievementUnlocked.length,top);button(c,18,top+48,345,top+92,tr("MISJE","MISSIONS"),missionMode==0?GOLD:PANEL2,missionMode==0?BG:TEXT,true);button(c,375,top+48,702,top+92,tr("OSIĄGNIĘCIA","ACHIEVEMENTS"),missionMode==1?PURPLE:PANEL2,missionMode==1?BG:TEXT,true);hits.add(new Hit(new RectF(18,top+45,345,top+96),"missionmode",0));hits.add(new Hit(new RectF(375,top+45,702,top+96),"missionmode",1));if(missionMode==0)drawMissionCards(c,top+112,bottom);else drawAchievementCards(c,top+112,bottom);}
    private void drawMissionCards(Canvas c,float y,float bottom){int per=6,pages=(s.missionClaimed.length+per-1)/per;missionPage=clamp(missionPage,0,pages-1);int start=missionPage*per,end=Math.min(s.missionClaimed.length,start+per);float row=Math.min(135,(bottom-y-48)/Math.max(1,end-start));for(int i=start;i<end;i++){float y2=y+row-8;boolean ready=s.missionReady(i),done=s.missionClaimed[i];panel(c,18,y,702,y2,PANEL,done?GREEN:ready?GOLD:STROKE,13);text(c,tr("Misja #","Mission #")+(i+1),36,y+28,10,done?GREEN:GOLD,Paint.Align.LEFT);text(c,s.goalLabel(s.missionType(i),s.missionTarget(i)),36,y+52,13,done?MUTED:TEXT,Paint.Align.LEFT);float pr=(float)Math.min(1,s.goalValue(s.missionType(i))/Math.max(1,s.missionTarget(i)));progress(c,36,y+68,475,y+80,pr,done?GREEN:GOLD);text(c,"+"+s.missionRewardCrystals(i)+" ◆  +"+s.missionRewardResearch(i)+" ◇",675,y+30,10,CYAN,Paint.Align.RIGHT);if(ready&&!done){button(c,505,y+52,675,y+88,tr("ODBIERZ","CLAIM"),Color.rgb(52,156,40),Color.WHITE,true);hits.add(new Hit(new RectF(495,y+44,684,y+98),"claimmission",i));}else text(c,done?tr("UKOŃCZONO","COMPLETED"):Math.round(pr*100)+"%",675,y+75,9,done?GREEN:MUTED,Paint.Align.RIGHT);y+=row;}pager(c,bottom,pages,missionPage,"missionpage");}
    private void drawAchievementCards(Canvas c,float y,float bottom){int per=7,pages=(s.achievementUnlocked.length+per-1)/per;missionPage=clamp(missionPage,0,pages-1);int start=missionPage*per,end=Math.min(s.achievementUnlocked.length,start+per);float row=Math.min(112,(bottom-y-48)/Math.max(1,end-start));for(int i=start;i<end;i++){float y2=y+row-7;boolean done=s.achievementUnlocked[i];panel(c,18,y,702,y2,PANEL,done?GOLD:STROKE,12);p.setColor(done?GOLD:Color.rgb(65,78,87));c.drawCircle(55,(y+y2)/2,25,p);text(c,"★",55,(y+y2)/2+8,21,done?BG:MUTED,Paint.Align.CENTER);text(c,tr("Osiągnięcie ","Achievement ")+(i+1),96,y+32,12,done?GOLD:TEXT,Paint.Align.LEFT);text(c,achievementDescription(i),96,y+57,9,MUTED,Paint.Align.LEFT);text(c,done?tr("ZDOBYTE","UNLOCKED"):tr("W TRAKCIE","IN PROGRESS"),672,y+48,9,done?GREEN:MUTED,Paint.Align.RIGHT);y+=row;}pager(c,bottom,pages,missionPage,"missionpage");}
    private String achievementDescription(int i){if(i<6)return tr("Klikaj coraz więcej","Tap more and more");if(i<11)return tr("Rozwijaj ekonomię","Grow your economy");if(i<15)return tr("Kupuj budynki","Buy buildings");if(i<20)return tr("Ulepszaj bohaterów","Level heroes");if(i<24)return tr("Buduj długie combo","Build long combos");if(i<27)return tr("Pokonuj bossów","Defeat bosses");return tr("Odrodź imperium","Rebirth the empire");}

    private void drawShop(Canvas c){float top=125;header(c,tr("SKLEP I BOOSTERY","SHOP & BOOSTERS"),tr("Bez pay-to-win • waluta zdobywana w grze","No pay-to-win • earn currency in game"),top);float y=top+70;String[] title={tr("x2 ZŁOTO","x2 GOLD"),tr("x4 XP BUDOWY","x4 BUILD XP"),"POWER TAP +1"};String[] desc={tr("30 minut podwójnych monet","30 minutes double coins"),tr("30 minut szybszej budowy","30 minutes faster building"),tr("Dodaje 1 ładunek","Adds 1 charge")};int[] cost={100,100,80},cols={GOLD,CYAN,ORANGE};for(int i=0;i<3;i++){float x=18+i*228;panel(c,x,y,x+210,y+250,Color.rgb(25,25,49),cols[i],16);p.setColor(cols[i]);c.drawCircle(x+105,y+65,43,p);text(c,i==0?"●":i==1?"◆":"⚡",x+105,y+77,31,Color.WHITE,Paint.Align.CENTER);text(c,title[i],x+105,y+126,14,TEXT,Paint.Align.CENTER);wrap(c,desc[i],x+105,y+151,178,9,MUTED);button(c,x+28,y+195,x+182,y+234,cost[i]+" ◆",s.crystals>=cost[i]?Color.rgb(49,154,41):PANEL2,s.crystals>=cost[i]?Color.WHITE:MUTED,true);hits.add(new Hit(new RectF(x+20,y+184,x+190,y+244),"shop",i));}y+=285;panel(c,18,y,702,y+190,PANEL,PURPLE,16);text(c,tr("JAK ZDOBYWAĆ KRYSZTAŁY?","HOW TO EARN CRYSTALS?"),36,y+35,15,TEXT,Paint.Align.LEFT);String[] lines={tr("• Misje i osiągnięcia","• Missions and achievements"),tr("• Nagrody dzienne i eventy","• Daily rewards and events"),tr("• Pokonywanie bossów","• Defeating bosses"),tr("• Awans do nowych epok","• Reaching new eras")};for(int i=0;i<lines.length;i++)text(c,lines[i],48,y+70+i*27,11,MUTED,Paint.Align.LEFT);text(c,tr("Sklep release używa tylko waluty zdobywanej w grze.","Release shop uses only currency earned in game."),36,y+178,9,GREEN,Paint.Align.LEFT);}

    private void drawEmpire(Canvas c){float top=125,bottom=navY()-10;header(c,tr("IMPERIUM","EMPIRE"),"Pixel Empire • Release Candidate",top);float y=top+65;int gain=s.availableLegacyStars();panel(c,18,y,702,y+150,PANEL,gain>0?PURPLE:STROKE,15);text(c,tr("ODRODZENIE / PRESTIŻ","REBIRTH / PRESTIGE"),36,y+34,16,TEXT,Paint.Align.LEFT);text(c,tr("Zacznij od początku z trwałym mnożnikiem.","Restart with a permanent multiplier."),36,y+62,10,MUTED,Paint.Align.LEFT);text(c,"★ "+s.legacyStars+"  •  x"+String.format(Locale.US,"%.2f",s.legacyMultiplier()),36,y+92,13,GOLD,Paint.Align.LEFT);button(c,475,y+48,682,y+105,gain>0?tr("ODRODŹ +","REBIRTH +")+gain+"★":tr("Epoka 10 wymagana","Era 10 required"),gain>0?PURPLE:PANEL2,Color.WHITE,gain>0);if(gain>0)hits.add(new Hit(new RectF(462,y+38,692,y+115),"rebirth",0));y+=168;
        panel(c,18,y,702,y+155,PANEL,s.bossAvailable()?RED:STROKE,15);text(c,tr("BOSS EPOKI","ERA BOSS"),36,y+35,16,TEXT,Paint.Align.LEFT);text(c,tr("Bohaterowie zadają DPS, a każde kliknięcie uderza bossa.","Heroes deal DPS and every tap hits the boss."),36,y+62,10,MUTED,Paint.Align.LEFT);text(c,tr("Pokonani: ","Defeated: ")+s.bossDefeats,36,y+91,12,GOLD,Paint.Align.LEFT);button(c,475,y+45,682,y+105,s.bossActive?tr("BOSS AKTYWNY","BOSS ACTIVE"):tr("WALCZ","FIGHT"),s.bossAvailable()?RED:PANEL2,Color.WHITE,s.bossAvailable());if(s.bossAvailable())hits.add(new Hit(new RectF(462,y+35,692,y+115),"boss",0));y+=173;
        panel(c,18,y,350,y+245,PANEL,STROKE,14);text(c,tr("STATYSTYKI","STATISTICS"),36,y+32,14,TEXT,Paint.Align.LEFT);stat(c,y+65,tr("Czas gry","Play time"),ReleaseGameState.duration(s.playSeconds));stat(c,y+94,tr("Kliknięcia","Taps"),Long.toString(s.totalTaps));stat(c,y+123,tr("Najlepsze combo","Best combo"),"x"+String.format(Locale.US,"%.1f",ReleaseGameState.comboMultiplier(s.bestComboTaps)));stat(c,y+152,tr("Dochód / s","Income / s"),s.format(s.getCps()));stat(c,y+181,tr("Bohater DPS","Hero DPS"),s.format(s.getHeroDps()));stat(c,y+210,tr("Odrodzenia","Rebirths"),Integer.toString(s.rebirths));
        panel(c,370,y,702,y+245,PANEL,STROKE,14);text(c,tr("USTAWIENIA","SETTINGS"),388,y+32,14,TEXT,Paint.Align.LEFT);setting(c,388,y+54,tr("Dźwięki","Sound"),s.sound,"sound");setting(c,388,y+98,tr("Wibracje","Haptics"),s.haptics,"haptic");setting(c,388,y+142,tr("Oszczędzanie energii","Low power"),s.lowPower,"lowpower");button(c,388,y+190,685,y+230,tr("JĘZYK: ","LANGUAGE: ")+L10n.languageLabel(s.language),Color.rgb(19,94,161),TEXT,true);hits.add(new Hit(new RectF(382,y+184,690,y+236),"language",0));
        float ry=Math.min(bottom-62,y+265);button(c,18,ry,702,ry+48,System.currentTimeMillis()<resetArmedUntil?tr("DOTKNIJ PONOWNIE — USUŃ ZAPIS","TAP AGAIN — DELETE SAVE"):tr("WYCZYŚĆ ZAPIS","RESET SAVE"),Color.rgb(91,28,34),RED,true);hits.add(new Hit(new RectF(18,ry,702,ry+50),"reset",0));}
    private void setting(Canvas c,float x,float y,String label,boolean on,String type){text(c,label,x,y+21,10,TEXT,Paint.Align.LEFT);p.setColor(on?GREEN:Color.rgb(65,76,84));c.drawRoundRect(new RectF(620,y+3,682,y+29),14,14,p);p.setColor(Color.WHITE);c.drawCircle(on?668:634,y+16,10,p);hits.add(new Hit(new RectF(380,y-3,694,y+36),type,0));}
    private void stat(Canvas c,float y,String l,String v){text(c,l,36,y,9,MUTED,Paint.Align.LEFT);text(c,v,332,y,10,TEXT,Paint.Align.RIGHT);}

    private void drawBottomNav(Canvas c){float y=navY();p.setColor(Color.rgb(4,13,19));c.drawRect(0,y,W,logicalH,p);p.setColor(Color.rgb(50,75,86));c.drawRect(0,y,W,y+3,p);String[] lab={tr("ŚWIAT","WORLD"),tr("BUDOWA","BUILD"),tr("BOHATER.","HEROES"),tr("BADANIA","RESEARCH"),tr("MISJE","MISSIONS"),tr("SKLEP","SHOP"),tr("IMPERIUM","EMPIRE")};float ww=W/7f;for(int i=0;i<7;i++){float x=i*ww;if(tab==i){panel(c,x+4,y+6,x+ww-4,logicalH-5,Color.rgb(18,49,64),CYAN,10);}drawNavIcon(c,x+ww/2,y+38,i,tab==i?CYAN:MUTED);text(c,lab[i],x+ww/2,y+82,7.3f,tab==i?TEXT:MUTED,Paint.Align.CENTER);hits.add(new Hit(new RectF(x,y,x+ww,logicalH),"tab",i));}}
    private void drawNavIcon(Canvas c,float x,float y,int i,int col){p.setColor(col);if(i==0){Path q=new Path();q.moveTo(x,y-21);q.lineTo(x+22,y-8);q.lineTo(x+15,y+18);q.lineTo(x-15,y+18);q.lineTo(x-22,y-8);q.close();c.drawPath(q,p);}else if(i==1){c.rotate(-35,x,y);c.drawRect(x-7,y-24,x+7,y+20,p);c.drawRect(x-24,y-7,x+24,y+7,p);c.rotate(35,x,y);}else if(i==2){c.drawCircle(x,y-10,14,p);c.drawRoundRect(new RectF(x-24,y+5,x+24,y+25),10,10,p);}else if(i==3){Path d=new Path();d.moveTo(x,y-24);d.lineTo(x+20,y);d.lineTo(x,y+24);d.lineTo(x-20,y);d.close();c.drawPath(d,p);}else if(i==4){c.drawRoundRect(new RectF(x-19,y-22,x+19,y+24),5,5,p);p.setColor(BG);for(int k=0;k<3;k++)c.drawRect(x-10,y-11+k*11,x+10,y-7+k*11,p);}else if(i==5){c.drawRect(x-24,y-10,x+24,y+22,p);c.drawRect(x-18,y-23,x+18,y-10,p);p.setColor(BG);c.drawCircle(x,y+4,9,p);}else{Path q=new Path();q.moveTo(x,y-24);q.lineTo(x+23,y-10);q.lineTo(x+17,y+22);q.lineTo(x,y+12);q.lineTo(x-17,y+22);q.lineTo(x-23,y-10);q.close();c.drawPath(q,p);}}

    private void drawFx(Canvas c){for(FloatFx f:fx){int a=(int)(255*Math.max(0,Math.min(1,f.life)));text(c,f.text,f.x,f.y,18,(f.color&0xffffff)|(a<<24),Paint.Align.CENTER);}}
    private void drawToast(Canvas c){if(toast.isEmpty()||System.currentTimeMillis()>=toastUntil)return;float y=navY()-70;panel(c,110,y,610,y+50,Color.argb(245,8,18,27),GOLD,13);text(c,toast,360,y+31,11,TEXT,Paint.Align.CENTER);}

    private void drawOffline(Canvas c){p.setColor(Color.argb(225,0,0,0));c.drawRect(0,0,W,logicalH,p);float h=390,y=(logicalH-h)/2;panel(c,65,y,655,y+h,Color.rgb(18,20,49),PURPLE,20);text(c,tr("WITAJ Z POWROTEM!","WELCOME BACK!"),360,y+55,25,TEXT,Paint.Align.CENTER);text(c,tr("Nie było Cię:","You were away:"),360,y+94,11,MUTED,Paint.Align.CENTER);text(c,ReleaseGameState.duration(s.startupOfflineSeconds),360,y+122,19,GOLD,Paint.Align.CENTER);text(c,tr("Stawka offline: x0.2 aktualnego dochodu / s","Offline rate: x0.2 of current income / s"),360,y+160,11,MUTED,Paint.Align.CENTER);text(c,tr("Maksymalnie 12 godzin","Maximum 12 hours"),360,y+185,10,PURPLE,Paint.Align.CENTER);text(c,"+"+s.format(s.startupOfflineGain),360,y+245,35,GOLD,Paint.Align.CENTER);button(c,135,y+305,585,y+355,tr("ODBIERZ","CLAIM"),Color.rgb(63,173,39),Color.WHITE,true);hits.add(new Hit(new RectF(65,y,655,y+h),"offline",0));}

    private void drawTutorial(Canvas c){p.setColor(Color.argb(235,1,7,12));c.drawRect(0,0,W,logicalH,p);float h=520,y=(logicalH-h)/2;panel(c,55,y,665,y+h,Color.rgb(10,25,35),GOLD,22);text(c,"PIXEL",360,y+65,34,Color.WHITE,Paint.Align.CENTER);text(c,"EMPIRE",360,y+103,42,GOLD,Paint.Align.CENTER);String body=tutorialStep==0?tr("Klikaj w DOWOLNE miejsce świata. Cała scena jest przyciskiem.","Tap ANYWHERE in the world. The whole scene is the button."):tutorialStep==1?tr("Kupuj budynki i bohaterów. Produkcja działa również bez klikania.","Buy buildings and heroes. Production works without tapping too."):tutorialStep==2?tr("Combo rośnie powoli: x1.1 po 50 kliknięciach, x1.2 po 100 i tak dalej.","Combo grows slowly: x1.1 after 50 taps, x1.2 after 100, and so on."):tr("Badania, misje, bossowie i Odrodzenie odblokowują kolejne warstwy gry.","Research, missions, bosses and Rebirth unlock deeper progression.");wrap(c,body,360,y+190,500,15,TEXT);text(c,(tutorialStep+1)+" / 4",360,y+355,11,MUTED,Paint.Align.CENTER);text(c,tr("DOTKNIJ, ABY KONTYNUOWAĆ","TAP TO CONTINUE"),360,y+430,15,GOLD,Paint.Align.CENTER);}

    public boolean handleBack(){if(offlinePopup){offlinePopup=false;invalidate();return true;}if(!s.tutorialSeen)return true;if(tab!=0){tab=0;invalidate();return true;}return false;}

    @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX()/scale,y=e.getY()/scale;if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;moved=false;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){if(Math.abs(x-downX)>14||Math.abs(y-downY)>14)moved=true;return true;}if(e.getAction()==MotionEvent.ACTION_UP){performClick();if(moved)return true;if(!s.tutorialSeen){tutorialStep++;if(tutorialStep>=4){s.tutorialSeen=true;s.save(getContext());toast="Pixel Empire!";toastUntil=System.currentTimeMillis()+1200;}return true;}if(offlinePopup){offlinePopup=false;feedback(true);return true;}for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(h.r.contains(x,y)){handle(h,x,y);break;}}return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}

    private void handle(Hit h,float x,float y){switch(h.t){case "tab":tab=h.id;feedback(false);break;case "tap":doTap(x,y);break;case "daily":if(s.canClaimDaily()){int r=s.claimDaily();show("+"+r+" ◆");feedback(true);}else show(tr("Nagroda już odebrana","Reward already claimed"));break;case "missions":tab=4;missionMode=0;break;case "achievements":tab=4;missionMode=1;break;case "event":if(s.eventType>=0){s.collectEvent();show("EVENT BONUS!");feedback(true);}break;case "power":if(s.activatePowerTap()){show("POWER TAP x3");feedback(true);}else show(tr("Brak ładunków","No charges"));break;case "buymode":buyMode=h.id;break;case "buybuilding":int cnt=selectedBuyCount(h.id);if(s.buyBuilding(h.id,cnt)){show("+"+(cnt<=0?0:cnt)+" "+ReleaseContent.buildingName(s.language,h.id));feedback(false);}else error();break;case "buildpage":buildPage+=h.id;break;case "hero":if(s.upgradeHero(h.id)){show(ReleaseContent.HEROES[h.id].name+" LVL "+s.heroes[h.id]);feedback(false);}else error();break;case "heropage":heroPage+=h.id;break;case "tech":if(s.buyTech(h.id)){show(ReleaseContent.techName(s.language,h.id)+" "+s.tech[h.id]+"/"+ReleaseContent.TECH[h.id].max);feedback(true);}else error();break;case "missionmode":missionMode=h.id;missionPage=0;break;case "claimmission":if(s.claimMission(h.id)){show(tr("Nagroda odebrana!","Reward claimed!"));feedback(true);}break;case "missionpage":missionPage+=h.id;break;case "shop":if(s.shopBuy(h.id)){show(tr("Booster kupiony","Booster purchased"));feedback(true);}else error();break;case "rebirth":rebirth();break;case "boss":if(s.startBoss()){tab=0;show(tr("BOSS NADCHODZI!","BOSS INCOMING!"));feedback(true);}break;case "sound":s.sound=!s.sound;break;case "haptic":s.haptics=!s.haptics;break;case "lowpower":s.lowPower=!s.lowPower;break;case "language":s.language=L10n.nextLang(s.language);break;case "reset":reset();break;case "offline":offlinePopup=false;break;}s.save(getContext());invalidate();}

    private void doTap(float x,float y){long now=System.currentTimeMillis();if(now-lastTapAt>1800)comboTaps=0;comboTaps++;lastTapAt=now;boolean crit=rng.nextDouble()<.055;double amount=s.tap(comboTaps,crit);String t=(crit?tr("KRYTYK! ","CRIT! "):"+")+s.format(amount);fx.add(new FloatFx(x,y-15,t,crit?PURPLE:GOLD));while(fx.size()>24)fx.remove(0);feedback(crit);}

    private void rebirth(){int gain=s.availableLegacyStars();if(gain<=0)return;long now=System.currentTimeMillis();if(now>rebirthArmedUntil){rebirthArmedUntil=now+5000;show(tr("Dotknij ponownie: Odrodzenie +","Tap again: Rebirth +")+gain+"★");return;}if(s.rebirth()){rebirthArmedUntil=0;tab=0;comboTaps=0;show("+"+gain+" ★");feedback(true);}}
    private void reset(){long now=System.currentTimeMillis();if(now>resetArmedUntil){resetArmedUntil=now+5000;show(tr("Dotknij ponownie, aby usunąć zapis","Tap again to delete save"));error();return;}s.hardReset(getContext());postDelayed(()->((Activity)getContext()).recreate(),250);}

    private int countReadyMissions(){int n=0;for(int i=0;i<s.missionClaimed.length;i++)if(s.missionReady(i))n++;return n;}
    private void show(String m){toast=m;toastUntil=System.currentTimeMillis()+1500;}
    private void feedback(boolean strong){if(s.sound&&tone!=null)try{tone.startTone(strong?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,strong?65:25);}catch(Throwable ignored){}if(s.haptics&&vib!=null&&vib.hasVibrator())try{if(Build.VERSION.SDK_INT>=26)vib.vibrate(VibrationEffect.createOneShot(strong?34:12,strong?130:55));else vib.vibrate(strong?34:12);}catch(Throwable ignored){}}
    private void error(){if(s.sound&&tone!=null)try{tone.startTone(ToneGenerator.TONE_PROP_NACK,45);}catch(Throwable ignored){}if(s.haptics&&vib!=null&&vib.hasVibrator())try{if(Build.VERSION.SDK_INT>=26)vib.vibrate(VibrationEffect.createOneShot(25,80));else vib.vibrate(25);}catch(Throwable ignored){}}

    private String tr(String pl,String en){return ReleaseGameState.tr(s.language,pl,en,en,en,en,en);}
    private int clamp(int v,int a,int b){return Math.max(a,Math.min(b,v));}
    private void header(Canvas c,String a,String b,float y){text(c,a,18,y+25,20,TEXT,Paint.Align.LEFT);text(c,b,702,y+25,9,MUTED,Paint.Align.RIGHT);p.setColor(Color.rgb(49,71,82));c.drawRect(18,y+39,702,y+42,p);}
    private void pager(Canvas c,float bottom,int pages,int page,String type){if(pages<=1)return;float y=bottom-43;button(c,18,y,150,y+38,"‹",PANEL2,TEXT,page>0);button(c,570,y,702,y+38,"›",PANEL2,TEXT,page<pages-1);text(c,(page+1)+" / "+pages,360,y+25,10,MUTED,Paint.Align.CENTER);if(page>0)hits.add(new Hit(new RectF(15,y-3,155,y+43),type,-1));if(page<pages-1)hits.add(new Hit(new RectF(565,y-3,705,y+43),type,1));}
    private void panel(Canvas c,float x1,float y1,float x2,float y2,int fill,int stroke,float r){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawRoundRect(new RectF(x1,y1,x2,y2),r,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(stroke);c.drawRoundRect(new RectF(x1,y1,x2,y2),r,r,p);p.setStyle(Paint.Style.FILL);}
    private void button(Canvas c,float x1,float y1,float x2,float y2,String label,int fill,int col,boolean enabled){p.setColor(enabled?fill:Color.rgb(39,49,57));c.drawRoundRect(new RectF(x1,y1,x2,y2),10,10,p);String[] lines=label.split("\\n");if(lines.length==1)text(c,lines[0],(x1+x2)/2,(y1+y2)/2+5,11,enabled?col:MUTED,Paint.Align.CENTER);else{text(c,lines[0],(x1+x2)/2,(y1+y2)/2-4,10,enabled?col:MUTED,Paint.Align.CENTER);text(c,lines[1],(x1+x2)/2,(y1+y2)/2+14,9,enabled?col:MUTED,Paint.Align.CENTER);}}
    private void progress(Canvas c,float x1,float y1,float x2,float y2,float v,int col){p.setColor(Color.rgb(31,45,52));c.drawRoundRect(new RectF(x1,y1,x2,y2),7,7,p);p.setColor(col);v=Math.max(0,Math.min(1,v));c.drawRoundRect(new RectF(x1,y1,x1+(x2-x1)*v,y2),7,7,p);}
    private void text(Canvas c,String t,float x,float y,float size,int col,Paint.Align a){tp.setTextAlign(a);tp.setTextSize(size);tp.setColor(col);tp.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));tp.setStyle(Paint.Style.FILL);c.drawText(t,x,y,tp);}
    private void wrap(Canvas c,String s,float x,float y,float max,float size,int col){String[] ws=s.split(" ");String line="";int n=0;tp.setTextSize(size);for(String w:ws){String q=line.isEmpty()?w:line+" "+w;if(tp.measureText(q)>max&&!line.isEmpty()){text(c,line,x,y+n*(size+7),size,col,Paint.Align.CENTER);n++;line=w;}else line=q;}if(!line.isEmpty())text(c,line,x,y+n*(size+7),size,col,Paint.Align.CENTER);}
}
