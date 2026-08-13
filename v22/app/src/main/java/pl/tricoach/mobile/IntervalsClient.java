package pl.tricoach.mobile;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public final class IntervalsClient {
    private static final String BASE = "https://intervals.icu/api/v1";
    private final String key;

    public IntervalsClient(String key) {
        this.key = key == null ? "" : key.trim();
    }

    private Object get(String path) throws Exception {
        URL url = new URL(BASE + path);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(18000);
        c.setRequestMethod("GET");
        c.setRequestProperty("Accept", "application/json");
        String auth = "API_KEY:" + key;
        c.setRequestProperty("Authorization", "Basic " +
                Base64.encodeToString(auth.getBytes(StandardCharsets.US_ASCII), Base64.NO_WRAP));
        c.setRequestProperty("User-Agent", "TriCoach-Android/2.2");

        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        String body = readAll(in);
        c.disconnect();
        if (code < 200 || code >= 300) {
            String shortBody = body == null ? "" : body;
            if (shortBody.length() > 220) shortBody = shortBody.substring(0, 220);
            throw new IllegalStateException("Intervals.icu HTTP " + code + (shortBody.isEmpty() ? "" : ": " + shortBody));
        }
        if (body == null || body.trim().isEmpty()) return new JSONObject();
        return new JSONTokener(body).nextValue();
    }

    private String readAll(InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) b.append(line);
        r.close();
        return b.toString();
    }

    private JSONArray asArray(Object value) {
        if (value instanceof JSONArray) return (JSONArray) value;
        if (value instanceof JSONObject) {
            JSONObject o = (JSONObject) value;
            for (String k : new String[]{"activities", "events", "wellness", "data"}) {
                JSONArray a = o.optJSONArray(k);
                if (a != null) return a;
            }
        }
        return new JSONArray();
    }

    public JSONObject athlete() throws Exception {
        Object o = get("/athlete/0");
        return o instanceof JSONObject ? (JSONObject) o : new JSONObject();
    }

    public JSONArray activities(int days) throws Exception {
        LocalDate newest = LocalDate.now().plusDays(1);
        LocalDate oldest = LocalDate.now().minusDays(days);
        return asArray(get("/athlete/0/activities?oldest=" + oldest + "&newest=" + newest));
    }

    public JSONArray events(int daysBack, int daysForward) throws Exception {
        LocalDate oldest = LocalDate.now().minusDays(daysBack);
        LocalDate newest = LocalDate.now().plusDays(daysForward);
        return asArray(get("/athlete/0/events?oldest=" + oldest + "&newest=" + newest));
    }

    public JSONArray wellness(int days) throws Exception {
        LocalDate oldest = LocalDate.now().minusDays(days);
        LocalDate newest = LocalDate.now().plusDays(1);
        return asArray(get("/athlete/0/wellness?oldest=" + oldest + "&newest=" + newest));
    }

    public JSONObject snapshot() throws Exception {
        JSONObject out = new JSONObject();
        out.put("athlete", athlete());
        out.put("activities", activities(70));
        out.put("events", events(14, 21));
        out.put("wellness", wellness(42));
        return out;
    }
}
