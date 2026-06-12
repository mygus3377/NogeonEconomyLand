package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class JobChangeMenu extends AbstractContainerMenu {
    private final String targetJobId;

    public JobChangeMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.JOB_CHANGE.get(), containerId);
        targetJobId = buffer.readUtf();
    }

    public JobChangeMenu(int containerId, String targetJobId) {
        super(ModMenus.JOB_CHANGE.get(), containerId);
        this.targetJobId = targetJobId == null ? "" : targetJobId;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(targetJobId);
    }

    public String targetJobId() {
        return targetJobId;
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
