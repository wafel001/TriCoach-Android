package com.pixelempire.clicker;

import java.util.Locale;

/** Immutable gameplay content for the Google Play release candidate. */
public final class ReleaseContent {
    private ReleaseContent() {}

    public static final int STAGES = 40;
    public static final int LEVELS_PER_STAGE = 25;
    public static final int MAX_LEVEL = STAGES * LEVELS_PER_STAGE;

    public static final String[] STAGE_PL = {
            "Schronienie z gałęzi","Szałas","Chata drwala","Obozowisko","Mała osada",
            "Wioska","Duża wioska","Osada handlowa","Kamienna osada","Miasteczko",
            "Drewniana forteca","Kamienna forteca","Zamek","Wielki zamek","Królewskie miasto",
            "Stolica","Miasto renesansu","Miasto parowe","Rewolucja przemysłowa","Megafabryka",
            "Miasto elektryczne","Wielka metropolia","Era komputerów","Neonowe miasto","Cybermetropolia",
            "Arcologia","Miasto w chmurach","Port orbitalny","Kolonia księżycowa","Miasto Marsa",
            "Stacja międzyplanetarna","Pierścień orbitalny","Kolonia gwiezdna","Megastruktura","Miasto kwantowe",
            "Cytadela antymaterii","Imperium galaktyczne","Rdzeń osobliwości","Wieża wieczności","Wieża Nieskończoności"
    };
    public static final String[] STAGE_EN = {
            "Branch Shelter","Hut","Lumber Cabin","Camp","Small Settlement","Village","Large Village","Trading Settlement","Stone Settlement","Town",
            "Wooden Fortress","Stone Fortress","Castle","Grand Castle","Royal City","Capital","Renaissance City","Steam City","Industrial Revolution","Megafactory",
            "Electric City","Grand Metropolis","Computer Age","Neon City","Cyber Metropolis","Arcology","Cloud City","Orbital Port","Moon Colony","Mars City",
            "Interplanetary Station","Orbital Ring","Star Colony","Megastructure","Quantum City","Antimatter Citadel","Galactic Empire","Singularity Core","Eternity Spire","Infinity Tower"
    };

    public static String stageName(String lang, int stage) {
        int i = Math.max(0, Math.min(STAGES - 1, stage));
        if ("pl".equals(lang)) return STAGE_PL[i];
        if ("en".equals(lang)) return STAGE_EN[i];
        String prefix = "es".equals(lang) ? "Era " : "cs".equals(lang) ? "Éra " : "ru".equals(lang) ? "Эра " : "zh".equals(lang) ? "时代 " : "Era ";
        return prefix + (i + 1) + " • " + STAGE_EN[i];
    }

    public static final class BuildingDef {
        public final String id, pl, en;
        public final double baseCps, baseCost, growth;
        public final int unlockStage;
        BuildingDef(String id,String pl,String en,double baseCps,double baseCost,double growth,int unlockStage){
            this.id=id;this.pl=pl;this.en=en;this.baseCps=baseCps;this.baseCost=baseCost;this.growth=growth;this.unlockStage=unlockStage;
        }
    }

    public static final BuildingDef[] BUILDINGS = {
            new BuildingDef("worker","Robotnik","Worker",1,15,1.14,0),
            new BuildingDef("camp","Obóz budowniczych","Builder Camp",5,80,1.145,0),
            new BuildingDef("lumber","Tartak","Lumber Mill",24,420,1.15,1),
            new BuildingDef("quarry","Kamieniołom","Quarry",115,2_400,1.155,3),
            new BuildingDef("farm","Farma","Farm",530,13_000,1.16,4),
            new BuildingDef("market","Rynek","Market",2_450,72_000,1.165,6),
            new BuildingDef("forge","Kuźnia","Forge",11_500,390_000,1.17,8),
            new BuildingDef("academy","Akademia","Academy",54_000,2_200_000,1.175,10),
            new BuildingDef("factory","Fabryka","Factory",260_000,12_500_000,1.18,16),
            new BuildingDef("power","Elektrownia","Power Plant",1_250_000,72_000_000,1.185,19),
            new BuildingDef("datacenter","Centrum danych","Data Center",6_200_000,430_000_000,1.19,22),
            new BuildingDef("robots","Fabryka robotów","Robot Factory",31_000_000,2_600_000_000L,1.195,24),
            new BuildingDef("fusion","Reaktor fuzyjny","Fusion Reactor",160_000_000,16_000_000_000L,1.20,27),
            new BuildingDef("orbital","Port orbitalny","Orbital Port",850_000_000,105_000_000_000L,1.205,30),
            new BuildingDef("quantum","Rdzeń kwantowy","Quantum Core",4_800_000_000L,760_000_000_000L,1.21,34)
    };

    public static String buildingName(String lang,int i){
        i=Math.max(0,Math.min(BUILDINGS.length-1,i));
        if("pl".equals(lang)) return BUILDINGS[i].pl;
        return BUILDINGS[i].en;
    }

    public static final class HeroDef {
        public final String name, rarity;
        public final double baseDps, baseCost;
        public final int unlockStage;
        public final int color;
        HeroDef(String name,String rarity,double dps,double cost,int stage,int color){this.name=name;this.rarity=rarity;this.baseDps=dps;this.baseCost=cost;this.unlockStage=stage;this.color=color;}
    }

    public static final HeroDef[] HEROES = {
            new HeroDef("Roland","COMMON",5,600,2,0xff8bc34a),
            new HeroDef("Mia","COMMON",18,2_400,4,0xff4caf50),
            new HeroDef("Ignis","RARE",75,11_000,6,0xff2196f3),
            new HeroDef("Tink","RARE",320,52_000,8,0xff03a9f4),
            new HeroDef("Ariana","EPIC",1_450,260_000,11,0xff9c27b0),
            new HeroDef("Thorvald","EPIC",6_800,1_350_000,14,0xffab47bc),
            new HeroDef("Kael","LEGENDARY",33_000,7_500_000,17,0xffff9800),
            new HeroDef("Luna","LEGENDARY",170_000,44_000_000,20,0xffffb300),
            new HeroDef("Generator-7","MYTHIC",920_000,270_000_000,24,0xffff4081),
            new HeroDef("Nova","MYTHIC",5_200_000,1_750_000_000L,28,0xffe91e63),
            new HeroDef("Orion","CELESTIAL",31_000_000,12_000_000_000L,32,0xff26c6da),
            new HeroDef("Aeternus","INFINITY",210_000_000,95_000_000_000L,36,0xff7c4dff)
    };

    public static final class TechDef {
        public final String id, pl, en;
        public final int kind, max, unlockStage, prereq;
        public final double bonus;
        public final int baseCost;
        TechDef(String id,String pl,String en,int kind,double bonus,int max,int cost,int stage,int prereq){
            this.id=id;this.pl=pl;this.en=en;this.kind=kind;this.bonus=bonus;this.max=max;this.baseCost=cost;this.unlockStage=stage;this.prereq=prereq;
        }
    }
    public static final TechDef[] TECH = {
            new TechDef("tools","Narzędzia","Tools",0,.10,5,2,0,-1),
            new TechDef("wood","Obróbka drewna","Woodworking",1,.08,5,3,0,0),
            new TechDef("stone","Kamieniarstwo","Stonework",1,.10,5,4,2,0),
            new TechDef("farming","Rolnictwo","Farming",2,.06,5,5,4,1),
            new TechDef("smith","Kowalstwo","Smithing",0,.12,5,7,6,2),
            new TechDef("writing","Pismo","Writing",3,.10,5,8,7,3),
            new TechDef("engineering","Inżynieria","Engineering",2,.08,5,10,9,4),
            new TechDef("chemistry","Chemia","Chemistry",1,.13,5,12,12,5),
            new TechDef("steam","Maszyna parowa","Steam Engine",2,.10,5,15,16,6),
            new TechDef("electricity","Elektryczność","Electricity",1,.16,5,18,19,8),
            new TechDef("industry","Przemysł","Industry",2,.12,5,22,20,9),
            new TechDef("computers","Komputery","Computers",3,.15,5,27,22,10),
            new TechDef("ai","Sztuczna inteligencja","Artificial Intelligence",5,.15,5,34,24,11),
            new TechDef("nanotech","Nanotechnologia","Nanotechnology",2,.15,5,42,26,12),
            new TechDef("fusion","Fuzja","Fusion",1,.20,5,52,28,13),
            new TechDef("orbital","Technologie orbitalne","Orbital Tech",5,.20,5,64,30,14),
            new TechDef("quantum","Technologia kwantowa","Quantum Tech",2,.22,5,78,33,15),
            new TechDef("antimatter","Antymateria","Antimatter",0,.30,5,95,35,16),
            new TechDef("singularity","Osobliwość","Singularity",2,.30,5,120,37,17),
            new TechDef("infinity","Nieskończoność","Infinity",2,.50,1,180,39,18)
    };

    public static String techName(String lang,int i){
        i=Math.max(0,Math.min(TECH.length-1,i));
        return "pl".equals(lang)?TECH[i].pl:TECH[i].en;
    }

    public static double xpForLevel(int level){
        int l=Math.max(1,Math.min(MAX_LEVEL,level));
        int stage=(l-1)/LEVELS_PER_STAGE;
        return 75.0*Math.pow(1.030,l-1)*Math.pow(1.18,stage);
    }

    public static double stageMultiplier(int stage){return Math.pow(1.17,Math.max(0,stage));}

    public static String fmtPlain(double v){
        if(v<1000)return String.format(Locale.US,"%.0f",v);
        String[] s={"K","M","B","T","Qa","Qi","Sx","Sp","Oc","No","Dc","Ud","Dd","Td","Qad","Qid"};
        int i=-1;while(Math.abs(v)>=1000&&i<s.length-1){v/=1000;i++;}
        return String.format(Locale.US,v>=100?"%.0f%s":v>=10?"%.1f%s":"%.2f%s",v,s[Math.max(0,i)]);
    }
}
