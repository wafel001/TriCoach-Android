package com.pixelempire.clicker;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public final class V3State {
    public static final long OFFLINE_CAP_MS=12L*60L*60L*1000L;
    public static final double OFFLINE_EFFICIENCY=0.20;
    private static final String PREFS="pixel_empire_save_v3";
    private static final String KEY="state";

    public double coins=0,runCoins=0,lifetimeCoins=0,buildXp=0;
    public int level=1,stage=0,crystals=0,science=0,legacyStars=0,rebirths=0;
    public long totalTaps=0,playSeconds=0,lastSaveTime=0,nextEventAt=0;
    public double bestTap=0,maxCps=0,bestComboMultiplier=1.0;
    public int bestLevel=1,eventsCollected=0,missionsClaimed=0;
    public boolean tutorialSeen=false,soundEnabled=true,hapticsEnabled=true,lowPower=false;
    public String language="pl",lastDaily="";
    public int dailyStreak=0;
    public int buyMode=1;

    public int[] buildings=new int[V3Content.buildings().size()];
    public int[] heroes=new int[V3Content.heroes().size()];
    public int[] techLevels=new int[V3Content.techs().size()];
    public boolean[] missionClaimed=new boolean[18];

    public double startupOfflineGain=0;
    public long startupOfflineSeconds=0;

    public int activeEvent=-1;
    public long activeEventUntil=0;
    public long tapBoostUntil=0,cpsBoostUntil=0,xpBoostUntil=0;

    public boolean bossActive=false;
    public double bossHp=0,bossMaxHp=0;
    public long bossEndAt=0;
    public int bossStage=0,bossesDefeated=0;

    private static final int M_TAPS=0,M_COINS=1,M_LEVEL=2,M_BUILDINGS=3,M_HEROES=4,M_COMBO=5,M_REBIRTH=6,M_BOSS=7;
    private static final int[] M_TYPES={M_TAPS,M_COINS,M_BUILDINGS,M_LEVEL,M_HEROES,M_COMBO,M_TAPS,M_COINS,M_BUILDINGS,M_LEVEL,M_BOSS,M_HEROES,M_REBIRTH,M_LEVEL,M_COINS,M_BOSS,M_BUILDINGS,M_LEVEL};
    private static final double[] M_TARGETS={50,5_000,10,10,3,1.5,2_000,2_000_000,100,100,1,25,1,300,1e12,10,600,1000};
    private static final int[] M_CRYSTALS={2,2,2,3,3,4,4,5,5,7,8,8,12,12,15,18,20,40};
    private static final int[] M_SCIENCE={1,1,1,1,1,2,2,2,3,4,4,5,6,8,10,12,15,25};

    public void load(Context c){
        SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        String raw=p.getString(KEY,"");
        if(raw==null||raw.isEmpty()){
            long now=System.currentTimeMillis();lastSaveTime=now;nextEventAt=now+55_000;return;
        }
        try{
            JSONObject o=new JSONObject(raw);
            coins=o.optDouble("coins",0);runCoins=o.optDouble("runCoins",0);lifetimeCoins=o.optDouble("lifetimeCoins",0);buildXp=o.optDouble("xp",0);
            level=Math.max(1,Math.min(V3Content.MAX_LEVEL,o.optInt("level",1)));stage=Math.max(0,Math.min(V3Content.STAGE_COUNT-1,o.optInt("stage",(level-1)/V3Content.LEVELS_PER_STAGE)));
            crystals=o.optInt("crystals",0);science=o.optInt("science",0);legacyStars=o.optInt("legacy",0);rebirths=o.optInt("rebirths",0);
            totalTaps=o.optLong("taps",0);playSeconds=o.optLong("play",0);lastSaveTime=o.optLong("saved",System.currentTimeMillis());nextEventAt=o.optLong("nextEvent",System.currentTimeMillis()+55_000);
            bestTap=o.optDouble("bestTap",0);maxCps=o.optDouble("maxCps",0);bestComboMultiplier=o.optDouble("bestCombo",1);bestLevel=o.optInt("bestLevel",level);eventsCollected=o.optInt("events",0);missionsClaimed=o.optInt("missionCount",0);
            tutorialSeen=o.optBoolean("tutorial",false);soundEnabled=o.optBoolean("sound",true);hapticsEnabled=o.optBoolean("haptics",true);lowPower=o.optBoolean("lowPower",false);language=o.optString("lang","pl");
            lastDaily=o.optString("lastDaily","");dailyStreak=o.optInt("streak",0);buyMode=o.optInt("buyMode",1);
            tapBoostUntil=o.optLong("tapBoost",0);cpsBoostUntil=o.optLong("cpsBoost",0);xpBoostUntil=o.optLong("xpBoost",0);
            bossesDefeated=o.optInt("bosses",0);
            readInts(o.optJSONArray("buildings"),buildings);readInts(o.optJSONArray("heroes"),heroes);readInts(o.optJSONArray("tech"),techLevels);
            JSONArray mm=o.optJSONArray("missions");if(mm!=null)for(int i=0;i<Math.min(mm.length(),missionClaimed.length);i++)missionClaimed[i]=mm.optBoolean(i,false);
        }catch(Exception ignored){}
        applyOffline();
    }

    private static void readInts(JSONArray a,int[] out){if(a==null)return;for(int i=0;i<Math.min(a.length(),out.length);i++)out[i]=Math.max(0,a.optInt(i,0));}
    private static JSONArray ints(int[] a){JSONArray x=new JSONArray();for(int v:a)x.put(v);return x;}
    private static JSONArray bools(boolean[] a){JSONArray x=new JSONArray();for(boolean v:a)x.put(v);return x;}

    public void save(Context c){
        try{
            long now=System.currentTimeMillis();lastSaveTime=now;
            JSONObject o=new JSONObject();
            o.put("coins",coins);o.put("runCoins",runCoins);o.put("lifetimeCoins",lifetimeCoins);o.put("xp",buildXp);o.put("level",level);o.put("stage",stage);
            o.put("crystals",crystals);o.put("science",science);o.put("legacy",legacyStars);o.put("rebirths",rebirths);o.put("taps",totalTaps);o.put("play",playSeconds);o.put("saved",lastSaveTime);o.put("nextEvent",nextEventAt);
            o.put("bestTap",bestTap);o.put("maxCps",maxCps);o.put("bestCombo",bestComboMultiplier);o.put("bestLevel",bestLevel);o.put("events",eventsCollected);o.put("missionCount",missionsClaimed);
            o.put("tutorial",tutorialSeen);o.put("sound",soundEnabled);o.put("haptics",hapticsEnabled);o.put("lowPower",lowPower);o.put("lang",language);o.put("lastDaily",lastDaily);o.put("streak",dailyStreak);o.put("buyMode",buyMode);
            o.put("tapBoost",tapBoostUntil);o.put("cpsBoost",cpsBoostUntil);o.put("xpBoost",xpBoostUntil);o.put("bosses",bossesDefeated);
            o.put("buildings",ints(buildings));o.put("heroes",ints(heroes));o.put("tech",ints(techLevels));o.put("missions",bools(missionClaimed));
            c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,o.toString()).apply();
        }catch(Exception ignored){}
    }

    public void hardReset(Context c){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().apply();}

    private void applyOffline(){
        long now=System.currentTimeMillis();if(lastSaveTime<=0||now<=lastSaveTime)return;
        long elapsed=Math.min(OFFLINE_CAP_MS,now-lastSaveTime);if(elapsed<20_000)return;
        double rate=getCps(false)*OFFLINE_EFFICIENCY;
        if(!(rate>0))return;
        startupOfflineSeconds=elapsed/1000L;
        startupOfflineGain=Math.min(1e300,rate*startupOfflineSeconds);
        addCoins(startupOfflineGain);
    }

    public void tick(double sec){
        if(sec<=0)return;
        long now=System.currentTimeMillis();
        if(activeEvent>=0&&now>=activeEventUntil){activeEvent=-1;activeEventUntil=0;nextEventAt=now+55_000+(long)(Math.random()*65_000);}
        if(activeEvent<0&&now>=nextEventAt){activeEvent=(int)(Math.random()*4);activeEventUntil=now+25_000;}
        if(bossActive&&now>=bossEndAt){bossActive=false;bossHp=0;}
        double cps=getCps();if(cps>0)addCoins(cps*sec);
        double xp=(0.12+Math.pow(Math.max(1,cps),0.29)*0.03)*sec*techMultiplier(3);
        if(now<xpBoostUntil||activeEvent==2)xp*=2.0;
        addXp(xp);
        maxCps=Math.max(maxCps,cps);
    }

    public double getTapPower(){
        double base=1+level*0.09+Math.pow(Math.max(0,totalTaps),0.30)*0.08;
        base*=techMultiplier(0)*techMultiplier(2)*legacyMultiplier();
        if(System.currentTimeMillis()<tapBoostUntil||activeEvent==3)base*=2.0;
        return base;
    }

    public double getCritChance(){return Math.min(.22,.045+techBonus(4));}

    public double tap(double comboMult,boolean critical){
        totalTaps++;bestComboMultiplier=Math.max(bestComboMultiplier,comboMult);
        double amount=getTapPower()*comboMult*(critical?5.0:1.0);
        bestTap=Math.max(bestTap,amount);addCoins(amount);
        double xp=(1+Math.pow(Math.max(1,getTapPower()),0.25)*.25)*comboMult*techMultiplier(3);
        if(System.currentTimeMillis()<xpBoostUntil||activeEvent==2)xp*=2.0;
        addXp(xp);
        if(bossActive){bossHp=Math.max(0,bossHp-amount*2.5);if(bossHp<=0)defeatBoss();}
        return amount;
    }

    public double getCps(){return getCps(true);}
    public double getCps(boolean includeTemporary){
        List<V3Content.BuildingDef>b=V3Content.buildings();double cps=0;
        for(int i=0;i<buildings.length;i++)cps+=buildings[i]*b.get(i).baseCps;
        List<V3Content.HeroDef>h=V3Content.heroes();double hero=0;
        for(int i=0;i<heroes.length;i++)if(heroes[i]>0)hero+=h.get(i).basePower*heroes[i]*Math.pow(1.055,heroes[i]-1);
        hero*=techMultiplier(6);cps+=hero;
        cps*=techMultiplier(1)*techMultiplier(2)*legacyMultiplier();
        if(includeTemporary&&(System.currentTimeMillis()<cpsBoostUntil||activeEvent==0))cps*=2.0;
        return Math.min(1e300,cps);
    }

    private double techBonus(int kind){double v=0;List<V3Content.TechDef>t=V3Content.techs();for(int i=0;i<techLevels.length;i++)if(t.get(i).kind==kind)v+=t.get(i).bonus*techLevels[i];return v;}
    private double techMultiplier(int kind){return 1.0+techBonus(kind);}
    private double legacyMultiplier(){return 1.0+legacyStars*0.025;}

    public void addCoins(double v){if(!(v>0)||Double.isNaN(v)||Double.isInfinite(v))return;coins=Math.min(1e300,coins+v);runCoins=Math.min(1e300,runCoins+v);lifetimeCoins=Math.min(1e300,lifetimeCoins+v);}

    public void addXp(double xp){
        if(!(xp>0)||level>=V3Content.MAX_LEVEL)return;
        buildXp+=xp;
        while(level<V3Content.MAX_LEVEL){double need=V3Content.xpForLevel(level);if(buildXp<need)break;buildXp-=need;level++;bestLevel=Math.max(bestLevel,level);science+=V3Content.scienceRewardForLevel(level);int newStage=(level-1)/V3Content.LEVELS_PER_STAGE;if(newStage>stage){stage=Math.min(V3Content.STAGE_COUNT-1,newStage);crystals+=V3Content.crystalsForStage(stage);}}
    }

    public double levelProgress(){if(level>=V3Content.MAX_LEVEL)return 1;return Math.max(0,Math.min(1,buildXp/V3Content.xpForLevel(level)));}

    public double buildingCost(int i,int count){
        if(i<0||i>=buildings.length)return Double.POSITIVE_INFINITY;V3Content.BuildingDef d=V3Content.buildings().get(i);int lvl=buildings[i];
        if(count<0){int n=maxAffordableBuilding(i);if(n<=0)return Double.POSITIVE_INFINITY;count=n;}
        if(count<=0)return 0;double first=d.baseCost*Math.pow(d.growth,lvl);double sum=first*(Math.pow(d.growth,count)-1)/(d.growth-1);return Math.min(1e300,sum);
    }

    public int maxAffordableBuilding(int i){
        V3Content.BuildingDef d=V3Content.buildings().get(i);if(stage<d.unlockStage)return 0;double first=d.baseCost*Math.pow(d.growth,buildings[i]);if(coins<first)return 0;double n=Math.log(1+coins*(d.growth-1)/first)/Math.log(d.growth);return Math.max(0,Math.min(100000,(int)Math.floor(n+1e-9)));
    }

    public int selectedBuyCount(int i){return buyMode<0?maxAffordableBuilding(i):buyMode;}
    public boolean buyBuilding(int i){
        if(i<0||i>=buildings.length)return false;V3Content.BuildingDef d=V3Content.buildings().get(i);if(stage<d.unlockStage)return false;int n=selectedBuyCount(i);if(n<=0)return false;double cost=buildingCost(i,n);if(!(coins>=cost))return false;coins-=cost;buildings[i]+=n;return true;
    }

    public double heroCost(int i){V3Content.HeroDef h=V3Content.heroes().get(i);return Math.min(1e300,h.baseCost*Math.pow(1.22,heroes[i]));}
    public boolean buyHero(int i){if(i<0||i>=heroes.length)return false;V3Content.HeroDef h=V3Content.heroes().get(i);if(stage<h.unlockStage)return false;double c=heroCost(i);if(coins<c)return false;coins-=c;heroes[i]++;return true;}

    public int techCost(int i){V3Content.TechDef t=V3Content.techs().get(i);return t.baseCost+techLevels[i]*(2+t.baseCost/3);}
    public boolean techUnlocked(int i){V3Content.TechDef t=V3Content.techs().get(i);return stage>=t.unlockStage&&(t.parent<0||techLevels[t.parent]>0);}
    public boolean buyTech(int i){if(i<0||i>=techLevels.length)return false;V3Content.TechDef t=V3Content.techs().get(i);if(!techUnlocked(i)||techLevels[i]>=t.maxLevel)return false;int c=techCost(i);if(science<c)return false;science-=c;techLevels[i]++;return true;}

    public boolean canClaimDaily(){return !LocalDate.now().toString().equals(lastDaily);}
    public int claimDaily(){if(!canClaimDaily())return 0;String today=LocalDate.now().toString();LocalDate prev;try{prev=lastDaily.isEmpty()?null:LocalDate.parse(lastDaily);}catch(Exception e){prev=null;}if(prev!=null&&prev.plusDays(1).toString().equals(today))dailyStreak=Math.min(7,dailyStreak+1);else dailyStreak=1;lastDaily=today;int r=2+dailyStreak*2+(dailyStreak==7?10:0);crystals+=r;science+=Math.max(1,dailyStreak/2);return r;}

    public void collectEvent(){if(activeEvent<0)return;eventsCollected++;long now=System.currentTimeMillis();if(activeEvent==1){crystals+=3+stage/8;science+=1;}else if(activeEvent==0)cpsBoostUntil=Math.max(cpsBoostUntil,now+120_000);else if(activeEvent==2)xpBoostUntil=Math.max(xpBoostUntil,now+120_000);else tapBoostUntil=Math.max(tapBoostUntil,now+120_000);activeEvent=-1;activeEventUntil=0;nextEventAt=now+65_000;}

    public boolean startBoss(){if(stage<2||bossActive)return false;bossStage=stage;bossMaxHp=Math.max(250,getTapPower()*320+getCps(false)*35)*Math.pow(1.12,stage);bossHp=bossMaxHp;bossEndAt=System.currentTimeMillis()+75_000;bossActive=true;return true;}
    private void defeatBoss(){bossActive=false;bossesDefeated++;double reward=Math.max(500,bossMaxHp*2.2);addCoins(reward);int c=3+stage/5;crystals+=c;science+=2+stage/10;}

    public int totalBuildingLevels(){int s=0;for(int x:buildings)s+=x;return s;}
    public int totalHeroLevels(){int s=0;for(int x:heroes)s+=x;return s;}

    public int missionCount(){return M_TYPES.length;}
    public double missionValue(int i){switch(M_TYPES[i]){case M_TAPS:return totalTaps;case M_COINS:return lifetimeCoins;case M_LEVEL:return bestLevel;case M_BUILDINGS:return totalBuildingLevels();case M_HEROES:return totalHeroLevels();case M_COMBO:return bestComboMultiplier;case M_REBIRTH:return rebirths;case M_BOSS:return bossesDefeated;default:return 0;}}
    public double missionTarget(int i){return M_TARGETS[i];}
    public boolean missionReady(int i){return !missionClaimed[i]&&missionValue(i)>=M_TARGETS[i];}
    public boolean claimMission(int i){if(i<0||i>=missionClaimed.length||!missionReady(i))return false;missionClaimed[i]=true;missionsClaimed++;crystals+=M_CRYSTALS[i];science+=M_SCIENCE[i];return true;}
    public int missionCrystalReward(int i){return M_CRYSTALS[i];}
    public int missionScienceReward(int i){return M_SCIENCE[i];}
    public String missionText(int i){switch(M_TYPES[i]){case M_TAPS:return"Kliknij "+format(M_TARGETS[i])+" razy";case M_COINS:return"Zdobądź "+format(M_TARGETS[i])+" monet";case M_LEVEL:return"Osiągnij poziom "+(int)M_TARGETS[i];case M_BUILDINGS:return"Kup "+(int)M_TARGETS[i]+" poziomów budynków";case M_HEROES:return"Ulepsz bohaterów "+(int)M_TARGETS[i]+" razy";case M_COMBO:return"Osiągnij combo x"+String.format(Locale.US,"%.1f",M_TARGETS[i]);case M_REBIRTH:return"Wykonaj Odrodzenie";case M_BOSS:return"Pokonaj "+(int)M_TARGETS[i]+" bossów";default:return"Misja";}}

    public int achievementCount(){int n=0;if(totalTaps>=100)n++;if(totalTaps>=10_000)n++;if(totalTaps>=1_000_000)n++;if(lifetimeCoins>=1e6)n++;if(lifetimeCoins>=1e12)n++;if(bestLevel>=100)n++;if(bestLevel>=500)n++;if(bestLevel>=1000)n++;if(totalBuildingLevels()>=100)n++;if(totalBuildingLevels()>=1000)n++;if(totalHeroLevels()>=50)n++;if(bestComboMultiplier>=2.0)n++;if(rebirths>=1)n++;if(rebirths>=10)n++;if(bossesDefeated>=10)n++;if(stage>=39)n++;return n;}

    public int availableLegacyStars(){if(stage<9)return 0;double a=Math.sqrt(Math.max(0,runCoins)/5e7);double b=(stage+1)*0.9;return Math.max(0,(int)Math.floor(a+b)-legacyStars/3);}
    public boolean rebirth(){int gain=availableLegacyStars();if(gain<=0)return false;legacyStars+=gain;rebirths++;coins=0;runCoins=0;buildXp=0;level=1;stage=0;for(int i=0;i<buildings.length;i++)buildings[i]=0;for(int i=0;i<heroes.length;i++)heroes[i]=0;bossActive=false;return true;}

    public static String formatDuration(long sec){long h=sec/3600,m=(sec%3600)/60,s=sec%60;if(h>0)return String.format(Locale.US,"%02dh %02dm %02ds",h,m,s);if(m>0)return String.format(Locale.US,"%02dm %02ds",m,s);return s+"s";}
    public String format(double v){
        if(Double.isNaN(v)||Double.isInfinite(v))return"0";double a=Math.abs(v);if(a<1000)return a<10?String.format(Locale.US,"%.2f",v):a<100?String.format(Locale.US,"%.1f",v):String.format(Locale.US,"%.0f",v);
        String[] u={"K","M","B","T","Qa","Qi","Sx","Sp","Oc","No","Dc","Ud","Dd","Td","Qad","Qid","Sxd","Spd","Ocd","Nod"};int i=-1;while(a>=1000&&i<u.length-1){a/=1000;v/=1000;i++;}if(i>=0)return String.format(Locale.US,a>=100?"%.0f%s":a>=10?"%.1f%s":"%.2f%s",v,u[i]);return String.format(Locale.US,"%.2e",v);
    }
}
