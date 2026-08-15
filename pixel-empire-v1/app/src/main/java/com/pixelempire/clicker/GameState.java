package com.pixelempire.clicker;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class GameState {
    public static final long OFFLINE_CAP_MS = 12L * 60L * 60L * 1000L;
    public static final double OFFLINE_EFFICIENCY = 0.72;
    private static final String PREFS = "pixel_empire_save_v2";
    private static final String KEY_JSON = "state";

    public static final int GOAL_TAPS = 0;
    public static final int GOAL_COINS = 1;
    public static final int GOAL_CPS = 2;
    public static final int GOAL_LEVEL = 3;
    public static final int GOAL_UPGRADES = 4;
    public static final int GOAL_STAGE = 5;
    public static final int GOAL_COMBO = 6;
    public static final int GOAL_ASCENSIONS = 7;

    public static final class Upgrade {
        public final GameContent.UpgradeDef def;
        public int level;
        Upgrade(GameContent.UpgradeDef def) { this.def = def; }
        public double cost() {
            double v = def.baseCost * Math.pow(def.growth, level);
            return Math.min(1e300, v);
        }
    }

    public static final class Achievement {
        public final int type;
        public final double target;
        public final int reward;
        public final int index;
        public boolean unlocked;
        Achievement(int index, int type, double target, int reward) {
            this.index=index; this.type=type; this.target=target; this.reward=reward;
        }
    }

    public static final class Mission {
        public final int type;
        public final double target;
        public final int researchReward;
        public final int crystalReward;
        public final int index;
        public boolean claimed;
        Mission(int index, int type, double target, int researchReward, int crystalReward) {
            this.index=index; this.type=type; this.target=target; this.researchReward=researchReward; this.crystalReward=crystalReward;
        }
    }

    public double coins = 0;
    public double runCoins = 0;
    public double lifetimeCoins = 0;
    public double buildXp = 0;
    public int level = 1;
    public int stage = 0;
    public long totalTaps = 0;
    public long totalUpgradeBuys = 0;
    public long playSeconds = 0;
    public int bestCombo = 0;
    public double bestTap = 0;
    public double maxCps = 0;
    public int crystals = 0;
    public int researchPoints = 0;
    public int legacyStars = 0;
    public int ascensions = 0;
    public int missionsClaimed = 0;
    public int eventsCollected = 0;

    public long turboUntil = 0;
    public long frenzyUntil = 0;
    public long xpRushUntil = 0;
    public long lastSaveTime = 0;
    public long nextEventAt = 0;
    public int activeEventType = -1;
    public long activeEventUntil = 0;

    public int dailyStreak = 0;
    public String lastDailyClaim = "";
    public boolean tutorialSeen = false;
    public boolean soundEnabled = true;
    public boolean hapticsEnabled = true;
    public boolean compactNumbers = true;
    public boolean lowPower = false;
    public String language = "pl";

    public double startupOfflineGain = 0;
    public long startupOfflineSeconds = 0;
    public int startupOfflineLevels = 0;

    public final LinkedHashMap<String, Upgrade> upgrades = new LinkedHashMap<>();
    public final LinkedHashSet<String> research = new LinkedHashSet<>();
    public final List<Achievement> achievements = new ArrayList<>();
    public final List<Mission> missions = new ArrayList<>();
    public final List<Achievement> justUnlocked = new ArrayList<>();

    public GameState() {
        for (GameContent.UpgradeDef d : GameContent.upgrades()) upgrades.put(d.id, new Upgrade(d));
        initAchievements();
        initMissions();
    }

    private void initAchievements() {
        int i=0;
        double[] taps={1,100,1_000,10_000,100_000,1_000_000};
        for(double x:taps) achievements.add(new Achievement(i++,GOAL_TAPS,x,1+i/4));
        double[] coinsT={1_000,100_000,1_000_000,100_000_000,1_000_000_000L,1_000_000_000_000L,1e15};
        for(double x:coinsT) achievements.add(new Achievement(i++,GOAL_COINS,x,2+i/6));
        double[] cps={10,100,1_000,100_000,1_000_000,1_000_000_000L};
        for(double x:cps) achievements.add(new Achievement(i++,GOAL_CPS,x,2+i/7));
        double[] levels={5,12,24,48,96,144,192,240,288};
        for(double x:levels) achievements.add(new Achievement(i++,GOAL_LEVEL,x,2+i/8));
        double[] buys={10,50,100,250,500,1_000};
        for(double x:buys) achievements.add(new Achievement(i++,GOAL_UPGRADES,x,2+i/9));
        double[] stages={3,6,9,12,15,18,21,24};
        for(double x:stages) achievements.add(new Achievement(i++,GOAL_STAGE,x,3+i/10));
        double[] combos={10,25,50};
        for(double x:combos) achievements.add(new Achievement(i++,GOAL_COMBO,x,4+i/12));
        double[] asc={1,5,10};
        for(double x:asc) achievements.add(new Achievement(i++,GOAL_ASCENSIONS,x,5+i/12));
    }

    private void initMissions() {
        int i=0;
        missions.add(new Mission(i++,GOAL_TAPS,25,1,1));
        missions.add(new Mission(i++,GOAL_COINS,250,1,1));
        missions.add(new Mission(i++,GOAL_UPGRADES,3,1,1));
        missions.add(new Mission(i++,GOAL_LEVEL,4,1,1));
        missions.add(new Mission(i++,GOAL_CPS,5,1,1));
        missions.add(new Mission(i++,GOAL_STAGE,2,2,1));
        missions.add(new Mission(i++,GOAL_TAPS,500,2,1));
        missions.add(new Mission(i++,GOAL_COINS,50_000,2,1));
        missions.add(new Mission(i++,GOAL_UPGRADES,25,2,2));
        missions.add(new Mission(i++,GOAL_LEVEL,24,2,2));
        missions.add(new Mission(i++,GOAL_CPS,1_000,3,2));
        missions.add(new Mission(i++,GOAL_STAGE,4,3,2));
        missions.add(new Mission(i++,GOAL_COMBO,20,3,2));
        missions.add(new Mission(i++,GOAL_COINS,10_000_000,4,3));
        missions.add(new Mission(i++,GOAL_LEVEL,60,4,3));
        missions.add(new Mission(i++,GOAL_STAGE,7,4,3));
        missions.add(new Mission(i++,GOAL_UPGRADES,150,5,3));
        missions.add(new Mission(i++,GOAL_CPS,1_000_000,5,4));
        missions.add(new Mission(i++,GOAL_ASCENSIONS,1,6,5));
        missions.add(new Mission(i++,GOAL_LEVEL,120,6,5));
        missions.add(new Mission(i++,GOAL_STAGE,12,7,5));
        missions.add(new Mission(i++,GOAL_COINS,1_000_000_000_000L,7,6));
        missions.add(new Mission(i++,GOAL_UPGRADES,400,8,6));
        missions.add(new Mission(i++,GOAL_CPS,1_000_000_000L,8,6));
        missions.add(new Mission(i++,GOAL_LEVEL,180,9,7));
        missions.add(new Mission(i++,GOAL_STAGE,18,10,8));
        missions.add(new Mission(i++,GOAL_ASCENSIONS,5,12,10));
        missions.add(new Mission(i++,GOAL_LEVEL,240,15,12));
        missions.add(new Mission(i++,GOAL_STAGE,24,20,15));
        missions.add(new Mission(i++,GOAL_LEVEL,288,25,20));
    }

    public void load(Context context) {
        SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String raw=prefs.getString(KEY_JSON,"");
        if(raw==null||raw.isEmpty()){
            long now=System.currentTimeMillis();
            lastSaveTime=now; nextEventAt=now+45_000;
            return;
        }
        try{
            JSONObject o=new JSONObject(raw);
            coins=o.optDouble("coins",0); runCoins=o.optDouble("runCoins",0); lifetimeCoins=o.optDouble("lifetimeCoins",0);
            buildXp=o.optDouble("buildXp",0); level=Math.max(1,o.optInt("level",1)); stage=Math.max(0,Math.min(23,o.optInt("stage",(level-1)/12)));
            totalTaps=o.optLong("totalTaps",0); totalUpgradeBuys=o.optLong("totalUpgradeBuys",0); playSeconds=o.optLong("playSeconds",0);
            bestCombo=o.optInt("bestCombo",0); bestTap=o.optDouble("bestTap",0); maxCps=o.optDouble("maxCps",0);
            crystals=o.optInt("crystals",0); researchPoints=o.optInt("researchPoints",0); legacyStars=o.optInt("legacyStars",0);
            ascensions=o.optInt("ascensions",0); missionsClaimed=o.optInt("missionsClaimed",0); eventsCollected=o.optInt("eventsCollected",0);
            turboUntil=o.optLong("turboUntil",0); frenzyUntil=o.optLong("frenzyUntil",0); xpRushUntil=o.optLong("xpRushUntil",0);
            lastSaveTime=o.optLong("lastSaveTime",System.currentTimeMillis()); nextEventAt=o.optLong("nextEventAt",System.currentTimeMillis()+45_000);
            dailyStreak=o.optInt("dailyStreak",0); lastDailyClaim=o.optString("lastDailyClaim",""); tutorialSeen=o.optBoolean("tutorialSeen",false);
            soundEnabled=o.optBoolean("soundEnabled",true); hapticsEnabled=o.optBoolean("hapticsEnabled",true); compactNumbers=o.optBoolean("compactNumbers",true);
            lowPower=o.optBoolean("lowPower",false); language=o.optString("language","pl");
            JSONObject levels=o.optJSONObject("upgrades");
            if(levels!=null) for(Map.Entry<String,Upgrade> e:upgrades.entrySet()) e.getValue().level=Math.max(0,levels.optInt(e.getKey(),0));
            JSONArray rr=o.optJSONArray("research"); if(rr!=null) for(int i=0;i<rr.length();i++) research.add(rr.optString(i,""));
            JSONArray aa=o.optJSONArray("achievements"); if(aa!=null) for(int i=0;i<aa.length();i++){int idx=aa.optInt(i,-1);if(idx>=0&&idx<achievements.size())achievements.get(idx).unlocked=true;}
            JSONArray mm=o.optJSONArray("missions"); if(mm!=null) for(int i=0;i<mm.length();i++){int idx=mm.optInt(i,-1);if(idx>=0&&idx<missions.size())missions.get(idx).claimed=true;}
        }catch(Exception ignored){}
        applyOfflineIncome();
        checkAchievements();
    }

    public void save(Context context) {
        try{
            long now=System.currentTimeMillis(); lastSaveTime=now;
            JSONObject o=new JSONObject();
            o.put("coins",coins);o.put("runCoins",runCoins);o.put("lifetimeCoins",lifetimeCoins);o.put("buildXp",buildXp);o.put("level",level);o.put("stage",stage);
            o.put("totalTaps",totalTaps);o.put("totalUpgradeBuys",totalUpgradeBuys);o.put("playSeconds",playSeconds);o.put("bestCombo",bestCombo);o.put("bestTap",bestTap);o.put("maxCps",maxCps);
            o.put("crystals",crystals);o.put("researchPoints",researchPoints);o.put("legacyStars",legacyStars);o.put("ascensions",ascensions);o.put("missionsClaimed",missionsClaimed);o.put("eventsCollected",eventsCollected);
            o.put("turboUntil",turboUntil);o.put("frenzyUntil",frenzyUntil);o.put("xpRushUntil",xpRushUntil);o.put("lastSaveTime",lastSaveTime);o.put("nextEventAt",nextEventAt);
            o.put("dailyStreak",dailyStreak);o.put("lastDailyClaim",lastDailyClaim);o.put("tutorialSeen",tutorialSeen);o.put("soundEnabled",soundEnabled);o.put("hapticsEnabled",hapticsEnabled);
            o.put("compactNumbers",compactNumbers);o.put("lowPower",lowPower);o.put("language",language);
            JSONObject ul=new JSONObject();for(Map.Entry<String,Upgrade> e:upgrades.entrySet())ul.put(e.getKey(),e.getValue().level);o.put("upgrades",ul);
            JSONArray rr=new JSONArray();for(String id:research)rr.put(id);o.put("research",rr);
            JSONArray aa=new JSONArray();for(Achievement a:achievements)if(a.unlocked)aa.put(a.index);o.put("achievements",aa);
            JSONArray mm=new JSONArray();for(Mission m:missions)if(m.claimed)mm.put(m.index);o.put("missions",mm);
            context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_JSON,o.toString()).apply();
        }catch(Exception ignored){}
    }

    public void hardReset(Context context){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().apply();}

    private void applyOfflineIncome(){
        long now=System.currentTimeMillis(); if(lastSaveTime<=0||now<=lastSaveTime)return;
        long elapsed=Math.min(OFFLINE_CAP_MS,now-lastSaveTime); if(elapsed<15_000)return;
        double base=getCps(false); if(base<=0)return;
        startupOfflineSeconds=elapsed/1000L; startupOfflineGain=base*(elapsed/1000.0)*OFFLINE_EFFICIENCY;
        int before=level; addCoins(startupOfflineGain);
        double xp=Math.max(0.1,Math.pow(Math.max(1,base),0.33)*0.045)*(elapsed/1000.0)*OFFLINE_EFFICIENCY*researchMultiplier(3);
        addBuildXp(xp); startupOfflineLevels=Math.max(0,level-before);
    }

    public void tick(double seconds){
        if(seconds<=0)return;
        double cps=getCps(); if(cps>0)addCoins(cps*seconds);
        double xp=Math.max(0.08,Math.pow(Math.max(1,cps),0.33)*0.045)*seconds*researchMultiplier(3);
        if(System.currentTimeMillis()<xpRushUntil)xp*=4.0;
        addBuildXp(xp);
        maxCps=Math.max(maxCps,cps);
    }

    public double tap(int combo,boolean critical){
        totalTaps++; bestCombo=Math.max(bestCombo,combo);
        double comboMult=1.0+Math.min(50,Math.max(0,combo-1))*0.022;
        double amount=getTapPower()*comboMult*(critical?8.0:1.0);
        if(System.currentTimeMillis()<frenzyUntil)amount*=3.0;
        bestTap=Math.max(bestTap,amount);addCoins(amount);
        double xp=(1.0+Math.pow(Math.max(1,getTapPower()),0.28)*0.34)*comboMult*researchMultiplier(3);
        if(System.currentTimeMillis()<xpRushUntil)xp*=4.0;
        addBuildXp(xp);checkAchievements();return amount;
    }

    public void addCoins(double amount){
        if(!(amount>0)||Double.isInfinite(amount)||Double.isNaN(amount))return;
        coins=Math.min(1e300,coins+amount);runCoins=Math.min(1e300,runCoins+amount);lifetimeCoins=Math.min(1e300,lifetimeCoins+amount);
    }

    public int addBuildXp(double amount){
        if(!(amount>0)||Double.isInfinite(amount)||Double.isNaN(amount))return 0;
        buildXp+=amount;int levels=0;
        while(level<GameContent.MAX_LEVEL){double req=GameContent.xpForLevel(level);if(buildXp<req)break;buildXp-=req;level++;levels++;
            int oldStage=stage;stage=Math.min(GameContent.STAGE_COUNT-1,(level-1)/GameContent.LEVELS_PER_STAGE);
            researchPoints+=GameContent.researchRewardForLevel(level);
            if(stage>oldStage)crystals+=GameContent.crystalsForNewStage(stage);
        }
        if(level>=GameContent.MAX_LEVEL)buildXp=Math.min(buildXp,GameContent.xpForLevel(GameContent.MAX_LEVEL));
        if(levels>0)checkAchievements();return levels;
    }

    public double levelRequirement(){return GameContent.xpForLevel(level);}
    public double levelProgress(){if(level>=GameContent.MAX_LEVEL)return 1.0;return Math.max(0,Math.min(1,buildXp/levelRequirement()));}
    public int subLevel(){return ((level-1)%GameContent.LEVELS_PER_STAGE)+1;}

    private double stageMultiplier(){return GameContent.stageVisualPower(stage);}
    public double legacyMultiplier(){return 1.0+legacyStars*0.08;}
    private double researchMultiplier(int kind){double m=1.0;for(GameContent.ResearchDef r:GameContent.research())if(research.contains(r.id)&&(r.kind==kind||r.kind==2))m+=r.bonus;return m;}

    public double getTapPower(){
        double v=1;for(Upgrade u:upgrades.values())if(u.def.tap)v+=u.level*u.def.value;
        return v*stageMultiplier()*legacyMultiplier()*researchMultiplier(0);
    }

    private double getCps(boolean boosts){
        double v=0;for(Upgrade u:upgrades.values())if(!u.def.tap)v+=u.level*u.def.value;
        v*=stageMultiplier()*legacyMultiplier()*researchMultiplier(1);
        if(boosts&&System.currentTimeMillis()<turboUntil)v*=2.0;
        return v;
    }
    public double getCps(){return getCps(true);}

    public boolean canBuyUpgrade(String id){Upgrade u=upgrades.get(id);return u!=null&&stage>=u.def.unlockStage&&coins+1e-9>=u.cost();}
    public boolean buyUpgrade(String id){Upgrade u=upgrades.get(id);if(u==null||stage<u.def.unlockStage)return false;double c=u.cost();if(coins+1e-9<c)return false;coins-=c;u.level++;totalUpgradeBuys++;checkAchievements();return true;}

    public GameContent.ResearchDef researchDef(String id){for(GameContent.ResearchDef r:GameContent.research())if(r.id.equals(id))return r;return null;}
    public boolean canResearch(String id){GameContent.ResearchDef r=researchDef(id);return r!=null&&!research.contains(id)&&stage>=r.unlockStage&&researchPoints>=r.cost;}
    public boolean buyResearch(String id){GameContent.ResearchDef r=researchDef(id);if(r==null||research.contains(id)||stage<r.unlockStage||researchPoints<r.cost)return false;researchPoints-=r.cost;research.add(id);return true;}

    public int availableLegacyStars(){
        if(stage<7)return 0;
        double economy=Math.sqrt(Math.max(1,runCoins)/1_000_000.0);
        double progress=stage/3.0+level/72.0;
        return Math.max(1,(int)Math.floor(economy+progress));
    }

    public boolean ascend(){
        int gain=availableLegacyStars();if(gain<=0)return false;
        legacyStars+=gain;ascensions++;coins=0;runCoins=0;buildXp=0;level=1;stage=0;turboUntil=0;frenzyUntil=0;xpRushUntil=0;
        for(Upgrade u:upgrades.values())u.level=0;
        checkAchievements();return true;
    }

    public boolean canClaimDaily(){return !today().equals(lastDailyClaim);}
    public int claimDaily(){
        String today=today();if(today.equals(lastDailyClaim))return 0;
        LocalDate now=LocalDate.now();LocalDate prev=null;try{if(!lastDailyClaim.isEmpty())prev=LocalDate.parse(lastDailyClaim);}catch(Exception ignored){}
        if(prev!=null&&prev.plusDays(1).equals(now))dailyStreak=Math.min(7,dailyStreak+1);else dailyStreak=1;
        lastDailyClaim=today;int reward=1+dailyStreak;crystals+=reward;if(dailyStreak==7)researchPoints+=5;return reward;
    }
    private String today(){return LocalDate.now().toString();}

    public void collectEvent(int type){
        if(type<0)return;eventsCollected++;
        long now=System.currentTimeMillis();
        if(type==0)addCoins(Math.max(250,getCps()*150+getTapPower()*80));
        else if(type==1)crystals+=3+stage/6;
        else if(type==2)frenzyUntil=Math.max(now,frenzyUntil)+60_000;
        else if(type==3)turboUntil=Math.max(now,turboUntil)+120_000;
        else if(type==4)xpRushUntil=Math.max(now,xpRushUntil)+60_000;
        activeEventType=-1;activeEventUntil=0;nextEventAt=now+45_000+(long)(Math.random()*65_000);
    }

    public double goalValue(int type){
        switch(type){
            case GOAL_TAPS:return totalTaps;
            case GOAL_COINS:return lifetimeCoins;
            case GOAL_CPS:return maxCps;
            case GOAL_LEVEL:return level;
            case GOAL_UPGRADES:return totalUpgradeBuys;
            case GOAL_STAGE:return stage+1;
            case GOAL_COMBO:return bestCombo;
            case GOAL_ASCENSIONS:return ascensions;
            default:return 0;
        }
    }

    public void checkAchievements(){
        for(Achievement a:achievements)if(!a.unlocked&&goalValue(a.type)>=a.target){a.unlocked=true;crystals+=a.reward;justUnlocked.add(a);}
    }
    public int unlockedAchievementCount(){int n=0;for(Achievement a:achievements)if(a.unlocked)n++;return n;}

    public boolean missionReady(Mission m){return m!=null&&!m.claimed&&goalValue(m.type)>=m.target;}
    public boolean claimMission(int index){if(index<0||index>=missions.size())return false;Mission m=missions.get(index);if(!missionReady(m))return false;m.claimed=true;missionsClaimed++;researchPoints+=m.researchReward;crystals+=m.crystalReward;return true;}

    public String goalText(int type,double target){
        String v=format(target);
        switch(type){
            case GOAL_TAPS:return label("Kliknięcia: ","Taps: ","Toques: ","Klepnutí: ","Нажатия: ","点击：")+v;
            case GOAL_COINS:return label("Zarób: ","Earn: ","Gana: ","Vydělej: ","Заработай: ","赚取：")+v;
            case GOAL_CPS:return label("Dochód / sek.: ","Income / sec: ","Ingreso / s: ","Příjem / s: ","Доход / сек: ","每秒收益：")+v;
            case GOAL_LEVEL:return label("Osiągnij poziom ","Reach level ","Alcanza nivel ","Dosáhni úrovně ","Достигни уровня ","达到等级 ")+((int)target);
            case GOAL_UPGRADES:return label("Kup ulepszenia: ","Buy upgrades: ","Compra mejoras: ","Kup vylepšení: ","Купи улучшения: ","购买升级：")+v;
            case GOAL_STAGE:return label("Dotrzyj do etapu ","Reach stage ","Alcanza etapa ","Dosáhni etapy ","Достигни этапа ","到达阶段 ")+((int)target);
            case GOAL_COMBO:return "Combo x"+((int)target);
            case GOAL_ASCENSIONS:return label("Odrodzenia: ","Ascensions: ","Ascensiones: ","Vzestupy: ","Вознесения: ","飞升：")+((int)target);
            default:return v;
        }
    }

    private String label(String pl,String en,String es,String cs,String ru,String zh){
        if("en".equals(language))return en;if("es".equals(language))return es;if("cs".equals(language))return cs;if("ru".equals(language))return ru;if("zh".equals(language))return zh;return pl;
    }

    public String format(double value){
        if(Double.isNaN(value)||Double.isInfinite(value))return "0";
        if(!compactNumbers&&Math.abs(value)>=1_000_000)return String.format(Locale.US,"%.3e",value);
        double a=Math.abs(value);String sign=value<0?"-":"";
        if(a<1_000)return sign+(a>=100?String.format(Locale.US,"%.0f",a):a>=10?String.format(Locale.US,"%.1f",a):String.format(Locale.US,"%.2f",a));
        String[] suf={"K","M","B","T","Qa","Qi","Sx","Sp","Oc","No","Dc","Ud","Dd","Td","Qad","Qid"};
        int i=-1;while(a>=1000&&i<suf.length-1){a/=1000;i++;}
        if(i>=suf.length-1&&a>=1000)return String.format(Locale.US,"%.2e",Math.abs(value));
        return sign+(a>=100?String.format(Locale.US,"%.0f",a):a>=10?String.format(Locale.US,"%.1f",a):String.format(Locale.US,"%.2f",a))+suf[Math.max(0,i)];
    }

    public static String formatDuration(long sec){long h=sec/3600;long m=(sec%3600)/60;long s=sec%60;if(h>0)return String.format(Locale.US,"%dh %02dm",h,m);if(m>0)return String.format(Locale.US,"%dm %02ds",m,s);return s+"s";}
}
