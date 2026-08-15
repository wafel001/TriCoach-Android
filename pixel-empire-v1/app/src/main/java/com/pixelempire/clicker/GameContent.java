package com.pixelempire.clicker;

import java.util.ArrayList;
import java.util.List;

public final class GameContent {
    private GameContent() {}

    public static final int STAGE_COUNT = 24;
    public static final int LEVELS_PER_STAGE = 12;
    public static final int MAX_LEVEL = STAGE_COUNT * LEVELS_PER_STAGE;

    public static final class UpgradeDef {
        public final String id;
        public final boolean tap;
        public final double value;
        public final double baseCost;
        public final double growth;
        public final int unlockStage;
        UpgradeDef(String id, boolean tap, double value, double baseCost, double growth, int unlockStage) {
            this.id=id; this.tap=tap; this.value=value; this.baseCost=baseCost; this.growth=growth; this.unlockStage=unlockStage;
        }
    }

    public static final class ResearchDef {
        public final String id;
        public final int kind; // 0 tap, 1 auto, 2 all, 3 build xp
        public final double bonus;
        public final int cost;
        public final int unlockStage;
        ResearchDef(String id, int kind, double bonus, int cost, int unlockStage) {
            this.id=id; this.kind=kind; this.bonus=bonus; this.cost=cost; this.unlockStage=unlockStage;
        }
    }

    public static List<UpgradeDef> upgrades() {
        ArrayList<UpgradeDef> u = new ArrayList<>();
        u.add(new UpgradeDef("hands", true, 1, 12, 1.34, 0));
        u.add(new UpgradeDef("tools", true, 5, 85, 1.38, 0));
        u.add(new UpgradeDef("hammer", true, 28, 650, 1.42, 1));
        u.add(new UpgradeDef("blueprint", true, 170, 5_200, 1.46, 3));
        u.add(new UpgradeDef("crew", false, 1, 35, 1.35, 0));
        u.add(new UpgradeDef("workshop", false, 6, 210, 1.39, 0));
        u.add(new UpgradeDef("mill", false, 35, 1_450, 1.43, 1));
        u.add(new UpgradeDef("quarry", false, 210, 9_500, 1.47, 3));
        u.add(new UpgradeDef("foundry", false, 1_250, 65_000, 1.51, 5));
        u.add(new UpgradeDef("factory", false, 7_800, 450_000, 1.55, 8));
        u.add(new UpgradeDef("robots", false, 48_000, 3_200_000, 1.58, 11));
        u.add(new UpgradeDef("nanites", false, 310_000, 24_000_000, 1.61, 14));
        u.add(new UpgradeDef("logistics", false, 1_900_000, 170_000_000, 1.64, 9));
        u.add(new UpgradeDef("drones", false, 12_000_000, 1_300_000_000L, 1.67, 12));
        u.add(new UpgradeDef("ai", false, 78_000_000, 10_500_000_000L, 1.70, 15));
        u.add(new UpgradeDef("fusion", false, 520_000_000, 90_000_000_000L, 1.73, 18));
        u.add(new UpgradeDef("orbital", false, 3_600_000_000L, 820_000_000_000L, 1.76, 20));
        u.add(new UpgradeDef("quantum", false, 28_000_000_000L, 8_500_000_000_000L, 1.79, 22));
        return u;
    }

    public static List<ResearchDef> research() {
        ArrayList<ResearchDef> r = new ArrayList<>();
        r.add(new ResearchDef("r1",0,0.15,2,0));
        r.add(new ResearchDef("r2",3,0.20,3,1));
        r.add(new ResearchDef("r3",2,0.10,4,2));
        r.add(new ResearchDef("r4",1,0.20,5,3));
        r.add(new ResearchDef("r5",2,0.15,7,5));
        r.add(new ResearchDef("r6",1,0.30,9,7));
        r.add(new ResearchDef("r7",0,0.35,12,9));
        r.add(new ResearchDef("r8",3,0.40,15,11));
        r.add(new ResearchDef("r9",2,0.25,18,13));
        r.add(new ResearchDef("r10",1,0.45,22,15));
        r.add(new ResearchDef("r11",0,0.55,28,17));
        r.add(new ResearchDef("r12",3,0.60,35,19));
        r.add(new ResearchDef("r13",2,0.40,45,20));
        r.add(new ResearchDef("r14",1,0.70,58,21));
        r.add(new ResearchDef("r15",0,0.85,75,22));
        r.add(new ResearchDef("r16",2,0.65,100,23));
        return r;
    }

    public static double xpForLevel(int level) {
        int l = Math.max(1, level);
        double stage = (l - 1) / (double)LEVELS_PER_STAGE;
        return 50.0 * Math.pow(1.125, l - 1) * Math.pow(1.22, stage);
    }

    public static double stageVisualPower(int stage) {
        return Math.pow(1.14, Math.max(0, stage));
    }

    public static int researchRewardForLevel(int level) {
        if (level <= 0) return 0;
        int reward = 0;
        if (level % 3 == 0) reward++;
        if (level % 12 == 0) reward += 2;
        if (level % 48 == 0) reward += 3;
        return reward;
    }

    public static int crystalsForNewStage(int stage) {
        return 2 + stage / 2;
    }
}
