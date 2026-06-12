package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeBrowserOpener;
import com.nogeon.economyland.menu.TradeOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.trade.TradeSession;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TradeRespondPacket {
    private final String requesterId;
    private final boolean accept;

    public TradeRespondPacket(String requesterId, boolean accept) {
        this.requesterId = requesterId;
        this.accept = accept;
    }

    public static void encode(TradeRespondPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.requesterId);
        buffer.writeBoolean(packet.accept);
    }

    public static TradeRespondPacket decode(FriendlyByteBuf buffer) {
        return new TradeRespondPacket(buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(TradeRespondPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer responder = context.getSender();
            if (responder == null) {
                return;
            }
            EconomyState state = EconomyState.get(responder.server);
            UUID requesterId = UUID.fromString(packet.requesterId);
            ServerPlayer requester = responder.server.getPlayerList().getPlayer(requesterId);
            if (requester == null) {
                state.clearTradeRequest(responder.getUUID());
                TradeBrowserOpener.open(responder);
                return;
            }
            if (!packet.accept) {
                state.clearTradeRequest(responder.getUUID());
                requester.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.request_denied", responder.getName()), false);
                TradeBrowserOpener.open(requester);
                TradeBrowserOpener.open(responder);
                return;
            }

            TradeSession session = state.acceptTrade(responder, requester);
            if (session == null) {
                responder.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.busy"), false);
                requester.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.busy"), false);
                TradeBrowserOpener.open(responder);
                TradeBrowserOpener.open(requester);
                return;
            }
            TradeOpener.refreshBoth(requester, responder, session);
        });
        context.setPacketHandled(true);
    }
}