package com.nogeon.economyland.player;

import java.util.Locale;

public enum SkillType {
    DELIVERY_BONUS("delivery_bonus", 5),
    EFFICIENCY("efficiency", 5),
    CONVENIENCE("convenience", 3);

    private final String id;
    private final int maxLevel;

    SkillType(String id, int maxLevel) {
        this.id = id;
        this.maxLevel = maxLevel;
    }

    public String id() {
        return id;
    }

    public int maxLevel() {
        return maxLevel;
    }

    public static SkillType byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (SkillType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown skill: " + id);
    }
}
