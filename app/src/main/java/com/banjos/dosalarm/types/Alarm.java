package com.banjos.dosalarm.types;

import java.io.Serializable;
import java.util.Calendar;
import java.util.UUID;

public class Alarm implements Serializable {

    private int id;

    private AlarmType type;
    private Calendar dateAndTime;
    private int duration;

    private String label;
    private boolean selected;

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Alarm alarm = (Alarm) o;

        if (id != alarm.id) return false;
        if (duration != alarm.duration) return false;
        if (type != alarm.type) return false;
        if (dateAndTime != null ? !dateAndTime.equals(alarm.dateAndTime) : alarm.dateAndTime != null)
            return false;
        if (selected != alarm.selected) return false;
        return label != null ? label.equals(alarm.label) : alarm.label == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (dateAndTime != null ? dateAndTime.hashCode() : 0);
        result = 31 * result + duration;
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (selected ? 1 : 0);
        return result;
    }
}
