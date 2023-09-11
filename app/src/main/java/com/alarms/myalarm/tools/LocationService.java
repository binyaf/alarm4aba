package com.alarms.myalarm.tools;

import com.kosherjava.zmanim.util.GeoLocation;

import java.util.TimeZone;

public class LocationService {

    public GeoLocation getGeoLocation() {
        //hardcoded for jerusalem meantime
       return new GeoLocation("Jerusalem",   31.7683, 35.2137, 800,
               TimeZone.getTimeZone("Asia/Jerusalem"));
    }
}
