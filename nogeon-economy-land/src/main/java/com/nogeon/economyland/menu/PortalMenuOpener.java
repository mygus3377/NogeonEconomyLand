package com.nogeon.economyland.menu;

import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class PortalMenuOpener {
    private PortalMenuOpener() {
    }

    public static void open(ServerPlayer player) {
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        PortalMenu snapshot = new PortalMenu(0, profile);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new PortalMenu(containerId, profile),
            Component.translatable("screen.nogeon_economy_land.portal")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
