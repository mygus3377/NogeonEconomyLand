package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class JobChangeOpener {
    private JobChangeOpener() {
    }

    public static void open(ServerPlayer player, String targetJobId) {
        JobChangeMenu snapshot = new JobChangeMenu(0, targetJobId);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new JobChangeMenu(containerId, targetJobId),
            Component.translatable("screen.nogeon_economy_land.job_change")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
