package com.nogeon.economyland.entity;

public enum TraderKind {
    GENERAL("general", "trader.nogeon_economy_land.general"),
    CROP("crop", "trader.nogeon_economy_land.crop"),
    FISHER("fisher", "trader.nogeon_economy_land.fisher"),
    MINER("miner", "trader.nogeon_economy_land.miner"),
    CHEF("chef", "trader.nogeon_economy_land.chef"),
    LOTTERY("lottery", "trader.nogeon_economy_land.lottery"),
    GAMBLER("gambler", "trader.nogeon_economy_land.gambler"),
    GACHA("gacha", "trader.nogeon_economy_land.gacha"),
    POTION("potion", "trader.nogeon_economy_land.potion"),
    SMITH("smith", "trader.nogeon_economy_land.smith"),
    LAND("land", "trader.nogeon_economy_land.land"),
    AUCTION("auction", "trader.nogeon_economy_land.auction"),
    GUN("gun", "trader.nogeon_economy_land.gun"),
    HUNTER("hunter", "trader.nogeon_economy_land.hunter"),
    ENGINEER("engineer", "trader.nogeon_economy_land.engineer");

    private final String id;
    private final String translationKey;

    TraderKind(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    public boolean supportsInventoryShop() {
        return switch (this) {
            case GENERAL, CROP, FISHER, MINER, CHEF, POTION, GUN, SMITH, ENGINEER -> true;
            default -> false;
        };
    }

    public static TraderKind byId(String id) {
        for (TraderKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        return GENERAL;
    }
}
