package com.pixelempire.clicker;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Random;

/** Complete persistent gameplay state for Pixel Empire release candidate. */
public final class ReleaseGameState {
    public static final long OFFLINE_CAP_MS = 12L * 60L * 60L * 1000L;
    public static final double OFFLINE_EFFICIENCY = 0.20;
    private static final String PREFS = "pixel_empire_release_v1";
    private static final String KEY = "state";

    public double coins=0, lifetimeCoins=0, buildXp=0;
    public int level=1, stage=0;
    public int crystals=8, researchPoints=3, legacyStars=0, rebirths=0;
    public long totalTaps=0, playSeconds=0, totalBuildingBuys=0, totalHeroLevels=0;
    public int bestComboTaps=0, bossDefeats=0;
    public double bestTap=0, maxCps=0;

    public final int[] buildings=new int[ReleaseContent.BUILDINGS.length];
    public final int[] heroes=new int[ReleaseContent.HEROES.length];
    public final int[] tech=new int[ReleaseContent.TECH.length];

    public boolean sound=true, haptics=true, lowPower=false, tutorialSeen=false;
    public String language="pl";
    public int dailyStreak=0;
    public String lastDaily="";
    public int powerTapCharges=3;
    public String powerChargeDate="";

    public long goldBoostUntil=0, buildBoostUntil=0, tapBoostUntil=0;
    public long nextEventAt=0, eventUntil=0;
    public int eventType=-1;

    public boolean bossActive=false;
    public double bossHp=0, bossMaxHp=0;
    public long bossUntil=0;

    public long lastSave=0;
    public long startupOfflineSeconds=0;
    public double startupOfflineGain=0;

    public final boolean[] missionClaimed=new boolean[18];
    public final boolean[] achievementUnlocked=new boolean[30];
    public int missionsClaimed=0, achievementsUnlocked=0;

    private final Random random=new Random();

    public void load(Context c){
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String raw=p.getString(KEY,"");
        long now=System.currentTimeMillis();
        if(raw==null||raw.isEmpty()){
            lastSave=now;nextEventAt=now+55_000;tutorialSeen=false;refreshDailyPower();return;
        }
        try{
            JSONObject o=new JSONObject(raw);
            coins=safe(o.optDouble("coins",0)); lifetimeCoins=safe(o.optDouble("lifetimeCoins",0)); buildXp=safe(o.optDouble("buildXp",0));
            level=clamp(o.optInt("level",1),1,ReleaseContent.MAX_LEVEL);
            stage=clamp(o.optInt("stage",(level-1)/ReleaseContent.LEVELS_PER_STAGE),0,ReleaseContent.STAGES-1);
            crystals=Math.max(0,o.optInt("crystals",0)); researchPoints=Math.max(0,o.optInt("researchPoints",0)); legacyStars=Math.max(0,o.optInt("legacyStars",0)); rebirths=Math.max(0,o.optInt("rebirths",0));
            totalTaps=Math.max(0,o.optLong("totalTaps",0)); playSeconds=Math.max(0,o.optLong("playSeconds",0)); totalBuildingBuys=Math.max(0,o.optLong("totalBuildingBuys",0)); totalHeroLevels=Math.max(0,o.optLong("totalHeroLevels",0));
            bestComboTaps=Math.max(0,o.optInt("bestComboTaps",0)); bossDefeats=Math.max(0,o.optInt("bossDefeats",0)); bestTap=safe(o.optDouble("bestTap",0)); maxCps=safe(o.optDouble("maxCps",0));
            readIntArray(o.optJSONArray("buildings"),buildings);readIntArray(o.optJSONArray("heroes"),heroes);readIntArray(o.optJSONArray("tech"),tech);
            sound=o.optBoolean("sound",true);haptics=o.optBoolean("haptics",true);lowPower=o.optBoolean("lowPower",false);tutorialSeen=o.optBoolean("tutorialSeen",false);language=o.optString("language","pl");
            dailyStreak=Math.max(0,o.optInt("dailyStreak",0));lastDaily=o.optString("lastDaily","");powerTapCharges=clamp(o.optInt("powerTapCharges",3),0,9);powerChargeDate=o.optString("powerChargeDate","");
            goldBoostUntil=o.optLong("goldBoostUntil",0);buildBoostUntil=o.optLong("buildBoostUntil",0);tapBoostUntil=o.optLong("tapBoostUntil",0);
            nextEventAt=o.optLong("nextEventAt",now+55_000);eventUntil=o.optLong("eventUntil",0);eventType=o.optInt("eventType",-1);
            bossActive=o.optBoolean("bossActive",false);bossHp=safe(o.optDouble("bossHp",0));bossMaxHp=safe(o.optDouble("bossMaxHp",0));bossUntil=o.optLong("bossUntil",0);
            lastSave=o.optLong("lastSave",now);
            readBoolArray(o.optJSONArray("missionClaimed"),missionClaimed);readBoolArray(o.optJSONArray("achievementUnlocked"),achievementUnlocked);
            missionsClaimed=countTrue(missionClaimed);achievementsUnlocked=countTrue(achievementUnlocked);
        }catch(Exception ignored){ lastSave=now; }
        refreshDailyPower();
        applyOffline();
        validateTimers();
        checkAchievements();
    }

    public void save(Context c){
        try{
            long now=System.currentTimeMillis();lastSave=now;
            JSONObject o=new JSONObject();
            o.put("coins",coins);o.put("lifetimeCoins",lifetimeCoins);o.put("buildXp",buildXp);o.put("level",level);o.put("stage",stage);
            o.put("crystals",crystals);o.put("researchPoints",researchPoints);o.put("legacyStars",legacyStars);o.put("rebirths",rebirths);
            o.put("totalTaps",totalTaps);o.put("playSeconds",playSeconds);o.put("totalBuildingBuys",totalBuildingBuys);o.put("totalHeroLevels",totalHeroLevels);o.put("bestComboTaps",bestComboTaps);o.put("bossDefeats",bossDefeats);o.put("bestTap",bestTap);o.put("maxCps",maxCps);
            o.put("buildings",toJson(buildings));o.put("heroes",toJson(heroes));o.put("tech",toJson(tech));
            o.put("sound",sound);o.put("haptics",haptics);o.put("lowPower",lowPower);o.put("tutorialSeen",tutorialSeen);o.put("language",language);
            o.put("dailyStreak",dailyStreak);o.put("lastDaily",lastDaily);o.put("powerTapCharges",powerTapCharges);o.put("powerChargeDate",powerChargeDate);
            o.put("goldBoostUntil",goldBoostUntil);o.put("buildBoostUntil",buildBoostUntil);o.put("tapBoostUntil",tapBoostUntil);
            o.put("nextEventAt",nextEventAt);o.put("eventUntil",eventUntil);o.put("eventType",eventType);
            o.put("bossActive",bossActive);o.put("bossHp",bossHp);o.put("bossMaxHp",bossMaxHp);o.put("bossUntil",bossUntil);o.put("lastSave",lastSave);
            o.put("missionClaimed",toJson(missionClaimed));o.put("achievementUnlocked",toJson(achievementUnlocked));
            c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,o.toString()).apply();
        }catch(Exception ignored){}
    }

    public void hardReset(Context c){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().apply();}

    private void applyOffline(){
        long now=System.currentTimeMillis();
        if(lastSave<=0||now<=lastSave)return;
        long elapsed=Math.min(OFFLINE_CAP_MS,now-lastSave);
        if(elapsed<30_000)return;
        double cps=getCps(false);
        if(cps<=0)return;
        startupOfflineSeconds=elapsed/1000L;
        startupOfflineGain=cps*startupOfflineSeconds*OFFLINE_EFFICIENCY;
        addCoins(startupOfflineGain);
    }

    private void validateTimers(){
        long now=System.currentTimeMillis();
        if(eventType>=0&&eventUntil<=now){eventType=-1;eventUntil=0;nextEventAt=now+55_000;}
        if(bossActive&&bossUntil<=now){bossActive=false;bossHp=0;bossMaxHp=0;bossUntil=0;}
    }

    public void tick(double sec){
        if(sec<=0)return;
        double cps=getCps(true);
        addCoins(cps*sec);
        maxCps=Math.max(maxCps,cps);
        double xp=(0.08+Math.pow(Math.max(1,cps),0.25)*0.055)*sec*techMultiplier(3);
        if(System.currentTimeMillis()<buildBoostUntil)xp*=4.0;
        addBuildXp(xp);
        updateEvent();
        updateBoss(sec);
        checkAchievements();
    }

    public double tap(int comboTaps,boolean critical){
        totalTaps++;bestComboTaps=Math.max(bestComboTaps,comboTaps);
        double combo=comboMultiplier(comboTaps);
        double amount=getTapPower()*combo*(critical?5.0:1.0);
        if(System.currentTimeMillis()<tapBoostUntil)amount*=3.0;
        if(System.currentTimeMillis()<goldBoostUntil)amount*=2.0;
        addCoins(amount);bestTap=Math.max(bestTap,amount);
        double xp=(1.2+Math.pow(Math.max(1,getTapPower()),.20)*.28)*combo*techMultiplier(3);
        if(System.currentTimeMillis()<buildBoostUntil)xp*=4.0;
        addBuildXp(xp);
        if(bossActive)damageBoss(getTapPower()*combo*(critical?5.0:1.0));
        return amount;
    }

    public static double comboMultiplier(int taps){
        int steps=Math.max(0,taps)/50;
        return Math.min(5.0,1.0+steps*0.1);
    }
    public static int comboNextThreshold(int taps){int step=Math.max(0,taps)/50;return Math.min(2000,(step+1)*50);}

    public double getTapPower(){
        double base=1.0+Math.pow(1.13,stage)*0.75;
        base*=ReleaseContent.stageMultiplier(stage)*legacyMultiplier()*techMultiplier(0)*techMultiplier(2);
        return base;
    }

    public double getCps(){return getCps(true);}
    public double getCps(boolean temporary){
        double sum=0;
        for(int i=0;i<buildings.length;i++){
            ReleaseContent.BuildingDef d=ReleaseContent.BUILDINGS[i];
            if(buildings[i]>0)sum+=buildings[i]*d.baseCps*Math.pow(1.012,buildings[i]);
        }
        double heroContribution=getHeroDps()*0.22;
        sum=(sum+heroContribution)*ReleaseContent.stageMultiplier(stage)*legacyMultiplier()*techMultiplier(1)*techMultiplier(2);
        if(temporary&&System.currentTimeMillis()<goldBoostUntil)sum*=2.0;
        return Math.max(0,sum);
    }

    public double getHeroDps(){
        double dps=0;for(int i=0;i<heroes.length;i++)if(heroes[i]>0)dps+=ReleaseContent.HEROES[i].baseDps*heroes[i]*Math.pow(1.055,heroes[i]-1);
        return dps*techMultiplier(5)*legacyMultiplier();
    }

    private double techMultiplier(int kind){
        double m=1.0;for(int i=0;i<tech.length;i++){ReleaseContent.TechDef d=ReleaseContent.TECH[i];if(d.kind==kind||d.kind==2)m+=tech[i]*d.bonus;}return m;
    }
    public double legacyMultiplier(){return 1.0+legacyStars*.06;}

    public void addCoins(double n){if(!(n>0)||Double.isNaN(n)||Double.isInfinite(n))return;coins=Math.min(1e300,coins+n);lifetimeCoins=Math.min(1e300,lifetimeCoins+n);}

    public int addBuildXp(double n){
        if(!(n>0)||Double.isNaN(n)||Double.isInfinite(n))return 0;
        buildXp+=n;int gained=0;
        while(level<ReleaseContent.MAX_LEVEL){double req=ReleaseContent.xpForLevel(level);if(buildXp<req)break;buildXp-=req;level++;gained++;
            int old=stage;stage=Math.min(ReleaseContent.STAGES-1,(level-1)/ReleaseContent.LEVELS_PER_STAGE);
            if(level%5==0)researchPoints++;
            if(stage>old){crystals+=3+stage/4;if(stage%5==0)researchPoints+=3;}
        }
        if(level>=ReleaseContent.MAX_LEVEL)buildXp=Math.min(buildXp,ReleaseContent.xpForLevel(level));
        return gained;
    }
    public double levelProgress(){if(level>=ReleaseContent.MAX_LEVEL)return 1;return Math.max(0,Math.min(1,buildXp/ReleaseContent.xpForLevel(level)));}
    public int stageSubLevel(){return ((level-1)%ReleaseContent.LEVELS_PER_STAGE)+1;}

    public double buildingCost(int i,int count){
        if(i<0||i>=buildings.length)return Double.POSITIVE_INFINITY;
        ReleaseContent.BuildingDef d=ReleaseContent.BUILDINGS[i];int have=buildings[i];count=Math.max(1,count);
        if(Math.abs(d.growth-1)<1e-9)return d.baseCost*count;
        double first=d.baseCost*Math.pow(d.growth,have);
        return first*(Math.pow(d.growth,count)-1)/(d.growth-1);
    }
    public int maxAffordableBuildingCount(int i){
        if(i<0||i>=buildings.length||stage<ReleaseContent.BUILDINGS[i].unlockStage)return 0;
        int lo=0,hi=1;while(hi<1_000_000&&buildingCost(i,hi)<=coins)hi*=2;hi=Math.min(hi,1_000_000);
        while(lo+1<hi){int mid=lo+(hi-lo)/2;if(buildingCost(i,mid)<=coins)lo=mid;else hi=mid;}
        if(buildingCost(i,hi)<=coins)return hi;return lo;
    }
    public boolean buyBuilding(int i,int count){
        if(i<0||i>=buildings.length||stage<ReleaseContent.BUILDINGS[i].unlockStage)return false;
        if(count<=0)count=maxAffordableBuildingCount(i);if(count<=0)return false;
        double cost=buildingCost(i,count);if(coins+1e-9<cost)return false;coins-=cost;buildings[i]+=count;totalBuildingBuys+=count;return true;
    }

    public double heroCost(int i){if(i<0||i>=heroes.length)return Double.POSITIVE_INFINITY;ReleaseContent.HeroDef d=ReleaseContent.HEROES[i];return d.baseCost*Math.pow(1.19,heroes[i]);}
    public boolean upgradeHero(int i){
        if(i<0||i>=heroes.length||stage<ReleaseContent.HEROES[i].unlockStage)return false;double c=heroCost(i);if(coins+1e-9<c)return false;coins-=c;heroes[i]++;totalHeroLevels++;return true;
    }

    public int techCost(int i){if(i<0||i>=tech.length)return Integer.MAX_VALUE;ReleaseContent.TechDef d=ReleaseContent.TECH[i];return (int)Math.ceil(d.baseCost*Math.pow(1.55,tech[i]));}
    public boolean techUnlocked(int i){if(i<0||i>=tech.length)return false;ReleaseContent.TechDef d=ReleaseContent.TECH[i];return stage>=d.unlockStage&&(d.prereq<0||tech[d.prereq]>0);}
    public boolean buyTech(int i){if(!techUnlocked(i))return false;ReleaseContent.TechDef d=ReleaseContent.TECH[i];if(tech[i]>=d.max)return false;int c=techCost(i);if(researchPoints<c)return false;researchPoints-=c;tech[i]++;return true;}

    public boolean canClaimDaily(){return !LocalDate.now().toString().equals(lastDaily);}
    public int claimDaily(){
        if(!canClaimDaily())return 0;LocalDate now=LocalDate.now();LocalDate prev=null;try{if(!lastDaily.isEmpty())prev=LocalDate.parse(lastDaily);}catch(Exception ignored){}
        if(prev!=null&&prev.plusDays(1).equals(now))dailyStreak=Math.min(7,dailyStreak+1);else dailyStreak=1;
        lastDaily=now.toString();int reward=5+dailyStreak*2;crystals+=reward;if(dailyStreak==7){researchPoints+=10;powerTapCharges=Math.min(9,powerTapCharges+2);}return reward;
    }
    private void refreshDailyPower(){String d=LocalDate.now().toString();if(!d.equals(powerChargeDate)){powerChargeDate=d;powerTapCharges=Math.max(powerTapCharges,3);}}

    public boolean activatePowerTap(){if(powerTapCharges<=0)return false;powerTapCharges--;tapBoostUntil=Math.max(System.currentTimeMillis(),tapBoostUntil)+30_000;return true;}
    public boolean shopBuy(int item){
        long now=System.currentTimeMillis();
        if(item==0&&crystals>=100){crystals-=100;goldBoostUntil=Math.max(now,goldBoostUntil)+30*60_000L;return true;}
        if(item==1&&crystals>=100){crystals-=100;buildBoostUntil=Math.max(now,buildBoostUntil)+30*60_000L;return true;}
        if(item==2&&crystals>=80){crystals-=80;powerTapCharges=Math.min(9,powerTapCharges+1);return true;}
        return false;
    }

    private void updateEvent(){
        long now=System.currentTimeMillis();
        if(eventType>=0&&now>=eventUntil){eventType=-1;eventUntil=0;nextEventAt=now+55_000+random.nextInt(75_000);}
        if(eventType<0&&now>=nextEventAt){eventType=random.nextInt(5);eventUntil=now+25_000;}
    }
    public void collectEvent(){
        if(eventType<0)return;long now=System.currentTimeMillis();
        switch(eventType){
            case 0: addCoins(Math.max(500,getCps(false)*180+getTapPower()*120));break;
            case 1: crystals+=3+stage/8;break;
            case 2: buildBoostUntil=Math.max(now,buildBoostUntil)+90_000;break;
            case 3: goldBoostUntil=Math.max(now,goldBoostUntil)+120_000;break;
            case 4: tapBoostUntil=Math.max(now,tapBoostUntil)+90_000;break;
        }
        eventType=-1;eventUntil=0;nextEventAt=now+55_000+random.nextInt(75_000);
    }

    public boolean bossAvailable(){return stage>=4&&!bossActive;}
    public boolean startBoss(){
        if(!bossAvailable())return false;long now=System.currentTimeMillis();double strength=Math.max(getTapPower()*35,getCps(false)*12+100);
        bossMaxHp=strength*(50+stage*4);bossHp=bossMaxHp;bossUntil=now+45_000;bossActive=true;return true;
    }
    private void updateBoss(double sec){
        if(!bossActive)return;long now=System.currentTimeMillis();if(now>=bossUntil){bossActive=false;bossHp=0;return;}
        damageBoss(getHeroDps()*sec);
    }
    public void damageBoss(double dmg){
        if(!bossActive||dmg<=0)return;bossHp-=dmg;if(bossHp<=0){bossHp=0;bossActive=false;bossDefeats++;crystals+=10+stage/2;researchPoints+=2+stage/10;addCoins(Math.max(1000,getCps(false)*600));}
    }

    public int availableLegacyStars(){if(stage<9)return 0;double a=Math.sqrt(Math.max(1,lifetimeCoins)/1_000_000.0);double b=stage*.9+level/100.0;return Math.max(1,(int)Math.floor(a+b));}
    public boolean rebirth(){int gain=availableLegacyStars();if(gain<=0)return false;legacyStars+=gain;rebirths++;coins=0;buildXp=0;level=1;stage=0;for(int i=0;i<buildings.length;i++)buildings[i]=0;bossActive=false;eventType=-1;return true;}

    public int missionType(int i){int[] t={0,1,2,4,3,0,1,2,5,6,4,1,2,3,7,4,6,1};return t[clamp(i,0,t.length-1)];}
    public double missionTarget(int i){double[] v={50,5_000,10,25,5,500,1_000_000,100,200,1,250,1e10,500,100,1,500,10,1e15};return v[clamp(i,0,v.length-1)];}
    public int missionRewardCrystals(int i){return 2+i/2;}
    public int missionRewardResearch(int i){return 1+i/4;}
    public double goalValue(int type){
        switch(type){case 0:return totalTaps;case 1:return lifetimeCoins;case 2:return totalBuildingBuys;case 3:return totalHeroLevels;case 4:return level;case 5:return bestComboTaps;case 6:return bossDefeats;case 7:return rebirths;default:return 0;}
    }
    public boolean missionReady(int i){return i>=0&&i<missionClaimed.length&&!missionClaimed[i]&&goalValue(missionType(i))>=missionTarget(i);}
    public boolean claimMission(int i){if(!missionReady(i))return false;missionClaimed[i]=true;missionsClaimed++;crystals+=missionRewardCrystals(i);researchPoints+=missionRewardResearch(i);return true;}

    public void checkAchievements(){
        double[] goals={10,100,1000,10_000,100_000,1_000_000, 1e4,1e6,1e9,1e12,1e15, 10,100,500,1000, 10,50,100,250,500, 50,250,500,1000, 1,5,10, 1,5,10};
        int[] types={0,0,0,0,0,0, 1,1,1,1,1, 2,2,2,2, 3,3,3,3,3, 5,5,5,5, 6,6,6, 7,7,7};
        for(int i=0;i<achievementUnlocked.length&&i<goals.length;i++)if(!achievementUnlocked[i]&&goalValue(types[i])>=goals[i]){achievementUnlocked[i]=true;achievementsUnlocked++;crystals+=2+i/4;}
    }

    public String goalLabel(int type,double target){
        String n=format(target);String l=language;
        if(type==0)return tr(l,"Kliknij ","Tap ","Toca ","Klepni ","Нажми ","点击 ")+n+tr(l," razy"," times"," veces","×"," раз"," 次");
        if(type==1)return tr(l,"Zdobądź ","Earn ","Gana ","Vydělej ","Заработай ","获得 ")+n+" coins";
        if(type==2)return tr(l,"Kup budynki: ","Buy buildings: ","Compra edificios: ","Kup budovy: ","Купи здания: ","购买建筑：")+n;
        if(type==3)return tr(l,"Ulepsz bohaterów: ","Hero levels: ","Niveles de héroe: ","Úrovně hrdinů: ","Уровни героев: ","英雄等级：")+n;
        if(type==4)return tr(l,"Osiągnij poziom ","Reach level ","Alcanza nivel ","Dosáhni úrovně ","Достигни уровня ","达到等级 ")+((int)target);
        if(type==5)return "Combo: "+((int)target)+" taps";
        if(type==6)return tr(l,"Pokonaj bossów: ","Defeat bosses: ","Derrota jefes: ","Poraz bossy: ","Победи боссов: ","击败首领：")+((int)target);
        return tr(l,"Odrodzenia: ","Rebirths: ","Renacimientos: ","Znovuzrození: ","Перерождения: ","重生：")+((int)target);
    }

    public String format(double v){
        if(Double.isNaN(v)||Double.isInfinite(v))return "0";if(Math.abs(v)<1000)return String.format(Locale.US,v>=100?"%.0f":v>=10?"%.1f":"%.2f",v);
        return ReleaseContent.fmtPlain(v);
    }
    public static String duration(long sec){long h=sec/3600,m=(sec%3600)/60,s=sec%60;if(h>0)return String.format(Locale.US,"%02dh %02dm %02ds",h,m,s);return String.format(Locale.US,"%02dm %02ds",m,s);}

    public static String tr(String lang,String pl,String en,String es,String cs,String ru,String zh){if("en".equals(lang))return en;if("es".equals(lang))return es;if("cs".equals(lang))return cs;if("ru".equals(lang))return ru;if("zh".equals(lang))return zh;return pl;}

    private static double safe(double d){return Double.isNaN(d)||Double.isInfinite(d)||d<0?0:Math.min(1e300,d);}
    private static int clamp(int v,int a,int b){return Math.max(a,Math.min(b,v));}
    private static int countTrue(boolean[] a){int n=0;for(boolean b:a)if(b)n++;return n;}
    private static JSONArray toJson(int[] a){JSONArray j=new JSONArray();for(int x:a)j.put(x);return j;}
    private static JSONArray toJson(boolean[] a){JSONArray j=new JSONArray();for(boolean x:a)j.put(x);return j;}
    private static void readIntArray(JSONArray a,int[] out){if(a==null)return;for(int i=0;i<out.length&&i<a.length();i++)out[i]=Math.max(0,a.optInt(i,0));}
    private static void readBoolArray(JSONArray a,boolean[] out){if(a==null)return;for(int i=0;i<out.length&&i<a.length();i++)out[i]=a.optBoolean(i,false);}
}
