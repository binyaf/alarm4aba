package com.banjos.dosalarm.tools;

import android.content.Context;

import com.banjos.dosalarm.types.AlarmLocation;
import com.kosherjava.zmanim.ZmanimCalendar;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;
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

    public static Date getCandleLightingTimeToday(AlarmLocation clientsLocation, Context context) {

        if (hasCandleLightingToday(clientsLocation, context)) {
            return getTodaysZmanimCalendar(clientsLocation).getCandleLighting();
        }
        return null;
    }

    public static boolean hasCandleLightingToday(AlarmLocation clientsLocation, Context context) {

        PreferencesService preferencesService = new PreferencesService(context);
        if (preferencesService.isTestMode()) {
            return true;
        }

        boolean inIsrael = isInIsrael(clientsLocation);

        JewishCalendar jc = new JewishCalendar();
        jc.setInIsrael(inIsrael);

        return jc.hasCandleLighting();
    }

    private static boolean isInIsrael(AlarmLocation alarmLocation) {
        return alarmLocation.getCityCode().endsWith("_IL");
    }

    public static String getHebrewDateStringFromDate(Date alarmDate) {

        JewishDate jd = new JewishDate(alarmDate);
        HebrewDateFormatter hdf = new HebrewDateFormatter();
        hdf.setHebrewFormat(true);
        String hebrewDate = hdf.format(jd);

        return hebrewDate;
    }

    public static boolean isNowAssurBemlacha(AlarmLocation alarmLocation) {

        boolean inIsrael = isInIsrael(alarmLocation);
        ZmanimCalendar zcalToday = new ZmanimCalendar();
        return zcalToday.isAssurBemlacha(new Date(), zcalToday.getTzais(), inIsrael);

    }
}
