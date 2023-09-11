package com.alarms.myalarm.types;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Calendar;
import java.util.UUID;

public class Alarm implements Serializable {

    private int id;

    private AlarmType type;
    private Calendar dateAndTime;

    private int duration;

    public Alarm(AlarmType alarmType, int alarmDurationSec, Calendar alarmDateAndTime) {
        this.id = UUID.randomUUID().hashCode();
        this.type = alarmType;
        this.dateAndTime = alarmDateAndTime;
        this.duration = alarmDurationSec;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public AlarmType getType() {
        return type;
    }

    public void setType(AlarmType type) {
        this.type = type;
    }

    public Calendar getDateAndTime() {
        return dateAndTime;
    }

    public void setDateAndTime(Calendar dateAndTime) {
        this.dateAndTime = dateAndTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }


}