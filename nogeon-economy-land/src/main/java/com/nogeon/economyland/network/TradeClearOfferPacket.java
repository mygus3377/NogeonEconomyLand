package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.trade.TradeSession;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TradeClearOfferPacket {
    private final String partnerId;

    public TradeClearOfferPacket(String partnerId) {
        this.partnerId = partnerId;
    }

    public static void encode(TradeClearOfferPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.partnerId);
    }

    public static TradeClearOfferPacket decode(FriendlyByteBuf buffer) {
        return new TradeClearOfferPacket(buffer.readUtf());
    }

    public static void handle(TradeClearOfferPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            state.clearTradeOffer(sender);
            TradeSession session = state.tradeSession(sender.getUUID());
            ServerPlayer partner = state.partnerPlayer(sender);
            if (session != null && partner != null) {
                TradeOpener.refreshBoth(sender, partner, session);
            }
        });
        context.setPacketHandled(true);
    }
}