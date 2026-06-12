package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeBrowserOpener;
import com.nogeon.economyland.menu.TradeOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.trade.TradeSession;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TradeConfirmPacket {
    private final String partnerId;

    public TradeConfirmPacket(String partnerId) {
        this.partnerId = partnerId;
    }

    public static void encode(TradeConfirmPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.partnerId);
    }

    public static TradeConfirmPacket decode(FriendlyByteBuf buffer) {
        return new TradeConfirmPacket(buffer.readUtf());
    }

    public static void handle(TradeConfirmPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            TradeSession session = state.tradeSession(sender.getUUID());
            ServerPlayer partner = state.partnerPlayer(sender);
            if (session == null || partner == null) {
                return;
            }
            state.confirmTrade(sender);
            if (session.fullyConfirmed()) {
                if (state.finalizeTrade(sender.server, sender.getUUID())) {
                    sender.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.completed"), false);
                    partner.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.completed"), false);
                    TradeBrowserOpener.open(sender);
                    TradeBrowserOpener.open(partner);
                } else {
                    sender.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.failed"), false);
                    partner.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.failed"), false);
                }
                return;
            }
            TradeOpener.refreshBoth(sender, partner, session);
        });
        context.setPacketHandled(true);
    }
}