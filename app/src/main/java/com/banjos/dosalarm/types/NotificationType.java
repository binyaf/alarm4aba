package com.banjos.dosalarm.types;

public enum NotificationType {

    CANDLE_LIGHTING("CANDLE_LIGHTING", 1),
    SHACHARIT_REMINDER("SHACHARIT_REMINDER", 2),
    STOP_SHACHARIT_REMINDER("STOP_SHACHARIT_REMINDER", 2),
    SNOOZE_SHACHARIT_REMINDER("SNOOZE_SHACHARIT_REMINDER", 2),

    MINCHA_REMINDER("MINCHA_REMINDER", 3),
    STOP_MINCHA_REMINDER("STOP_MINCHA_REMINDER", 3),
    SNOOZE_MINCHA_REMINDER("SNOOZE_MINCHA_REMINDER", 3),
    MAARIV_REMINDER("MAARIV_REMINDER", 4),
    STOP_MAARIV_REMINDER("STOP_MAARIV_REMINDER", 4),
    SNOOZE_MAARIV_REMINDER("SNOOZE_MAARIV_REMINDER", 4);


    private final String type;
    private final int id;


    NotificationType(String type, int id) {
        this.type = type;
        this.id = id;
    }

    // Getter for the string value
    public String getType() {
        return type;
    }

    // Getter for the int value
    public int getId() {
        return id;
    }

}
