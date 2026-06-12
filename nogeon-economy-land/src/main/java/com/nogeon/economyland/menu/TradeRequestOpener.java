package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class TradeRequestOpener {
    private TradeRequestOpener() {
    }

    public static void open(ServerPlayer target, ServerPlayer requester) {
        TradeRequestMenu snapshot = new TradeRequestMenu(0, requester.getUUID().toString(), requester.getName().getString());
        NetworkHooks.openScreen(target, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new TradeRequestMenu(containerId, requester.getUUID().toString(), requester.getName().getString()),
            Component.translatable("screen.nogeon_economy_land.trade_request")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}