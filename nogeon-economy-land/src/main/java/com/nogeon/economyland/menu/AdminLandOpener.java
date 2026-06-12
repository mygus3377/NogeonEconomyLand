package com.nogeon.economyland.menu;

import com.nogeon.economyland.state.EconomyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class AdminLandOpener {
    private AdminLandOpener() {
    }

    public static void open(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            return;
        }
        EconomyState state = EconomyState.get(player.server);
        AdminLandMenu snapshot = new AdminLandMenu(0, state);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new AdminLandMenu(containerId, state),
            Component.translatable("screen.nogeon_economy_land.admin_land")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
