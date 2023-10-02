package com.banjos.dosalarm.types;

public class AlarmLocation {
    private String cityCode;
    private int altitude;
   private double latitude;
   private double longitude;

    private String timeZone;

    public AlarmLocation(String cityCode, int altitude, double latitude, double longitude, String timeZone) {
        this.cityCode = cityCode;
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timeZone = timeZone;
    }

    public String getCityCode() {
        return cityCode;
    }

    public int getAltitude() {
        return altitude;
    }

    public void setAltitude(int altitude) {
        this.altitude = altitude;
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

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
}
