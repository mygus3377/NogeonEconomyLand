package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.trade.TradeSession;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TradeChatPacket {
    private final String partnerId;
    private final String message;

    public TradeChatPacket(String partnerId, String message) {
        this.partnerId = partnerId;
        this.message = message;
    }

    public static void encode(TradeChatPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.partnerId);
        buffer.writeUtf(packet.message, 128);
    }

    public static TradeChatPacket decode(FriendlyByteBuf buffer) {
        return new TradeChatPacket(buffer.readUtf(), buffer.readUtf(128));
    }

    public static void handle(TradeChatPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            TradeSession session = state.tradeSession(sender.getUUID());
            if (session == null) {
                return;
            }
            String message = packet.message == null ? "" : packet.message.trim();
            if (message.isEmpty()) {
                return;
            }
            if (message.length() > 96) {
                message = message.substring(0, 96);
            }
            session.addChat(sender.getUUID(), sender.getName().getString(), message);
            ServerPlayer partner = state.partnerPlayer(sender);
            if (session != null && partner != null) {
                TradeOpener.refreshBoth(sender, partner, session);
            }
        });
        context.setPacketHandled(true);
    }
}