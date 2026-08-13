package pl.tricoach.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

public final class Store {
    private static final String PREFS = "tricoach";
    private final SharedPreferences p;

    public Store(Context context) {
        p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String language() { return p.getString("language", "pl"); }
    public void setLanguage(String v) { p.edit().putString("language", v).apply(); }

    public String theme() { return p.getString("theme", "dark"); }
    public void setTheme(String v) { p.edit().putString("theme", v).apply(); }

    public boolean notifications() { return p.getBoolean("notifications", true); }
    public void setNotifications(boolean v) { p.edit().putBoolean("notifications", v).apply(); }

    public boolean autoSync() { return p.getBoolean("autoSync", true); }
    public void setAutoSync(boolean v) { p.edit().putBoolean("autoSync", v).apply(); }

    public long lastSync() { return p.getLong("lastSync", 0L); }
    public void setLastSync(long v) { p.edit().putLong("lastSync", v).apply(); }

    public JSONObject dashboard() {
        try { return new JSONObject(p.getString("dashboard", "{}")); }
        catch (Exception e) { return new JSONObject(); }
    }

    public void saveDashboard(JSONObject value) {
        p.edit()
            .putString("dashboard", value == null ? "{}" : value.toString())
            .putLong("lastSync", System.currentTimeMillis())
            .apply();
        if (value != null) appendHistory(value);
    }

    private void appendHistory(JSONObject value) {
        try {
            JSONArray old = new JSONArray(p.getString("history", "[]"));
            JSONArray next = new JSONArray();
            JSONObject snapshot = new JSONObject(value.toString());
            snapshot.put("_savedAt", System.currentTimeMillis());
            next.put(snapshot);
            for (int i = 0; i < old.length() && i < 29; i++) next.put(old.get(i));
            p.edit().putString("history", next.toString()).apply();
        } catch (Exception ignored) {}
    }

    public JSONArray history() {
        try { return new JSONArray(p.getString("history", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }
}
