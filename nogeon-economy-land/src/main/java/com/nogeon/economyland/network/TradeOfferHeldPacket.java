package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.trade.TradeSession;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TradeOfferHeldPacket {
    private final String partnerId;

    public TradeOfferHeldPacket(String partnerId) {
        this.partnerId = partnerId;
    }

    public static void encode(TradeOfferHeldPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.partnerId);
    }

    public static TradeOfferHeldPacket decode(FriendlyByteBuf buffer) {
        return new TradeOfferHeldPacket(buffer.readUtf());
    }

    public static void handle(TradeOfferHeldPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
            if (!state.addHeldTradeOffer(sender)) {
                sender.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.empty_hand"), false);
                return;
            }
            ServerPlayer partner = state.partnerPlayer(sender);
            if (partner != null) {
                TradeOpener.refreshBoth(sender, partner, session);
            }
        });
        context.setPacketHandled(true);
    }
}