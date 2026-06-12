package com.nogeon.economyland.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;

public final class SocketUpgradeOpener {
    private SocketUpgradeOpener() {
    }

    public static void open(ServerPlayer player, int selectedSlot, Component status) {
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.nogeon_economy_land.socket_upgrade");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new SocketUpgradeMenu(containerId, selectedSlot, status);
            }
        }, buffer -> new SocketUpgradeMenu(0, selectedSlot, status).write(buffer));
    }
}
