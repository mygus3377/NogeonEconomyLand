package com.nogeon.economyland.menu;

import com.nogeon.economyland.item.ReforgeService;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;

public final class ReforgeOpener {
    public static void open(ServerPlayer player, int selectedSlot, Component status) {
        if (selectedSlot >= 0 && selectedSlot < player.getInventory().getContainerSize()) {
            if (ReforgeService.migrateBalance(player.getInventory().getItem(selectedSlot))) {
                player.inventoryMenu.broadcastChanges();
                EconomyState.get(player.server).setDirty();
            }
        }
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.nogeon_economy_land.reforge");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new ReforgeMenu(containerId, selectedSlot, status);
            }
        }, buffer -> {
            new ReforgeMenu(0, selectedSlot, status).write(buffer);
        });
    }
}
