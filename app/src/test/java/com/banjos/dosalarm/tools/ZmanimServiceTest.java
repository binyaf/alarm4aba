package com.banjos.dosalarm.tools;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Date;


public class ZmanimServiceTest {

    @Test
    public void getHebrewDateStringFromDateTest() {

        Date date = new Date();
        date.setTime(1893448800000l);  //2030/01/01 00:00
        String hebrewDate = ZmanimService.getHebrewDateStringFromDate(date);
        System.out.println(hebrewDate);
        assertEquals("כ״ו טבת תש״צ", hebrewDate);

    }
}
