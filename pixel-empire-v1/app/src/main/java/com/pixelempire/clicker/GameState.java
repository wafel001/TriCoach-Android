package com.pixelempire.clicker;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GameState {
    public static final long OFFLINE_CAP_MS = 12L * 60L * 60L * 1000L;
    public static final double OFFLINE_EFFICIENCY = 0.75;
    private static final String PREFS = "pixel_empire_save_v1";
    private static final String KEY_JSON = "state";

    public static final class Upgrade {
        public final String id;
        public final String name;
        public final String description;
        public final double baseCost;
        public final double growth;
        public final double value;
        public final boolean click;
        public int level;

        Upgrade(String id, String name, String description, double baseCost, double growth,
                double value, boolean click) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.baseCost = baseCost;
            this.growth = growth;
            this.value = value;
            this.click = click;
        }

        public double cost() {
            return baseCost * Math.pow(growth, level);
        }
    }

    public static final class Achievement {
        public final String id;
        public final String title;
        public final String description;
        public final int gemReward;
        public boolean unlocked;

        Achievement(String id, String title, String description, int gemReward) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.gemReward = gemReward;
        }
    }

    public double coins = 0;
    public double runCoinsEarned = 0;
    public double lifetimeCoins = 0;
    public long totalClicks = 0;
    public long playSeconds = 0;
    public double maxCps = 0;
    public double bestTap = 0;
    public int bestCombo = 0;
    public int eventsCollected = 0;
    public int prestigeCount = 0;
    public int prestigeCores = 0;
    public int gems = 0;

    public long turboUntil = 0;
    public long frenzyUntil = 0;
    public long lastSaveTime = 0;
    public long lastTickTime = 0;
    public long nextEventAt = 0;
    public int activeEventType = -1;
    public long activeEventUntil = 0;

    public int dailyStreak = 0;
    public String lastDailyClaim = "";
    public boolean tutorialSeen = false;
    public boolean soundEnabled = true;
    public boolean hapticsEnabled = true;
    public boolean scientificNotation = false;

    public double startupOfflineGain = 0;
    public long startupOfflineSeconds = 0;

    public final LinkedHashMap<String, Upgrade> upgrades = new LinkedHashMap<>();
    public final List<Achievement> achievements = new ArrayList<>();
    public final List<Achievement> justUnlocked = new ArrayList<>();

    public GameState() {
        addUpgrade("finger", "Mocniejszy palec", "+1 / klik", 15, 1.48, 1, true);
        addUpgrade("glove", "Pixelowa rękawica", "+5 / klik", 120, 1.54, 5, true);
        addUpgrade("drill", "Wiertło kwantowe", "+25 / klik", 950, 1.58, 25, true);
        addUpgrade("worker", "Pomocnik", "+1 / sek.", 50, 1.47, 1, false);
        addUpgrade("mine", "Kopalnia", "+8 / sek.", 350, 1.52, 8, false);
        addUpgrade("factory", "Fabryka", "+50 / sek.", 2600, 1.57, 50, false);
        addUpgrade("bank", "Bank imperium", "+250 / sek.", 18500, 1.61, 250, false);
        addUpgrade("server", "Centrum danych", "+1500 / sek.", 135000, 1.65, 1500, false);
        addUpgrade("space", "Port orbitalny", "+9000 / sek.", 980000, 1.69, 9000, false);

        addAchievement("first", "Pierwszy piksel", "Kliknij pierwszy raz", 1);
        addAchievement("click100", "Rozgrzewka", "Wykonaj 100 kliknięć", 2);
        addAchievement("click1000", "Maszyna do klikania", "Wykonaj 1 000 kliknięć", 4);
        addAchievement("click10000", "Palec ze stali", "Wykonaj 10 000 kliknięć", 8);
        addAchievement("coins1k", "Pierwszy tysiąc", "Zarób 1 000 monet", 2);
        addAchievement("coins1m", "Milioner", "Zarób 1 000 000 monet", 6);
        addAchievement("coins1b", "Pixelowy magnat", "Zarób 1 000 000 000 monet", 12);
        addAchievement("cps100", "Automatyzacja", "Osiągnij 100 / sek.", 4);
        addAchievement("cps10k", "Przemysł 2.0", "Osiągnij 10 000 / sek.", 10);
        addAchievement("levels50", "Urbanista", "Kup łącznie 50 poziomów", 6);
        addAchievement("combo20", "Combo!", "Osiągnij combo x20", 4);
        addAchievement("event5", "Łowca okazji", "Zbierz 5 losowych eventów", 5);
        addAchievement("prestige1", "Nowe imperium", "Wykonaj pierwszy Prestige", 8);
        addAchievement("cores10", "Rdzeń mocy", "Zdobądź 10 Rdzeni Imperium", 10);
        addAchievement("hour", "Stały bywalec", "Spędź w grze godzinę", 5);
        addAchievement("gems50", "Kolekcjoner", "Posiadaj 50 klejnotów", 8);
    }

    private void addUpgrade(String id, String name, String desc, double baseCost, double growth,
                            double value, boolean click) {
        upgrades.put(id, new Upgrade(id, name, desc, baseCost, growth, value, click));
    }

    private void addAchievement(String id, String title, String desc, int reward) {
        achievements.add(new Achievement(id, title, desc, reward));
    }

    public void load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_JSON, "");
        if (raw == null || raw.isEmpty()) {
            long now = System.currentTimeMillis();
            lastSaveTime = now;
            nextEventAt = now + 60000;
            return;
        }
        try {
            JSONObject o = new JSONObject(raw);
            coins = o.optDouble("coins", 0);
            runCoinsEarned = o.optDouble("runCoinsEarned", 0);
            lifetimeCoins = o.optDouble("lifetimeCoins", 0);
            totalClicks = o.optLong("totalClicks", 0);
            playSeconds = o.optLong("playSeconds", 0);
            maxCps = o.optDouble("maxCps", 0);
            bestTap = o.optDouble("bestTap", 0);
            bestCombo = o.optInt("bestCombo", 0);
            eventsCollected = o.optInt("eventsCollected", 0);
            prestigeCount = o.optInt("prestigeCount", 0);
            prestigeCores = o.optInt("prestigeCores", 0);
            gems = o.optInt("gems", 0);
            turboUntil = o.optLong("turboUntil", 0);
            frenzyUntil = o.optLong("frenzyUntil", 0);
            lastSaveTime = o.optLong("lastSaveTime", System.currentTimeMillis());
            nextEventAt = o.optLong("nextEventAt", System.currentTimeMillis() + 60000);
            activeEventType = -1;
            activeEventUntil = 0;
            dailyStreak = o.optInt("dailyStreak", 0);
            lastDailyClaim = o.optString("lastDailyClaim", "");
            tutorialSeen = o.optBoolean("tutorialSeen", false);
            soundEnabled = o.optBoolean("soundEnabled", true);
            hapticsEnabled = o.optBoolean("hapticsEnabled", true);
            scientificNotation = o.optBoolean("scientificNotation", false);

            JSONObject levels = o.optJSONObject("upgrades");
            if (levels != null) {
                for (Map.Entry<String, Upgrade> entry : upgrades.entrySet()) {
                    entry.getValue().level = Math.max(0, levels.optInt(entry.getKey(), 0));
                }
            }
            JSONArray unlocked = o.optJSONArray("achievements");
            if (unlocked != null) {
                for (int i = 0; i < unlocked.length(); i++) {
                    String id = unlocked.optString(i, "");
                    for (Achievement a : achievements) {
                        if (a.id.equals(id)) {
                            a.unlocked = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        applyOfflineIncome();
        checkAchievements();
    }

    public void save(Context context) {
        try {
            long now = System.currentTimeMillis();
            lastSaveTime = now;
            JSONObject o = new JSONObject();
            o.put("coins", coins);
            o.put("runCoinsEarned", runCoinsEarned);
            o.put("lifetimeCoins", lifetimeCoins);
            o.put("totalClicks", totalClicks);
            o.put("playSeconds", playSeconds);
            o.put("maxCps", maxCps);
            o.put("bestTap", bestTap);
            o.put("bestCombo", bestCombo);
            o.put("eventsCollected", eventsCollected);
            o.put("prestigeCount", prestigeCount);
            o.put("prestigeCores", prestigeCores);
            o.put("gems", gems);
            o.put("turboUntil", turboUntil);
            o.put("frenzyUntil", frenzyUntil);
            o.put("lastSaveTime", lastSaveTime);
            o.put("nextEventAt", nextEventAt);
            o.put("dailyStreak", dailyStreak);
            o.put("lastDailyClaim", lastDailyClaim);
            o.put("tutorialSeen", tutorialSeen);
            o.put("soundEnabled", soundEnabled);
            o.put("hapticsEnabled", hapticsEnabled);
            o.put("scientificNotation", scientificNotation);

            JSONObject levels = new JSONObject();
            for (Map.Entry<String, Upgrade> entry : upgrades.entrySet()) {
                levels.put(entry.getKey(), entry.getValue().level);
            }
            o.put("upgrades", levels);

            JSONArray unlocked = new JSONArray();
            for (Achievement a : achievements) if (a.unlocked) unlocked.put(a.id);
            o.put("achievements", unlocked);

            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(KEY_JSON, o.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public void hardReset(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    private void applyOfflineIncome() {
        long now = System.currentTimeMillis();
        if (lastSaveTime <= 0 || now <= lastSaveTime) return;
        long elapsed = Math.min(OFFLINE_CAP_MS, now - lastSaveTime);
        if (elapsed < 15000) return;
        double baseCps = getBaseCpsWithPrestige();
        if (baseCps <= 0) return;
        startupOfflineSeconds = elapsed / 1000L;
        startupOfflineGain = baseCps * (elapsed / 1000.0) * OFFLINE_EFFICIENCY;
        addCoins(startupOfflineGain);
    }

    public void tick(double seconds) {
        if (seconds <= 0) return;
        double gain = getCps() * seconds;
        if (gain > 0) addCoins(gain);
        maxCps = Math.max(maxCps, getCps());
    }

    public double tap(int combo, boolean critical) {
        totalClicks++;
        bestCombo = Math.max(bestCombo, combo);
        double comboMult = 1.0 + Math.min(40, Math.max(0, combo - 1)) * 0.025;
        double amount = getClickValue() * comboMult * (critical ? 10.0 : 1.0);
        bestTap = Math.max(bestTap, amount);
        addCoins(amount);
        checkAchievements();
        return amount;
    }

    public void addCoins(double amount) {
        if (!(amount > 0) || Double.isInfinite(amount) || Double.isNaN(amount)) return;
        coins += amount;
        runCoinsEarned += amount;
        lifetimeCoins += amount;
        if (coins > 1e300) coins = 1e300;
        if (runCoinsEarned > 1e300) runCoinsEarned = 1e300;
        if (lifetimeCoins > 1e300) lifetimeCoins = 1e300;
    }

    public double prestigeMultiplier() {
        return 1.0 + prestigeCores * 0.10;
    }

    public double getClickValue() {
        double value = 1.0;
        for (Upgrade u : upgrades.values()) if (u.click) value += u.level * u.value;
        value *= prestigeMultiplier();
        if (System.currentTimeMillis() < frenzyUntil) value *= 3.0;
        return value;
    }

    private double getBaseCpsWithPrestige() {
        double value = 0;
        for (Upgrade u : upgrades.values()) if (!u.click) value += u.level * u.value;
        return value * prestigeMultiplier();
    }

    public double getCps() {
        double value = getBaseCpsWithPrestige();
        if (System.currentTimeMillis() < turboUntil) value *= 2.0;
        return value;
    }

    public boolean buyUpgrade(String id) {
        Upgrade u = upgrades.get(id);
        if (u == null) return false;
        double cost = u.cost();
        if (coins + 1e-9 < cost) return false;
        coins -= cost;
        u.level++;
        checkAchievements();
        return true;
    }

    public int totalUpgradeLevels() {
        int total = 0;
        for (Upgrade u : upgrades.values()) total += u.level;
        return total;
    }

    public int availablePrestigeCores() {
        if (runCoinsEarned < 1_000_000) return 0;
        return Math.max(1, (int) Math.floor(Math.sqrt(runCoinsEarned / 1_000_000.0)));
    }

    public boolean prestige() {
        int gain = availablePrestigeCores();
        if (gain <= 0) return false;
        prestigeCores += gain;
        prestigeCount++;
        gems += Math.max(1, gain / 2);
        coins = 0;
        runCoinsEarned = 0;
        for (Upgrade u : upgrades.values()) u.level = 0;
        turboUntil = 0;
        frenzyUntil = 0;
        checkAchievements();
        return true;
    }

    public boolean buyTurbo() {
        if (gems < 10) return false;
        gems -= 10;
        turboUntil = Math.max(System.currentTimeMillis(), turboUntil) + 5L * 60L * 1000L;
        return true;
    }

    public boolean buyFrenzy() {
        if (gems < 8) return false;
        gems -= 8;
        frenzyUntil = Math.max(System.currentTimeMillis(), frenzyUntil) + 2L * 60L * 1000L;
        return true;
    }

    public boolean canClaimDaily() {
        return !today().equals(lastDailyClaim);
    }

    public int claimDaily() {
        if (!canClaimDaily()) return 0;
        LocalDate now = LocalDate.now();
        try {
            if (!lastDailyClaim.isEmpty()) {
                LocalDate last = LocalDate.parse(lastDailyClaim, DateTimeFormatter.ISO_LOCAL_DATE);
                if (last.plusDays(1).equals(now)) dailyStreak = Math.min(7, dailyStreak + 1);
                else dailyStreak = 1;
            } else dailyStreak = 1;
        } catch (Exception e) {
            dailyStreak = 1;
        }
        lastDailyClaim = today();
        double coinReward = Math.max(100, getBaseCpsWithPrestige() * (120 + dailyStreak * 60));
        coinReward += Math.pow(10, Math.min(8, dailyStreak));
        addCoins(coinReward);
        int gemReward = dailyStreak == 7 ? 10 : (dailyStreak >= 4 ? 2 : 1);
        gems += gemReward;
        checkAchievements();
        return gemReward;
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public void collectEvent(int type) {
        eventsCollected++;
        if (type == 0) {
            addCoins(Math.max(250, getCps() * 90 + getClickValue() * 60));
        } else if (type == 1) {
            gems += 2 + (eventsCollected % 3 == 0 ? 1 : 0);
        } else {
            turboUntil = Math.max(System.currentTimeMillis(), turboUntil) + 60_000L;
        }
        activeEventType = -1;
        activeEventUntil = 0;
        nextEventAt = System.currentTimeMillis() + 45_000L + (eventsCollected % 5) * 13_000L;
        checkAchievements();
    }

    public void checkAchievements() {
        unlock("first", totalClicks >= 1);
        unlock("click100", totalClicks >= 100);
        unlock("click1000", totalClicks >= 1000);
        unlock("click10000", totalClicks >= 10000);
        unlock("coins1k", lifetimeCoins >= 1_000);
        unlock("coins1m", lifetimeCoins >= 1_000_000);
        unlock("coins1b", lifetimeCoins >= 1_000_000_000L);
        unlock("cps100", getCps() >= 100);
        unlock("cps10k", getCps() >= 10_000);
        unlock("levels50", totalUpgradeLevels() >= 50);
        unlock("combo20", bestCombo >= 20);
        unlock("event5", eventsCollected >= 5);
        unlock("prestige1", prestigeCount >= 1);
        unlock("cores10", prestigeCores >= 10);
        unlock("hour", playSeconds >= 3600);
        unlock("gems50", gems >= 50);
    }

    private void unlock(String id, boolean condition) {
        if (!condition) return;
        for (Achievement a : achievements) {
            if (a.id.equals(id) && !a.unlocked) {
                a.unlocked = true;
                gems += a.gemReward;
                justUnlocked.add(a);
                return;
            }
        }
    }

    public int unlockedAchievementCount() {
        int n = 0;
        for (Achievement a : achievements) if (a.unlocked) n++;
        return n;
    }

    public int cityTier() {
        double score = lifetimeCoins + totalUpgradeLevels() * 1000.0 + prestigeCores * 100000.0;
        if (score >= 1e9) return 5;
        if (score >= 1e7) return 4;
        if (score >= 250_000) return 3;
        if (score >= 15_000) return 2;
        if (score >= 800) return 1;
        return 0;
    }

    public String format(double value) {
        if (Double.isNaN(value)) return "0";
        if (scientificNotation && Math.abs(value) >= 1_000_000) {
            return String.format(Locale.US, "%.2e", value);
        }
        double a = Math.abs(value);
        String[] suffix = {"", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};
        int idx = 0;
        while (a >= 1000 && idx < suffix.length - 1) {
            a /= 1000.0;
            value /= 1000.0;
            idx++;
        }
        if (idx == 0) {
            if (a < 100) return String.format(Locale.US, "%.1f", value);
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, a >= 100 ? "%.0f%s" : a >= 10 ? "%.1f%s" : "%.2f%s", value, suffix[idx]);
    }

    public static String formatDuration(long seconds) {
        if (seconds < 60) return seconds + " s";
        long m = seconds / 60;
        if (m < 60) return m + " min";
        long h = m / 60;
        long rm = m % 60;
        return h + " h " + rm + " min";
    }
}
