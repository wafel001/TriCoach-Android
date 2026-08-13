package pl.tricoach.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowInsets;
import android.webkit.*;
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private WebView web;
    private Store store;
    private SecretStore secrets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);
        secrets = new SecretStore(this);

        Window w = getWindow();
        w.setStatusBarColor(Color.rgb(7, 11, 18));
        w.setNavigationBarColor(Color.rgb(7, 11, 18));

        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(7, 11, 18));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setTextZoom(100);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        if (Build.VERSION.SDK_INT >= 23) {
            web.setOnApplyWindowInsetsListener((v, insets) -> {
                int left, top, right, bottom;
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(
                            WindowInsets.Type.statusBars() |
                            WindowInsets.Type.navigationBars() |
                            WindowInsets.Type.displayCutout());
                    left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom;
                } else {
                    left = insets.getSystemWindowInsetLeft();
                    top = insets.getSystemWindowInsetTop();
                    right = insets.getSystemWindowInsetRight();
                    bottom = insets.getSystemWindowInsetBottom();
                }
                v.setPadding(left, top, right, bottom);
                return insets;
            });
        }

        web.addJavascriptInterface(new AndroidBridge(this), "TriCoachNative");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith("file:///android_asset/")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
                return true;
            }
        });

        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
        ensureNotificationPermission();
        JobSchedulerHelper.schedule(this);
    }

    public void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && store.notifications() &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2202);
        }
    }

    public void syncNow() {
        if (!secrets.has()) {
            sendSyncError("Brak klucza Intervals.icu.");
            return;
        }
        new Thread(() -> {
            try {
                JSONObject raw = new IntervalsClient(secrets.get()).snapshot();
                JSONObject dashboard = DashboardBuilder.build(raw, store.language());
                store.saveDashboard(dashboard);
                JSONObject result = new JSONObject();
                result.put("ok", true);
                result.put("dashboard", dashboard);
                runOnUiThread(() -> eval("window.onNativeSync(" + result + ")"));
            } catch (Exception e) {
                sendSyncError(e.getMessage() == null ? "Sync error" : e.getMessage());
            }
        }, "tricoach-manual-sync").start();
    }

    private void sendSyncError(String message) {
        runOnUiThread(() -> {
            JSONObject r = new JSONObject();
            try { r.put("ok", false); r.put("error", message); } catch (Exception ignored) {}
            eval("window.onNativeSync(" + r + ")");
        });
    }

    private void eval(String js) {
        if (web != null) web.evaluateJavascript(js, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null && store.dashboard().length() > 0) {
            JSONObject r = new JSONObject();
            try {
                r.put("ok", true);
                r.put("dashboard", store.dashboard());
                eval("window.onNativeSync(" + r + ")");
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
