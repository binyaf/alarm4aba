package com.banjos.dosalarm.types;

public enum NotificationType {

    CANDLE_LIGHTING_REMINDER("CANDLE_LIGHTING_REMINDER", 1, 10),
    STOP_CANDLE_LIGHTING_REMINDER("STOP_CANDLE_LIGHTING_REMINDER", 1, 11),
    SNOOZE_CANDLE_LIGHTING_REMINDER("SNOOZE_CANDLE_LIGHTING_REMINDER", 1, 12),
    SHACHARIT_REMINDER("SHACHARIT_REMINDER", 2, 20),
    STOP_SHACHARIT_REMINDER("STOP_SHACHARIT_REMINDER", 2, 21),
    SNOOZE_SHACHARIT_REMINDER("SNOOZE_SHACHARIT_REMINDER", 2, 22),
    MINCHA_REMINDER("MINCHA_REMINDER", 3, 30),
    STOP_MINCHA_REMINDER("STOP_MINCHA_REMINDER", 3, 31),
    SNOOZE_MINCHA_REMINDER("SNOOZE_MINCHA_REMINDER", 3, 32),
    MAARIV_REMINDER("MAARIV_REMINDER", 4, 40),
    STOP_MAARIV_REMINDER("STOP_MAARIV_REMINDER", 4, 41),
    SNOOZE_MAARIV_REMINDER("SNOOZE_MAARIV_REMINDER", 4, 42);


    private final String type;
    private final int id;
    private int requestCode;

    NotificationType(String type, int id, int requestCode) {
        this.type = type;
        this.id = id;
        this.requestCode = requestCode;
    }

    // Getter for the string value
    public String getType() {
        return type;
    }

    // Getter for the int value
    public int getId() {
        return id;
    }

    public int getRequestCode() {
        return requestCode;
    }

}
