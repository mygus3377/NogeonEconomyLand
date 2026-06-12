package com.nogeon.economyland.land;

import java.util.Locale;

public enum LandFlag {
    DENY_MONSTER_SPAWN("deny_monster_spawn", "gui.nogeon_economy_land.land_flag.deny_monster_spawn", true),
    NPC_INVINCIBLE("npc_invincible", "gui.nogeon_economy_land.land_flag.npc_invincible", true),
    DENY_TRAMPLE("deny_trample", "gui.nogeon_economy_land.land_flag.deny_trample", true);

    private final String id;
    private final String translationKey;
    private final boolean defaultValue;

    LandFlag(String id, String translationKey, boolean defaultValue) {
        this.id = id;
        this.translationKey = translationKey;
        this.defaultValue = defaultValue;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    public boolean defaultValue() {
        return defaultValue;
    }

    public static LandFlag byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (LandFlag flag : values()) {
            if (flag.id.equals(normalized)) {
                return flag;
            }
        }
        throw new IllegalArgumentException("Unknown land flag: " + id);
    }
}
