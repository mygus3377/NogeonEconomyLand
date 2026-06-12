package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.trade.TradeSession;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TradeOfferLandPacket {
    private final String partnerId;
    private final int landId;

    public TradeOfferLandPacket(String partnerId, int landId) {
        this.partnerId = partnerId;
        this.landId = landId;
    }

    public static void encode(TradeOfferLandPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.partnerId);
        buffer.writeVarInt(packet.landId);
    }

    public static TradeOfferLandPacket decode(FriendlyByteBuf buffer) {
        return new TradeOfferLandPacket(buffer.readUtf(), buffer.readVarInt());
    }

    public static void handle(TradeOfferLandPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            String errorKey = state.addLandTradeOffer(sender, packet.landId);
            if (errorKey != null) {
                sender.displayClientMessage(Component.translatable(errorKey), false);
                return;
            }
            TradeSession session = state.tradeSession(sender.getUUID());
            ServerPlayer partner = state.partnerPlayer(sender);
            if (session != null && partner != null) {
                TradeOpener.refreshBoth(sender, partner, session);
            }
        });
        context.setPacketHandled(true);
    }
}