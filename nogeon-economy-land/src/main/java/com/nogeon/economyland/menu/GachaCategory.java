package com.nogeon.economyland.menu;

public enum GachaCategory {
    WEAPON("weapon", "gui.nogeon_economy_land.gacha_category_weapon"),
    ARMOR("armor", "gui.nogeon_economy_land.gacha_category_armor"),
    ITEM("item", "gui.nogeon_economy_land.gacha_category_item"),
    GUN_BOW("gun_bow", "gui.nogeon_economy_land.gacha_category_gun_bow");

    private final String id;
    private final String translationKey;

    GachaCategory(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    public static GachaCategory byId(String id) {
        for (GachaCategory value : values()) {
            if (value.id.equals(id)) {
                return value;
            }
        }
        return ITEM;
    }
}