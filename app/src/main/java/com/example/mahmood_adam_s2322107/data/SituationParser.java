package com.example.mahmood_adam_s2322107.data;

import android.util.Log;

import com.example.mahmood_adam_s2322107.model.GeoPoint;
import com.example.mahmood_adam_s2322107.model.Situation;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SituationParser {

    private static final String TAG = "SituationParser";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    // Matches road names like M8, A9, A720, B7066, A74(M)
    private static final Pattern ROAD_PATTERN = Pattern.compile("\\b([MAB]\\d{1,4}(\\(M\\))?)\\b");

    // Parses the downloaded XML String into a list of Situations.
    public List<Situation> parse(String xml, Situation.Category category) {
        List<Situation> results = new ArrayList<>();
        if (xml == null || xml.isEmpty()) return results;

        Situation current = null;
        boolean typeSet = false;
        Deque<String> path = new ArrayDeque<>();   // stack of open tags, so we know where we are
        Double pendingLat = null;
        Double pendingLon = null;

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xml));

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG) {
                    String name = xpp.getName();
                    path.push(name);

                    if (name.equals("situation")) {
                        current = new Situation();
                        current.setCategory(category);
                        current.setId(xpp.getAttributeValue(null, "id"));
                        typeSet = false;
                    } else if (current != null && name.equals("situationRecord") && !typeSet) {
                        String t = getXsiType(xpp);
                        if (t != null) {
                            current.setType(t);
                            typeSet = true;
                        }
                    }

                } else if (eventType == XmlPullParser.TEXT && current != null && !path.isEmpty()) {
                    String text = xpp.getText().trim();
                    if (!text.isEmpty()) {
                        String tag = path.peek();
                        switch (tag) {
                            case "value":
                                if (path.contains("generalPublicComment")) current.appendComment(text);
                                else if (path.contains("roadName")) current.setRoadName(text);
                                break;
                            case "roadNumber":
                                current.setRoadName(text);
                                break;
                            case "overallSeverity":
                            case "severity":
                                if (current.getSeverity() == null) current.setSeverity(text);
                                break;
                            case "validityStatus":
                                current.setValidityStatus(text);
                                break;
                            case "overallStartTime":
                                current.setStartTime(parseDate(text));
                                break;
                            case "overallEndTime":
                                current.setEndTime(parseDate(text));
                                break;
                            case "delayTimeValue":
                                Double secs = parseDouble(text);
                                if (secs != null) current.setDelayMinutes(secs / 60.0);
                                break;
                            case "impactOnTraffic":
                                current.setImpact(text);
                                break;
                            case "latitude":
                                pendingLat = parseDouble(text);
                                break;
                            case "longitude":
                                pendingLon = parseDouble(text);
                                break;
                            default:
                                // Unknown tags are ignored, so new fields won't crash the app
                                break;
                        }
                        if (pendingLat != null && pendingLon != null) {
                            current.addPoint(new GeoPoint(pendingLat, pendingLon));
                            pendingLat = null;
                            pendingLon = null;
                        }
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                    String name = xpp.getName();
                    if (!path.isEmpty()) path.pop();
                    if (name.equals("situation") && current != null) {
                        finish(current);
                        results.add(current);
                        current = null;
                    }
                }

                eventType = xpp.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while parsing XML", e);
        }
        return results;
    }

    // Further processing once a situation has been fully read.
    private void finish(Situation s) {
        // If the XML had no road tag, extract the road name from the comment text
        if (s.getRoadName() == null) {
            Matcher m = ROAD_PATTERN.matcher(s.getComment());
            if (m.find()) s.setRoadName(m.group(1));
        }
    }

    /** Reads xsi:type, e.g. "Accident", "MaintenanceWorks". */
    private String getXsiType(XmlPullParser xpp) {
        String t = xpp.getAttributeValue(XSI_NS, "type");
        if (t == null) {
            for (int i = 0; i < xpp.getAttributeCount(); i++) {
                if (xpp.getAttributeName(i).endsWith("type")) {
                    t = xpp.getAttributeValue(i);
                    break;
                }
            }
        }
        if (t != null && t.contains(":")) t = t.substring(t.indexOf(':') + 1);
        return t;
    }

    private OffsetDateTime parseDate(String text) {
        try { return OffsetDateTime.parse(text); } catch (Exception e) { return null; }
    }

    private Double parseDouble(String text) {
        try { return Double.parseDouble(text); } catch (Exception e) { return null; }
    }
}