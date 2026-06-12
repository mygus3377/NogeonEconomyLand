package com.nogeon.economyland.state;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public record AuctionEntry(int id, UUID sellerId, String sellerName, String itemId, int count, long price, CompoundTag stackNbt) {
    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("id", id);
        nbt.putUUID("sellerId", sellerId);
        nbt.putString("sellerName", sellerName);
        nbt.putString("itemId", itemId);
        nbt.putInt("count", count);
        nbt.putLong("price", price);
        if (stackNbt != null && !stackNbt.isEmpty()) {
            nbt.put("stack", stackNbt.copy());
        }
        return nbt;
    }

    public ItemStack stack() {
        if (stackNbt != null && !stackNbt.isEmpty()) {
            return ItemStack.of(stackNbt.copy());
        }
        return ItemStack.EMPTY;
    }

    public static AuctionEntry fromNbt(CompoundTag nbt) {
        return new AuctionEntry(
            nbt.getInt("id"),
            nbt.getUUID("sellerId"),
            nbt.getString("sellerName"),
            nbt.getString("itemId"),
            Math.max(1, nbt.getInt("count")),
            Math.max(0L, nbt.getLong("price")),
            nbt.contains("stack", CompoundTag.TAG_COMPOUND) ? nbt.getCompound("stack") : new CompoundTag()
        );
    }
}