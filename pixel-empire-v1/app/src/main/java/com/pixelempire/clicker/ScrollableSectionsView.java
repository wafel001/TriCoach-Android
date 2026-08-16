package com.pixelempire.clicker;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Continuous-scroll release UI. Each game category stays separate, but its complete content
 * lives in one vertically scrollable surface instead of page 1/2/3 pagination.
 */
public final class ScrollableSectionsView extends View implements Runnable {
    private static final float W=720f;
    private static final int BG=Color.rgb(4,11,17), PANEL=Color.rgb(13,29,40), PANEL2=Color.rgb(19,40,54), EDGE=Color.rgb(65,98,116);
    private static final int TEXT=Color.rgb(248,250,252), MUTED=Color.rgb(166,185,196), GOLD=Color.rgb(255,190,28), GREEN=Color.rgb(72,205,58), CYAN=Color.rgb(49,207,236), PURPLE=Color.rgb(174,80,255), RED=Color.rgb(239,66,73), ORANGE=Color.rgb(255,132,30), BLUE=Color.rgb(39,123,207);

    private final ReleaseGameState s;
    private final UltraGameView ultra;
    private final PremiumGameView base;
    private final Field tabField;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG), tp=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Hit> hits=new ArrayList<>();
    private final float[] scroll=new float[7];
    private final float[] maxScroll=new float[7];
    private final float[] missionScroll=new float[4];
    private final float[] missionMax=new float[4];

    private float scale=1f, logicalH=1500f, downX, downY, lastY;
    private boolean running, dragging;
    private int buyMode=0, missionSub=0;
    private long resetConfirmUntil=0, rebirthConfirmUntil=0;

    private static final class Hit {
        final RectF r; final String type; final int id;
        Hit(RectF r,String type,int id){this.r=r;this.type=type;this.id=id;}
    }

    public ScrollableSectionsView(Context c, ReleaseGameState state, UltraGameView ultra){
        super(c);this.s=state;this.ultra=ultra;tp.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
        PremiumGameView found=null;Field tf=null;
        try{
            Field gf=UltraGameView.class.getDeclaredField("game");gf.setAccessible(true);found=(PremiumGameView)gf.get(ultra);
            tf=PremiumGameView.class.getDeclaredField("tab");tf.setAccessible(true);
        }catch(Throwable ignored){}
        base=found;tabField=tf;setBackgroundColor(Color.TRANSPARENT);setWillNotDraw(false);
    }

    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();running=true;post(this);}
    @Override protected void onDetachedFromWindow(){running=false;removeCallbacks(this);super.onDetachedFromWindow();}
    @Override public void run(){if(!running)return;invalidate();postDelayed(this,s.lowPower?140:70);}

    private int tab(){try{return base!=null&&tabField!=null?Math.max(0,Math.min(6,tabField.getInt(base))):0;}catch(Throwable ignored){return 0;}}
    private void setTab(int t){
        t=Math.max(0,Math.min(6,t));
        if(t==0){while(tab()!=0){if(!ultra.handleBack())break;}invalidate();return;}
        try{if(base!=null&&tabField!=null)tabField.setInt(base,t);}catch(Throwable ignored){}
        invalidate();
    }

    public boolean handleBack(){if(tab()==0)return false;setTab(0);return true;}

    private float contentTop(){return 202f;}
    private float navY(){return logicalH-100f;}
    private float viewportH(){return Math.max(80f,navY()-contentTop()-8f);}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);int t=tab();if(t==0)return;
        scale=getWidth()/W;if(scale<=0)scale=1;logicalH=getHeight()/scale;c.save();c.scale(scale,scale);hits.clear();
        p.setShader(new LinearGradient(0,126,0,navY(),Color.rgb(7,22,32),BG,Shader.TileMode.CLAMP));c.drawRect(0,126,W,navY(),p);p.setShader(null);
        drawHeader(c,t);
        c.save();c.clipRect(0,contentTop(),W,navY()-4);
        if(t==1)drawBuild(c);else if(t==2)drawHeroes(c);else if(t==3)drawTech(c);else if(t==4)drawMissions(c);else if(t==5)drawShop(c);else if(t==6)drawEmpire(c);
        c.restore();
        drawScrollBar(c,t);drawNav(c,t);c.restore();
    }

    private void drawHeader(Canvas c,int t){
        String[] titles={"",tr("BUDOWA IMPERIUM","EMPIRE BUILD"),tr("BOHATEROWIE","HEROES"),tr("DRZEWKO TECHNOLOGII","TECH TREE"),tr("MISJE I OSIĄGNIĘCIA","MISSIONS & ACHIEVEMENTS"),tr("SKLEP I BOOSTERY","SHOP & BOOSTERS"),tr("IMPERIUM","EMPIRE")};
        String[] subs={"",tr("Wszystkie budynki • przewiń w dół","All buildings • scroll down"),tr("Wszyscy bohaterowie • przewiń w dół","All heroes • scroll down"),tr("Wszystkie technologie • przewiń drzewko","All technologies • scroll the tree"),tr("Każda kategoria ma własny scroll","Each category has its own scroll"),tr("Wszystkie oferty w jednej liście","All offers in one list"),tr("Statystyki • Odrodzenie • Ustawienia","Stats • Rebirth • Settings")};
        text(c,titles[t],22,157,21,TEXT,Paint.Align.LEFT);text(c,subs[t],698,157,8,MUTED,Paint.Align.RIGHT);p.setColor(EDGE);c.drawRect(22,168,698,170,p);
        if(t==1)drawBuyMode(c,176);
        if(t==4)drawMissionTabs(c,176);
        else{text(c,tr("PRZECIĄGNIJ ↑↓","SWIPE ↑↓"),690,191,7,CYAN,Paint.Align.RIGHT);}
    }

    private void drawBuyMode(Canvas c,float y){String[] a={"x1","x10","x25","x100","MAX"};for(int i=0;i<5;i++){float x=22+i*136;button(c,x,y,x+122,y+36,a[i],buyMode==i?Color.rgb(28,139,49):PANEL2,TEXT,true);hits.add(new Hit(new RectF(x,y,x+122,y+40),"buyMode",i));}}
    private void drawMissionTabs(Canvas c,float y){String[] a={tr("DZIENNE","DAILY"),tr("TYGODNIOWE","WEEKLY"),tr("SPECJALNE","SPECIAL"),tr("OSIĄGNIĘCIA","ACHIEVEMENTS")};for(int i=0;i<4;i++){float x=14+i*174;button(c,x,y,x+164,y+36,a[i],missionSub==i?BLUE:PANEL2,TEXT,true);hits.add(new Hit(new RectF(x,y,x+164,y+40),"missionSub",i));}}

    private void drawBuild(Canvas c){
        float y=contentTop()+8-scroll[1], card=118, gap=10;
        for(int i=0;i<ReleaseContent.BUILDINGS.length;i++){drawBuildingCard(c,i,18,y,702,y+card);y+=card+gap;}
        float total=ReleaseContent.BUILDINGS.length*(card+gap)+16;maxScroll[1]=Math.max(0,total-viewportH());scroll[1]=clamp(scroll[1],0,maxScroll[1]);
    }
    private void drawBuildingCard(Canvas c,int i,float x1,float y1,float x2,float y2){
        ReleaseContent.BuildingDef d=ReleaseContent.BUILDINGS[i];boolean locked=s.stage<d.unlockStage;panel(c,x1,y1,x2,y2,locked?Color.rgb(23,29,32):PANEL,locked?Color.rgb(54,63,68):Color.rgb(48,102,129),13);
        drawRoundIcon(c,x1+54,(y1+y2)/2,39,i%3==0?GOLD:i%3==1?CYAN:ORANGE,"▣");
        text(c,ReleaseContent.buildingName(s.language,i),x1+108,y1+27,15,locked?MUTED:TEXT,Paint.Align.LEFT);text(c,"Lv. "+s.buildings[i],x2-18,y1+26,10,GOLD,Paint.Align.RIGHT);
        double unit=d.baseCps*Math.pow(1.012,Math.max(0,s.buildings[i]));text(c,s.format(unit)+" / s",x1+108,y1+51,11,GREEN,Paint.Align.LEFT);
        int n=buyCount(i);double cost=s.buildingCost(i,Math.max(1,n));text(c,tr("Koszt: ","Cost: ")+s.format(cost),x1+108,y1+76,9,MUTED,Paint.Align.LEFT);
        String label=locked?tr("ODBLOKUJ EPOKĘ ","UNLOCK ERA ")+(d.unlockStage+1):tr("KUP ","BUY ")+(buyMode==4?"MAX":"x"+Math.max(1,n));
        button(c,x2-205,y1+57,x2-18,y2-17,label,locked?Color.rgb(48,55,60):Color.rgb(43,164,43),TEXT,!locked&&n>0&&s.coins+1e-9>=cost);
        if(!locked)hits.add(new Hit(new RectF(x2-210,y1+52,x2-12,y2-12),"building",i));
    }
    private int buyCount(int i){return buyMode==0?1:buyMode==1?10:buyMode==2?25:buyMode==3?100:s.maxAffordableBuildingCount(i);}

    private void drawHeroes(Canvas c){
        float y=contentTop()+8-scroll[2],card=118,gap=10;
        for(int i=0;i<ReleaseContent.HEROES.length;i++){drawHeroCard(c,i,18,y,702,y+card);y+=card+gap;}
        float total=ReleaseContent.HEROES.length*(card+gap)+16;maxScroll[2]=Math.max(0,total-viewportH());scroll[2]=clamp(scroll[2],0,maxScroll[2]);
    }
    private void drawHeroCard(Canvas c,int i,float x1,float y1,float x2,float y2){
        ReleaseContent.HeroDef d=ReleaseContent.HEROES[i];boolean locked=s.stage<d.unlockStage;panel(c,x1,y1,x2,y2,locked?Color.rgb(24,28,31):Color.rgb(20,29,47),locked?Color.rgb(61,66,69):d.color,13);
        drawRoundIcon(c,x1+54,(y1+y2)/2,40,d.color,"♟");text(c,d.name,x1+108,y1+26,15,locked?MUTED:TEXT,Paint.Align.LEFT);text(c,d.rarity,x1+108,y1+45,8,d.color,Paint.Align.LEFT);text(c,"Lv. "+s.heroes[i],x2-18,y1+25,10,GOLD,Paint.Align.RIGHT);
        double dps=d.baseDps*Math.max(1,s.heroes[i])*Math.pow(1.055,Math.max(0,s.heroes[i]-1));text(c,"DPS: "+s.format(dps),x1+108,y1+69,11,CYAN,Paint.Align.LEFT);double cost=s.heroCost(i);text(c,tr("Koszt: ","Cost: ")+s.format(cost),x1+108,y1+91,8,MUTED,Paint.Align.LEFT);
        button(c,x2-184,y1+57,x2-18,y2-17,locked?tr("EPOKA ","ERA ")+(d.unlockStage+1):tr("ULEPSZ","UPGRADE"),locked?Color.rgb(51,57,60):Color.rgb(47,166,43),TEXT,!locked&&s.coins>=cost);if(!locked)hits.add(new Hit(new RectF(x2-190,y1+52,x2-12,y2-12),"hero",i));
    }

    private void drawTech(Canvas c){
        float baseY=contentTop()+60-scroll[3],step=112;int n=ReleaseContent.TECH.length;
        for(int i=0;i<n;i++){
            float x=(i%2==0)?220:500,y=baseY+i*step;ReleaseContent.TechDef d=ReleaseContent.TECH[i];boolean unlocked=s.techUnlocked(i),max=s.tech[i]>=d.max;int col=max?GREEN:unlocked?CYAN:Color.rgb(91,102,108);
            if(i>0){float px=((i-1)%2==0)?220:500,py=baseY+(i-1)*step;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(s.tech[Math.max(0,i-1)]>0?GREEN:Color.rgb(68,82,91));c.drawLine(px,py+41,x,y-41,p);p.setStyle(Paint.Style.FILL);}
            p.setColor(Color.rgb(13,28,39));c.drawCircle(x,y,43,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(col);c.drawCircle(x,y,43,p);p.setStyle(Paint.Style.FILL);text(c,d.kind==0?"⚒":d.kind==1?"▣":d.kind==3?"✦":d.kind==5?"♟":"◆",x,y+9,23,col,Paint.Align.CENTER);
            text(c,ReleaseContent.techName(s.language,i),x+(i%2==0?-65:65),y-7,9,TEXT,i%2==0?Paint.Align.RIGHT:Paint.Align.LEFT);text(c,s.tech[i]+" / "+d.max,x+(i%2==0?-65:65),y+13,8,max?GREEN:GOLD,i%2==0?Paint.Align.RIGHT:Paint.Align.LEFT);if(!max)text(c,s.techCost(i)+" RP",x+(i%2==0?-65:65),y+32,7,MUTED,i%2==0?Paint.Align.RIGHT:Paint.Align.LEFT);
            hits.add(new Hit(new RectF(x-50,y-50,x+50,y+50),"tech",i));
        }
        float total=n*step+100;maxScroll[3]=Math.max(0,total-viewportH());scroll[3]=clamp(scroll[3],0,maxScroll[3]);
    }

    private void drawMissions(Canvas c){
        float off=missionScroll[missionSub], y=contentTop()+8-off,card=102,gap=9;int count;
        if(missionSub<3){int start=missionSub*6;count=6;for(int r=0;r<6;r++){int i=start+r;if(i>=s.missionClaimed.length)break;drawMissionCard(c,i,18,y,702,y+card);y+=card+gap;}}
        else{count=30;for(int i=0;i<30;i++){drawAchievementCard(c,i,18,y,702,y+card);y+=card+gap;}}
        float total=count*(card+gap)+16;missionMax[missionSub]=Math.max(0,total-viewportH());missionScroll[missionSub]=clamp(missionScroll[missionSub],0,missionMax[missionSub]);
    }
    private void drawMissionCard(Canvas c,int i,float x1,float y1,float x2,float y2){
        int type=s.missionType(i);double target=s.missionTarget(i),val=s.goalValue(type);boolean ready=s.missionReady(i),claimed=s.missionClaimed[i];panel(c,x1,y1,x2,y2,claimed?Color.rgb(24,45,37):PANEL,ready?GREEN:claimed?Color.rgb(59,93,73):Color.rgb(50,92,117),12);
        text(c,s.goalLabel(type,target),x1+18,y1+27,11,TEXT,Paint.Align.LEFT);bar(c,x1+18,y1+43,x2-185,y1+58,(float)Math.min(1,val/Math.max(1,target)),ready?GREEN:CYAN);text(c,s.format(val)+" / "+s.format(target),x1+18,y1+79,8,MUTED,Paint.Align.LEFT);
        String lab=claimed?tr("ODEBRANO","CLAIMED"):ready?tr("ODBIERZ","CLAIM"):"◆ "+s.missionRewardCrystals(i)+"  RP "+s.missionRewardResearch(i);button(c,x2-162,y1+24,x2-18,y2-20,lab,ready?Color.rgb(44,163,41):PANEL2,TEXT,ready);if(ready)hits.add(new Hit(new RectF(x2-170,y1+18,x2-10,y2-12),"mission",i));
    }
    private void drawAchievementCard(Canvas c,int i,float x1,float y1,float x2,float y2){
        double[] goals={10,100,1000,10000,100000,1000000,1e4,1e6,1e9,1e12,1e15,10,100,500,1000,10,50,100,250,500,50,250,500,1000,1,5,10,1,5,10};int[] types={0,0,0,0,0,0,1,1,1,1,1,2,2,2,2,3,3,3,3,3,5,5,5,5,6,6,6,7,7,7};boolean done=s.achievementUnlocked[i];double val=s.goalValue(types[i]);panel(c,x1,y1,x2,y2,done?Color.rgb(25,55,39):PANEL,done?GREEN:Color.rgb(55,90,109),12);text(c,done?"★":"☆",x1+42,y1+61,29,done?GOLD:MUTED,Paint.Align.CENTER);text(c,tr("Osiągnięcie #","Achievement #")+(i+1),x1+85,y1+28,13,TEXT,Paint.Align.LEFT);text(c,s.goalLabel(types[i],goals[i]),x1+85,y1+50,8,MUTED,Paint.Align.LEFT);bar(c,x1+85,y1+65,x2-22,y1+80,(float)Math.min(1,val/Math.max(1,goals[i])),done?GREEN:GOLD);text(c,done?tr("UKOŃCZONE","COMPLETED"):s.format(val)+" / "+s.format(goals[i]),x2-22,y1+96,8,done?GREEN:MUTED,Paint.Align.RIGHT);
    }

    private void drawShop(Canvas c){
        float y=contentTop()+10-scroll[5];String[] names={tr("x2 ZŁOTO","x2 GOLD"),tr("x4 XP BUDOWY","x4 BUILD XP"),"POWER TAP +1"};String[] desc={tr("30 minut podwójnego dochodu","30 minutes double income"),tr("30 minut szybszej budowy","30 minutes faster building"),tr("Dodatkowy ładunek aktywnej mocy","One extra active-power charge")};int[] cost={100,100,80},col={GOLD,CYAN,ORANGE};
        for(int i=0;i<3;i++){panel(c,24,y,696,y+142,Color.rgb(25,24,49),col[i],14);drawRoundIcon(c,78,y+71,42,col[i],i==0?"●":i==1?"◆":"⚡");text(c,names[i],140,y+35,16,TEXT,Paint.Align.LEFT);text(c,desc[i],140,y+61,9,MUTED,Paint.Align.LEFT);text(c,tr("Posiadasz: ","You have: ")+s.crystals+" ◆",140,y+88,9,CYAN,Paint.Align.LEFT);button(c,500,y+44,672,y+103,cost[i]+" ◆",Color.rgb(26,82,109),TEXT,s.crystals>=cost[i]);hits.add(new Hit(new RectF(492,y+36,680,y+111),"shop",i));y+=154;}
        panel(c,24,y,696,y+300,Color.rgb(14,33,44),PURPLE,14);text(c,tr("ZDOBYWANIE KRYSZTAŁÓW","EARNING CRYSTALS"),44,y+38,15,TEXT,Paint.Align.LEFT);String[] q={tr("Misje i osiągnięcia","Missions and achievements"),tr("Nagrody dzienne","Daily rewards"),tr("Eventy specjalne","Special events"),tr("Pokonywanie bossów","Defeating bosses"),tr("Awans do kolejnych epok","Reaching new eras")};for(int i=0;i<q.length;i++)text(c,"• "+q[i],52,y+78+i*37,10,MUTED,Paint.Align.LEFT);text(c,tr("Sklep nie wymaga pieniędzy — używa waluty zdobywanej w grze.","The shop uses currency earned in-game."),52,y+272,8,GREEN,Paint.Align.LEFT);y+=320;
        maxScroll[5]=Math.max(0,(y-(contentTop()+10))-viewportH());scroll[5]=clamp(scroll[5],0,maxScroll[5]);
    }

    private void drawEmpire(Canvas c){
        float y=contentTop()+10-scroll[6];panel(c,20,y,700,y+285,PANEL,PURPLE,14);text(c,tr("STATYSTYKI","STATISTICS"),42,y+38,17,TEXT,Paint.Align.LEFT);String[] l={tr("Łącznie zarobione","Total earned"),tr("Łącznie kliknięć","Total taps"),tr("Najlepsze combo","Best combo"),tr("Pokonani bossowie","Bosses defeated"),tr("Czas gry","Play time"),tr("Odrodzenia","Rebirths")};String[] v={s.format(s.lifetimeCoins),Long.toString(s.totalTaps),"x"+String.format(Locale.US,"%.1f",ReleaseGameState.comboMultiplier(s.bestComboTaps)),Integer.toString(s.bossDefeats),ReleaseGameState.duration(s.playSeconds),Integer.toString(s.rebirths)};for(int i=0;i<6;i++){float yy=y+82+i*31;text(c,l[i],44,yy,10,MUTED,Paint.Align.LEFT);text(c,v[i],674,yy,11,TEXT,Paint.Align.RIGHT);}y+=300;
        panel(c,20,y,700,y+185,Color.rgb(26,27,48),GOLD,14);text(c,tr("ODRODZENIE","REBIRTH"),42,y+39,17,GOLD,Paint.Align.LEFT);int gain=s.availableLegacyStars();text(c,tr("Gwiazd Dziedzictwa po resecie: ","Legacy Stars after reset: ")+gain,42,y+75,10,TEXT,Paint.Align.LEFT);text(c,tr("Stały mnożnik: x","Permanent multiplier: x")+String.format(Locale.US,"%.2f",s.legacyMultiplier()),42,y+104,9,MUTED,Paint.Align.LEFT);button(c,458,y+53,674,y+140,gain>0?tr("ODRODŹ +","REBIRTH +")+gain:tr("WYMAGA EPOKI 10","REQUIRES ERA 10"),gain>0?Color.rgb(109,48,170):Color.rgb(51,58,62),TEXT,gain>0);if(gain>0)hits.add(new Hit(new RectF(450,y+45,682,y+148),"rebirth",0));y+=200;
        panel(c,20,y,700,y+360,PANEL,CYAN,14);text(c,tr("USTAWIENIA","SETTINGS"),42,y+38,17,TEXT,Paint.Align.LEFT);setting(c,y+62,tr("JĘZYK","LANGUAGE"),languageLabel(),"lang",true);setting(c,y+119,tr("DŹWIĘK","SOUND"),s.sound?"ON":"OFF","sound",s.sound);setting(c,y+176,tr("WIBRACJE","HAPTICS"),s.haptics?"ON":"OFF","haptic",s.haptics);setting(c,y+233,tr("TRYB OSZCZĘDNY","LOW POWER"),s.lowPower?"ON":"OFF","lowpower",s.lowPower);button(c,470,y+293,674,y+340,tr("WYCZYŚĆ ZAPIS","RESET SAVE"),Color.rgb(116,38,44),TEXT,true);hits.add(new Hit(new RectF(462,y+286,682,y+347),"reset",0));y+=380;
        maxScroll[6]=Math.max(0,(y-(contentTop()+10))-viewportH());scroll[6]=clamp(scroll[6],0,maxScroll[6]);
    }
    private void setting(Canvas c,float y,String l,String v,String type,boolean on){text(c,l,44,y+29,11,TEXT,Paint.Align.LEFT);button(c,512,y,674,y+43,v,on?Color.rgb(30,121,59):PANEL2,TEXT,true);hits.add(new Hit(new RectF(505,y-5,681,y+48),type,0));}

    private void drawScrollBar(Canvas c,int t){
        float max=t==4?missionMax[missionSub]:maxScroll[t],cur=t==4?missionScroll[missionSub]:scroll[t];if(max<=1)return;float top=contentTop()+4,bottom=navY()-9,h=bottom-top,thumb=Math.max(40,h*h/(h+max)),y=top+(h-thumb)*(cur/max);p.setColor(Color.argb(65,255,255,255));c.drawRoundRect(new RectF(708,top,713,bottom),3,3,p);p.setColor(CYAN);c.drawRoundRect(new RectF(708,y,713,y+thumb),3,3,p);
    }
    private void drawNav(Canvas c,int active){float y=navY();p.setColor(Color.rgb(2,10,15));c.drawRect(0,y,W,logicalH,p);String[] labs={tr("ŚWIAT","WORLD"),tr("BUDOWA","BUILD"),tr("BOHATEROWIE","HEROES"),tr("BADANIA","RESEARCH"),tr("MISJE","MISSIONS"),tr("SKLEP","SHOP"),tr("IMPERIUM","EMPIRE")};String[] ico={"⬟","⚒","♟","◆","▤","▣","♛"};int[] col={CYAN,ORANGE,GOLD,PURPLE,GOLD,CYAN,GOLD};float w=W/7f;for(int i=0;i<7;i++){float x=i*w;if(i==active)panel(c,x+3,y+4,x+w-3,logicalH-4,Color.rgb(15,59,76),CYAN,9);text(c,ico[i],x+w/2,y+47,25,i==active?CYAN:col[i],Paint.Align.CENTER);text(c,labs[i],x+w/2,y+78,i==2?6.2f:7f,TEXT,Paint.Align.CENTER);hits.add(new Hit(new RectF(x,y,x+w,logicalH),"tab",i));}}

    @Override public boolean onTouchEvent(MotionEvent e){int t=tab();if(t==0)return false;scale=getWidth()/W;if(scale<=0)scale=1;logicalH=getHeight()/scale;float x=e.getX()/scale,y=e.getY()/scale;if(y<126)return false;
        if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;lastY=y;dragging=false;return true;}
        if(e.getAction()==MotionEvent.ACTION_MOVE){float dy=y-lastY;if(Math.abs(y-downY)>8)dragging=true;if(y>=contentTop()&&y<navY()){if(t==4)missionScroll[missionSub]=clamp(missionScroll[missionSub]-dy,0,missionMax[missionSub]);else scroll[t]=clamp(scroll[t]-dy,0,maxScroll[t]);invalidate();}lastY=y;return true;}
        if(e.getAction()==MotionEvent.ACTION_UP){if(dragging||Math.abs(y-downY)>12)return true;for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(h.r.contains(x,y)){act(h);return true;}}return true;}return true;}

    private void act(Hit h){String t=h.type;if("tab".equals(t)){setTab(h.id);return;}if("buyMode".equals(t)){buyMode=h.id;invalidate();return;}if("missionSub".equals(t)){missionSub=h.id;invalidate();return;}boolean ok=false;
        if("building".equals(t)){int n=buyCount(h.id);ok=s.buyBuilding(h.id,n);}else if("hero".equals(t))ok=s.upgradeHero(h.id);else if("tech".equals(t))ok=s.buyTech(h.id);else if("mission".equals(t))ok=s.claimMission(h.id);else if("shop".equals(t))ok=s.shopBuy(h.id);
        else if("rebirth".equals(t)){long now=System.currentTimeMillis();if(now<rebirthConfirmUntil){ok=s.rebirth();rebirthConfirmUntil=0;scroll[6]=0;}else rebirthConfirmUntil=now+3500;}
        else if("lang".equals(t)){String[] l={"pl","en","es","cs","ru","zh"};int ix=0;for(int i=0;i<l.length;i++)if(l[i].equals(s.language))ix=i;s.language=l[(ix+1)%l.length];s.save(getContext());invalidate();return;}else if("sound".equals(t)){s.sound=!s.sound;}else if("haptic".equals(t)){s.haptics=!s.haptics;}else if("lowpower".equals(t)){s.lowPower=!s.lowPower;}else if("reset".equals(t)){long now=System.currentTimeMillis();if(now<resetConfirmUntil){s.hardReset(getContext());resetConfirmUntil=0;}else resetConfirmUntil=now+3500;}
        if(ok)s.save(getContext());invalidate();
    }

    private String languageLabel(){if("en".equals(s.language))return "English";if("es".equals(s.language))return "Español";if("cs".equals(s.language))return "Čeština";if("ru".equals(s.language))return "Русский";if("zh".equals(s.language))return "中文";return "Polski";}
    private String tr(String pl,String en){return ReleaseGameState.tr(s.language,pl,en,en,en,en,en);}
    private void drawRoundIcon(Canvas c,float x,float y,float r,int col,String icon){p.setColor(Color.rgb(7,18,26));c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(col);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);text(c,icon,x,y+8,22,col,Paint.Align.CENTER);}
    private void panel(Canvas c,float x1,float y1,float x2,float y2,int fill,int edge,float r){p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(fill);c.drawRoundRect(new RectF(x1,y1,x2,y2),r,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setColor(edge);c.drawRoundRect(new RectF(x1,y1,x2,y2),r,r,p);p.setStyle(Paint.Style.FILL);}
    private void button(Canvas c,float x1,float y1,float x2,float y2,String label,int fill,int color,boolean enabled){p.setColor(enabled?fill:Color.rgb(48,55,60));c.drawRoundRect(new RectF(x1,y1,x2,y2),8,8,p);if(enabled){p.setShader(new LinearGradient(0,y1,0,y2,Color.argb(55,255,255,255),Color.TRANSPARENT,Shader.TileMode.CLAMP));c.drawRoundRect(new RectF(x1+2,y1+2,x2-2,y1+(y2-y1)*.42f),7,7,p);p.setShader(null);}text(c,label,(x1+x2)/2,(y1+y2)/2+4,9,enabled?color:MUTED,Paint.Align.CENTER);}
    private void bar(Canvas c,float x1,float y1,float x2,float y2,float v,int col){v=clamp(v,0,1);p.setColor(Color.rgb(29,44,52));c.drawRoundRect(new RectF(x1,y1,x2,y2),6,6,p);if(v>0){p.setColor(col);c.drawRoundRect(new RectF(x1,y1,x1+(x2-x1)*v,y2),6,6,p);}}
    private void text(Canvas c,String t,float x,float y,float size,int color,Paint.Align a){tp.setTextSize(size);tp.setTextAlign(a);tp.setColor(color);tp.setStyle(Paint.Style.FILL);c.drawText(t,x,y,tp);}
    private float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
