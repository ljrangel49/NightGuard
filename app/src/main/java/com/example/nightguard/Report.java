package com.example.nightguard;

public class Report {
    private double latitude;
    private double longitude;
    private String description;
    private String photoUrl;
    private String timestamp;

    public Report(){}

    public Report(double latitude, double longitude, String description, String photoUrl, String timestamp)
    {
        this.latitude=latitude;
        this.longitude=longitude;
        this.description=description;
        this.photoUrl=photoUrl;
        this.timestamp=timestamp;
    }


    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
