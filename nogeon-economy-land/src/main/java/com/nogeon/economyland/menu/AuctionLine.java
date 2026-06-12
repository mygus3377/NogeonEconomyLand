package com.nogeon.economyland.menu;

import net.minecraft.world.item.ItemStack;

public record AuctionLine(int auctionId, String sellerName, String itemId, String itemKey, int count, long price, boolean mine, ItemStack stack) {
}