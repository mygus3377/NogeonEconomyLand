package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class HelpMenu extends AbstractContainerMenu {
    public HelpMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId);
    }

    public HelpMenu(int containerId) {
        super(ModMenus.HELP.get(), containerId);
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
