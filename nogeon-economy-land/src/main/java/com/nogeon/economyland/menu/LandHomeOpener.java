package com.nogeon.economyland.menu;

import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class LandHomeOpener {
    private LandHomeOpener() {
    }

    public static void open(ServerPlayer player) {
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        LandHomeMenu snapshot = new LandHomeMenu(0, profile, state, player.getUUID(), player.hasPermissions(2));
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new LandHomeMenu(containerId, profile, state, player.getUUID(), player.hasPermissions(2)),
            Component.translatable("screen.nogeon_economy_land.land_home")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
