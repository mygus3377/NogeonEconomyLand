package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class DroneStorageOpener {
    private DroneStorageOpener() {}

    public static void open(ServerPlayer player) {
        int invLvl = player.getPersistentData().getInt("nogeon_engineer_drone_upgrade_inventory_level");
        if (invLvl <= 0 && player.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_inventory")) {
            invLvl = 1;
            player.getPersistentData().putInt("nogeon_engineer_drone_upgrade_inventory_level", 1);
        }
        final int level = invLvl;

        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new DroneStorageMenu(containerId, inventory, level),
            Component.literal("드론 보관함")
        ), (FriendlyByteBuf buffer) -> {
            buffer.writeVarInt(level);
        });
    }
}
