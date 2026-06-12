package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class AdminCommandMenu extends AbstractContainerMenu {
    private final List<String> onlinePlayers;

    public AdminCommandMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.ADMIN_COMMAND.get(), containerId);
        int count = buffer.readVarInt();
        onlinePlayers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            onlinePlayers.add(buffer.readUtf());
        }
    }

    public AdminCommandMenu(int containerId, List<String> onlinePlayers) {
        super(ModMenus.ADMIN_COMMAND.get(), containerId);
        this.onlinePlayers = List.copyOf(onlinePlayers);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(onlinePlayers.size());
        for (String name : onlinePlayers) {
            buffer.writeUtf(name);
        }
    }

    public List<String> onlinePlayers() {
        return onlinePlayers;
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
