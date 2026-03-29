package com.example.tripsync.data.model;

public class TripModel {

    private String title;
    private String subtitle;

    public TripModel(String title, String subtitle) {
        this.title = title;
        this.subtitle = subtitle;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }
}