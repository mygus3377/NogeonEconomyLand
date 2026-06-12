package com.nogeon.economyland.shop;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ShopEntry(String id, ItemStack stack, long price, int dailyLimit) {
    public ShopEntry {
        stack = stack.copy();
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("ShopEntry stack cannot be empty");
        }
    }

    public ShopEntry(String id, Item item, int count, long price, int dailyLimit) {
        this(id, new ItemStack(item, count), price, dailyLimit);
    }

    @Override
    public ItemStack stack() {
        return stack.copy();
    }

    public Item item() {
        return stack.getItem();
    }

    public int count() {
        return stack.getCount();
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", id);
        nbt.put("stack", stack.save(new CompoundTag()));
        nbt.putLong("price", price);
        nbt.putInt("dailyLimit", dailyLimit);
        return nbt;
    }

    public static ShopEntry fromNbt(CompoundTag nbt) {
        if (nbt.contains("stack")) {
            return new ShopEntry(nbt.getString("id"), ItemStack.of(nbt.getCompound("stack")), nbt.getLong("price"), nbt.getInt("dailyLimit"));
        }
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(nbt.getString("item")));
        return new ShopEntry(nbt.getString("id"), item, nbt.getInt("count"), nbt.getLong("price"), nbt.getInt("dailyLimit"));
    }
}
