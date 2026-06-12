package com.nogeon.economyland.menu;

import net.minecraft.world.item.ItemStack;

public record ShopLine(String kindId, String id, ItemStack stack, long price, int remaining, boolean delivery, int currentSaturation, int maxSaturation) {
	public int count() {
		return stack.getCount();
	}

    public ShopLine(String kindId, String id, ItemStack stack, long price, int remaining, boolean delivery) {
        this(kindId, id, stack, price, remaining, delivery, 0, 0);
    }
}
