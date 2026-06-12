package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class EnhancementScrollMenu extends AbstractContainerMenu {
    private final int selectedSlot;
    private final Component status;

    public EnhancementScrollMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.ENHANCEMENT_SCROLL.get(), containerId);
        selectedSlot = buffer.readVarInt();
        status = buffer.readComponent();
    }

    public EnhancementScrollMenu(int containerId, int selectedSlot, Component status) {
        super(ModMenus.ENHANCEMENT_SCROLL.get(), containerId);
        this.selectedSlot = selectedSlot;
        this.status = status;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(selectedSlot);
        buffer.writeComponent(status);
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
