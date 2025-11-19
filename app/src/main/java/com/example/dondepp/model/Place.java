package com.example.dondepp.model;

public class Place {
    private String name;
    private double latitude;
    private double longitude;
    private String type;
    private String address;
    private double distance; // em metros

    public Place(String name, double latitude, double longitude, String type) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.address = "";
        this.distance = 0;
    }

    // Getters e Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    // Formatar distância para exibição
    public String getFormattedDistance() {
        if (distance < 1000) {
            return Math.round(distance) + " m";
        } else {
            return String.format("%.1f km", distance / 1000);
        }
    }
}