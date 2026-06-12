package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeBrowserOpener;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TradeCancelPacket {
    private final String partnerId;

    public TradeCancelPacket(String partnerId) {
        this.partnerId = partnerId;
    }

    public static void encode(TradeCancelPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.partnerId);
    }

    public static TradeCancelPacket decode(FriendlyByteBuf buffer) {
        return new TradeCancelPacket(buffer.readUtf());
    }

    public static void handle(TradeCancelPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }
            EconomyState state = EconomyState.get(sender.server);
            ServerPlayer partner = state.partnerPlayer(sender);
            state.cancelTrade(sender.getUUID());
            sender.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.cancelled"), false);
            TradeBrowserOpener.open(sender);
            if (partner != null) {
                partner.displayClientMessage(Component.translatable("message.nogeon_economy_land.trade.cancelled_partner", sender.getName()), false);
                TradeBrowserOpener.open(partner);
            }
        });
        context.setPacketHandled(true);
    }
}