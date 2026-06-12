package com.nogeon.economyland.state;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record GachaRewardResult(ItemStack stack, int rarity, boolean jackpot) {
    public static GachaRewardResult of(Item item, int count, int rarity, boolean jackpot) {
        return of(new ItemStack(item, Math.max(1, count)), rarity, jackpot);
    }

    public static GachaRewardResult of(ItemStack stack, int rarity, boolean jackpot) {
        ItemStack copy = stack.copy();
        if (copy.isEmpty()) {
            copy = new ItemStack(Items.AIR);
        }
        return new GachaRewardResult(copy, rarity, jackpot);
    }

    public String itemId() {
        Item item = stack.getItem();
        return Item.getId(item) == 0 ? "minecraft:air" : item.builtInRegistryHolder().key().location().toString();
    }

    public String itemKey() {
        return stack.getDescriptionId();
    }

    public int count() {
        return stack.getCount();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeItem(stack);
        buffer.writeVarInt(rarity);
        buffer.writeBoolean(jackpot);
    }

    public static GachaRewardResult read(FriendlyByteBuf buffer) {
        return new GachaRewardResult(buffer.readItem(), buffer.readVarInt(), buffer.readBoolean());
    }
}
