package com.banjos.dosalarm.tools;

import com.banjos.dosalarm.types.AlarmLocation;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.util.GeoLocation;

import java.util.Date;

public class ZmanimService {

    private static int CANDLE_LIGHTING_OFFSET_JERUSALEM = 40;

    private static int CANDLE_LIGHTING_OFFSET = 20;


    public static ZmanimCalendar getTodaysZmanimCalendar(AlarmLocation clientsLocation) {

        ZmanimCalendar zcalToday = new ZmanimCalendar();

        if (clientsLocation.getCityCode().equals("JLM_IL")) {
            zcalToday.setCandleLightingOffset(CANDLE_LIGHTING_OFFSET_JERUSALEM);
        } else {
            zcalToday.setCandleLightingOffset(CANDLE_LIGHTING_OFFSET);
        }
        GeoLocation gl = LocationService.getGeoLocationFromAlarmLocation(clientsLocation);
        zcalToday.setGeoLocation(gl);

        return zcalToday;

    }

    public static Date getCandleLightingTimeToday(AlarmLocation clientsLocation) {

        if (hasCandleLightingToday(clientsLocation)) {
            return getTodaysZmanimCalendar(clientsLocation).getCandleLighting();
        }
        return null;
    }

    public static boolean hasCandleLightingToday(AlarmLocation clientsLocation) {
        boolean inIsrael = clientsLocation.getCityCode().endsWith("_IL");

        JewishCalendar jc = new JewishCalendar();
        jc.setInIsrael(inIsrael);
        return jc.hasCandleLighting();
    }
}
