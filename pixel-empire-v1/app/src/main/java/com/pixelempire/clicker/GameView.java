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
import java.util.Random;

public final class GameView extends View implements Runnable {
    private static final float W = 540f;
    private static final int BG = Color.rgb(6, 10, 20);
    private static final int PANEL = Color.rgb(15, 23, 42);
    private static final int PANEL2 = Color.rgb(24, 35, 58);
    private static final int STROKE = Color.rgb(58, 74, 102);
    private static final int TEXT = Color.rgb(244, 247, 252);
    private static final int MUTED = Color.rgb(148, 163, 184);
    private static final int GOLD = Color.rgb(251, 191, 36);
    private static final int GREEN = Color.rgb(52, 211, 153);
    private static final int CYAN = Color.rgb(34, 211, 238);
    private static final int PURPLE = Color.rgb(196, 132, 252);
    private static final int RED = Color.rgb(248, 113, 113);

    private final GameState state;
    private final Paint p = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final List<Hit> hits = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatText> floatTexts = new ArrayList<>();
    private final Vibrator vibrator;
    private ToneGenerator tone;

    private float scale = 1f;
    private float logicalH = 960f;
    private boolean running;
    private long lastFrameNs;
    private long lastAutosave;
    private double secondAccumulator;
    private long lastTapAt;
    private int combo;
    private int tab;
    private int upgradePage;
    private int goalPage;
    private int researchPage;
    private int goalMode;
    private int tutorialStep;
    private float downX, downY;
    private boolean moved;
    private boolean offlinePopup;
    private String toast = "";
    private long toastUntil;
    private long resetArmedUntil;
    private long ascendArmedUntil;
    private int lastSeenLevel;
    private int lastSeenStage;

    private static final class Hit {
        final RectF rect;
        final String type;
        final String id;
        Hit(RectF rect, String type, String id) { this.rect = rect; this.type = type; this.id = id; }
    }

    private static final class Particle {
        float x, y, vx, vy, life, size;
        final int color;
        Particle(float x, float y, float vx, float vy, float life, float size, int color) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy; this.life=life; this.size=size; this.color=color;
        }
    }

    private static final class FloatText {
        float x, y, life = 1f;
        final String text;
        final int color;
        FloatText(float x, float y, String text, int color) { this.x=x; this.y=y; this.text=text; this.color=color; }
    }

    public GameView(Context context, GameState state) {
        super(context);
        this.state = state;
        p.setAntiAlias(false);
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        setFocusable(true);
        setBackgroundColor(BG);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        try { tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 28); } catch (Throwable ignored) {}
        offlinePopup = state.startupOfflineGain > 0.01;
        lastSeenLevel = state.level;
        lastSeenStage = state.stage;
        if (state.nextEventAt <= 0) state.nextEventAt = System.currentTimeMillis() + 45_000;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        lastFrameNs = System.nanoTime();
        post(this);
    }

    @Override protected void onDetachedFromWindow() {
        running = false;
        removeCallbacks(this);
        if (tone != null) { try { tone.release(); } catch (Throwable ignored) {} tone = null; }
        super.onDetachedFromWindow();
    }

    @Override public void run() {
        if (!running) return;
        long nowNs = System.nanoTime();
        double dt = Math.min(0.25, Math.max(0, (nowNs - lastFrameNs) / 1_000_000_000.0));
        lastFrameNs = nowNs;
        state.tick(dt);
        secondAccumulator += dt;
        if (secondAccumulator >= 1.0) {
            long whole = (long) secondAccumulator;
            state.playSeconds += whole;
            secondAccumulator -= whole;
            state.checkAchievements();
        }
        updateEvent();
        updateEffects((float) dt);
        progressFeedback();
        achievementFeedback();
        long now = System.currentTimeMillis();
        if (lastAutosave == 0 || now - lastAutosave >= 5000) {
            state.save(getContext());
            lastAutosave = now;
        }
        invalidate();
        postDelayed(this, state.lowPower ? 100 : 33);
    }

    private void updateEvent() {
        long now = System.currentTimeMillis();
        if (state.activeEventType >= 0 && now >= state.activeEventUntil) {
            state.activeEventType = -1;
            state.activeEventUntil = 0;
            state.nextEventAt = now + 45_000 + random.nextInt(65_000);
        } else if (state.activeEventType < 0 && now >= state.nextEventAt) {
            state.activeEventType = random.nextInt(5);
            state.activeEventUntil = now + 18_000;
        }
    }

    private void updateEffects(float dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle q = particles.get(i);
            q.life -= dt;
            q.x += q.vx * dt;
            q.y += q.vy * dt;
            q.vy += 90f * dt;
            if (q.life <= 0) particles.remove(i);
        }
        for (int i = floatTexts.size() - 1; i >= 0; i--) {
            FloatText f = floatTexts.get(i);
            f.life -= dt * 0.78f;
            f.y -= 42f * dt;
            if (f.life <= 0) floatTexts.remove(i);
        }
    }

    private void progressFeedback() {
        if (state.stage > lastSeenStage) {
            lastSeenStage = state.stage;
            lastSeenLevel = state.level;
            showToast(L10n.t(state.language, "new_stage") + "  " + L10n.stageName(state.language, state.stage), 2600);
            burst(270, 390, CYAN, 36);
            feedback(true);
        } else if (state.level > lastSeenLevel) {
            lastSeenLevel = state.level;
            showToast(L10n.t(state.language, "new_level") + "  " + state.level, 1300);
            burst(270, 420, GOLD, 18);
        }
    }

    private void achievementFeedback() {
        if (!state.justUnlocked.isEmpty() && System.currentTimeMillis() > toastUntil) {
            GameState.Achievement a = state.justUnlocked.remove(0);
            showToast(L10n.t(state.language, "achievement") + "  +" + a.reward + " ◆", 1800);
        }
    }

    private void showToast(String s, long duration) {
        toast = s;
        toastUntil = System.currentTimeMillis() + duration;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        scale = getWidth() / W;
        if (scale <= 0) scale = 1;
        logicalH = getHeight() / scale;
        canvas.save();
        canvas.scale(scale, scale);
        hits.clear();
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);
        p.setColor(BG);
        canvas.drawRect(0, 0, W, logicalH, p);

        float top = 82f;
        float bottom = logicalH - 70f;
        if (tab == 0) drawWorld(canvas, top, bottom);
        else {
            drawDarkBackground(canvas);
            if (tab == 1) drawUpgrades(canvas, top, bottom);
            else if (tab == 2) drawGoals(canvas, top, bottom);
            else if (tab == 3) drawResearch(canvas, top, bottom);
            else drawMenu(canvas, top, bottom);
        }
        drawTopBar(canvas);
        drawEvent(canvas);
        drawBottomNav(canvas);
        drawEffects(canvas);
        drawToast(canvas);
        if (offlinePopup) drawOfflinePopup(canvas);
        if (!state.tutorialSeen) drawTutorial(canvas);
        canvas.restore();
    }

    private void drawDarkBackground(Canvas c) {
        p.setShader(new LinearGradient(0, 0, 0, logicalH, Color.rgb(6, 11, 23), Color.rgb(11, 21, 38), Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, logicalH, p);
        p.setShader(null);
        for (int i=0;i<20;i++) pixel(c, (i*79)%535, 95+(i*113)%760, i%4==0?3:2, Color.argb(55,100,160,220));
    }

    private void drawTopBar(Canvas c) {
        p.setColor(Color.argb(240, 8, 14, 26)); c.drawRect(0,0,W,82,p);
        p.setColor(Color.rgb(38,52,74)); c.drawRect(0,80,W,82,p);
        coin(c,24,28,12);
        text(c,state.format(state.coins),45,34,20,TEXT,Paint.Align.LEFT);
        text(c,state.format(state.getCps())+" "+L10n.t(state.language,"per_sec"),46,57,10,MUTED,Paint.Align.LEFT);
        diamond(c,356,28,9,PURPLE); text(c,Integer.toString(state.crystals),373,34,14,TEXT,Paint.Align.LEFT);
        diamond(c,426,28,9,CYAN); text(c,Integer.toString(state.researchPoints),443,34,14,TEXT,Paint.Align.LEFT);
        text(c,"★ "+state.legacyStars,510,34,14,GOLD,Paint.Align.RIGHT);
        text(c,L10n.t(state.language,"level")+" "+state.level,510,58,9,MUTED,Paint.Align.RIGHT);
    }

    private void drawWorld(Canvas c, float top, float bottom) {
        hits.add(new Hit(new RectF(0, top, W, bottom), "tap", ""));
        int s = state.stage;
        boolean night = LocalTime.now().getHour() < 6 || LocalTime.now().getHour() >= 20;
        int sky1, sky2;
        if (s >= 20) { sky1=Color.rgb(7,8,31); sky2=Color.rgb(48,19,84); night=true; }
        else if (s >= 15) { sky1=night?Color.rgb(8,15,40):Color.rgb(42,91,151); sky2=night?Color.rgb(35,21,75):Color.rgb(173,101,179); }
        else { sky1=night?Color.rgb(8,18,44):Color.rgb(61,135,203); sky2=night?Color.rgb(25,43,83):Color.rgb(164,221,240); }
        p.setShader(new LinearGradient(0,top,0,bottom,sky1,sky2,Shader.TileMode.CLAMP));
        c.drawRect(0,top,W,bottom,p); p.setShader(null);
        drawSky(c,top,night,s);
        drawMountains(c,bottom,s);
        float ground = bottom - 126f;
        drawGround(c,ground,bottom,s);
        drawAmbient(c,ground,s);
        drawBuilding(c,270,ground,s,(float)state.levelProgress());
        drawWorldHud(c,top,bottom);
    }

    private void drawSky(Canvas c,float top,boolean night,int stage) {
        if (night) {
            for(int i=0;i<34;i++) pixel(c,(i*83+17)%530,top+18+(i*47)%245,i%4==0?4:2,Color.rgb(224,235,255));
            p.setColor(Color.rgb(245,240,202)); c.drawRect(450,top+34,480,top+64,p);
            p.setColor(Color.rgb(30,36,73)); c.drawRect(461,top+29,485,top+55,p);
        } else {
            p.setColor(Color.rgb(255,224,92)); c.drawRect(452,top+34,482,top+64,p); c.drawRect(460,top+26,474,top+72,p);
        }
        float t=(System.currentTimeMillis()%22000L)/22000f;
        cloud(c,50+t*70,top+79,1f);
        cloud(c,385-t*55,top+130,.72f);
        if(stage>=18){p.setColor(Color.argb(185,190,225,255));c.drawRect(55,top+61,122,top+65,p);c.drawRect(69,top+56,110,top+70,p);}
    }

    private void cloud(Canvas c,float x,float y,float z) {
        p.setColor(Color.argb(205,255,255,255));
        c.drawRect(x,y+8*z,x+59*z,y+20*z,p);
        c.drawRect(x+10*z,y,x+46*z,y+25*z,p);
        c.drawRect(x+23*z,y-7*z,x+39*z,y+25*z,p);
    }

    private void drawMountains(Canvas c,float bottom,int stage) {
        float base=bottom-118;
        p.setColor(stage>=15?Color.rgb(34,45,77):Color.rgb(71,103,119));
        Path a=new Path(); a.moveTo(0,base);
        for(int x=0;x<=540;x+=60) a.lineTo(x,base-72-(float)(28*Math.sin((x+stage*13)*.028)));
        a.lineTo(540,base);a.close();c.drawPath(a,p);
        p.setColor(stage>=15?Color.rgb(23,34,59):Color.rgb(48,78,78));
        Path b=new Path();b.moveTo(0,base+22);
        for(int x=0;x<=540;x+=45)b.lineTo(x,base-31-(float)(20*Math.sin((x+80)*.037)));
        b.lineTo(540,base+22);b.close();c.drawPath(b,p);
    }

    private void drawGround(Canvas c,float ground,float bottom,int stage) {
        if(stage<12){
            p.setColor(Color.rgb(50,93,62));c.drawRect(0,ground,W,bottom,p);
            p.setColor(Color.rgb(86,125,72));c.drawRect(0,ground,W,ground+12,p);
            p.setColor(Color.rgb(58,47,38));c.drawRect(0,bottom-24,W,bottom,p);
        }else if(stage<18){
            p.setColor(Color.rgb(66,70,78));c.drawRect(0,ground,W,bottom,p);
            p.setColor(Color.rgb(122,111,83));c.drawRect(0,ground,W,ground+10,p);
            for(int x=0;x<540;x+=32){p.setColor(Color.rgb(45,49,57));c.drawRect(x,bottom-22,x+20,bottom-18,p);}
        }else{
            p.setColor(Color.rgb(23,32,51));c.drawRect(0,ground,W,bottom,p);
            p.setColor(CYAN);for(int x=0;x<540;x+=64)c.drawRect(x,ground+7,x+38,ground+10,p);
            p.setColor(Color.rgb(10,18,31));c.drawRect(0,bottom-23,W,bottom,p);
        }
    }

    private void drawAmbient(Canvas c,float ground,int stage) {
        if(stage<12){tree(c,34,ground-4,1f);tree(c,498,ground-4,.88f);if(stage>=3)tree(c,75,ground-3,.62f);}
        if(stage>=12&&stage<18){for(int i=0;i<3;i++){float x=45+i*185;p.setColor(Color.rgb(92,106,118));c.drawRect(x,ground-34,x+8,ground,p);p.setColor(Color.rgb(245,158,11));c.drawRect(x+1,ground-40,x+7,ground-33,p);}}
        float walk=(System.currentTimeMillis()%9000L)/9000f;
        int count=Math.min(5,1+stage/4);
        for(int i=0;i<count;i++){float x=((walk*620+i*138)%650)-55;worker(c,x,ground-18,i,stage);}
    }

    private void tree(Canvas c,float x,float y,float z){p.setColor(Color.rgb(87,57,39));c.drawRect(x-3*z,y-28*z,x+3*z,y,p);p.setColor(Color.rgb(35,111,61));c.drawRect(x-14*z,y-46*z,x+14*z,y-22*z,p);c.drawRect(x-9*z,y-57*z,x+9*z,y-36*z,p);}
    private void worker(Canvas c,float x,float y,int i,int stage){int body=stage>=15?CYAN:(i%2==0?Color.rgb(52,97,160):Color.rgb(158,88,63));p.setColor(Color.rgb(237,200,166));c.drawRect(x,y-13,x+6,y-7,p);p.setColor(body);c.drawRect(x-1,y-7,x+7,y+5,p);p.setColor(Color.rgb(20,25,35));c.drawRect(x,y+5,x+2,y+12,p);c.drawRect(x+5,y+5,x+7,y+12,p);if(stage>=15){p.setColor(PURPLE);c.drawRect(x+1,y-11,x+5,y-9,p);}}

    private void drawBuilding(Canvas c,float cx,float ground,int stage,float progress) {
        float g=.78f+.22f*progress;
        if(stage==0) drawLeanTo(c,cx,ground,g);
        else if(stage<=2) drawHut(c,cx,ground,g,stage);
        else if(stage<=5) drawVillage(c,cx,ground,g,stage);
        else if(stage<=11) drawCastle(c,cx,ground,g,stage);
        else if(stage<=15) drawIndustrial(c,cx,ground,g,stage);
        else drawFuture(c,cx,ground,g,stage);
    }

    private void drawLeanTo(Canvas c,float cx,float ground,float g){float w=145*g,h=82*g;p.setColor(Color.rgb(84,54,35));c.drawRect(cx-w/2,ground-h*.12f,cx+w/2,ground,p);for(int i=0;i<8;i++){p.setColor(i%2==0?Color.rgb(129,85,48):Color.rgb(105,68,40));float x=cx-w/2+i*w/8;c.drawRect(x,ground-h,x+w/8+2,ground-h*.08f,p);}p.setColor(Color.rgb(59,39,27));c.drawRect(cx-8,ground-h*.75f,cx+8,ground,p);}

    private void drawHut(Canvas c,float cx,float ground,float g,int stage){float w=(150+stage*18)*g,h=(94+stage*14)*g;p.setColor(Color.rgb(132,83,49));c.drawRect(cx-w/2,ground-h*.65f,cx+w/2,ground,p);p.setColor(Color.rgb(81,49,34));c.drawRect(cx-w*.58f,ground-h*.79f,cx+w*.58f,ground-h*.62f,p);c.drawRect(cx-w*.42f,ground-h*.94f,cx+w*.42f,ground-h*.77f,p);p.setColor(Color.rgb(250,213,96));c.drawRect(cx-w*.28f,ground-h*.45f,cx-w*.1f,ground-h*.25f,p);p.setColor(Color.rgb(55,38,29));c.drawRect(cx+w*.15f,ground-h*.46f,cx+w*.33f,ground,p);if(stage>=2){p.setColor(Color.rgb(90,60,38));c.drawRect(cx-w*.48f,ground-14,cx+w*.48f,ground-9,p);}}

    private void drawVillage(Canvas c,float cx,float ground,float g,int stage){int huts=2+Math.min(3,stage-3);for(int i=0;i<huts;i++){float x=cx+(i-(huts-1)/2f)*105*g;drawHut(c,x,ground-(i%2)*4,g*.55f,1);}float h=(100+(stage-3)*18)*g;p.setColor(Color.rgb(119,107,86));c.drawRect(cx-40*g,ground-h,cx+40*g,ground,p);p.setColor(Color.rgb(69,67,63));c.drawRect(cx-49*g,ground-h-12*g,cx+49*g,ground-h,p);p.setColor(GOLD);c.drawRect(cx-8*g,ground-h*.63f,cx+8*g,ground-h*.45f,p);if(stage>=5){p.setColor(Color.rgb(93,65,47));c.drawRect(cx-4,ground-h-48*g,cx+4,ground-h,p);p.setColor(Color.rgb(184,50,55));c.drawRect(cx+4,ground-h-47*g,cx+35,ground-h-36*g,p);}}

    private void drawCastle(Canvas c,float cx,float ground,float g,int stage){float w=(190+(stage-6)*15)*g,h=(108+(stage-6)*14)*g;int stone=stage>=10?Color.rgb(184,180,172):Color.rgb(120,127,134);p.setColor(stone);c.drawRect(cx-w/2,ground-h,cx+w/2,ground,p);int towers=stage>=9?4:2;for(int i=0;i<towers;i++){float tx=cx-w/2+i*(w/(towers-1));float tw=43*g,th=h+43*g+(i%2)*10*g;p.setColor(stone);c.drawRect(tx-tw/2,ground-th,tx+tw/2,ground,p);battlement(c,tx-tw/2,ground-th,tw,stone);}battlement(c,cx-w/2,ground-h,w,stone);p.setColor(Color.rgb(62,48,43));c.drawRect(cx-18*g,ground-50*g,cx+18*g,ground,p);for(int x=-75;x<=75;x+=37){p.setColor(Color.rgb(254,220,112));c.drawRect(cx+x*g-5,ground-h+31*g,cx+x*g+5,ground-h+44*g,p);}if(stage>=10){p.setColor(PURPLE);c.drawRect(cx-3,ground-h-83*g,cx+3,ground-h,p);c.drawRect(cx+3,ground-h-82*g,cx+40,ground-h-69*g,p);}if(stage==11){p.setColor(GOLD);c.drawRect(cx-w*.42f,ground-h-5,cx+w*.42f,ground-h,p);}}
    private void battlement(Canvas c,float x,float y,float w,int color){p.setColor(color);for(float xx=x;xx<x+w;xx+=18)c.drawRect(xx,y-12,Math.min(xx+11,x+w),y+3,p);}

    private void drawIndustrial(Canvas c,float cx,float ground,float g,int stage){float w=(215+(stage-12)*24)*g,h=(120+(stage-12)*22)*g;p.setColor(Color.rgb(70,78,89));c.drawRect(cx-w/2,ground-h,cx+w/2,ground,p);for(int i=0;i<3+(stage-12);i++){float x=cx-w/2+26+i*44;p.setColor(Color.rgb(250,190,60));c.drawRect(x,ground-h+26,x+18,ground-h+45,p);}p.setColor(Color.rgb(47,55,65));c.drawRect(cx-w/2+22,ground-h-95*g,cx-w/2+48,ground-h,p);c.drawRect(cx+w/2-57,ground-h-70*g,cx+w/2-34,ground-h,p);float smoke=(System.currentTimeMillis()%4000L)/4000f;for(int i=0;i<3;i++){p.setColor(Color.argb(125-i*25,190,198,205));float yy=ground-h-112*g-i*20-smoke*18;c.drawRect(cx-w/2+16-i*7,yy,cx-w/2+54+i*7,yy+14,p);}if(stage>=13){p.setColor(CYAN);for(int y=0;y<4;y++)c.drawRect(cx-w/2+12,ground-h+18+y*24,cx+w/2-12,ground-h+21+y*24,p);}if(stage>=15){p.setColor(PURPLE);c.drawRect(cx-w/2-7,ground-h-9,cx+w/2+7,ground-h-3,p);text(c,"PIXEL",cx,ground-h-17,14,CYAN,Paint.Align.CENTER);}}

    private void drawFuture(Canvas c,float cx,float ground,float g,int stage){float w=(190+(stage-16)*12)*g,h=(155+(stage-16)*35)*g;p.setColor(Color.rgb(25,37,58));c.drawRect(cx-w/2,ground-h*.72f,cx+w/2,ground,p);p.setColor(Color.rgb(55,75,103));c.drawRect(cx-w*.34f,ground-h,cx+w*.34f,ground,p);for(int i=0;i<7;i++){float y=ground-h+22+i*(h-38)/7;p.setColor(i%2==0?CYAN:PURPLE);c.drawRect(cx-w*.26f,y,cx+w*.26f,y+5,p);}p.setColor(Color.rgb(9,18,32));c.drawRect(cx-17,ground-65,cx+17,ground,p);if(stage>=17){p.setColor(Color.argb(105,34,211,238));c.drawRect(cx-w*.48f,ground-h*.69f,cx+w*.48f,ground-h*.64f,p);}if(stage>=18){floatingPlatform(c,cx-150,ground-h*.55f,.75f);floatingPlatform(c,cx+150,ground-h*.42f,.65f);}if(stage>=19){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(7);p.setColor(Color.argb(190,150,220,255));c.drawOval(new RectF(cx-150,ground-h-78,cx+150,ground-h+5),p);p.setStyle(Paint.Style.FILL);}if(stage>=20){p.setColor(Color.rgb(181,190,205));c.drawRect(cx-120,ground-38,cx-80,ground,p);c.drawRect(cx+80,ground-50,cx+125,ground,p);}if(stage>=21){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);p.setColor(PURPLE);c.drawOval(new RectF(cx-210,ground-h*.73f,cx+210,ground-h*.34f),p);p.setStyle(Paint.Style.FILL);}if(stage>=22){for(int i=0;i<5;i++){p.setColor(Color.argb(70+i*25,100,220,255));float o=i*12;c.drawRect(cx-w*.34f-o,ground-h-o,cx+w*.34f+o,ground-h+4-o,p);}}if(stage>=23){p.setColor(Color.rgb(245,245,255));c.drawRect(cx-7,ground-h-120,cx+7,ground-h+5,p);p.setColor(CYAN);c.drawRect(cx-2,ground-h-182,cx+2,ground-h-120,p);for(int i=0;i<4;i++){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(i%2==0?CYAN:PURPLE);c.drawCircle(cx,ground-h-40,66+i*22,p);}p.setStyle(Paint.Style.FILL);}}
    private void floatingPlatform(Canvas c,float x,float y,float g){p.setColor(Color.rgb(30,43,64));c.drawRect(x-50*g,y-10*g,x+50*g,y+10*g,p);p.setColor(CYAN);c.drawRect(x-37*g,y+11*g,x+37*g,y+15*g,p);p.setColor(Color.argb(80,80,220,255));c.drawRect(x-25*g,y+15*g,x+25*g,y+35*g,p);}

    private void drawWorldHud(Canvas c,float top,float bottom){float y=top+12;panel(c,12,y,354,y+75,Color.argb(215,10,18,31),Color.argb(160,100,140,190),10);text(c,L10n.stageName(state.language,state.stage),28,y+27,16,TEXT,Paint.Align.LEFT);text(c,L10n.t(state.language,"stage")+" "+(state.stage+1)+"/24  •  "+L10n.t(state.language,"level")+" "+state.level+"/288",28,y+49,9,MUTED,Paint.Align.LEFT);progress(c,28,y+60,330,y+68,(float)state.levelProgress(),state.stage>=18?CYAN:GOLD);if(state.canClaimDaily()){button(c,369,y,528,y+75,L10n.t(state.language,"daily"),GOLD,BG,true);hits.add(new Hit(new RectF(369,y,528,y+75),"daily",""));}float hy=bottom-90;panel(c,72,hy,468,hy+47,Color.argb(188,8,15,27),Color.argb(155,255,255,255),12);text(c,L10n.t(state.language,"tap_anywhere"),270,hy+20,9,TEXT,Paint.Align.CENTER);text(c,"+"+state.format(state.getTapPower())+"  •  "+L10n.t(state.language,"combo")+" x"+Math.max(1,combo),270,hy+38,10,GOLD,Paint.Align.CENTER);}

    private void drawUpgrades(Canvas c,float top,float bottom){header(c,L10n.t(state.language,"upgrades"),"18",top);List<GameState.Upgrade> list=new ArrayList<>(state.upgrades.values());int per=6,pages=(list.size()+per-1)/per;upgradePage=clamp(upgradePage,0,pages-1);int start=upgradePage*per,end=Math.min(list.size(),start+per);float y=top+58,row=Math.min(88,(bottom-y-52-(end-start-1)*7)/Math.max(1,end-start));for(int i=start;i<end;i++){GameState.Upgrade u=list.get(i);float y2=y+row;boolean unlocked=state.stage>=u.def.unlockStage,can=state.canBuyUpgrade(u.def.id);panel(c,12,y,528,y2,PANEL,unlocked?(u.def.tap?GOLD:GREEN):STROKE,10);p.setColor(u.def.tap?GOLD:GREEN);c.drawRect(13,y+1,18,y2-1,p);text(c,L10n.upgradeName(state.language,u.def.id),31,y+25,13,unlocked?TEXT:MUTED,Paint.Align.LEFT);String d=unlocked?L10n.upgradeDesc(state.language,u.def.tap,u.def.value)+" • "+L10n.t(state.language,"lvl")+" "+u.level:L10n.t(state.language,"locked")+" • "+L10n.t(state.language,"stage")+" "+(u.def.unlockStage+1);text(c,d,31,y+49,9,MUTED,Paint.Align.LEFT);if(unlocked){text(c,state.format(u.cost()),500,y+28,12,can?GOLD:MUTED,Paint.Align.RIGHT);text(c,L10n.t(state.language,"buy"),500,y+51,9,can?GREEN:MUTED,Paint.Align.RIGHT);hits.add(new Hit(new RectF(12,y,528,y2),"upgrade",u.def.id));}y=y2+7;}pager(c,bottom,pages,upgradePage,"up_prev","up_next");}

    private void drawGoals(Canvas c,float top,float bottom){header(c,goalMode==0?L10n.t(state.language,"missions"):L10n.t(state.language,"achievement"),goalMode==0?state.missionsClaimed+"/"+state.missions.size():state.unlockedAchievementCount()+"/"+state.achievements.size(),top);float ty=top+47;button(c,12,ty,260,ty+38,L10n.t(state.language,"missions"),goalMode==0?GOLD:PANEL2,goalMode==0?BG:TEXT,true);button(c,280,ty,528,ty+38,L10n.t(state.language,"achievement"),goalMode==1?PURPLE:PANEL2,goalMode==1?BG:TEXT,true);hits.add(new Hit(new RectF(12,ty,260,ty+38),"goal_mode","0"));hits.add(new Hit(new RectF(280,ty,528,ty+38),"goal_mode","1"));if(goalMode==0)drawMissions(c,ty+50,bottom);else drawAchievements(c,ty+50,bottom);}

    private void drawMissions(Canvas c,float y,float bottom){int per=5,pages=(state.missions.size()+per-1)/per;goalPage=clamp(goalPage,0,pages-1);int start=goalPage*per,end=Math.min(state.missions.size(),start+per);float row=Math.min(105,(bottom-y-52-(end-start-1)*8)/Math.max(1,end-start));for(int i=start;i<end;i++){GameState.Mission m=state.missions.get(i);float y2=y+row;boolean ready=state.missionReady(m);panel(c,12,y,528,y2,PANEL,m.claimed?GREEN:ready?GOLD:STROKE,10);text(c,L10n.t(state.language,"mission")+" #"+(i+1),29,y+24,10,m.claimed?GREEN:GOLD,Paint.Align.LEFT);text(c,state.goalText(m.type,m.target),29,y+49,12,m.claimed?MUTED:TEXT,Paint.Align.LEFT);progress(c,29,y+62,390,y+70,(float)Math.min(1,state.goalValue(m.type)/Math.max(1,m.target)),m.claimed?GREEN:GOLD);text(c,"+"+m.researchReward+" ◇  +"+m.crystalReward+" ◆",498,y+28,10,CYAN,Paint.Align.RIGHT);if(m.claimed)text(c,L10n.t(state.language,"completed"),498,y+58,9,GREEN,Paint.Align.RIGHT);else if(ready){text(c,L10n.t(state.language,"claim"),498,y+58,10,GOLD,Paint.Align.RIGHT);hits.add(new Hit(new RectF(12,y,528,y2),"mission",Integer.toString(i)));}y=y2+8;}pager(c,bottom,pages,goalPage,"goal_prev","goal_next");}

    private void drawAchievements(Canvas c,float y,float bottom){int per=6,pages=(state.achievements.size()+per-1)/per;goalPage=clamp(goalPage,0,pages-1);int start=goalPage*per,end=Math.min(state.achievements.size(),start+per);float row=Math.min(83,(bottom-y-52-(end-start-1)*7)/Math.max(1,end-start));for(int i=start;i<end;i++){GameState.Achievement a=state.achievements.get(i);float y2=y+row;panel(c,12,y,528,y2,PANEL,a.unlocked?GREEN:STROKE,10);diamond(c,36,y+row/2,9,a.unlocked?GREEN:Color.rgb(70,83,104));text(c,L10n.t(state.language,"achievement")+" #"+(i+1),58,y+25,10,a.unlocked?GREEN:MUTED,Paint.Align.LEFT);text(c,state.goalText(a.type,a.target),58,y+49,11,a.unlocked?TEXT:MUTED,Paint.Align.LEFT);text(c,"+"+a.reward+" ◆",500,y+40,10,a.unlocked?PURPLE:MUTED,Paint.Align.RIGHT);y=y2+7;}pager(c,bottom,pages,goalPage,"goal_prev","goal_next");}

    private void drawResearch(Canvas c,float top,float bottom){header(c,L10n.t(state.language,"research"),L10n.t(state.language,"research_points")+": "+state.researchPoints,top);List<GameContent.ResearchDef> list=GameContent.research();int per=6,pages=(list.size()+per-1)/per;researchPage=clamp(researchPage,0,pages-1);int start=researchPage*per,end=Math.min(list.size(),start+per);float y=top+58,row=Math.min(88,(bottom-y-52-(end-start-1)*7)/Math.max(1,end-start));for(int i=start;i<end;i++){GameContent.ResearchDef r=list.get(i);float y2=y+row;boolean owned=state.research.contains(r.id),unlocked=state.stage>=r.unlockStage,can=state.canResearch(r.id);panel(c,12,y,528,y2,PANEL,owned?CYAN:unlocked?PURPLE:STROKE,10);diamond(c,36,y+row/2,10,owned?CYAN:unlocked?PURPLE:Color.rgb(62,73,92));text(c,L10n.researchName(state.language,r.id),60,y+27,12,unlocked||owned?TEXT:MUTED,Paint.Align.LEFT);text(c,L10n.researchDesc(state.language,r.kind,r.bonus),60,y+51,9,MUTED,Paint.Align.LEFT);if(owned)text(c,L10n.t(state.language,"owned"),500,y+39,9,CYAN,Paint.Align.RIGHT);else if(unlocked){text(c,r.cost+" ◇",500,y+28,11,can?PURPLE:MUTED,Paint.Align.RIGHT);text(c,L10n.t(state.language,"unlock"),500,y+51,9,can?GREEN:MUTED,Paint.Align.RIGHT);hits.add(new Hit(new RectF(12,y,528,y2),"research",r.id));}else text(c,L10n.t(state.language,"stage")+" "+(r.unlockStage+1),500,y+39,9,MUTED,Paint.Align.RIGHT);y=y2+7;}pager(c,bottom,pages,researchPage,"res_prev","res_next");}

    private void drawMenu(Canvas c,float top,float bottom){header(c,L10n.t(state.language,"menu"),"Pixel Empire 2.0",top);float y=top+54;int gain=state.availableLegacyStars();panel(c,12,y,528,y+116,PANEL,gain>0?GOLD:STROKE,12);text(c,L10n.t(state.language,"prestige"),29,y+28,15,TEXT,Paint.Align.LEFT);text(c,L10n.t(state.language,"prestige_desc"),29,y+52,9,MUTED,Paint.Align.LEFT);text(c,gain>0?"+"+gain+" ★":L10n.t(state.language,"prestige_need"),500,y+31,10,gain>0?GOLD:MUTED,Paint.Align.RIGHT);button(c,29,y+69,511,y+104,System.currentTimeMillis()<ascendArmedUntil?L10n.t(state.language,"prestige_ready")+"?":L10n.t(state.language,"prestige"),gain>0?GOLD:PANEL2,gain>0?BG:MUTED,gain>0);if(gain>0)hits.add(new Hit(new RectF(29,y+69,511,y+104),"ascend",""));y+=130;setting(c,y,L10n.t(state.language,"sound"),state.soundEnabled,"sound");y+=58;setting(c,y,L10n.t(state.language,"haptics"),state.hapticsEnabled,"haptic");y+=58;setting(c,y,L10n.t(state.language,"notation"),state.compactNumbers,"notation");y+=58;setting(c,y,L10n.t(state.language,"low_power"),state.lowPower,"power");y+=58;panel(c,12,y,528,y+54,PANEL,STROKE,9);text(c,L10n.t(state.language,"language"),29,y+33,11,TEXT,Paint.Align.LEFT);text(c,L10n.languageLabel(state.language)+"  ›",500,y+33,11,CYAN,Paint.Align.RIGHT);hits.add(new Hit(new RectF(12,y,528,y+54),"setting","lang"));y+=66;float statBottom=Math.min(bottom-68,y+118);panel(c,12,y,528,statBottom,PANEL,STROKE,10);stat(c,y+25,L10n.t(state.language,"total_earned"),state.format(state.lifetimeCoins));stat(c,y+48,L10n.t(state.language,"total_taps"),Long.toString(state.totalTaps));stat(c,y+71,L10n.t(state.language,"best_combo"),"x"+state.bestCombo);stat(c,y+94,L10n.t(state.language,"play_time"),GameState.formatDuration(state.playSeconds));float ry=bottom-55;button(c,12,ry,528,bottom-6,System.currentTimeMillis()<resetArmedUntil?L10n.t(state.language,"reset_confirm"):L10n.t(state.language,"reset"),Color.rgb(69,28,35),RED,true);hits.add(new Hit(new RectF(12,ry,528,bottom-6),"reset",""));}

    private void setting(Canvas c,float y,String label,boolean on,String id){panel(c,12,y,528,y+52,PANEL,STROKE,9);text(c,label,29,y+32,11,TEXT,Paint.Align.LEFT);p.setColor(on?GREEN:Color.rgb(60,72,92));c.drawRoundRect(new RectF(448,y+14,508,y+38),12,12,p);p.setColor(Color.WHITE);c.drawCircle(on?496:460,y+26,9,p);hits.add(new Hit(new RectF(12,y,528,y+52),"setting",id));}
    private void stat(Canvas c,float y,String label,String value){text(c,label,29,y,9,MUTED,Paint.Align.LEFT);text(c,value,500,y,10,TEXT,Paint.Align.RIGHT);}
    private void header(Canvas c,String title,String sub,float top){text(c,title,18,top+27,19,TEXT,Paint.Align.LEFT);text(c,sub,522,top+27,9,MUTED,Paint.Align.RIGHT);p.setColor(Color.rgb(37,50,70));c.drawRect(18,top+40,522,top+42,p);}
    private void pager(Canvas c,float bottom,int pages,int page,String prev,String next){if(pages<=1)return;float y=bottom-42;button(c,12,y,145,bottom-4,"‹",PANEL2,TEXT,page>0);button(c,395,y,528,bottom-4,"›",PANEL2,TEXT,page<pages-1);text(c,(page+1)+" / "+pages,270,y+25,10,MUTED,Paint.Align.CENTER);if(page>0)hits.add(new Hit(new RectF(12,y,145,bottom-4),prev,""));if(page<pages-1)hits.add(new Hit(new RectF(395,y,528,bottom-4),next,""));}

    private void drawEvent(Canvas c){if(state.activeEventType<0||offlinePopup||!state.tutorialSeen)return;float y=86;int col=state.activeEventType==0?GOLD:state.activeEventType==1?PURPLE:state.activeEventType==2?RED:state.activeEventType==3?GREEN:CYAN;String key=state.activeEventType==0?"event_gold":state.activeEventType==1?"event_crystal":state.activeEventType==2?"event_frenzy":state.activeEventType==3?"event_auto":"event_xp";panel(c,44,y,496,y+42,Color.argb(235,12,20,34),col,12);text(c,L10n.t(state.language,key),62,y+27,10,col,Paint.Align.LEFT);long left=Math.max(0,(state.activeEventUntil-System.currentTimeMillis()+999)/1000);text(c,left+"s",480,y+27,9,TEXT,Paint.Align.RIGHT);hits.add(new Hit(new RectF(44,y,496,y+42),"event",""));}

    private void drawBottomNav(Canvas c){float y=logicalH-70;p.setColor(Color.rgb(7,13,24));c.drawRect(0,y,W,logicalH,p);p.setColor(Color.rgb(38,51,72));c.drawRect(0,y,W,y+2,p);String[] labels={L10n.t(state.language,"world"),L10n.t(state.language,"upgrades"),L10n.t(state.language,"missions"),L10n.t(state.language,"research"),L10n.t(state.language,"menu")};float ww=W/5;for(int i=0;i<5;i++){float x=i*ww;if(tab==i){p.setColor(Color.rgb(20,31,51));c.drawRect(x+4,y+4,x+ww-4,logicalH-4,p);p.setColor(GOLD);c.drawRect(x+23,y+4,x+ww-23,y+7,p);}navIcon(c,i,x+ww/2,y+24,tab==i?GOLD:MUTED);text(c,labels[i],x+ww/2,y+55,7.5f,tab==i?TEXT:MUTED,Paint.Align.CENTER);hits.add(new Hit(new RectF(x,y,x+ww,logicalH),"tab",Integer.toString(i)));}}
    private void navIcon(Canvas c,int i,float x,float y,int col){p.setColor(col);if(i==0){c.drawRect(x-13,y-2,x+13,y+13,p);c.drawRect(x-8,y-12,x+8,y+15,p);}else if(i==1){c.drawRect(x-13,y-10,x+13,y-4,p);c.drawRect(x-10,y,x+10,y+13,p);}else if(i==2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawRect(x-12,y-11,x+12,y+14,p);c.drawLine(x-7,y-3,x+7,y-3,p);c.drawLine(x-7,y+4,x+7,y+4,p);p.setStyle(Paint.Style.FILL);}else if(i==3)diamond(c,x,y,12,col);else{c.drawCircle(x,y+1,11,p);p.setColor(BG);c.drawCircle(x,y+1,4,p);}}

    private void drawEffects(Canvas c){for(Particle q:particles){int a=(int)(255*Math.max(0,Math.min(1,q.life)));p.setColor((q.color&0x00ffffff)|(a<<24));c.drawRect(q.x-q.size/2,q.y-q.size/2,q.x+q.size/2,q.y+q.size/2,p);}for(FloatText f:floatTexts){int a=(int)(255*Math.max(0,Math.min(1,f.life)));text(c,f.text,f.x,f.y,14,(f.color&0xffffff)|(a<<24),Paint.Align.CENTER);}}
    private void drawToast(Canvas c){if(toast.isEmpty()||System.currentTimeMillis()>=toastUntil)return;float y=logicalH-128;panel(c,48,y,492,y+44,Color.argb(240,13,22,38),GOLD,11);text(c,toast,270,y+28,10,TEXT,Paint.Align.CENTER);}

    private void drawOfflinePopup(Canvas c){p.setColor(Color.argb(222,0,0,0));c.drawRect(0,0,W,logicalH,p);float h=300,y=(logicalH-h)/2;panel(c,40,y,500,y+h,Color.rgb(13,22,38),GOLD,18);coin(c,270,y+55,25);text(c,L10n.t(state.language,"welcome_back"),270,y+103,20,TEXT,Paint.Align.CENTER);text(c,L10n.t(state.language,"away")+": "+GameState.formatDuration(state.startupOfflineSeconds),270,y+136,10,MUTED,Paint.Align.CENTER);text(c,L10n.t(state.language,"offline"),270,y+164,10,MUTED,Paint.Align.CENTER);text(c,"+"+state.format(state.startupOfflineGain),270,y+202,27,GOLD,Paint.Align.CENTER);if(state.startupOfflineLevels>0)text(c,"+"+state.startupOfflineLevels+" "+L10n.t(state.language,"level"),270,y+225,10,CYAN,Paint.Align.CENTER);button(c,85,y+246,455,y+284,L10n.t(state.language,"continue"),GOLD,BG,true);hits.add(new Hit(new RectF(40,y,500,y+h),"offline",""));}

    private void drawTutorial(Canvas c){p.setColor(Color.argb(234,2,7,16));c.drawRect(0,0,W,logicalH,p);float y=logicalH/2-180;panel(c,32,y,508,y+360,Color.rgb(12,21,37),GOLD,18);String body=tutorialStep==0?L10n.t(state.language,"tutorial_1"):tutorialStep==1?L10n.t(state.language,"tutorial_2"):tutorialStep==2?L10n.t(state.language,"tutorial_3"):L10n.t(state.language,"tutorial_4");if(tutorialStep==0)drawHut(c,270,y+116,.48f,1);else if(tutorialStep==1){coin(c,245,y+78,23);diamond(c,295,y+78,15,PURPLE);}else if(tutorialStep==2)drawCastle(c,270,y+127,.37f,9);else drawFuture(c,270,y+145,.29f,23);text(c,L10n.t(state.language,"tutorial_title"),270,y+153,23,TEXT,Paint.Align.CENTER);wrap(c,body,270,y+198,420,12,MUTED);text(c,(tutorialStep+1)+" / 4",270,y+281,10,MUTED,Paint.Align.CENTER);text(c,L10n.t(state.language,"tap_continue"),270,y+327,11,GOLD,Paint.Align.CENTER);}
    private void wrap(Canvas c,String s,float x,float y,float max,float size,int color){String[] words=s.split(" ");String line="";int n=0;textPaint.setTextSize(size);for(String w:words){String t=line.isEmpty()?w:line+" "+w;if(textPaint.measureText(t)>max&&!line.isEmpty()){text(c,line,x,y+n*20,size,color,Paint.Align.CENTER);n++;line=w;}else line=t;}if(!line.isEmpty())text(c,line,x,y+n*20,size,color,Paint.Align.CENTER);}

    @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX()/scale,y=e.getY()/scale;if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;moved=false;return true;}if(e.getAction()==MotionEvent.ACTION_MOVE){if(Math.abs(x-downX)>13||Math.abs(y-downY)>13)moved=true;return true;}if(e.getAction()==MotionEvent.ACTION_UP){performClick();if(moved)return true;if(!state.tutorialSeen){tutorialStep++;if(tutorialStep>=4){state.tutorialSeen=true;state.save(getContext());showToast("Pixel Empire",1300);}invalidate();return true;}if(offlinePopup){offlinePopup=false;feedback(false);invalidate();return true;}for(int i=hits.size()-1;i>=0;i--){Hit h=hits.get(i);if(h.rect.contains(x,y)){handleHit(h,x,y);break;}}return true;}return true;}
    @Override public boolean performClick(){super.performClick();return true;}

    private void handleHit(Hit h,float x,float y){switch(h.type){case "tab":tab=Integer.parseInt(h.id);feedback(false);break;case "tap":tap(x,y);break;case "daily":if(state.canClaimDaily()){int r=state.claimDaily();showToast(L10n.t(state.language,"daily")+" +"+r+" ◆",1800);burst(x,y,GOLD,22);feedback(true);}break;case "event":int type=state.activeEventType;state.collectEvent(type);showToast(type==1?"+◆":type==4?"XP x4":"BOOST!",1600);burst(x,y,type==1?PURPLE:type==4?CYAN:GOLD,32);feedback(true);break;case "upgrade":if(state.buyUpgrade(h.id)){showToast(L10n.upgradeName(state.language,h.id)+" +1",850);feedback(false);}else errorFeedback();break;case "up_prev":upgradePage--;break;case "up_next":upgradePage++;break;case "goal_mode":goalMode=Integer.parseInt(h.id);goalPage=0;break;case "mission":if(state.claimMission(Integer.parseInt(h.id))){showToast(L10n.t(state.language,"reward")+"!",1000);burst(x,y,CYAN,16);feedback(true);}break;case "goal_prev":goalPage--;break;case "goal_next":goalPage++;break;case "research":if(state.buyResearch(h.id)){showToast(L10n.researchName(state.language,h.id),1100);burst(x,y,PURPLE,18);feedback(true);}else errorFeedback();break;case "res_prev":researchPage--;break;case "res_next":researchPage++;break;case "setting":setting(h.id);break;case "ascend":ascend();break;case "reset":reset();break;case "offline":offlinePopup=false;break;}state.save(getContext());invalidate();}

    private void tap(float x,float y){long now=System.currentTimeMillis();combo=now-lastTapAt<=780?Math.min(50,combo+1):1;lastTapAt=now;boolean crit=random.nextDouble()<.06;double amount=state.tap(combo,crit);floatTexts.add(new FloatText(x,y-14,(crit?L10n.t(state.language,"critical")+" ":"+")+state.format(amount),crit?PURPLE:GOLD));while(floatTexts.size()>18)floatTexts.remove(0);burst(x,y,crit?PURPLE:GOLD,crit?18:8);feedback(crit);}
    private void burst(float x,float y,int color,int count){for(int i=0;i<count;i++){double a=random.nextDouble()*Math.PI*2;float speed=30+random.nextFloat()*105;particles.add(new Particle(x,y,(float)Math.cos(a)*speed,(float)Math.sin(a)*speed-35,.45f+random.nextFloat()*.55f,2+random.nextFloat()*5,color));}while(particles.size()>100)particles.remove(0);}
    private void setting(String id){if("sound".equals(id))state.soundEnabled=!state.soundEnabled;else if("haptic".equals(id))state.hapticsEnabled=!state.hapticsEnabled;else if("notation".equals(id))state.compactNumbers=!state.compactNumbers;else if("power".equals(id))state.lowPower=!state.lowPower;else if("lang".equals(id))state.language=L10n.nextLang(state.language);feedback(false);}
    private void ascend(){int gain=state.availableLegacyStars();if(gain<=0)return;long now=System.currentTimeMillis();if(now>ascendArmedUntil){ascendArmedUntil=now+5000;showToast(L10n.t(state.language,"prestige")+"? +"+gain+" ★",1800);return;}if(state.ascend()){ascendArmedUntil=0;tab=0;lastSeenLevel=state.level;lastSeenStage=state.stage;showToast("+"+gain+" ★",2200);burst(270,380,GOLD,45);feedback(true);}}
    private void reset(){long now=System.currentTimeMillis();if(now>resetArmedUntil){resetArmedUntil=now+5000;showToast(L10n.t(state.language,"reset_confirm"),1800);errorFeedback();return;}state.hardReset(getContext());postDelayed(()->((Activity)getContext()).recreate(),350);}

    private void feedback(boolean strong){if(state.soundEnabled&&tone!=null)try{tone.startTone(strong?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,strong?70:30);}catch(Throwable ignored){}if(state.hapticsEnabled&&vibrator!=null&&vibrator.hasVibrator())try{if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createOneShot(strong?38:14,strong?130:60));else vibrator.vibrate(strong?38:14);}catch(Throwable ignored){}}
    private void errorFeedback(){if(state.soundEnabled&&tone!=null)try{tone.startTone(ToneGenerator.TONE_PROP_NACK,55);}catch(Throwable ignored){}if(state.hapticsEnabled&&vibrator!=null&&vibrator.hasVibrator())try{if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createOneShot(28,80));else vibrator.vibrate(28);}catch(Throwable ignored){}}

    private int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private void panel(Canvas c,float x1,float y1,float x2,float y2,int fill,int stroke,float radius){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawRoundRect(new RectF(x1,y1,x2,y2),radius,radius,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(stroke);c.drawRoundRect(new RectF(x1,y1,x2,y2),radius,radius,p);p.setStyle(Paint.Style.FILL);}
    private void button(Canvas c,float x1,float y1,float x2,float y2,String label,int fill,int color,boolean enabled){p.setColor(enabled?fill:Color.rgb(37,46,62));c.drawRoundRect(new RectF(x1,y1,x2,y2),9,9,p);text(c,label,(x1+x2)/2,(y1+y2)/2+4,9.5f,enabled?color:MUTED,Paint.Align.CENTER);}
    private void progress(Canvas c,float x1,float y1,float x2,float y2,float value,int color){p.setColor(Color.rgb(39,50,68));c.drawRoundRect(new RectF(x1,y1,x2,y2),5,5,p);p.setColor(color);float v=Math.max(0,Math.min(1,value));c.drawRoundRect(new RectF(x1,y1,x1+(x2-x1)*v,y2),5,5,p);}
    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align){textPaint.setShader(null);textPaint.setStyle(Paint.Style.FILL);textPaint.setColor(color);textPaint.setTextSize(size);textPaint.setTextAlign(align);textPaint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText(s,x,y,textPaint);}
    private void pixel(Canvas c,float x,float y,float size,int color){p.setColor(color);c.drawRect(x-size/2,y-size/2,x+size/2,y+size/2,p);}
    private void coin(Canvas c,float x,float y,float r){p.setColor(Color.rgb(180,110,8));c.drawRect(x-r,y-r/2,x+r,y+r/2,p);c.drawRect(x-r/2,y-r,x+r/2,y+r,p);p.setColor(GOLD);c.drawRect(x-r+3,y-r/2+2,x+r-3,y+r/2-2,p);c.drawRect(x-r/2+2,y-r+3,x+r/2-2,y+r-3,p);p.setColor(Color.rgb(255,245,190));c.drawRect(x-r/3,y-r/2,x-1,y-2,p);}
    private void diamond(Canvas c,float x,float y,float r,int color){p.setColor(color);Path d=new Path();d.moveTo(x,y-r);d.lineTo(x+r,y);d.lineTo(x,y+r);d.lineTo(x-r,y);d.close();c.drawPath(d,p);p.setColor(Color.argb(130,255,255,255));c.drawRect(x-r*.28f,y-r*.48f,x,y-r*.18f,p);}
}
