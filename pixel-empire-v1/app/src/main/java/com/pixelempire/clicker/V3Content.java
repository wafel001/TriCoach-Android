package com.pixelempire.clicker;

import java.util.ArrayList;
import java.util.List;

public final class V3Content {
    private V3Content() {}

    public static final int STAGE_COUNT = 40;
    public static final int LEVELS_PER_STAGE = 25;
    public static final int MAX_LEVEL = STAGE_COUNT * LEVELS_PER_STAGE;

    public static final class BuildingDef {
        public final String id;
        public final double baseCps;
        public final double baseCost;
        public final double growth;
        public final int unlockStage;
        public BuildingDef(String id,double baseCps,double baseCost,double growth,int unlockStage){
            this.id=id;this.baseCps=baseCps;this.baseCost=baseCost;this.growth=growth;this.unlockStage=unlockStage;
        }
    }

    public static final class HeroDef {
        public final String id;
        public final String role;
        public final double basePower;
        public final double baseCost;
        public final int unlockStage;
        public final int rarity;
        public HeroDef(String id,String role,double basePower,double baseCost,int unlockStage,int rarity){
            this.id=id;this.role=role;this.basePower=basePower;this.baseCost=baseCost;this.unlockStage=unlockStage;this.rarity=rarity;
        }
    }

    public static final class TechDef {
        public final String id;
        public final int kind;
        public final double bonus;
        public final int maxLevel;
        public final int baseCost;
        public final int unlockStage;
        public final int parent;
        public TechDef(String id,int kind,double bonus,int maxLevel,int baseCost,int unlockStage,int parent){
            this.id=id;this.kind=kind;this.bonus=bonus;this.maxLevel=maxLevel;this.baseCost=baseCost;this.unlockStage=unlockStage;this.parent=parent;
        }
    }

    private static final String[] STAGES = {
        "Schronienie z gałęzi","Leśny szałas","Drewniana chatka","Obozowisko","Gospodarstwo",
        "Mała osada","Wioska","Miasteczko","Kamienna warownia","Zamek",
        "Wielki zamek","Królewska cytadela","Miasto kupieckie","Port rzeczny","Miasto manufaktur",
        "Rewolucja przemysłowa","Metropolia parowa","Miasto elektryczne","Megafabryka","Stalowa metropolia",
        "Miasto neonów","Cyberdzielnica","Arcologia","Megamiasto","Miasto w chmurach",
        "Stacja stratosferyczna","Port orbitalny","Kolonia księżycowa","Miasto marsjańskie","Pierścień orbitalny",
        "Megastruktura","Miasto kwantowe","Cytadela AI","Świat nanotechnologii","Wrota międzygwiezdne",
        "Kolonia gwiezdna","Pierścień planetarny","Sfera energetyczna","Kwantowe imperium","Wieża Nieskończoności"
    };

    public static String stageName(int stage){
        return STAGES[Math.max(0,Math.min(STAGES.length-1,stage))];
    }

    public static List<BuildingDef> buildings(){
        ArrayList<BuildingDef> b=new ArrayList<>();
        b.add(new BuildingDef("gatherer",0.6,10,1.145,0));
        b.add(new BuildingDef("woodcutter",3.2,55,1.15,0));
        b.add(new BuildingDef("farm",14,310,1.155,1));
        b.add(new BuildingDef("quarry",72,1_900,1.16,3));
        b.add(new BuildingDef("caravan",420,13_000,1.165,5));
        b.add(new BuildingDef("market",2_600,92_000,1.17,7));
        b.add(new BuildingDef("forge",16_000,680_000,1.175,9));
        b.add(new BuildingDef("workshop",100_000,5_200_000,1.18,12));
        b.add(new BuildingDef("factory",720_000,42_000_000,1.185,15));
        b.add(new BuildingDef("powerplant",5_500_000,360_000_000,1.19,17));
        b.add(new BuildingDef("datacenter",46_000_000,3_400_000_000L,1.195,20));
        b.add(new BuildingDef("robotics",410_000_000,36_000_000_000L,1.20,23));
        b.add(new BuildingDef("orbital",4_200_000_000L,440_000_000_000L,1.205,26));
        b.add(new BuildingDef("quantum",52_000_000_000L,6_000_000_000_000L,1.21,31));
        b.add(new BuildingDef("singularity",900_000_000_000L,120_000_000_000_000L,1.215,36));
        return b;
    }

    public static List<HeroDef> heroes(){
        ArrayList<HeroDef> h=new ArrayList<>();
        h.add(new HeroDef("mira","Builder",4,120,0,0));
        h.add(new HeroDef("borin","Foreman",18,950,2,0));
        h.add(new HeroDef("luna","Scout",90,8_000,4,1));
        h.add(new HeroDef("roland","Knight",520,72_000,7,1));
        h.add(new HeroDef("ada","Engineer",3_800,680_000,11,2));
        h.add(new HeroDef("marco","Merchant",29_000,6_800_000,14,2));
        h.add(new HeroDef("ignis","Inventor",240_000,76_000_000,17,2));
        h.add(new HeroDef("tink","Cyber Tech",2_200_000,920_000_000,20,3));
        h.add(new HeroDef("nova","Pilot",22_000_000,12_000_000_000L,25,3));
        h.add(new HeroDef("aion","AI Architect",260_000_000,180_000_000_000L,30,4));
        h.add(new HeroDef("astra","Star Warden",3_600_000_000L,3_200_000_000_000L,34,4));
        h.add(new HeroDef("omega","Infinity Core",60_000_000_000L,70_000_000_000_000L,38,5));
        return h;
    }

    public static List<TechDef> techs(){
        ArrayList<TechDef> t=new ArrayList<>();
        t.add(new TechDef("tools",0,.10,5,2,0,-1));
        t.add(new TechDef("gathering",1,.10,5,2,0,-1));
        t.add(new TechDef("planning",3,.12,5,3,1,0));
        t.add(new TechDef("trade",2,.08,5,4,3,1));
        t.add(new TechDef("metal",1,.14,5,5,6,2));
        t.add(new TechDef("architecture",0,.14,5,5,8,2));
        t.add(new TechDef("guilds",6,.10,5,7,10,3));
        t.add(new TechDef("steam",1,.18,5,8,14,4));
        t.add(new TechDef("electricity",2,.12,5,10,17,5));
        t.add(new TechDef("logistics",1,.18,5,12,18,7));
        t.add(new TechDef("computers",2,.14,5,14,20,8));
        t.add(new TechDef("automation",6,.14,5,16,22,9));
        t.add(new TechDef("fusion",1,.22,5,18,25,10));
        t.add(new TechDef("nanites",0,.25,5,20,27,11));
        t.add(new TechDef("ai",6,.22,5,24,29,11));
        t.add(new TechDef("orbital",2,.20,5,28,32,12));
        t.add(new TechDef("quantum",2,.25,5,34,35,13));
        t.add(new TechDef("time",5,.05,4,40,36,16));
        t.add(new TechDef("antimatter",1,.35,5,48,37,16));
        t.add(new TechDef("singularity",2,.50,1,90,39,18));
        return t;
    }

    public static double xpForLevel(int level){
        int l=Math.max(1,level);
        double stage=(l-1)/(double)LEVELS_PER_STAGE;
        return 28.0*Math.pow(1.075,l-1)*Math.pow(1.16,stage);
    }

    public static int scienceRewardForLevel(int level){
        int r=0;
        if(level%5==0)r++;
        if(level%25==0)r+=2;
        if(level%100==0)r+=5;
        return r;
    }

    public static int crystalsForStage(int stage){return 2+stage/3;}
}
