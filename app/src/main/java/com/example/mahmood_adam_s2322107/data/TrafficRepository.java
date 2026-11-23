package com.example.mahmood_adam_s2322107.data;

import android.util.Log;

import com.example.mahmood_adam_s2322107.model.Situation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

//downloads the feeds, caches them for offline use, and parses them
public class TrafficRepository {

    private static final String TAG = "TrafficRepository";
    private static final String UNPLANNED_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    private static final String ROADWORKS_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";
    private static final String UNPLANNED_CACHE = "unplanned.xml";
    private static final String ROADWORKS_CACHE = "roadworks.xml";

    private final String clientId;
    private final String clientKey;
    private final File cacheDir;
    private final SituationParser parser = new SituationParser();

    //What one refresh produced
    public static class Result {
        public final List<Situation> situations;
        public final boolean fromCache;
        public final long timestamp;

        Result(List<Situation> situations, boolean fromCache, long timestamp) {
            this.situations = situations;
            this.fromCache = fromCache;
            this.timestamp = timestamp;
        }
    }

    public TrafficRepository(String clientId, String clientKey, File cacheDir) {
        this.clientId = clientId;
        this.clientKey = clientKey;
        this.cacheDir = cacheDir;
    }



    //Must be called on a background thread
    public Result fetchAll() {
        boolean fromCache = false;

        String unplanned = downloadWithRetry(UNPLANNED_URL);
        if (unplanned != null) saveCache(UNPLANNED_CACHE, unplanned);
        else { unplanned = loadCache(UNPLANNED_CACHE); fromCache = true; }

        String roadworks = downloadWithRetry(ROADWORKS_URL);
        if (roadworks != null) saveCache(ROADWORKS_CACHE, roadworks);
        else { roadworks = loadCache(ROADWORKS_CACHE); fromCache = true; }

        List<Situation> all = new ArrayList<>();
        List<Situation> incidents = parser.parse(unplanned, Situation.Category.INCIDENT);
        List<Situation> works = parser.parse(roadworks, Situation.Category.ROADWORKS);
        all.addAll(incidents);
        all.addAll(works);

        long time = fromCache
                ? new File(cacheDir, UNPLANNED_CACHE).lastModified()
                : System.currentTimeMillis();
        return new Result(all, fromCache, time);
    }

    // returns XML String, or null if there's no connection or the request fails
    private String download(String urlString) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Authorization", Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientKey).getBytes()));

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "HTTP " + conn.getResponseCode() + " for " + urlString);
                return null;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buffer = new char[8192];
                int n;
                while ((n = in.read(buffer)) != -1) sb.append(buffer, 0, n);
            }
            String result = sb.toString();
            int i = result.indexOf("<?");
            if (i > 0) result = result.substring(i);
            return result;
        } catch (IOException e) {
            Log.e(TAG, "Download failed: " + urlString, e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // Tries up to 3 times because mobile connections can drop in middle of download
    private String downloadWithRetry(String url) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String xml = download(url);
            if (xml != null) return xml;
            Log.w(TAG, "Attempt " + attempt + " failed for " + url);
            try { Thread.sleep(1000); } catch (InterruptedException e) { return null; }
        }
        return null;
    }
    private void saveCache(String name, String xml) {
        try {
            Files.write(new File(cacheDir, name).toPath(), xml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "Could not save cache " + name, e);
        }
    }

    private String loadCache(String name) {
        File f = new File(cacheDir, name);
        if (!f.exists()) return null;
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}