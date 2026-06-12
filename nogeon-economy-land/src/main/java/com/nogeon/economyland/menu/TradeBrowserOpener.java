package com.nogeon.economyland.menu;

import com.nogeon.economyland.state.EconomyState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class TradeBrowserOpener {
    private TradeBrowserOpener() {
    }

    public static void open(ServerPlayer player) {
        EconomyState state = EconomyState.get(player.server);
        List<TradeTargetLine> lines = new ArrayList<>();
        for (ServerPlayer nearby : state.nearbyPlayers(player, 8.0D)) {
            lines.add(new TradeTargetLine(
                nearby.getUUID().toString(),
                nearby.getName().getString(),
                (int) Math.round(Math.sqrt(player.distanceToSqr(nearby))),
                state.tradeSession(nearby.getUUID()) != null
            ));
        }
        TradeBrowserMenu snapshot = new TradeBrowserMenu(0, lines);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new TradeBrowserMenu(containerId, lines),
            Component.translatable("screen.nogeon_economy_land.trade_browser")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}