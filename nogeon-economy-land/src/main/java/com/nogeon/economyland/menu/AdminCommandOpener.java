package com.nogeon.economyland.menu;

import java.util.Comparator;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class AdminCommandOpener {
    private AdminCommandOpener() {
    }

    public static void open(ServerPlayer player) {
        if (!player.hasPermissions(2)) {
            player.displayClientMessage(Component.literal("OP 권한이 필요합니다."), false);
            return;
        }
        List<String> players = player.server.getPlayerList().getPlayers().stream()
            .map(serverPlayer -> serverPlayer.getGameProfile().getName())
            .sorted(Comparator.naturalOrder())
            .toList();
        AdminCommandMenu snapshot = new AdminCommandMenu(0, players);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new AdminCommandMenu(containerId, players),
            Component.literal("관리자 명령 콘솔")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
