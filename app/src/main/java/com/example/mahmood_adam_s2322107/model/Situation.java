package com.example.mahmood_adam_s2322107.model;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Situation {

    public enum Category { INCIDENT, ROADWORKS }

    private String id;
    private Category category;
    private String type = "Unknown";
    private String severity;
    private String validityStatus;
    private String comment = "";          // generalPublicComment
    private String roadName;
    private String impact;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Double delayMinutes;
    private final List<GeoPoint> points = new ArrayList<>();

    //  Derived values (the further breaking down of the data)

    // RoadOrCarriagewayOrLaneManagement to Road Or Carriageway Or Lane Management
    public String getDisplayType() {
        return type.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    // Duration in hours, used for colour-coding roadworks. Null if unknown.
    public Long getDurationHours() {
        if (startTime == null || endTime == null) return null;
        return Duration.between(startTime, endTime).toHours();
    }

    public GeoPoint getFrom() { return points.isEmpty() ? null : points.get(0); }
    public GeoPoint getTo()   { return points.size() < 2 ? null : points.get(points.size() - 1); }

    public void appendComment(String text) {
        comment = comment.isEmpty() ? text : comment + " " + text;
    }

    public void addPoint(GeoPoint p) { points.add(p); }



    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getValidityStatus() { return validityStatus; }
    public void setValidityStatus(String validityStatus) { this.validityStatus = validityStatus; }
    public String getComment() { return comment; }
    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName; }
    public String getImpact() { return impact; }
    public void setImpact(String impact) { this.impact = impact; }
    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }
    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
    public Double getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(Double delayMinutes) { this.delayMinutes = delayMinutes; }
    public List<GeoPoint> getPoints() { return points; }

    @Override
    public String toString() {
        return category + " | " + type + " | road=" + roadName + " | severity=" + severity
                + " | start=" + startTime + " | end=" + endTime + " | points=" + points.size()
                + " | " + comment;
    }
}