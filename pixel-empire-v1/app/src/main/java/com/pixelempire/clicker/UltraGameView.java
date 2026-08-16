package com.pixelempire.clicker;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.Locale;

/**
 * Production shell for the premium renderer. It keeps the living world fully interactive,
 * but overlays the dense home dashboard requested for the Google Play version.
 */
public final class UltraGameView extends FrameLayout {
    private final ReleaseGameState state;
    private final PremiumGameView game;
    private final Dashboard dashboard;

    public UltraGameView(Context context, ReleaseGameState state) {
        super(context);
        this.state = state;
        setClipChildren(false);
        game = new PremiumGameView(context, state);
        dashboard = new Dashboard(context, state, game);
        addView(game, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(dashboard, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public boolean handleBack() {
        boolean used = game.handleBack();
        if (used) dashboard.returnHome();
        return used;
    }

    private static final class Dashboard extends View {
        private static final float W=720f;
        private static final int BG=Color.rgb(4,11,17), PANEL=Color.rgb(11,27,38), PANEL2=Color.rgb(18,39,53), EDGE=Color.rgb(65,96,113);
        private static final int TEXT=Color.rgb(248,250,252), MUTED=Color.rgb(168,184,195), GOLD=Color.rgb(255,190,28), GREEN=Color.rgb(72,205,58), CYAN=Color.rgb(49,207,236), PURPLE=Color.rgb(174,80,255), RED=Color.rgb(239,66,73), ORANGE=Color.rgb(255,132,30);
        private final ReleaseGameState s;
        private final PremiumGameView base;
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG), tp=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float scale=1f, logicalH=1536f, downX,downY;
        private int currentTab=0;
        private long rebirthConfirmUntil=0;

        Dashboard(Context c, ReleaseGameState state, PremiumGameView base){
            super(c);s=state;this.base=base;tp.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));setBackgroundColor(Color.TRANSPARENT);setWillNotDraw(false);
        }
        void returnHome(){currentTab=0;invalidate();}

        private float overlayTop(){return Math.min(790f,Math.max(690f,logicalH*.51f));}
        private float utilityTop(){return logicalH-54f;}

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);scale=getWidth()/W;if(scale<=0)scale=1;logicalH=getHeight()/scale;
            if(currentTab!=0)return;
            c.save();c.scale(scale,scale);
            float top=overlayTop(), util=utilityTop();
            p.setShader(new LinearGradient(0,top,0,top+70,Color.argb(25,2,8,12),Color.argb(250,3,11,17),Shader.TileMode.CLAMP));c.drawRect(0,top,W,top+72,p);p.setShader(null);
            drawCombo(c,top+4);drawPower(c,top+4);drawNav(c,top+82);
            float dashTop=top+180;
            if(dashTop<util-130)drawDashboard(c,dashTop,util-7);
            drawUtility(c,util);
            c.restore();
        }

        private void drawCombo(Canvas c,float y){
            panel(c,12,y,540,y+70,Color.argb(248,5,17,24),Color.rgb(76,106,119),12);
            double m=ReleaseGameState.comboMultiplier((int)Math.max(0,s.bestComboTaps));
            text(c,"COMBO",28,y+19,9,CYAN,Paint.Align.LEFT);
            text(c,"x"+String.format(Locale.US,"%.1f",m),28,y+51,27,GOLD,Paint.Align.LEFT);
            text(c,tr("Mnożnik rośnie o x0.1 co 50 kolejnych kliknięć","Multiplier grows x0.1 every 50 consecutive taps"),150,y+18,8,TEXT,Paint.Align.LEFT);
            int shown=Math.max(0,s.bestComboTaps);int next=((shown/50)+1)*50;float progress=(shown%50)/50f;
            bar(c,150,y+29,520,y+41,progress,GOLD);
            text(c,shown+" / "+next+" "+tr("kliknięć","taps"),335,y+58,8,MUTED,Paint.Align.CENTER);
        }
        private void drawPower(Canvas c,float y){
            panel(c,550,y,708,y+70,Color.rgb(26,111,25),Color.rgb(111,240,76),12);
            text(c,"⚡",575,y+43,27,GOLD,Paint.Align.CENTER);text(c,"POWER TAP",635,y+25,10,TEXT,Paint.Align.CENTER);text(c,s.powerTapCharges+" / 9",635,y+48,15,TEXT,Paint.Align.CENTER);text(c,tr("AKTYWUJ","ACTIVATE"),635,y+63,6.5f,GREEN,Paint.Align.CENTER);
        }

        private void drawNav(Canvas c,float y){
            String[] names={tr("ŚWIAT","WORLD"),tr("BUDOWA","BUILD"),tr("BOHATEROWIE","HEROES"),tr("BADANIA","RESEARCH"),tr("MISJE","MISSIONS"),tr("SKLEP","SHOP"),tr("IMPERIUM","EMPIRE")};
            String[] icons={"⬟","⚒","♟","◆","▤","▣","♛"};int[] cols={CYAN,ORANGE,GOLD,PURPLE,GOLD,CYAN,GOLD};
            for(int i=0;i<7;i++){float x=5+i*101.5f;panel(c,x,y,x+94,y+87,i==0?Color.rgb(15,60,77):Color.rgb(10,24,33),i==0?CYAN:Color.rgb(59,83,95),10);text(c,icons[i],x+47,y+38,24,cols[i],Paint.Align.CENTER);text(c,names[i],x+47,y+69,i==2?6.2f:7.2f,TEXT,Paint.Align.CENTER);if(i>0&&readyBadge(i)>0){p.setColor(RED);c.drawCircle(x+86,y+8,10,p);text(c,Integer.toString(readyBadge(i)),x+86,y+11,7,Color.WHITE,Paint.Align.CENTER);}}
        }
        private int readyBadge(int tab){if(tab==4){int n=0;for(int i=0;i<s.missionClaimed.length;i++)if(s.missionReady(i))n++;return n;}if(tab==1&&s.coins>ReleaseContent.BUILDINGS[0].baseCost)return 1;if(tab==2&&s.stage>=2)return 1;if(tab==3&&s.researchPoints>0)return 1;return 0;}

        private void drawDashboard(Canvas c,float top,float bottom){
            float gap=7,col=(W-gap*4)/3f,mid=top+(bottom-top)*.51f;
            techCard(c,gap,top,gap+col,mid-4);heroCard(c,gap*2+col,top,gap*2+col*2,mid-4);missionCard(c,gap*3+col*2,top,W-gap,mid-4);
            achievementCard(c,gap,mid+4,gap+col,bottom);offlineCard(c,gap*2+col,mid+4,gap*2+col*2,bottom);shopCard(c,gap*3+col*2,mid+4,W-gap,bottom);
        }

        private void techCard(Canvas c,float x1,float y1,float x2,float y2){
            panel(c,x1,y1,x2,y2,Color.rgb(7,27,43),Color.rgb(35,100,147),9);text(c,"TECH TREE",x1+11,y1+22,10,GOLD,Paint.Align.LEFT);text(c,Integer.toString(s.researchPoints)+" RP",x2-10,y1+22,8,PURPLE,Paint.Align.RIGHT);
            float[][] pos={{.24f,.28f},{.66f,.28f},{.45f,.52f},{.21f,.73f},{.68f,.73f},{.45f,.9f}};
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.rgb(75,96,108));for(int i=1;i<pos.length;i++){float ax=x1+(x2-x1)*pos[Math.max(0,i-1)][0],ay=y1+(y2-y1)*pos[Math.max(0,i-1)][1],bx=x1+(x2-x1)*pos[i][0],by=y1+(y2-y1)*pos[i][1];c.drawLine(ax,ay,bx,by,p);}p.setStyle(Paint.Style.FILL);
            for(int i=0;i<6&&i<ReleaseContent.TECH.length;i++){float cx=x1+(x2-x1)*pos[i][0],cy=y1+(y2-y1)*pos[i][1];boolean unlocked=s.techUnlocked(i);int co=s.tech[i]>0?GREEN:unlocked?GOLD:Color.rgb(87,94,97);p.setColor(Color.rgb(16,31,39));c.drawCircle(cx,cy,18,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);p.setColor(co);c.drawCircle(cx,cy,18,p);p.setStyle(Paint.Style.FILL);text(c,unlocked?"◆":"×",cx,cy+4,10,co,Paint.Align.CENTER);text(c,s.tech[i]+"/"+ReleaseContent.TECH[i].max,cx,cy+29,6,TEXT,Paint.Align.CENTER);}
        }
        private void heroCard(Canvas c,float x1,float y1,float x2,float y2){
            panel(c,x1,y1,x2,y2,Color.rgb(17,25,47),Color.rgb(73,71,143),9);text(c,tr("BOHATEROWIE","HEROES"),x1+11,y1+22,10,GOLD,Paint.Align.LEFT);text(c,s.format(s.getHeroDps())+" DPS",x2-10,y1+22,7,CYAN,Paint.Align.RIGHT);
            int row=0;for(int i=0;i<ReleaseContent.HEROES.length&&row<4;i++){ReleaseContent.HeroDef h=ReleaseContent.HEROES[i];if(s.stage<h.unlockStage)continue;float yy=y1+34+row*49;p.setColor(Color.argb(225,35,28,59));c.drawRoundRect(new RectF(x1+7,yy,x2-7,yy+43),6,6,p);p.setColor(h.color);c.drawCircle(x1+27,yy+21,14,p);text(c,"♟",x1+27,yy+26,13,TEXT,Paint.Align.CENTER);text(c,h.name,x1+49,yy+17,7.5f,TEXT,Paint.Align.LEFT);text(c,"Lv "+s.heroes[i]+" • "+s.format(h.baseDps*Math.max(1,s.heroes[i])),x1+49,yy+32,6.2f,CYAN,Paint.Align.LEFT);p.setColor(s.coins>=s.heroCost(i)?Color.rgb(47,147,40):Color.rgb(48,60,66));c.drawRoundRect(new RectF(x2-55,yy+8,x2-11,yy+35),5,5,p);text(c,tr("UP","UP"),x2-33,yy+26,6,TEXT,Paint.Align.CENTER);row++;}
            if(row==0)text(c,tr("Pierwszy bohater od Epoki 3","First hero unlocks in Era 3"),(x1+x2)/2,y1+82,7,MUTED,Paint.Align.CENTER);
        }
        private void missionCard(Canvas c,float x1,float y1,float x2,float y2){
            panel(c,x1,y1,x2,y2,Color.rgb(10,31,46),Color.rgb(38,97,142),9);text(c,tr("MISJE","MISSIONS"),x1+11,y1+22,10,GOLD,Paint.Align.LEFT);int row=0;for(int i=0;i<s.missionClaimed.length&&row<4;i++){if(s.missionClaimed[i])continue;double target=s.missionTarget(i),val=Math.min(target,s.goalValue(s.missionType(i)));float yy=y1+36+row*48;text(c,s.goalLabel(s.missionType(i),target),x1+9,yy+10,6.5f,TEXT,Paint.Align.LEFT);bar(c,x1+9,yy+18,x2-43,yy+28,(float)(target<=0?0:val/target),GREEN);if(s.missionReady(i)){p.setColor(Color.rgb(42,146,39));c.drawRoundRect(new RectF(x2-38,yy+7,x2-8,yy+33),5,5,p);text(c,"✓",x2-23,yy+25,9,TEXT,Paint.Align.CENTER);}row++;}}
        private void achievementCard(Canvas c,float x1,float y1,float x2,float y2){
            panel(c,x1,y1,x2,y2,Color.rgb(9,29,37),Color.rgb(39,88,106),9);text(c,tr("OSIĄGNIĘCIA","ACHIEVEMENTS"),x1+11,y1+21,9,GOLD,Paint.Align.LEFT);text(c,s.achievementsUnlocked+" / 30",x2-10,y1+21,7,TEXT,Paint.Align.RIGHT);String[] n={tr("Pierwsze kroki","First steps"),tr("Milioner","Millionaire"),tr("Budowniczy","Builder"),tr("Mistrz combo","Combo master")};for(int i=0;i<4;i++){float yy=y1+35+i*43;boolean done=s.achievementUnlocked[i];p.setColor(Color.argb(205,20,39,45));c.drawRoundRect(new RectF(x1+7,yy,x2-7,yy+37),5,5,p);text(c,done?"✓":"★",x1+25,yy+24,12,done?GREEN:GOLD,Paint.Align.CENTER);text(c,n[i],x1+44,yy+16,7,TEXT,Paint.Align.LEFT);text(c,done?tr("UKOŃCZONE","COMPLETE"):tr("W TRAKCIE","IN PROGRESS"),x1+44,yy+29,5.6f,done?GREEN:MUTED,Paint.Align.LEFT);}}
        private void offlineCard(Canvas c,float x1,float y1,float x2,float y2){
            panel(c,x1,y1,x2,y2,Color.rgb(29,20,49),Color.rgb(105,69,160),9);text(c,tr("DOCHÓD OFFLINE","OFFLINE INCOME"),x1+11,y1+21,9,GOLD,Paint.Align.LEFT);text(c,"x0.2",x1+12,y1+58,25,GOLD,Paint.Align.LEFT);text(c,tr("aktualnego dochodu/s","of current income/s"),x1+78,y1+53,6.5f,MUTED,Paint.Align.LEFT);text(c,tr("Limit: 12 godzin","Cap: 12 hours"),x1+12,y1+82,7,TEXT,Paint.Align.LEFT);text(c,tr("Stawka teraz: ","Rate now: ")+s.format(s.getCps()*.2)+"/s",x1+12,y1+108,7,CYAN,Paint.Align.LEFT);text(c,"●●●",(x1+x2)/2,y2-28,18,GOLD,Paint.Align.CENTER);}
        private void shopCard(Canvas c,float x1,float y1,float x2,float y2){
            panel(c,x1,y1,x2,y2,Color.rgb(28,20,52),Color.rgb(98,68,164),9);text(c,tr("SKLEP","SHOP"),x1+11,y1+21,10,GOLD,Paint.Align.LEFT);String[] n={"x2 "+tr("Złoto","Gold"),"x4 XP","Power Tap"};int[] cost={100,100,80},co={GOLD,CYAN,ORANGE};for(int i=0;i<3;i++){float yy=y1+35+i*48;panel(c,x1+7,yy,x2-7,yy+42,Color.rgb(40,25,68),co[i],5);text(c,i==0?"●":i==1?"◆":"⚡",x1+26,yy+26,13,co[i],Paint.Align.CENTER);text(c,n[i],x1+44,yy+16,6.7f,TEXT,Paint.Align.LEFT);text(c,cost[i]+" ◆",x1+44,yy+31,6.5f,CYAN,Paint.Align.LEFT);}}

        private void drawUtility(Canvas c,float y){
            p.setColor(Color.rgb(3,11,16));c.drawRect(0,y,W,logicalH,p);p.setColor(Color.rgb(53,77,88));c.drawRect(0,y,W,y+1.5f,p);
            String[] l={tr("USTAW.","SETTINGS"),tr("STAT.","STATS"),"REBIRTH",tr("RANKING","RANKING"),tr("POCZTA","MAIL"),tr("KODEKS","CODEX"),tr("EVENTY","EVENTS")};String[] ic={"⚙","▥","◉","♛","✉","▣","★"};
            for(int i=0;i<7;i++){float x=i*(W/7f),cx=x+W/14f;text(c,ic[i],cx,y+20,12,i==2?PURPLE:i==6?RED:MUTED,Paint.Align.CENTER);text(c,l[i],cx,y+40,5.7f,TEXT,Paint.Align.CENTER);}
        }

        @Override public boolean onTouchEvent(MotionEvent e){
            scale=getWidth()/W;if(scale<=0)scale=1;logicalH=getHeight()/scale;float x=e.getX()/scale,y=e.getY()/scale;
            boolean baseNav=y>=logicalH-108;
            if(currentTab!=0){
                if(baseNav){if(e.getAction()==MotionEvent.ACTION_UP){int idx=Math.max(0,Math.min(6,(int)(x/(W/7f))));currentTab=idx;forwardTab(idx);invalidate();}return true;}return false;
            }
            float top=overlayTop();if(y<top)return false;
            if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;return true;}
            if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            if(Math.abs(x-downX)>18||Math.abs(y-downY)>18)return true;
            if(y>=utilityTop()){
                int idx=Math.max(0,Math.min(6,(int)(x/(W/7f))));if(idx==0){switchTab(6);}else if(idx==2){long now=System.currentTimeMillis();if(now<rebirthConfirmUntil){s.rebirth();rebirthConfirmUntil=0;}else rebirthConfirmUntil=now+3500;}else if(idx==6&&s.eventType>=0)s.collectEvent();invalidate();return true;
            }
            if(y>=top+82&&y<=top+169){int idx=Math.max(0,Math.min(6,(int)((x-5)/101.5f)));switchTab(idx);return true;}
            if(x>=550&&y<=top+74){s.activatePowerTap();invalidate();return true;}
            float dashTop=top+180,mid=dashTop+(utilityTop()-7-dashTop)*.51f;float col=(W-28)/3f;
            if(y>=dashTop){int column=x<(7+col)?0:x<(14+col*2)?1:2;int row=y<mid?0:1;if(row==0&&column==0)switchTab(3);else if(row==0&&column==1)switchTab(2);else if(row==0&&column==2)switchTab(4);else if(row==1&&column==0)switchTab(4);else if(row==1&&column==2)switchTab(5);return true;}
            return true;
        }
        private void switchTab(int idx){currentTab=idx;forwardTab(idx);invalidate();}
        private void forwardTab(int idx){
            float gameScale=getWidth()/W,lh=getHeight()/gameScale,lx=(idx+.5f)*(W/7f),ly=lh-50,px=lx*gameScale,py=ly*gameScale;long now=System.currentTimeMillis();
            MotionEvent d=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,px,py,0),u=MotionEvent.obtain(now,now+20,MotionEvent.ACTION_UP,px,py,0);base.onTouchEvent(d);base.onTouchEvent(u);d.recycle();u.recycle();
        }

        private String tr(String pl,String en){return ReleaseGameState.tr(s.language,pl,en,en,en,en,en);}
        private void panel(Canvas c,float x1,float y1,float x2,float y2,int fill,int edge,float r){p.setStyle(Paint.Style.FILL);p.setShader(null);p.setColor(fill);c.drawRoundRect(new RectF(x1,y1,x2,y2),r,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(edge);c.drawRoundRect(new RectF(x1,y1,x2,y2),r,r,p);p.setStyle(Paint.Style.FILL);}
        private void bar(Canvas c,float x1,float y1,float x2,float y2,float v,int col){v=Math.max(0,Math.min(1,v));p.setColor(Color.rgb(29,44,52));c.drawRoundRect(new RectF(x1,y1,x2,y2),6,6,p);if(v>0){p.setColor(col);c.drawRoundRect(new RectF(x1,y1,x1+(x2-x1)*v,y2),6,6,p);}}
        private void text(Canvas c,String t,float x,float y,float size,int color,Paint.Align a){tp.setTextSize(size);tp.setTextAlign(a);tp.setColor(color);tp.setStyle(Paint.Style.FILL);c.drawText(t,x,y,tp);}
    }
}
