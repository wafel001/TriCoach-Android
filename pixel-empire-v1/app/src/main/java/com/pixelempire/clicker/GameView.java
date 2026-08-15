package com.pixelempire.clicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
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
    private static final float DESIGN_W = 540f;
    private static final long FRAME_MS = 50L;

    private final GameState state;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final List<HitTarget> hits = new ArrayList<>();
    private final List<FloatText> floatTexts = new ArrayList<>();
    private final Vibrator vibrator;
    private ToneGenerator tone;

    private float scale = 1f;
    private float logicalH = 960f;
    private int tab = 0;
    private int achievementPage = 0;
    private long lastFrameNanos = 0;
    private double secondAccumulator = 0;
    private long lastAutosave = 0;
    private long lastTapAt = 0;
    private int combo = 0;
    private float downX, downY;
    private boolean moved = false;
    private boolean running = false;

    private boolean offlinePopup;
    private int tutorialStep = 0;
    private String toastText = "";
    private long toastUntil = 0;
    private long resetArmedUntil = 0;
    private int prestigeConfirmStage = 0;
    private long prestigeConfirmUntil = 0;

    private static final int BG = Color.rgb(7, 11, 22);
    private static final int PANEL = Color.rgb(20, 29, 49);
    private static final int PANEL_2 = Color.rgb(29, 41, 66);
    private static final int STROKE = Color.rgb(57, 72, 99);
    private static final int GOLD = Color.rgb(251, 191, 36);
    private static final int GOLD_DARK = Color.rgb(217, 119, 6);
    private static final int TEXT = Color.rgb(241, 245, 249);
    private static final int MUTED = Color.rgb(148, 163, 184);
    private static final int GREEN = Color.rgb(52, 211, 153);
    private static final int RED = Color.rgb(248, 113, 113);
    private static final int PURPLE = Color.rgb(192, 132, 252);
    private static final int CYAN = Color.rgb(34, 211, 238);

    private static final class HitTarget {
        final RectF rect;
        final String type;
        final String id;
        HitTarget(RectF rect, String type, String id) {
            this.rect = rect;
            this.type = type;
            this.id = id;
        }
    }

    private static final class FloatText {
        float x, y, life;
        final String text;
        final int color;
        FloatText(float x, float y, String text, int color) {
            this.x = x; this.y = y; this.text = text; this.color = color; this.life = 1.0f;
        }
    }

    public GameView(Context context, GameState state) {
        super(context);
        this.state = state;
        setFocusable(true);
        p.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        try { tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 35); } catch (Throwable ignored) {}
        offlinePopup = state.startupOfflineGain > 0.01;
        if (state.nextEventAt <= 0) state.nextEventAt = System.currentTimeMillis() + 60000;
        setBackgroundColor(BG);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        lastFrameNanos = System.nanoTime();
        post(this);
    }

    @Override protected void onDetachedFromWindow() {
        running = false;
        removeCallbacks(this);
        if (tone != null) {
            try { tone.release(); } catch (Throwable ignored) {}
            tone = null;
        }
        super.onDetachedFromWindow();
    }

    @Override public void run() {
        if (!running) return;
        long nowNanos = System.nanoTime();
        double dt = Math.min(0.25, Math.max(0, (nowNanos - lastFrameNanos) / 1_000_000_000.0));
        lastFrameNanos = nowNanos;
        state.tick(dt);
        secondAccumulator += dt;
        if (secondAccumulator >= 1.0) {
            long whole = (long) secondAccumulator;
            state.playSeconds += whole;
            secondAccumulator -= whole;
            state.checkAchievements();
        }
        updateEvents();
        updateFloats((float) dt);
        consumeAchievementToast();
        long now = System.currentTimeMillis();
        if (lastAutosave == 0 || now - lastAutosave >= 5000) {
            state.save(getContext());
            lastAutosave = now;
        }
        invalidate();
        postDelayed(this, FRAME_MS);
    }

    private void updateEvents() {
        long now = System.currentTimeMillis();
        if (state.activeEventType >= 0 && now >= state.activeEventUntil) {
            state.activeEventType = -1;
            state.activeEventUntil = 0;
            state.nextEventAt = now + 45000 + random.nextInt(55000);
        } else if (state.activeEventType < 0 && now >= state.nextEventAt) {
            state.activeEventType = random.nextInt(3);
            state.activeEventUntil = now + 15000;
        }
    }

    private void updateFloats(float dt) {
        for (int i = floatTexts.size() - 1; i >= 0; i--) {
            FloatText f = floatTexts.get(i);
            f.y -= 45f * dt;
            f.life -= dt * 0.9f;
            if (f.life <= 0) floatTexts.remove(i);
        }
    }

    private void consumeAchievementToast() {
        if (!state.justUnlocked.isEmpty() && System.currentTimeMillis() > toastUntil) {
            GameState.Achievement a = state.justUnlocked.remove(0);
            showToast("OSIĄGNIĘCIE: " + a.title + "  +" + a.gemReward + " klej.", 2600);
        }
    }

    private void showToast(String text, long ms) {
        toastText = text;
        toastUntil = System.currentTimeMillis() + ms;
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        scale = getWidth() / DESIGN_W;
        if (scale <= 0) scale = 1;
        logicalH = getHeight() / scale;
        canvas.save();
        canvas.scale(scale, scale);
        hits.clear();
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(BG);
        canvas.drawRect(0, 0, DESIGN_W, logicalH, p);

        drawTopBar(canvas);
        float contentTop = 94f;
        float contentBottom = logicalH - 76f;
        if (tab == 0) drawCityTab(canvas, contentTop, contentBottom);
        else if (tab == 1) drawShopTab(canvas, contentTop, contentBottom);
        else if (tab == 2) drawAchievementsTab(canvas, contentTop, contentBottom);
        else if (tab == 3) drawPrestigeTab(canvas, contentTop, contentBottom);
        else drawSettingsTab(canvas, contentTop, contentBottom);

        drawEventBanner(canvas);
        drawBottomNav(canvas);
        drawFloatTexts(canvas);
        drawToast(canvas);
        if (offlinePopup) drawOfflinePopup(canvas);
        if (!state.tutorialSeen) drawTutorial(canvas);
        canvas.restore();
    }

    private void drawTopBar(Canvas c) {
        panel(c, 12, 10, 528, 86, PANEL, STROKE, 12);
        pixelCoin(c, 38, 48, 19, GOLD);
        text(c, state.format(state.coins), 67, 44, 25, TEXT, Paint.Align.LEFT);
        text(c, state.format(state.getCps()) + " / sek.", 68, 69, 12, MUTED, Paint.Align.LEFT);

        text(c, "KLEJNOTY", 395, 35, 9, MUTED, Paint.Align.CENTER);
        pixelDiamond(c, 357, 54, 11, PURPLE);
        text(c, Integer.toString(state.gems), 377, 61, 16, TEXT, Paint.Align.LEFT);

        text(c, "RDZENIE", 474, 35, 9, MUTED, Paint.Align.CENTER);
        pixelDiamond(c, 442, 54, 10, CYAN);
        text(c, Integer.toString(state.prestigeCores), 460, 61, 16, TEXT, Paint.Align.LEFT);
    }

    private void drawCityTab(Canvas c, float top, float bottom) {
        float cityBottom = Math.min(bottom - 170, top + 440);
        drawPixelCity(c, top + 4, cityBottom);

        float coreY = top + 226;
        RectF coreHit = new RectF(190, coreY - 78, 350, coreY + 82);
        hits.add(new HitTarget(coreHit, "tap", ""));
        drawCore(c, 270, coreY, 62);
        text(c, "KLIKNIJ RDZEŃ", 270, coreY + 92, 13, MUTED, Paint.Align.CENTER);
        text(c, "+" + state.format(state.getClickValue()) + " bazowo", 270, coreY + 112, 11, GOLD, Paint.Align.CENTER);
        if (combo >= 2 && System.currentTimeMillis() - lastTapAt < 900) {
            text(c, "COMBO x" + combo, 270, coreY - 92, 14, GREEN, Paint.Align.CENTER);
        }

        float statsY = cityBottom + 10;
        float cardH = 66;
        statCard(c, 12, statsY, 174, statsY + cardH, "KLIK", state.format(state.getClickValue()), GOLD);
        statCard(c, 183, statsY, 357, statsY + cardH, "AUTO", state.format(state.getCps()) + "/s", GREEN);
        statCard(c, 366, statsY, 528, statsY + cardH, "BONUS", String.format(Locale.US, "x%.1f", state.prestigeMultiplier()), CYAN);

        float dailyY = statsY + cardH + 10;
        boolean ready = state.canClaimDaily();
        int dailyColor = ready ? GOLD : PANEL_2;
        button(c, 12, dailyY, 528, Math.min(bottom - 5, dailyY + 66),
                ready ? "ODBIERZ CODZIENNĄ NAGRODĘ  •  seria " + Math.max(1, state.dailyStreak) + "/7" :
                        "CODZIENNA NAGRODA ODEBRANA  •  seria " + state.dailyStreak + "/7",
                dailyColor, ready ? BG : MUTED, ready);
        if (ready) hits.add(new HitTarget(new RectF(12, dailyY, 528, Math.min(bottom - 5, dailyY + 66)), "daily", ""));
    }

    private void drawPixelCity(Canvas c, float top, float bottom) {
        boolean night = LocalTime.now().getHour() < 6 || LocalTime.now().getHour() >= 20;
        int sky1 = night ? Color.rgb(15, 23, 54) : Color.rgb(56, 118, 196);
        int sky2 = night ? Color.rgb(30, 41, 89) : Color.rgb(125, 211, 252);
        p.setShader(new LinearGradient(0, top, 0, bottom, sky1, sky2, Shader.TileMode.CLAMP));
        c.drawRoundRect(new RectF(12, top, 528, bottom), 12, 12, p);
        p.setShader(null);
        strokeRound(c, 12, top, 528, bottom, STROKE, 12, 2);

        if (night) {
            for (int i = 0; i < 18; i++) {
                float x = 27 + ((i * 79) % 480);
                float y = top + 18 + ((i * 37) % Math.max(60, (int)(bottom - top - 130)));
                pixel(c, x, y, (i % 3 == 0 ? 4 : 2), Color.rgb(226, 232, 240));
            }
            pixel(c, 462, top + 42, 22, Color.rgb(254, 249, 195));
        } else {
            pixel(c, 465, top + 40, 28, Color.rgb(253, 224, 71));
            cloud(c, 78, top + 55, 1.0f);
            cloud(c, 410, top + 92, 0.8f);
        }

        float groundY = bottom - 78;
        p.setColor(Color.rgb(35, 71, 55)); c.drawRect(13, groundY, 527, bottom - 1, p);
        p.setColor(Color.rgb(55, 90, 60)); c.drawRect(13, groundY, 527, groundY + 12, p);
        p.setColor(Color.rgb(54, 45, 37)); c.drawRect(13, bottom - 24, 527, bottom - 1, p);

        int tier = state.cityTier();
        for (int i = 0; i < 6 + tier; i++) {
            float bw = 34 + (i % 3) * 8;
            float bh = 48 + ((i * 31 + tier * 17) % 75) + tier * 8;
            float bx = 26 + i * (470f / (5 + tier));
            float by = groundY - bh;
            building(c, bx, by, bw, bh, i, tier, true);
        }
        if (tier == 0) {
            hut(c, 43, groundY - 48);
            hut(c, 430, groundY - 42);
        } else {
            building(c, 44, groundY - 104 - tier * 8, 72, 104 + tier * 8, 7, tier, false);
            building(c, 414, groundY - 86 - tier * 10, 74, 86 + tier * 10, 9, tier, false);
        }
        if (tier >= 2) building(c, 131, groundY - 132 - tier * 9, 74, 132 + tier * 9, 12, tier, false);
        if (tier >= 3) building(c, 338, groundY - 150 - tier * 12, 62, 150 + tier * 12, 15, tier, false);
        if (tier >= 4) {
            p.setColor(Color.rgb(103, 232, 249));
            c.drawRect(258, groundY - 180, 282, groundY, p);
            p.setColor(Color.rgb(23, 37, 84));
            c.drawRect(263, groundY - 174, 277, groundY - 8, p);
        }
        if (tier >= 5) {
            p.setColor(PURPLE);
            c.drawRect(240, top + 48, 300, top + 56, p);
            c.drawRect(248, top + 40, 292, top + 64, p);
            p.setColor(Color.rgb(30, 27, 75));
            c.drawRect(258, top + 46, 282, top + 58, p);
        }
        text(c, "POZIOM MIASTA " + (tier + 1) + "/6", 26, top + 24, 10, Color.argb(210, 255,255,255), Paint.Align.LEFT);
    }

    private void building(Canvas c, float x, float y, float w, float h, int seed, int tier, boolean back) {
        int body = back ? Color.rgb(42, 55, 78) : (tier >= 4 ? Color.rgb(46, 64, 83) : Color.rgb(67, 74, 91));
        p.setColor(body); c.drawRect(x, y, x+w, y+h, p);
        p.setColor(Color.rgb(30, 41, 59)); c.drawRect(x, y, x+w, y+6, p);
        int cols = Math.max(2, (int)(w / 17));
        int rows = Math.max(2, (int)(h / 21));
        for (int r=0;r<rows;r++) for(int col=0;col<cols;col++) {
            if (((r*cols+col+seed)%4)==0 && back) continue;
            float wx=x+7+col*15, wy=y+13+r*20;
            p.setColor(((r+col+seed)%3)==0 ? Color.rgb(253,224,71) : Color.rgb(125,211,252));
            c.drawRect(wx, wy, Math.min(x+w-5, wx+7), Math.min(y+h-5, wy+8), p);
        }
        if (!back && tier>=3) {
            p.setColor(CYAN); c.drawRect(x+w-4, y+8, x+w, y+h-8, p);
        }
    }

    private void hut(Canvas c, float x, float y) {
        p.setColor(Color.rgb(120, 80, 50)); c.drawRect(x, y+15, x+58, y+48, p);
        p.setColor(Color.rgb(87, 49, 33));
        c.drawRect(x-7, y+10, x+65, y+18, p); c.drawRect(x+4, y+3, x+54, y+11, p);
        p.setColor(Color.rgb(253,224,71)); c.drawRect(x+12,y+25,x+23,y+36,p);
        p.setColor(Color.rgb(60,40,30)); c.drawRect(x+37,y+25,x+50,y+48,p);
    }

    private void cloud(Canvas c, float x, float y, float s) {
        p.setColor(Color.argb(210,255,255,255));
        c.drawRect(x, y+8*s, x+58*s, y+20*s, p);
        c.drawRect(x+10*s, y, x+44*s, y+25*s, p);
    }

    private void drawCore(Canvas c, float x, float y, float r) {
        p.setColor(Color.argb(35, 251,191,36)); c.drawCircle(x,y,r+24,p);
        p.setColor(GOLD_DARK);
        c.drawRect(x-r, y-r+12, x+r, y+r-12, p);
        c.drawRect(x-r+12, y-r, x+r-12, y+r, p);
        p.setColor(GOLD);
        c.drawRect(x-r+9, y-r+18, x+r-9, y+r-18, p);
        c.drawRect(x-r+18, y-r+9, x+r-18, y+r-9, p);
        p.setColor(Color.rgb(255,247,214));
        c.drawRect(x-20,y-28,x+5,y-3,p);
        p.setColor(Color.rgb(245,158,11));
        c.drawRect(x+8,y+9,x+29,y+30,p);
        strokeRound(c, x-r-1,y-r-1,x+r+1,y+r+1, Color.rgb(120,53,15), 18, 3);
    }

    private void drawShopTab(Canvas c, float top, float bottom) {
        text(c, "ULEPSZENIA IMPERIUM", 18, top + 24, 18, TEXT, Paint.Align.LEFT);
        text(c, "Kupuj kolejne poziomy. Cena rośnie wraz z poziomem.", 18, top + 44, 10, MUTED, Paint.Align.LEFT);
        float y = top + 56;
        float avail = bottom - y - 4;
        float rowH = Math.min(72f, Math.max(55f, (avail - 8*6f) / 9f));
        for (GameState.Upgrade u : state.upgrades.values()) {
            float y2 = y + rowH;
            boolean can = state.coins + 1e-9 >= u.cost();
            panel(c, 12, y, 528, y2, PANEL, can ? Color.rgb(71,85,105) : Color.rgb(42,52,70), 8);
            int accent = u.click ? GOLD : GREEN;
            p.setColor(accent); c.drawRect(13, y+1, 18, y2-1, p);
            text(c, u.name, 29, y + 22, 13, TEXT, Paint.Align.LEFT);
            text(c, u.description + "  •  poz. " + u.level, 29, y + 42, 10, MUTED, Paint.Align.LEFT);
            text(c, state.format(u.cost()), 505, y + 28, 13, can ? GOLD : MUTED, Paint.Align.RIGHT);
            text(c, "KUP", 505, y + 48, 9, can ? GREEN : Color.rgb(100,116,139), Paint.Align.RIGHT);
            hits.add(new HitTarget(new RectF(12, y, 528, y2), "upgrade", u.id));
            y = y2 + 6;
        }
    }

    private void drawAchievementsTab(Canvas c, float top, float bottom) {
        int unlocked = state.unlockedAchievementCount();
        text(c, "OSIĄGNIĘCIA", 18, top + 24, 18, TEXT, Paint.Align.LEFT);
        text(c, unlocked + "/" + state.achievements.size() + " odblokowanych  •  nagrody wpadają automatycznie", 18, top + 44, 10, MUTED, Paint.Align.LEFT);
        int perPage = 8;
        int pages = (state.achievements.size() + perPage - 1) / perPage;
        achievementPage = Math.max(0, Math.min(pages - 1, achievementPage));
        float y = top + 58;
        float footer = 48;
        float rowH = Math.min(76f, (bottom - y - footer - 7*6f) / 8f);
        int start = achievementPage * perPage;
        int end = Math.min(state.achievements.size(), start + perPage);
        for (int i=start;i<end;i++) {
            GameState.Achievement a = state.achievements.get(i);
            float y2=y+rowH;
            int border = a.unlocked ? GREEN : Color.rgb(51,65,85);
            panel(c, 12,y,528,y2,PANEL,border,8);
            if (a.unlocked) pixelDiamond(c,34,y+rowH/2,10,GREEN);
            else {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2); p.setColor(MUTED);
                c.drawRect(25,y+rowH/2-8,41,y+rowH/2+8,p); p.setStyle(Paint.Style.FILL);
            }
            text(c,a.title,55,y+23,12,a.unlocked?TEXT:MUTED,Paint.Align.LEFT);
            text(c,a.description,55,y+42,9,MUTED,Paint.Align.LEFT);
            text(c,"+"+a.gemReward,497,y+30,11,a.unlocked?PURPLE:MUTED,Paint.Align.RIGHT);
            pixelDiamond(c,508,y+26,7,a.unlocked?PURPLE:Color.rgb(71,85,105));
            y=y2+6;
        }
        float fy = bottom - 40;
        if (pages > 1) {
            button(c, 12, fy, 160, bottom-2, "< POPRZ.", PANEL_2, TEXT, achievementPage > 0);
            button(c, 380, fy, 528, bottom-2, "DALEJ >", PANEL_2, TEXT, achievementPage < pages-1);
            text(c, (achievementPage+1)+" / "+pages,270,fy+25,12,MUTED,Paint.Align.CENTER);
            hits.add(new HitTarget(new RectF(12,fy,160,bottom-2),"ach_prev",""));
            hits.add(new HitTarget(new RectF(380,fy,528,bottom-2),"ach_next",""));
        }
    }

    private void drawPrestigeTab(Canvas c, float top, float bottom) {
        text(c, "RDZEŃ IMPERIUM", 18, top + 24, 18, TEXT, Paint.Align.LEFT);
        text(c, "Resetuj ekonomię, zachowaj postęp meta i zyskaj stały mnożnik.", 18, top + 44, 10, MUTED, Paint.Align.LEFT);

        float y=top+60;
        panel(c,12,y,528,y+150,PANEL,CYAN,12);
        pixelDiamond(c,60,y+52,24,CYAN);
        text(c,Integer.toString(state.prestigeCores),102,y+52,32,TEXT,Paint.Align.LEFT);
        text(c,"RDZENI IMPERIUM",103,y+77,10,MUTED,Paint.Align.LEFT);
        text(c,String.format(Locale.US,"stały bonus x%.2f",state.prestigeMultiplier()),103,y+101,13,CYAN,Paint.Align.LEFT);
        int gain=state.availablePrestigeCores();
        text(c,"Ten reset: +"+gain+" rdzeni",500,y+45,12,gain>0?GREEN:MUTED,Paint.Align.RIGHT);
        text(c,"Zarobek tej ery: "+state.format(state.runCoinsEarned),500,y+69,10,MUTED,Paint.Align.RIGHT);
        text(c,"Próg startowy: 1.00M",500,y+91,9,MUTED,Paint.Align.RIGHT);

        float py=y+162;
        boolean can=gain>0;
        String ptxt;
        if (prestigeConfirmStage==1 && System.currentTimeMillis()<prestigeConfirmUntil) ptxt="DOTKNIJ JESZCZE RAZ — RESET ERY";
        else ptxt=can?"PRESTIGE  •  +"+gain+" RDZENI":"PRESTIGE ODBLOKUJE SIĘ OD 1.00M / ERĘ";
        button(c,12,py,528,py+64,ptxt,can?CYAN:PANEL_2,can?BG:MUTED,can);
        hits.add(new HitTarget(new RectF(12,py,528,py+64),"prestige",""));

        float by=py+82;
        text(c,"BOOSTERY ZA KLEJNOTY",18,by,13,TEXT,Paint.Align.LEFT);
        by+=14;
        panel(c,12,by,258,by+126,PANEL,STROKE,10);
        text(c,"TURBO",28,by+28,15,GREEN,Paint.Align.LEFT);
        text(c,"x2 dochód automatyczny",28,by+50,9,MUTED,Paint.Align.LEFT);
        text(c,"5 minut",28,by+68,9,MUTED,Paint.Align.LEFT);
        long turboLeft=Math.max(0,(state.turboUntil-System.currentTimeMillis())/1000);
        if(turboLeft>0) text(c,"AKTYWNE: "+GameState.formatDuration(turboLeft),28,by+91,9,GREEN,Paint.Align.LEFT);
        button(c,28,by+93,242,by+118,"10 KLEJ.  •  AKTYWUJ",GREEN,BG,state.gems>=10);
        hits.add(new HitTarget(new RectF(28,by+93,242,by+118),"turbo",""));

        panel(c,270,by,528,by+126,PANEL,STROKE,10);
        text(c,"FRENZY",286,by+28,15,GOLD,Paint.Align.LEFT);
        text(c,"x3 siła kliknięcia",286,by+50,9,MUTED,Paint.Align.LEFT);
        text(c,"2 minuty",286,by+68,9,MUTED,Paint.Align.LEFT);
        long frenzyLeft=Math.max(0,(state.frenzyUntil-System.currentTimeMillis())/1000);
        if(frenzyLeft>0) text(c,"AKTYWNE: "+GameState.formatDuration(frenzyLeft),286,by+91,9,GOLD,Paint.Align.LEFT);
        button(c,286,by+93,512,by+118,"8 KLEJ.  •  AKTYWUJ",GOLD,BG,state.gems>=8);
        hits.add(new HitTarget(new RectF(286,by+93,512,by+118),"frenzy",""));

        float infoY=by+142;
        panel(c,12,infoY,528,Math.min(bottom-2,infoY+105),PANEL,STROKE,10);
        text(c,"CO ZOSTAJE PO PRESTIGE?",28,infoY+26,11,TEXT,Paint.Align.LEFT);
        text(c,"✓ Rdzenie i stały mnożnik   ✓ klejnoty   ✓ osiągnięcia",28,infoY+49,9,GREEN,Paint.Align.LEFT);
        text(c,"✓ statystyki całkowite   ✓ serie dzienne",28,infoY+69,9,GREEN,Paint.Align.LEFT);
        text(c,"Reset: monety, ulepszenia i aktywne boostery.",28,infoY+90,9,MUTED,Paint.Align.LEFT);
    }

    private void drawSettingsTab(Canvas c, float top, float bottom) {
        text(c,"OPCJE I STATYSTYKI",18,top+24,18,TEXT,Paint.Align.LEFT);
        text(c,"Pixel Empire Clicker  •  v1.0.0",18,top+44,10,MUTED,Paint.Align.LEFT);
        float y=top+62;
        settingsToggle(c,y,"DŹWIĘKI",state.soundEnabled,"sound"); y+=62;
        settingsToggle(c,y,"WIBRACJE",state.hapticsEnabled,"haptic"); y+=62;
        settingsToggle(c,y,"NOTACJA NAUKOWA",state.scientificNotation,"notation"); y+=76;

        text(c,"STATYSTYKI",18,y,13,TEXT,Paint.Align.LEFT); y+=14;
        panel(c,12,y,528,y+184,PANEL,STROKE,10);
        statLine(c,y+28,"Łącznie zarobione",state.format(state.lifetimeCoins));
        statLine(c,y+54,"Łącznie kliknięć",Long.toString(state.totalClicks));
        statLine(c,y+80,"Najlepsze combo","x"+state.bestCombo);
        statLine(c,y+106,"Najlepszy klik",state.format(state.bestTap));
        statLine(c,y+132,"Najwyższe / sek.",state.format(state.maxCps));
        statLine(c,y+158,"Czas w grze",GameState.formatDuration(state.playSeconds));
        statLine(c,y+180,"Prestige / eventy",state.prestigeCount+" / "+state.eventsCollected);
        y+=202;
        String resetText=System.currentTimeMillis()<resetArmedUntil?"NA PEWNO? DOTKNIJ PONOWNIE":"WYCZYŚĆ CAŁY ZAPIS";
        button(c,12,y,528,Math.min(bottom-4,y+58),resetText,Color.rgb(70,27,33),RED,true);
        hits.add(new HitTarget(new RectF(12,y,528,Math.min(bottom-4,y+58)),"reset",""));
    }

    private void settingsToggle(Canvas c,float y,String label,boolean on,String id) {
        panel(c,12,y,528,y+52,PANEL,STROKE,9);
        text(c,label,28,y+32,12,TEXT,Paint.Align.LEFT);
        p.setColor(on?GREEN:Color.rgb(71,85,105));
        c.drawRoundRect(new RectF(442,y+13,510,y+39),13,13,p);
        p.setColor(Color.WHITE);
        float cx=on?496:456; c.drawCircle(cx,y+26,10,p);
        hits.add(new HitTarget(new RectF(12,y,528,y+52),"setting",id));
    }

    private void statLine(Canvas c,float y,String label,String value) {
        text(c,label,28,y,9,MUTED,Paint.Align.LEFT);
        text(c,value,512,y,10,TEXT,Paint.Align.RIGHT);
    }

    private void drawEventBanner(Canvas c) {
        if (state.activeEventType < 0 || offlinePopup || !state.tutorialSeen) return;
        float y=96;
        int color=state.activeEventType==0?GOLD:state.activeEventType==1?PURPLE:GREEN;
        String label=state.activeEventType==0?"ZŁOTY DRON — ODBIERZ MONETY":state.activeEventType==1?"DRON KLEJNOTÓW — ZŁAP GO":"AWARIA SIECI — 60 s TURBO";
        long left=Math.max(0,(state.activeEventUntil-System.currentTimeMillis()+999)/1000);
        panel(c,40,y,500,y+42,Color.rgb(18,27,45),color,10);
        text(c,label,58,y+26,10,color,Paint.Align.LEFT);
        text(c,left+" s",484,y+26,10,TEXT,Paint.Align.RIGHT);
        hits.add(new HitTarget(new RectF(40,y,500,y+42),"event",""));
    }

    private void drawBottomNav(Canvas c) {
        float y=logicalH-70;
        p.setColor(Color.rgb(10,16,29)); c.drawRect(0,y-6,DESIGN_W,logicalH,p);
        p.setColor(Color.rgb(30,41,59)); c.drawRect(0,y-6,DESIGN_W,y-4,p);
        String[] labels={"MIASTO","SKLEP","CELE","RDZEŃ","OPCJE"};
        float w=DESIGN_W/5f;
        for(int i=0;i<5;i++) {
            float x=i*w;
            if(tab==i){p.setColor(PANEL_2);c.drawRoundRect(new RectF(x+5,y,x+w-5,logicalH-7),9,9,p);p.setColor(GOLD);c.drawRect(x+24,y,x+w-24,y+3,p);}
            int iconColor=tab==i?GOLD:MUTED;
            drawNavIcon(c,i,x+w/2,y+18,iconColor);
            text(c,labels[i],x+w/2,y+49,8,tab==i?TEXT:MUTED,Paint.Align.CENTER);
            hits.add(new HitTarget(new RectF(x,y-4,x+w,logicalH),"tab",Integer.toString(i)));
        }
    }

    private void drawNavIcon(Canvas c,int i,float x,float y,int color){
        p.setColor(color);
        if(i==0){c.drawRect(x-12,y,x+12,y+14,p);c.drawRect(x-7,y-7,x+7,y+16,p);}
        else if(i==1){c.drawRect(x-13,y-7,x+13,y-1,p);c.drawRect(x-10,y+2,x+10,y+15,p);}
        else if(i==2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawRect(x-12,y-8,x+12,y+16,p);c.drawLine(x-7,y,x+7,y,p);c.drawLine(x-7,y+6,x+7,y+6,p);p.setStyle(Paint.Style.FILL);}
        else if(i==3){pixelDiamond(c,x,y+4,13,color);}
        else {c.drawCircle(x,y+4,12,p);p.setColor(BG);c.drawCircle(x,y+4,5,p);}
    }

    private void drawFloatTexts(Canvas c) {
        for(FloatText f:floatTexts){
            int alpha=(int)(255*Math.max(0,Math.min(1,f.life)));
            int col=(f.color&0x00FFFFFF)|(alpha<<24);
            text(c,f.text,f.x,f.y,15,col,Paint.Align.CENTER);
        }
    }

    private void drawToast(Canvas c) {
        if(toastText.isEmpty()||System.currentTimeMillis()>=toastUntil)return;
        float y=logicalH-126;
        panel(c,54,y,486,y+42,Color.rgb(15,23,42),GOLD,10);
        text(c,toastText,270,y+26,10,TEXT,Paint.Align.CENTER);
    }

    private void drawOfflinePopup(Canvas c) {
        p.setColor(Color.argb(210,0,0,0));c.drawRect(0,0,DESIGN_W,logicalH,p);
        float h=274,y=(logicalH-h)/2f;
        panel(c,44,y,496,y+h,Color.rgb(15,23,42),GOLD,16);
        pixelCoin(c,270,y+58,28,GOLD);
        text(c,"WITAJ Z POWROTEM",270,y+105,19,TEXT,Paint.Align.CENTER);
        text(c,"Nie było Cię: "+GameState.formatDuration(state.startupOfflineSeconds),270,y+135,11,MUTED,Paint.Align.CENTER);
        text(c,"Dochód offline (75%):",270,y+159,10,MUTED,Paint.Align.CENTER);
        text(c,"+"+state.format(state.startupOfflineGain),270,y+193,25,GOLD,Paint.Align.CENTER);
        button(c,88,y+218,452,y+258,"ODBIERZ I GRAJ",GOLD,BG,true);
        hits.add(new HitTarget(new RectF(44,y,496,y+h),"offline",""));
    }

    private void drawTutorial(Canvas c) {
        p.setColor(Color.argb(225,3,7,18));c.drawRect(0,0,DESIGN_W,logicalH,p);
        float y=logicalH/2f-170;
        panel(c,34,y,506,y+340,Color.rgb(15,23,42),GOLD,16);
        String title,body1,body2;
        if(tutorialStep==0){title="PIXEL EMPIRE";body1="Zbuduj imperium od jednego kliknięcia.";body2="Dotknij ekranu, aby rozpocząć samouczek.";drawCore(c,270,y+86,42);}
        else if(tutorialStep==1){title="1. KLIKANIE I AUTO";body1="Klikaj Rdzeń, zdobywaj monety i combo.";body2="W Sklepie kupisz też automatyczny dochód.";pixelCoin(c,270,y+88,32,GOLD);}
        else {title="2. ROZWIJAJ IMPERIUM";body1="Cele dają klejnoty, a Prestige — stały bonus.";body2="Gra nalicza też dochód, gdy jest wyłączona.";pixelDiamond(c,270,y+88,34,CYAN);}
        text(c,title,270,y+155,19,TEXT,Paint.Align.CENTER);
        text(c,body1,270,y+194,11,MUTED,Paint.Align.CENTER);
        text(c,body2,270,y+217,11,MUTED,Paint.Align.CENTER);
        text(c,tutorialStep<2?"DOTKNIJ, ABY DALEJ":"DOTKNIJ — ZACZYNAMY",270,y+300,12,GOLD,Paint.Align.CENTER);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x=e.getX()/scale,y=e.getY()/scale;
        if(e.getAction()==MotionEvent.ACTION_DOWN){downX=x;downY=y;moved=false;return true;}
        if(e.getAction()==MotionEvent.ACTION_MOVE){if(Math.abs(x-downX)>12||Math.abs(y-downY)>12)moved=true;return true;}
        if(e.getAction()==MotionEvent.ACTION_UP){
            performClick();
            if(moved)return true;
            if(!state.tutorialSeen){
                tutorialStep++;
                if(tutorialStep>=3){state.tutorialSeen=true;state.save(getContext());showToast("Powodzenia! Zbuduj własne imperium.",2200);}
                invalidate();return true;
            }
            if(offlinePopup){offlinePopup=false;playFeedback(false);invalidate();return true;}
            for(int i=hits.size()-1;i>=0;i--){HitTarget h=hits.get(i);if(h.rect.contains(x,y)){handleHit(h,x,y);break;}}
            return true;
        }
        return true;
    }

    @Override public boolean performClick(){super.performClick();return true;}

    private void handleHit(HitTarget h,float x,float y){
        switch(h.type){
            case "tab": tab=Integer.parseInt(h.id); playFeedback(false); break;
            case "tap": doTap(x,y); break;
            case "upgrade":
                if(state.buyUpgrade(h.id)){playFeedback(false);showToast("Kupiono: "+state.upgrades.get(h.id).name,900);}else{playError();showToast("Za mało monet",900);}break;
            case "daily":
                if(state.canClaimDaily()){int g=state.claimDaily();playFeedback(true);showToast("Nagroda dzienna! +"+g+" klej.",1800);}break;
            case "event":
                if(state.activeEventType>=0){int type=state.activeEventType;state.collectEvent(type);playFeedback(true);showToast(type==0?"Dron przejęty — monety zdobyte!":type==1?"Złapano klejnoty!":"Turbo aktywne przez 60 s!",1900);}break;
            case "ach_prev": if(achievementPage>0)achievementPage--; break;
            case "ach_next": if(achievementPage<1)achievementPage++; break;
            case "turbo": if(state.buyTurbo()){playFeedback(true);showToast("TURBO x2 aktywne przez 5 min",1700);}else{playError();showToast("Potrzebujesz 10 klejnotów",1200);}break;
            case "frenzy": if(state.buyFrenzy()){playFeedback(true);showToast("FRENZY x3 aktywne przez 2 min",1700);}else{playError();showToast("Potrzebujesz 8 klejnotów",1200);}break;
            case "prestige": handlePrestige(); break;
            case "setting": handleSetting(h.id); break;
            case "reset": handleReset(); break;
        }
        state.save(getContext());
        invalidate();
    }

    private void doTap(float x,float y){
        long now=System.currentTimeMillis();
        if(now-lastTapAt<=850)combo=Math.min(50,combo+1);else combo=1;
        lastTapAt=now;
        boolean crit=random.nextDouble()<0.055;
        double amount=state.tap(combo,crit);
        floatTexts.add(new FloatText(x,y-14,(crit?"KRYTYK! +":"+")+state.format(amount),crit?PURPLE:GOLD));
        if(floatTexts.size()>16)floatTexts.remove(0);
        playFeedback(crit);
    }

    private void handlePrestige(){
        int gain=state.availablePrestigeCores();
        if(gain<=0){playError();showToast("Najpierw zarób 1.00M monet w tej erze",1400);return;}
        long now=System.currentTimeMillis();
        if(prestigeConfirmStage==0||now>prestigeConfirmUntil){prestigeConfirmStage=1;prestigeConfirmUntil=now+5000;showToast("Prestige resetuje monety i ulepszenia — dotknij ponownie",2500);return;}
        if(state.prestige()){prestigeConfirmStage=0;playFeedback(true);showToast("NOWE IMPERIUM! +"+gain+" Rdzeni",2500);tab=0;}
    }

    private void handleSetting(String id){
        if("sound".equals(id))state.soundEnabled=!state.soundEnabled;
        else if("haptic".equals(id))state.hapticsEnabled=!state.hapticsEnabled;
        else if("notation".equals(id))state.scientificNotation=!state.scientificNotation;
        playFeedback(false);
    }

    private void handleReset(){
        long now=System.currentTimeMillis();
        if(now>resetArmedUntil){resetArmedUntil=now+5000;playError();showToast("Dotknij ponownie w ciągu 5 s, aby usunąć zapis",2200);return;}
        state.hardReset(getContext());
        showToast("Zapis usunięty. Uruchamiam grę od nowa…",1800);
        postDelayed(() -> {
            android.app.Activity a=(android.app.Activity)getContext();
            a.recreate();
        },700);
    }

    private void playFeedback(boolean strong){
        if(state.soundEnabled&&tone!=null){try{tone.startTone(strong?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,strong?80:35);}catch(Throwable ignored){}}
        if(state.hapticsEnabled&&vibrator!=null&&vibrator.hasVibrator()){
            try{
                if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createOneShot(strong?45:18,strong?150:70));
                else vibrator.vibrate(strong?45:18);
            }catch(Throwable ignored){}
        }
    }

    private void playError(){
        if(state.soundEnabled&&tone!=null){try{tone.startTone(ToneGenerator.TONE_PROP_NACK,70);}catch(Throwable ignored){}}
        if(state.hapticsEnabled&&vibrator!=null&&vibrator.hasVibrator()){
            try{if(Build.VERSION.SDK_INT>=26)vibrator.vibrate(VibrationEffect.createOneShot(35,90));else vibrator.vibrate(35);}catch(Throwable ignored){}
        }
    }

    private void statCard(Canvas c,float x1,float y1,float x2,float y2,String label,String value,int accent){
        panel(c,x1,y1,x2,y2,PANEL,STROKE,8);
        text(c,label,(x1+x2)/2,y1+20,8,MUTED,Paint.Align.CENTER);
        text(c,value,(x1+x2)/2,y1+45,14,accent,Paint.Align.CENTER);
    }

    private void panel(Canvas c,float x1,float y1,float x2,float y2,int fill,int stroke,float radius){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawRoundRect(new RectF(x1,y1,x2,y2),radius,radius,p);
        strokeRound(c,x1,y1,x2,y2,stroke,radius,1.5f);
    }

    private void strokeRound(Canvas c,float x1,float y1,float x2,float y2,int color,float radius,float width){
        p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setColor(color);c.drawRoundRect(new RectF(x1,y1,x2,y2),radius,radius,p);p.setStyle(Paint.Style.FILL);
    }

    private void button(Canvas c,float x1,float y1,float x2,float y2,String label,int fill,int color,boolean enabled){
        p.setColor(enabled?fill:Color.rgb(38,48,66));c.drawRoundRect(new RectF(x1,y1,x2,y2),9,9,p);
        strokeRound(c,x1,y1,x2,y2,enabled?lighten(fill):Color.rgb(71,85,105),9,1.5f);
        text(c,label,(x1+x2)/2,(y1+y2)/2+4,10,enabled?color:MUTED,Paint.Align.CENTER);
    }

    private int lighten(int color){
        return Color.rgb(Math.min(255,Color.red(color)+25),Math.min(255,Color.green(color)+25),Math.min(255,Color.blue(color)+25));
    }

    private void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTextAlign(align);p.setTypeface(Typeface.create(Typeface.MONOSPACE,Typeface.BOLD));
        c.drawText(s,x,y,p);
    }

    private void pixel(Canvas c,float x,float y,float size,int color){p.setColor(color);c.drawRect(x-size/2,y-size/2,x+size/2,y+size/2,p);}

    private void pixelCoin(Canvas c,float x,float y,float r,int color){
        p.setColor(GOLD_DARK);c.drawRect(x-r,y-r/2,x+r,y+r/2,p);c.drawRect(x-r/2,y-r,x+r/2,y+r,p);
        p.setColor(color);c.drawRect(x-r+4,y-r/2+2,x+r-4,y+r/2-2,p);c.drawRect(x-r/2+2,y-r+4,x+r/2-2,y+r-4,p);
        p.setColor(Color.rgb(255,247,214));c.drawRect(x-r/3,y-r/2,x-1,y-2,p);
    }

    private void pixelDiamond(Canvas c,float x,float y,float r,int color){
        p.setColor(color);
        for(int i=0;i<(int)r;i+=3){float half=r-i;c.drawRect(x-half,y-r+i,x+half,y-r+i+3,p);c.drawRect(x-half,y+r-i-3,x+half,y+r-i,p);}
        p.setColor(Color.argb(150,255,255,255));c.drawRect(x-r/3,y-r/2,x,y-r/4,p);
    }
}
