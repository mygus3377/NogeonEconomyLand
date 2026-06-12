package com.nogeon.economyland.land;

import java.util.Locale;

public enum LandType {
    BASIC("basic", 0, false),
    NORMAL("normal", 100, true),
    INDUSTRIAL("industrial", 100, true),
    ADMIN("admin", 0, true);

    private final String id;
    private final long pricePerBlock;
    private final boolean protectedLand;

    LandType(String id, long pricePerBlock, boolean protectedLand) {
        this.id = id;
        this.pricePerBlock = pricePerBlock;
        this.protectedLand = protectedLand;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "land_type.nogeon_economy_land." + id;
    }

    public long pricePerBlock() {
        return pricePerBlock;
    }

    public boolean protectedLand() {
        return protectedLand;
    }

    public static LandType byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (LandType type : values()) {
            if (type.id.equals(normalized)) {
                return type;
            }
        }
        return BASIC;
    }
}
