package com.example.mahmood_adam_s2322107.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.mahmood_adam_s2322107.MainActivity;
import com.example.mahmood_adam_s2322107.model.GeoPoint;
import com.example.mahmood_adam_s2322107.model.Situation;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** View: shows the filtered situations (or just the selected one) on a Google Map. */
public class SituationMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    private static final String ARG_SINGLE = "single";
    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);

    private MainViewModel viewModel;
    private GoogleMap map;
    private final Map<String, BitmapDescriptor> iconCache = new HashMap<>();

    /** single = true shows only the selected situation; false shows the filtered list. */
    public static SituationMapFragment newInstance(boolean single) {
        SituationMapFragment f = new SituationMapFragment();
        Bundle b = new Bundle();
        b.putBoolean(ARG_SINGLE, single);
        f.setArguments(b);
        return f;
    }

    public boolean isSingle() {
        return getArguments() != null && getArguments().getBoolean(ARG_SINGLE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);

        // Tapping a marker's info box opens the detail screen
        map.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Situation) {
                viewModel.select((Situation) tag);
                ((MainActivity) requireActivity()).showDetail();
            }
        });

        if (isSingle()) {
            viewModel.getSelected().observe(getViewLifecycleOwner(), s ->
                    draw(s == null ? new ArrayList<>() : Collections.singletonList(s)));
        } else {
            viewModel.getFilteredSituations().observe(getViewLifecycleOwner(), this::draw);
        }
    }

    private void draw(List<Situation> situations) {
        if (map == null) return;
        map.clear();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int count = 0;

        // Distance search, shows the centre point and the search radius
        Integer radius = viewModel.getRadiusMiles().getValue();
        if (!isSingle() && radius != null && radius > 0) {
            LatLng home = toLatLng(MainViewModel.HOME);
            map.addCircle(new CircleOptions().center(home).radius(radius * MainViewModel.METRES_PER_MILE)
                    .strokeColor(0xFF1565C0).fillColor(0x221565C0).strokeWidth(3f));
            map.addMarker(new MarkerOptions().position(home).title("Search centre")
                    .snippet(MainViewModel.HOME_NAME)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
            bounds.include(home);
            count++;
        }

        for (Situation s : situations) {
            GeoPoint from = s.getFrom();
            if (from == null) continue;
            int colour = SituationStyle.colourFor(requireContext(), s);

            Marker m = map.addMarker(new MarkerOptions()
                    .position(toLatLng(from))
                    .title(s.getDisplayType())
                    .snippet(s.getComment())
                    .icon(iconFor(s, colour))
                    .anchor(0.5f, 0.5f));
            if (m != null) m.setTag(s);
            bounds.include(toLatLng(from));
            count++;

            // Situations with a to point are drawn as a line as from -> to
            GeoPoint to = s.getTo();
            if (to != null && (to.getLatitude() != from.getLatitude()
                    || to.getLongitude() != from.getLongitude())) {
                map.addPolyline(new PolylineOptions().add(toLatLng(from), toLatLng(to))
                        .color(colour).width(8f));
                bounds.include(toLatLng(to));
            }
        }
        moveCamera(bounds, count);
    }

    private void moveCamera(LatLngBounds.Builder builder, int count) {
        if (count == 0) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6f));
            return;
        }
        LatLngBounds b = builder.build();
        if (b.northeast.equals(b.southwest)) {      // a single point, zoom in on it
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(b.getCenter(), 13f));
            return;
        }
        int padding = (int) (48 * getResources().getDisplayMetrics().density);
        View v = getView();
        if (v != null && v.getWidth() > 0 && v.getHeight() > 0) {
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(b, v.getWidth(), v.getHeight(), padding));
        } else {
            map.setOnMapLoadedCallback(() ->
                    map.moveCamera(CameraUpdateFactory.newLatLngBounds(b, padding)));
        }
    }

    // Draws the situation's icon inside a white circle with a coloured border
    private BitmapDescriptor iconFor(Situation s, int colour) {
        int iconRes = SituationStyle.iconFor(s);
        String key = iconRes + "_" + colour;
        BitmapDescriptor cached = iconCache.get(key);
        if (cached != null) return cached;

        float dp = getResources().getDisplayMetrics().density;
        int size = (int) (34 * dp);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2 * dp);
        paint.setColor(colour);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - dp, paint);

        Drawable d = ContextCompat.getDrawable(requireContext(), iconRes);
        if (d != null) {
            d = d.mutate();
            int pad = (int) (7 * dp);
            d.setBounds(pad, pad, size - pad, size - pad);
            d.setTint(colour);
            d.draw(canvas);
        }
        BitmapDescriptor result = BitmapDescriptorFactory.fromBitmap(bmp);
        iconCache.put(key, result);
        return result;
    }

    private static LatLng toLatLng(GeoPoint p) {
        return new LatLng(p.getLatitude(), p.getLongitude());
    }
}