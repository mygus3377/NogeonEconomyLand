package com.nogeon.economyland.menu;

import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.network.SyncCreditsPacket;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class WalletOpener {
    private WalletOpener() {
    }

    public static void open(ServerPlayer player) {
        PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
        SyncCreditsPacket.send(player, profile.credits());
        boolean admin = player.hasPermissions(2);
        WalletMenu snapshot = new WalletMenu(0, profile, admin);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new WalletMenu(containerId, profile, admin),
            Component.translatable("screen.nogeon_economy_land.wallet")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
