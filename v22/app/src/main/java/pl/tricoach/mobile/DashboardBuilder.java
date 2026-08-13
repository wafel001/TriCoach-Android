package pl.tricoach.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class DashboardBuilder {
    private DashboardBuilder() {}

    public static JSONObject build(JSONObject raw, String language) {
        JSONArray activities = raw.optJSONArray("activities");
        JSONArray events = raw.optJSONArray("events");
        JSONArray wellness = raw.optJSONArray("wellness");
        if (activities == null) activities = new JSONArray();
        if (events == null) events = new JSONArray();
        if (wellness == null) wellness = new JSONArray();

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(7);

        double weekLoad = 0, prevLoad = 0;
        int completedWeek = 0, sessionCount = 0, totalMinutes = 0;
        int swimMinutes = 0, bikeMinutes = 0, runMinutes = 0;

        for (int i = 0; i < activities.length(); i++) {
            JSONObject a = activities.optJSONObject(i);
            if (a == null) continue;
            LocalDate d = dateOf(a);
            if (d == null) continue;
            double load = number(a, "icu_training_load", "training_load", "load");
            int minutes = durationMinutes(a);
            String sport = a.optString("type", a.optString("sport", ""));

            if (!d.isBefore(today.minusDays(6)) && !d.isAfter(today)) {
                weekLoad += load;
                sessionCount++;
                totalMinutes += minutes;
                if (sport.equalsIgnoreCase("Swim")) swimMinutes += minutes;
                else if (sport.equalsIgnoreCase("Ride") || sport.equalsIgnoreCase("Bike")) bikeMinutes += minutes;
                else if (sport.equalsIgnoreCase("Run")) runMinutes += minutes;
            }
            if (!d.isBefore(today.minusDays(13)) && d.isBefore(today.minusDays(6))) prevLoad += load;
            if (!d.isBefore(weekStart) && d.isBefore(weekEnd)) completedWeek++;
        }

        int plannedWeek = 0;
        JSONObject nextWorkout = null;
        List<JSONObject> future = new ArrayList<>();
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.optJSONObject(i);
            if (e == null || !isWorkout(e)) continue;
            LocalDate d = dateOf(e);
            if (d == null) continue;
            if (!d.isBefore(weekStart) && d.isBefore(weekEnd)) plannedWeek++;
            if (!d.isBefore(today)) future.add(e);
        }
        future.sort(Comparator.comparing(o -> {
            LocalDate d = dateOf(o);
            return d == null ? LocalDate.MAX : d;
        }));
        if (!future.isEmpty()) nextWorkout = workoutJson(future.get(0));

        JSONObject latestWellness = latest(wellness);
        double sleepMinutes = sleepMinutes(latestWellness);
        double hrv = number(latestWellness, "hrv", "hrvRMSSD", "rmssd");
        double restingHr = number(latestWellness, "restingHR", "resting_hr", "restingHr");
        double ctl = number(latestWellness, "ctl", "fitness");
        double atl = number(latestWellness, "atl", "fatigue");
        double form = number(latestWellness, "form", "tsb");
        if (form == 0 && (ctl != 0 || atl != 0)) form = ctl - atl;

        double hrvBase = meanWellness(wellness, "hrv", "hrvRMSSD", "rmssd");
        double rhrBase = meanWellness(wellness, "restingHR", "resting_hr", "restingHr");

        int readiness = 72;
        if (sleepMinutes > 0) {
            if (sleepMinutes >= 450) readiness += 8;
            else if (sleepMinutes < 360) readiness -= 14;
            else if (sleepMinutes < 420) readiness -= 6;
        }
        if (hrv > 0 && hrvBase > 0) {
            double ratio = hrv / hrvBase;
            if (ratio > 1.05) readiness += 7;
            else if (ratio < .90) readiness -= 10;
        }
        if (restingHr > 0 && rhrBase > 0) {
            double delta = restingHr - rhrBase;
            if (delta >= 7) readiness -= 12;
            else if (delta >= 4) readiness -= 6;
            else if (delta <= -3) readiness += 4;
        }
        if (form < -20) readiness -= 10;
        else if (form > -5 && form < 15) readiness += 5;
        readiness = clamp(readiness, 15, 96);

        int recoveryHours = readiness >= 80 ? 2 : readiness >= 65 ? 5 : readiness >= 50 ? 10 : 18;
        double change = prevLoad > 0 ? ((weekLoad - prevLoad) / prevLoad) * 100.0 : 0.0;

        JSONObject out = new JSONObject();
        try {
            out.put("readiness", readiness);
            out.put("readinessLabel", readinessLabel(readiness, language));
            out.put("recoveryHours", recoveryHours);
            out.put("sleepMinutes", Math.round(sleepMinutes));
            out.put("hrv", hrv);
            out.put("restingHr", restingHr);
            out.put("fitness", ctl);
            out.put("fatigue", atl);
            out.put("form", form);
            out.put("weeklyLoad", Math.round(weekLoad));
            out.put("previousWeeklyLoad", Math.round(prevLoad));
            out.put("weeklyChangePct", Math.round(change));
            out.put("completedWeek", completedWeek);
            out.put("plannedWeek", plannedWeek);
            out.put("sessionCount", sessionCount);
            out.put("totalMinutes", totalMinutes);
            out.put("dailyAvgMinutes", sessionCount == 0 ? 0 : totalMinutes / sessionCount);
            out.put("swimMinutes", swimMinutes);
            out.put("bikeMinutes", bikeMinutes);
            out.put("runMinutes", runMinutes);
            if (nextWorkout != null) out.put("nextWorkout", nextWorkout);

            JSONObject coach = coach(readiness, change, form, language);
            out.put("coachTitle", coach.optString("title"));
            out.put("coachBody", coach.optString("body"));
            out.put("syncedAt", System.currentTimeMillis());
        } catch (Exception ignored) {}
        return out;
    }

    private static JSONObject coach(int readiness, double change, double form, String lang) {
        String title, body;
        boolean pl = "pl".equals(lang), en = "en".equals(lang);
        if (readiness < 50) {
            title = pl ? "Dziś bez ego." : en ? "No ego today." : "Recovery first";
            body = pl ? "Gotowość jest niska. Zmniejsz intensywność, zachowaj technikę i nie nadrabiaj planu." : en ? "Readiness is low. Reduce intensity, protect technique and do not chase missed work." : "Readiness is low. Reduce intensity and protect recovery.";
        } else if (change > 25 || form < -20) {
            title = pl ? "Uważaj na dokładanie obciążenia." : en ? "Be careful with extra load." : "Control the load";
            body = pl ? "Obciążenie rośnie szybko. Wykonaj plan, ale nie dokładaj dodatkowej intensywności ani objętości." : en ? "Load is rising quickly. Execute the plan, but do not add extra intensity or volume." : "Load is rising quickly. Keep the session controlled.";
        } else if (readiness >= 80) {
            title = pl ? "Zielone światło." : en ? "Green light." : "Ready";
            body = pl ? "Regeneracja wygląda dobrze. Zrób zaplanowaną jakość, ale nie zamieniaj treningu w test." : en ? "Recovery looks good. Do the planned quality, but do not turn the workout into a test." : "Recovery looks good. Execute the planned quality.";
        } else {
            title = pl ? "Dziś jakościowo, nie siłowo." : en ? "Quality over force today." : "Quality first";
            body = pl ? "Organizm wygląda stabilnie. Zrób plan zgodnie z założeniem i zostaw odrobinę rezerwy." : en ? "Your body looks stable. Execute the plan and leave a little reserve." : "Your body looks stable. Execute the plan with control.";
        }
        JSONObject o = new JSONObject();
        try { o.put("title", title); o.put("body", body); } catch (Exception ignored) {}
        return o;
    }

    private static JSONObject workoutJson(JSONObject e) {
        JSONObject o = new JSONObject();
        try {
            o.put("name", e.optString("name", e.optString("title", "Workout")));
            o.put("sport", e.optString("type", ""));
            o.put("durationMinutes", durationMinutes(e));
            double distance = number(e, "distance");
            if (distance > 0) o.put("distanceKm", distance > 1000 ? distance / 1000.0 : distance);
            String zone = extractZone(e.optString("description", ""));
            if (!zone.isEmpty()) o.put("zone", zone);
            LocalDate d = dateOf(e);
            if (d != null) o.put("date", d.toString());
        } catch (Exception ignored) {}
        return o;
    }

    private static String extractZone(String s) {
        if (s == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\bZ([1-5])\\b", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(s);
        return m.find() ? "Z" + m.group(1) : "";
    }

    private static boolean isWorkout(JSONObject e) {
        String category = e.optString("category", "");
        return category.isEmpty() || category.equalsIgnoreCase("WORKOUT");
    }

    private static JSONObject latest(JSONArray a) {
        JSONObject best = new JSONObject();
        LocalDate bestDate = LocalDate.MIN;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            LocalDate d = dateOf(o);
            if (d != null && !d.isBefore(bestDate)) { bestDate = d; best = o; }
        }
        return best;
    }

    private static double meanWellness(JSONArray a, String... keys) {
        double sum = 0; int n = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            double v = number(o, keys);
            if (v > 0) { sum += v; n++; }
        }
        return n == 0 ? 0 : sum / n;
    }

    private static double sleepMinutes(JSONObject o) {
        double s = number(o, "sleepSecs", "sleep_seconds", "sleepSecsTotal");
        if (s > 0) return s / 60.0;
        double minutes = number(o, "sleepMinutes", "sleep_minutes");
        if (minutes > 0) return minutes;
        double hours = number(o, "sleep", "sleepHours");
        if (hours > 0 && hours < 24) return hours * 60.0;
        return 0;
    }

    private static int durationMinutes(JSONObject o) {
        double min = number(o, "durationMinutes");
        if (min > 0) return (int)Math.round(min);
        double sec = number(o, "moving_time", "elapsed_time", "duration", "durationSeconds");
        return sec > 0 ? (int)Math.round(sec / 60.0) : 0;
    }

    private static double number(JSONObject o, String... keys) {
        if (o == null) return 0;
        for (String k : keys) {
            Object v = o.opt(k);
            if (v instanceof Number) return ((Number)v).doubleValue();
            if (v instanceof String) {
                try { return Double.parseDouble(((String)v).replace(',', '.')); } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private static LocalDate dateOf(JSONObject o) {
        if (o == null) return null;
        for (String k : new String[]{"start_date_local", "start_date", "date", "idate", "start"}) {
            String s = o.optString(k, "");
            if (s.isEmpty()) continue;
            try { if (s.length() >= 10) return LocalDate.parse(s.substring(0, 10)); }
            catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static String readinessLabel(int r, String lang) {
        if ("pl".equals(lang)) return r >= 80 ? "Bardzo dobra" : r >= 65 ? "Dobra" : r >= 50 ? "Średnia" : "Niska";
        if ("en".equals(lang)) return r >= 80 ? "Very good" : r >= 65 ? "Good" : r >= 50 ? "Moderate" : "Low";
        if ("es".equals(lang)) return r >= 80 ? "Muy buena" : r >= 65 ? "Buena" : r >= 50 ? "Media" : "Baja";
        if ("cs".equals(lang)) return r >= 80 ? "Velmi dobrá" : r >= 65 ? "Dobrá" : r >= 50 ? "Střední" : "Nízká";
        if ("ru".equals(lang)) return r >= 80 ? "Очень высокая" : r >= 65 ? "Хорошая" : r >= 50 ? "Средняя" : "Низкая";
        if ("zh".equals(lang)) return r >= 80 ? "很好" : r >= 65 ? "良好" : r >= 50 ? "一般" : "较低";
        return r >= 65 ? "Good" : "Low";
    }
}
