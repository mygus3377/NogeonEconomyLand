package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.trade.TradeSession;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TradeSetCreditsPacket {
    private final String partnerId;
    private final long credits;

    public TradeSetCreditsPacket(String partnerId, long credits) {
        this.partnerId = partnerId;
        this.credits = credits;
    }

    public static void encode(TradeSetCreditsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.partnerId);
        buffer.writeLong(packet.credits);
    }

    public static TradeSetCreditsPacket decode(FriendlyByteBuf buffer) {
        return new TradeSetCreditsPacket(buffer.readUtf(), buffer.readLong());
    }

    public static void handle(TradeSetCreditsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            if (!state.setTradeCredits(sender, packet.credits)) {
                sender.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.credit_invalid"), false);
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