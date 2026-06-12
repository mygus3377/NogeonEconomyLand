package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class SocketUpgradeMenu extends AbstractContainerMenu {
    private final int selectedSlot;
    private final Component status;

    public SocketUpgradeMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.SOCKET_UPGRADE.get(), containerId);
        this.selectedSlot = buffer.readVarInt();
        this.status = buffer.readComponent();
    }

    public SocketUpgradeMenu(int containerId, int selectedSlot, Component status) {
        super(ModMenus.SOCKET_UPGRADE.get(), containerId);
        this.selectedSlot = selectedSlot;
        this.status = status;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(selectedSlot);
        buffer.writeComponent(status != null ? status : Component.empty());
    }

    public int selectedSlot() {
        return selectedSlot;
    }

    public Component status() {
        return status;
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
