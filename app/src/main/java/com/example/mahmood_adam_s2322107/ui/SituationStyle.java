package com.example.mahmood_adam_s2322107.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
import android.widget.TextView;

import com.example.mahmood_adam_s2322107.R;
import com.example.mahmood_adam_s2322107.model.Situation;
import java.util.Locale;
import java.util.regex.Pattern;

// Decides the icon, colour and duration label for a Situation.
public final class SituationStyle {

    public enum DurationBand { SHORT, MEDIUM, LONG, UNKNOWN }

    private SituationStyle() { }

    private static final Pattern TTL = Pattern.compile("\\bttl");   // "TTL", "TTLs", but not "little"

    /** Reads the kind of roadworks from the comment, e.g. "M8 WB - Lane Closure". */
    public static String worksTypeFor(Situation s) {
        String c = s.getComment().toLowerCase(Locale.UK);
        if (c.contains("lane closure") || c.contains("hard shoulder") || c.contains("contraflow")
                || c.contains("narrow") || c.contains("convoy"))
            return "Lane restrictions";
        if (c.contains("closure") || c.contains("closed")) return "Road closure";
        if (TTL.matcher(c).find() || c.contains("traffic light") || c.contains("temp lights")
                || c.contains("stop and go"))
            return "Temporary traffic lights";
        if (c.contains("speed restriction")) return "Speed restriction";
        if (c.contains("signage") || c.contains("signs") || c.contains("diversion"))
            return "Signage / diversion";
        return "Maintenance works";
    }


    // One icon per situation type with an order e.g. VehicleObstruction before Obstruction
    public static int iconFor(Situation s) {

        if (s.getCategory() == Situation.Category.ROADWORKS) {
            switch (worksTypeFor(s)) {
                case "Lane restrictions":        return R.drawable.ic_rw_lane;
                case "Road closure":             return R.drawable.ic_rw_closure;
                case "Temporary traffic lights": return R.drawable.ic_rw_lights;
                case "Speed restriction":        return R.drawable.ic_rw_speed;
                case "Signage / diversion":      return R.drawable.ic_rw_diversion;
                default:                         return R.drawable.ic_rw_works;
            }
        }
        String t = s.getType().toLowerCase();
        if (t.contains("accident")) return R.drawable.ic_accident;
        if (t.contains("vehicle")) return R.drawable.ic_vehicle;
        if (t.contains("animal")) return R.drawable.ic_animal;
        if (t.contains("environment") || t.contains("weather")) return R.drawable.ic_weather;
        if (t.contains("traffic")) return R.drawable.ic_traffic;
        if (t.contains("works") || t.contains("maintenance") || t.contains("construction"))
            return R.drawable.ic_roadworks;
        if (t.contains("lane") || t.contains("carriageway")) return R.drawable.ic_lane_closure;
        if (t.contains("obstruction")) return R.drawable.ic_obstruction;
        if (s.getCategory() == Situation.Category.ROADWORKS) return R.drawable.ic_roadworks;
        return R.drawable.ic_info;
    }

    // Short - under 1 day. Medium - under 1 week. Long - 1 week or more
    public static DurationBand bandFor(Situation s) {
        Long h = s.getDurationHours();
        if (h == null) return DurationBand.UNKNOWN;
        if (h < 24) return DurationBand.SHORT;
        if (h < 24 * 7) return DurationBand.MEDIUM;
        return DurationBand.LONG;
    }

    public static int colourFor(Context ctx, Situation s) {
        if (s.getCategory() != Situation.Category.ROADWORKS) return ctx.getColor(R.color.incident);
        switch (bandFor(s)) {
            case SHORT:  return ctx.getColor(R.color.duration_short);
            case MEDIUM: return ctx.getColor(R.color.duration_medium);
            case LONG:   return ctx.getColor(R.color.duration_long);
            default:     return ctx.getColor(R.color.duration_unknown);
        }
    }

    public static String durationText(Situation s) {
        Long h = s.getDurationHours();
        if (h == null) return null;
        if (h < 24) return h + (h == 1 ? " hour" : " hours");
        long days = h / 24;
        return days + (days == 1 ? " day" : " days");
    }

    // Coloured badge shown for roadworks only.
    public static void applyBadge(TextView badge, Situation s) {
        if (s.getCategory() != Situation.Category.ROADWORKS) {
            badge.setVisibility(View.GONE);
            return;
        }
        String label;
        switch (bandFor(s)) {
            case SHORT:  label = "Short"; break;
            case MEDIUM: label = "Medium"; break;
            case LONG:   label = "Long"; break;
            default:     label = "Duration unknown"; break;
        }
        String d = durationText(s);
        badge.setText(d == null ? label : label + " · " + d);
        badge.setBackgroundTintList(ColorStateList.valueOf(colourFor(badge.getContext(), s)));
        badge.setVisibility(View.VISIBLE);
    }
}