package com.example.dondepp.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class OverpassResponse {
    @SerializedName("elements")
    private List<Element> elements;

    public List<Element> getElements() {
        return elements;
    }

    public static class Element {
        @SerializedName("lat")
        private double lat;

        @SerializedName("lon")
        private double lon;

        @SerializedName("tags")
        private Map<String, String> tags;

        public double getLat() { return lat; }
        public double getLon() { return lon; }
        public Map<String, String> getTags() { return tags; }

        public String getName() {
            if (tags != null && tags.containsKey("name")) {
                return tags.get("name");
            }
            return "Sem nome";
        }

        public String getAddress() {
            if (tags != null) {
                String street = tags.get("addr:street");
                String number = tags.get("addr:housenumber");
                if (street != null) {
                    return street + (number != null ? ", " + number : "");
                }
            }
            return "";
        }
    }
}
