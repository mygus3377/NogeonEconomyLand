package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class HelpOpener {
    private HelpOpener() {
    }

    public static void open(ServerPlayer player) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new HelpMenu(containerId),
            Component.translatable("screen.nogeon_economy_land.help")
        ), (FriendlyByteBuf buffer) -> {
        });
    }
}
