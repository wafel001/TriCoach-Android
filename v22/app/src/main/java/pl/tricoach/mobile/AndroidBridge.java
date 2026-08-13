package pl.tricoach.mobile;

import android.webkit.JavascriptInterface;
import org.json.JSONObject;

public final class AndroidBridge {
    private final MainActivity activity;
    private final Store store;
    private final SecretStore secrets;

    public AndroidBridge(MainActivity activity) {
        this.activity = activity;
        this.store = new Store(activity);
        this.secrets = new SecretStore(activity);
    }

    @JavascriptInterface
    public String getAppState() {
        JSONObject o = new JSONObject();
        try {
            o.put("language", store.language());
            o.put("theme", store.theme());
            o.put("notifications", store.notifications());
            o.put("hasApiKey", secrets.has());
            JSONObject d = store.dashboard();
            if (d.length() > 0) o.put("dashboard", d);
        } catch (Exception ignored) {}
        return o.toString();
    }

    @JavascriptInterface
    public boolean hasApiKey() { return secrets.has(); }

    @JavascriptInterface
    public void saveApiKey(String key) {
        if (key != null && !key.trim().isEmpty()) secrets.put(key.trim());
    }

    @JavascriptInterface
    public void saveLanguage(String code) {
        if (code != null && code.matches("pl|en|es|cs|ru|zh")) store.setLanguage(code);
    }

    @JavascriptInterface
    public void saveTheme(String theme) {
        if ("dark".equals(theme) || "light".equals(theme)) store.setTheme(theme);
    }

    @JavascriptInterface
    public void saveNotifications(boolean enabled) {
        store.setNotifications(enabled);
        activity.ensureNotificationPermission();
    }

    @JavascriptInterface
    public void syncNow() { activity.syncNow(); }
}
