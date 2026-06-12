package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class TradeRequestMenu extends AbstractContainerMenu {
    private final String requesterId;
    private final String requesterName;

    public TradeRequestMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.TRADE_REQUEST.get(), containerId);
        requesterId = buffer.readUtf();
        requesterName = buffer.readUtf();
    }

    public TradeRequestMenu(int containerId, String requesterId, String requesterName) {
        super(ModMenus.TRADE_REQUEST.get(), containerId);
        this.requesterId = requesterId;
        this.requesterName = requesterName;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(requesterId);
        buffer.writeUtf(requesterName);
    }

    public String requesterId() {
        return requesterId;
    }

    public String requesterName() {
        return requesterName;
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