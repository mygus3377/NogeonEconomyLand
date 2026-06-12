package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class TradeItemMenu extends AbstractContainerMenu {
    private final String partnerId;
    private final List<ItemStack> items;

    public TradeItemMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.TRADE_ITEM.get(), containerId);
        partnerId = buffer.readUtf();
        items = new ArrayList<>();
        int count = buffer.readVarInt();
        for (int index = 0; index < count; index++) {
            items.add(buffer.readItem());
        }
    }

    public TradeItemMenu(int containerId, String partnerId, List<ItemStack> items) {
        super(ModMenus.TRADE_ITEM.get(), containerId);
        this.partnerId = partnerId;
        this.items = items;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(partnerId);
        buffer.writeVarInt(items.size());
        for (ItemStack stack : items) {
            buffer.writeItem(stack);
        }
    }

    public String partnerId() {
        return partnerId;
    }

    public List<ItemStack> items() {
        return items;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
