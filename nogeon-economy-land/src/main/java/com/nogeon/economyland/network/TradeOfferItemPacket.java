package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.trade.TradeSession;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TradeOfferItemPacket {
    private final String itemId;

    public TradeOfferItemPacket(String itemId) {
        this.itemId = itemId == null ? "" : itemId;
    }

    public static void encode(TradeOfferItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.itemId);
    }

    public static TradeOfferItemPacket decode(FriendlyByteBuf buffer) {
        return new TradeOfferItemPacket(buffer.readUtf());
    }

    public static void handle(TradeOfferItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            if (!state.addInventoryTradeOffer(sender, packet.itemId)) {
                sender.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.item_unavailable"), false);
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
