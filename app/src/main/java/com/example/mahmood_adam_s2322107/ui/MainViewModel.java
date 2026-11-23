package com.example.mahmood_adam_s2322107.ui;

import android.app.Application;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mahmood_adam_s2322107.data.TrafficRepository;
import com.example.mahmood_adam_s2322107.model.GeoPoint;
import com.example.mahmood_adam_s2322107.model.Situation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

// ViewModel class. It holds the data, survives rotation, runs downloads on a background thread.
public class MainViewModel extends AndroidViewModel {

    // Refresh every hour for demo video
    private static final long REFRESH_MINUTES = 1;
    private static final String META_ID = "com.example.mahmood_adam_s2322107.MY_ID";
    private static final String META_KEY = "com.example.mahmood_adam_s2322107.MY_KEY";

    //  Made current location the GCU campus
    public static final GeoPoint HOME = new GeoPoint(55.8667, -4.2500);
    public static final String HOME_NAME = "GCU, Glasgow";

    // Metres in one mile. Location.distanceBetween() gives metres.
    public static final double METRES_PER_MILE = 1609.344;

    // Which screen the user is on, so it can be rebuilt after rotation.
    public enum Screen { LIST, DETAIL, MAP, MAP_SINGLE }
    private Screen screen = Screen.LIST;

    private final MutableLiveData<List<Situation>> allSituations = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> roadQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> radiusMiles = new MutableLiveData<>(0);   // 0 = off
    private final MediatorLiveData<List<Situation>> filteredSituations = new MediatorLiveData<>();
    private final MutableLiveData<Situation> selected = new MutableLiveData<>();
    private final MutableLiveData<String> status = new MutableLiveData<>("Loading…");
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Situation.Category> categoryFilter = new MutableLiveData<>();   // null = all
    private final MutableLiveData<int[]> categoryCounts = new MutableLiveData<>(new int[]{0, 0, 0});
    private final TrafficRepository repository;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public MainViewModel(@NonNull Application app) {
        super(app);
        // The filtered list updates when the data, the road search OR the distance changes
        filteredSituations.addSource(allSituations, list -> applyFilter());
        filteredSituations.addSource(roadQuery, q -> applyFilter());
        filteredSituations.addSource(radiusMiles, r -> applyFilter());
        filteredSituations.addSource(categoryFilter, c -> applyFilter());

        String[] keys = readKeys(app);
        repository = new TrafficRepository(keys[0], keys[1], app.getFilesDir());
        // Load immediately at start-up, then repeat every REFRESH_MINUTES
        executor.scheduleWithFixedDelay(this::loadData, 0, REFRESH_MINUTES, TimeUnit.MINUTES);
    }

    // Exposed to the Views
    public LiveData<List<Situation>> getFilteredSituations() { return filteredSituations; }
    public LiveData<String> getRoadQuery() { return roadQuery; }
    public LiveData<Integer> getRadiusMiles() { return radiusMiles; }
    public LiveData<Situation> getSelected() { return selected; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<Boolean> isLoading() { return loading; }


    public Screen getScreen() { return screen; }
    public LiveData<Situation.Category> getCategoryFilter() { return categoryFilter; }
    public LiveData<int[]> getCategoryCounts() { return categoryCounts; }
    public void setCategoryFilter(Situation.Category c) { categoryFilter.setValue(c); }
    public void setScreen(Screen s) { screen = s; }

    public void setRoadQuery(String q) { roadQuery.setValue(q == null ? "" : q.trim()); }
    public void setRadiusMiles(int miles) { radiusMiles.setValue(miles); }
    public void select(Situation s) { selected.setValue(s); }
    public void refresh() { executor.execute(this::loadData); }

    // Filtering for road search + distance search
    // Filtering for  road search + distance search + incidents/roadworks
    private void applyFilter() {
        List<Situation> all = allSituations.getValue();
        if (all == null) all = new ArrayList<>();
        String q = roadQuery.getValue();
        boolean hasQuery = q != null && !q.isEmpty();
        Integer r = radiusMiles.getValue();
        boolean hasRadius = r != null && r > 0;
        Situation.Category cat = categoryFilter.getValue();

        int incidents = 0, roadworks = 0;
        List<Situation> out = new ArrayList<>();
        for (Situation s : all) {
            if (hasQuery && !matchesRoad(s, q)) continue;
            if (hasRadius && distanceMiles(s, HOME) > r) continue;
            // Count before the category filter, so the chips show how many of each there are
            if (s.getCategory() == Situation.Category.INCIDENT) incidents++; else roadworks++;
            if (cat != null && s.getCategory() != cat) continue;
            out.add(s);
        }
        if (hasRadius) out.sort(Comparator.comparingDouble(s -> distanceMiles(s, HOME)));  // nearest first
        categoryCounts.setValue(new int[]{ incidents + roadworks, incidents, roadworks });
        filteredSituations.setValue(out);
    }

    // Make sure roads match exactly 'M8' to 'M8' not to 'M80'
    public static boolean matchesRoad(Situation s, String query) {
        if (s.getRoadName() != null && s.getRoadName().equalsIgnoreCase(query)) return true;
        Pattern p = Pattern.compile("(?<![A-Za-z0-9])" + Pattern.quote(query) + "(?![A-Za-z0-9])",
                Pattern.CASE_INSENSITIVE);
        return p.matcher(s.getComment()).find();
    }

    // Distance in miles from origin to the nearest point of a situation
    public static double distanceMiles(Situation s, GeoPoint origin) {
        double best = Double.MAX_VALUE;
        float[] result = new float[1];
        for (GeoPoint p : s.getPoints()) {
            Location.distanceBetween(origin.getLatitude(), origin.getLongitude(),
                    p.getLatitude(), p.getLongitude(), result);
            best = Math.min(best, result[0] / METRES_PER_MILE);
        }
        return best;
    }

    // Loading data in background
    private void loadData() {
        try {
            loading.postValue(true);
            TrafficRepository.Result r = repository.fetchAll();

            allSituations.postValue(r.situations);

            String time = new SimpleDateFormat("HH:mm", Locale.UK).format(new Date(r.timestamp));
            if (r.fromCache && r.situations.isEmpty()) {
                status.postValue("No internet connection and no saved data");
            } else if (r.fromCache) {
                status.postValue("Offline – showing saved data from " + time);
            } else {
                status.postValue("Updated " + time + " · " + r.situations.size() + " situations");
            }
        } catch (Exception e) {
            // Must catch everything, an uncaught exception would stop the scheduled refresh
            Log.e("MainViewModel", "Refresh failed", e);
            status.postValue("Could not update data");
        } finally {
            loading.postValue(false);
        }
    }

    private static String[] readKeys(Application app) {
        try {
            Bundle md = app.getPackageManager()
                    .getApplicationInfo(app.getPackageName(), PackageManager.GET_META_DATA).metaData;
            return new String[]{ String.valueOf(md.get(META_ID)), String.valueOf(md.get(META_KEY)) };
        } catch (PackageManager.NameNotFoundException e) {
            return new String[]{ "", "" };
        }
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}