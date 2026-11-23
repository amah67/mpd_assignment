package com.example.mahmood_adam_s2322107.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mahmood_adam_s2322107.R;
import com.example.mahmood_adam_s2322107.model.GeoPoint;
import com.example.mahmood_adam_s2322107.model.Situation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Turns each Situation into one row of the list
public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.ViewHolder> {

    public interface OnSituationClickListener { void onSituationClick(Situation s); }

    private final List<Situation> items = new ArrayList<>();
    private final OnSituationClickListener listener;
    private GeoPoint origin;   // set when distance search is on

    public SituationAdapter(OnSituationClickListener listener) { this.listener = listener; }

    public void setSituations(List<Situation> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void setOrigin(GeoPoint origin) {
        this.origin = origin;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_situation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Situation s = items.get(position);
        boolean roadworks = s.getCategory() == Situation.Category.ROADWORKS;

        String typeLine = roadworks ? "Roadworks · " + SituationStyle.worksTypeFor(s)
                : "Incident · " + s.getDisplayType();
        if (origin != null) {
            double miles = MainViewModel.distanceMiles(s, origin);
            if (miles < Double.MAX_VALUE) typeLine += String.format(Locale.UK, " · %.1f mi", miles);
        }
        h.type.setText(typeLine);
        h.comment.setText(s.getComment());
        h.icon.setImageResource(SituationStyle.iconFor(s));
        h.icon.setColorFilter(SituationStyle.colourFor(h.itemView.getContext(), s));
        SituationStyle.applyBadge(h.badge, s);
        h.itemView.setOnClickListener(v -> listener.onSituationClick(s));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView type, comment, badge;
        ViewHolder(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            type = v.findViewById(R.id.typeText);
            comment = v.findViewById(R.id.commentText);
            badge = v.findViewById(R.id.durationBadge);
        }
    }
}