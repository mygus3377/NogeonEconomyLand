package com.nogeon.economyland.player;

import java.util.Locale;

public enum JobType {
    FARMER("farmer", "농사꾼"),
    FISHER("fisher", "어부"),
    MINER("miner", "광부"),
    COOK("cook", "요리사"),
    HUNTER("hunter", "사냥꾼"),
    ENGINEER("engineer", "공학자");

    private final String id;
    private final String displayName;

    JobType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static JobType byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (JobType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown job: " + id);
    }
}
