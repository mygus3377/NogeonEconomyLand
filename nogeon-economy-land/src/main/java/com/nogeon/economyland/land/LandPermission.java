package com.nogeon.economyland.land;

import java.util.Locale;

public enum LandPermission {
    NONE("none"),
    INTERACT("interact"),
    BUILD("build");

    private final String id;

    LandPermission(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static LandPermission byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (LandPermission permission : values()) {
            if (permission.id.equals(normalized)) {
                return permission;
            }
        }
        return NONE;
    }
}
