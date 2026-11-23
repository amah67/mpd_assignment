package com.example.mahmood_adam_s2322107.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mahmood_adam_s2322107.R;
import com.example.mahmood_adam_s2322107.model.GeoPoint;
import com.example.mahmood_adam_s2322107.model.Situation;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import android.widget.Button;
import com.example.mahmood_adam_s2322107.MainActivity;

// Shows full details of the selected situation
public class SituationDetailFragment extends Fragment {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK);

    public SituationDetailFragment() { super(R.layout.fragment_situtation_detail); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        viewModel.getSelected().observe(getViewLifecycleOwner(), s -> show(view, s));
    }

    private void show(View v, Situation s) {
        if (s == null) return;
        boolean roadworks = s.getCategory() == Situation.Category.ROADWORKS;

        ImageView icon = v.findViewById(R.id.detailIcon);
        icon.setImageResource(SituationStyle.iconFor(s));
        icon.setColorFilter(SituationStyle.colourFor(requireContext(), s));
        ((TextView) v.findViewById(R.id.detailCategory)).setText(roadworks ? "ROADWORKS" : "INCIDENT");
        ((TextView) v.findViewById(R.id.detailType)).setText(
                roadworks ? SituationStyle.worksTypeFor(s) : s.getDisplayType());
        ((TextView) v.findViewById(R.id.detailComment)).setText(s.getComment());
        SituationStyle.applyBadge(v.findViewById(R.id.detailBadge), s);

        Button mapButton = v.findViewById(R.id.showOnMapButton);
        mapButton.setVisibility(s.getFrom() == null ? View.GONE : View.VISIBLE);
        mapButton.setOnClickListener(b -> ((MainActivity) requireActivity()).showMap(true));

        LinearLayout c = v.findViewById(R.id.detailsContainer);
        c.removeAllViews();
        addRow(c, "Road", s.getRoadName());
        addRow(c, "Record type", s.getDisplayType());
        addRow(c, "Severity", s.getSeverity());
        addRow(c, "Status", s.getValidityStatus());
        addRow(c, "Impact", s.getImpact());
        addRow(c, "Starts", formatDate(s.getStartTime()));
        addRow(c, "Ends", formatDate(s.getEndTime()));
        addRow(c, "Duration", SituationStyle.durationText(s));
        if (s.getDelayMinutes() != null) {
            addRow(c, "Expected delay", String.format(Locale.UK, "%.0f minutes", s.getDelayMinutes()));
        }
        addRow(c, "From", formatPoint(s.getFrom()));
        addRow(c, "To", formatPoint(s.getTo()));
        addRow(c, "Reference", s.getId());
    }

    // Adds a label/value pair. skips anything missing so no row is empty.
    private void addRow(LinearLayout container, String label, String value) {
        if (value == null || value.isEmpty() || value.equals("null")) return;
        float dp = getResources().getDisplayMetrics().density;

        TextView l = new TextView(requireContext());
        l.setText(label);
        l.setAllCaps(true);
        l.setTextSize(12);
        l.setAlpha(0.6f);
        l.setPadding(0, (int) (14 * dp), 0, 0);

        TextView val = new TextView(requireContext());
        val.setText(value);
        val.setTextSize(16);

        container.addView(l);
        container.addView(val);
    }

    private String formatDate(OffsetDateTime t) {
        return t == null ? null : t.atZoneSameInstant(ZoneId.of("Europe/London")).format(FMT);
    }

    private String formatPoint(GeoPoint p) {
        return p == null ? null
                : String.format(Locale.UK, "%.5f, %.5f", p.getLatitude(), p.getLongitude());
    }
}