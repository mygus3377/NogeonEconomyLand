package com.nogeon.economyland.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class ShopItemProtection {
    private static final String SHOP_PURCHASED_TAG = "NoGeonShopPurchased";
    private static final String ITEM_LOCKED_TAG = "NoGeonItemLocked";

    private ShopItemProtection() {
    }

    public static void markPurchased(ItemStack stack) {
        if (!stack.isEmpty()) {
            stack.getOrCreateTag().putBoolean(SHOP_PURCHASED_TAG, true);
        }
    }

    public static boolean isShopPurchased(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(SHOP_PURCHASED_TAG);
    }

    public static boolean isLocked(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(ITEM_LOCKED_TAG);
    }

    public static boolean isSellBlocked(ItemStack stack) {
        return isLocked(stack);
    }

    public static boolean toggleLocked(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        boolean locked = !isLocked(stack);
        stack.getOrCreateTag().putBoolean(ITEM_LOCKED_TAG, locked);
        return locked;
    }
}
