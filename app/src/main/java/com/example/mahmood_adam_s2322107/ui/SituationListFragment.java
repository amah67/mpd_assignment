package com.example.mahmood_adam_s2322107.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mahmood_adam_s2322107.MainActivity;
import com.example.mahmood_adam_s2322107.R;
import com.example.mahmood_adam_s2322107.model.Situation;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/** View: shows the (filtered) list of situations and observes the ViewModel. */
public class SituationListFragment extends Fragment {

    public SituationListFragment() { super(R.layout.fragment_situation_list); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MainViewModel viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        TextView status = view.findViewById(R.id.statusText);
        TextView banner = view.findViewById(R.id.filterBanner);
        TextView empty = view.findViewById(R.id.emptyText);
        ProgressBar progress = view.findViewById(R.id.progress);
        RecyclerView list = view.findViewById(R.id.situationList);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        SituationAdapter adapter = new SituationAdapter(s -> {
            viewModel.select(s);
            ((MainActivity) requireActivity()).showDetail();
        });
        list.setAdapter(adapter);

        viewModel.getFilteredSituations().observe(getViewLifecycleOwner(), situations -> {
            adapter.setSituations(situations);
            empty.setVisibility(situations.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.getRoadQuery().observe(getViewLifecycleOwner(), q ->
                empty.setText(q.isEmpty() ? "No situations match the current filter"
                        : "No incidents or roadworks found on " + q.toUpperCase()));

        // Distance filter banner - tap to turn it off
        banner.setOnClickListener(v -> viewModel.setRadiusMiles(0));
        viewModel.getRadiusMiles().observe(getViewLifecycleOwner(), r -> {
            boolean on = r != null && r > 0;
            banner.setVisibility(on ? View.VISIBLE : View.GONE);
            banner.setText("Within " + r + " miles of " + MainViewModel.HOME_NAME
                    + " · nearest first · tap to clear");
            adapter.setOrigin(on ? MainViewModel.HOME : null);
        });

        // All / Incidents / Roadworks filter
        ChipGroup chips = view.findViewById(R.id.categoryChips);
        Chip chipAll = view.findViewById(R.id.chipAll);
        Chip chipIncidents = view.findViewById(R.id.chipIncidents);
        Chip chipRoadworks = view.findViewById(R.id.chipRoadworks);

        // Restore the selected chip
        Situation.Category current = viewModel.getCategoryFilter().getValue();
        if (current == Situation.Category.INCIDENT) chips.check(R.id.chipIncidents);
        else if (current == Situation.Category.ROADWORKS) chips.check(R.id.chipRoadworks);
        else chips.check(R.id.chipAll);

        chips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chipIncidents) viewModel.setCategoryFilter(Situation.Category.INCIDENT);
            else if (id == R.id.chipRoadworks) viewModel.setCategoryFilter(Situation.Category.ROADWORKS);
            else viewModel.setCategoryFilter(null);
        });

        viewModel.getCategoryCounts().observe(getViewLifecycleOwner(), c -> {
            chipAll.setText("All (" + c[0] + ")");
            chipIncidents.setText("Incidents (" + c[1] + ")");
            chipRoadworks.setText("Roadworks (" + c[2] + ")");
        });


        viewModel.getStatus().observe(getViewLifecycleOwner(), status::setText);
        viewModel.isLoading().observe(getViewLifecycleOwner(),
                l -> progress.setVisibility(l ? View.VISIBLE : View.GONE));
    }
}