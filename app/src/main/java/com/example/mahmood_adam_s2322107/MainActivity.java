//
// Name                 Adam Mahmood
// Student ID           S2322107
// Programme of Study   Software Development
//
package com.example.mahmood_adam_s2322107;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.mahmood_adam_s2322107.ui.MainViewModel;
import com.example.mahmood_adam_s2322107.ui.MainViewModel.Screen;
import com.example.mahmood_adam_s2322107.ui.SituationDetailFragment;
import com.example.mahmood_adam_s2322107.ui.SituationListFragment;
import com.example.mahmood_adam_s2322107.ui.SituationMapFragment;


 //Single Activity: hosts the toolbar and the fragments.
 // Portrait: one fragment at a time. Landscape: list on the left, map/details on the right.

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private boolean twoPane;   // true when the landscape layout (with detail_container) is in use

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        twoPane = findViewById(R.id.detail_container) != null;

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState != null) {
            // After rotation the old fragments may belong to the other layout,
            // so remove them and rebuild from the ViewModel's saved screen.
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            FragmentTransaction t = fm.beginTransaction();
            for (Fragment f : fm.getFragments()) t.remove(f);
            t.commitNow();
        }
        buildScreen();

        fm.addOnBackStackChangedListener(() -> {
            updateScreenFromFragments();
            updateUpButton();
        });
        updateUpButton();
    }

    // Building the screen for the current orientation

    private void buildScreen() {
        Screen screen = viewModel.getScreen();
        FragmentManager fm = getSupportFragmentManager();

        if (twoPane) {
            // Left always the list. Right details, or the map
            Fragment right = (screen == Screen.DETAIL)
                    ? new SituationDetailFragment()
                    : SituationMapFragment.newInstance(screen == Screen.MAP_SINGLE);
            fm.beginTransaction()
                    .replace(R.id.fragment_container, new SituationListFragment())
                    .replace(R.id.detail_container, right)
                    .commitNow();
        } else {
            fm.beginTransaction()
                    .replace(R.id.fragment_container, new SituationListFragment())
                    .commitNow();
            if (screen == Screen.DETAIL) {
                push(new SituationDetailFragment(), "detail");
            } else if (screen == Screen.MAP || screen == Screen.MAP_SINGLE) {
                push(SituationMapFragment.newInstance(screen == Screen.MAP_SINGLE), "map");
            }
        }
    }

    // Shows a fragment where it belongs: right pane in landscape, full screen in portrait
    private void push(Fragment f, String name) {
        int container = twoPane ? R.id.detail_container : R.id.fragment_container;
        getSupportFragmentManager().beginTransaction()
                .replace(container, f)
                .addToBackStack(name)
                .commit();
    }

    // Keeps the ViewModel's screen in step when the user presses back.
    private void updateScreenFromFragments() {
        Fragment top = getSupportFragmentManager()
                .findFragmentById(twoPane ? R.id.detail_container : R.id.fragment_container);
        if (top instanceof SituationDetailFragment) {
            viewModel.setScreen(Screen.DETAIL);
        } else if (top instanceof SituationMapFragment) {
            SituationMapFragment map = (SituationMapFragment) top;
            if (map.isSingle()) viewModel.setScreen(Screen.MAP_SINGLE);
            else viewModel.setScreen(twoPane ? Screen.LIST : Screen.MAP);
        } else {
            viewModel.setScreen(Screen.LIST);
        }
    }

    // Navigation called by the fragments

    public void showDetail() {
        viewModel.setScreen(Screen.DETAIL);
        push(new SituationDetailFragment(), "detail");
    }

    // onlySelected = true shows just the selected situation. false shows the filtered list
    public void showMap(boolean onlySelected) {
        viewModel.setScreen(onlySelected ? Screen.MAP_SINGLE : Screen.MAP);
        push(SituationMapFragment.newInstance(onlySelected), "map");
    }

    //  After a new search in portrait, leave the detail screen so the results are visible
    private void leaveDetailScreen() {
        if (twoPane) return;   // in landscape the list is always visible
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current instanceof SituationDetailFragment) {
            getSupportFragmentManager().popBackStack();
        }
    }

    private void updateUpButton() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(
                    getSupportFragmentManager().getBackStackEntryCount() > 0);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        }
        return true;
    }

    // Distance search dialog

    private void showDistanceDialog() {
        Integer current = viewModel.getRadiusMiles().getValue();
        int start = (current != null && current > 0) ? current : 15;
        float dp = getResources().getDisplayMetrics().density;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (24 * dp);
        layout.setPadding(pad, (int) (8 * dp), pad, 0);

        TextView label = new TextView(this);
        label.setTextSize(16);
        label.setText("Within " + start + " miles of " + MainViewModel.HOME_NAME);

        SeekBar bar = new SeekBar(this);
        bar.setMax(99);                  // 1 to 100 miles
        bar.setProgress(start - 1);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                label.setText("Within " + (p + 1) + " miles of " + MainViewModel.HOME_NAME);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });

        layout.addView(label);
        layout.addView(bar);

        new AlertDialog.Builder(this)
                .setTitle("Search by distance")
                .setView(layout)
                .setPositiveButton("Apply", (d, w) -> {
                    viewModel.setRadiusMiles(bar.getProgress() + 1);
                    leaveDetailScreen();
                })
                .setNeutralButton("Turn off", (d, w) -> viewModel.setRadiusMiles(0))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Toolbar menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Road name, e.g. M8");

        // Restore the search after rotation (before adding listeners)
        String current = viewModel.getRoadQuery().getValue();
        if (current != null && !current.isEmpty()) {
            searchItem.expandActionView();
            searchView.setQuery(current, false);
            searchView.clearFocus();
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String q) {
                viewModel.setRoadQuery(q);
                searchView.clearFocus();   // hide keyboard
                leaveDetailScreen();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String q) {
                viewModel.setRoadQuery(q);  // filter as you type
                return true;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem item) { return true; }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                viewModel.setRoadQuery("");  // closing search shows everything again
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            viewModel.refresh();
            return true;
        } else if (id == R.id.action_map) {
            showMap(false);
            return true;
        } else if (id == R.id.action_nearby) {
            showDistanceDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}