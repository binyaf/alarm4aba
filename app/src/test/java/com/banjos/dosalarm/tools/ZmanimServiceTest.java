package com.banjos.dosalarm.tools;


import static org.junit.Assert.assertEquals;

import android.content.Context;

import com.banjos.dosalarm.types.AlarmLocation;

import org.junit.Test;
import org.mockito.Mock;

import java.util.Date;


public class ZmanimServiceTest {

    @Mock
    Context mockContext;

    @Test
    public void getHebrewDateStringFromDateTest() {

        Date date = new Date();
        date.setTime(1893448800000l);  //2030/01/01 00:00
        String hebrewDate = ZmanimService.getHebrewDateStringFromDate(date);
        System.out.println(hebrewDate);
        assertEquals("כ״ו טבת תש״צ", hebrewDate);
    }


    @Test
    public void getCandleLightingTimeTodayTest() {

        AlarmLocation alarmLocation = new AlarmLocation("JLM_IL", 754, 31.7683,
                35.2137, "Asia/Jerusalem");
        Date candleLightingTimeToday = ZmanimService.getCandleLightingTimeToday(alarmLocation, mockContext);
        System.out.println("today has candle lighting? " + candleLightingTimeToday);
    }
}
