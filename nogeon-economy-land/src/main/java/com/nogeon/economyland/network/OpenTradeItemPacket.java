package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeItemOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenTradeItemPacket {
    public static void encode(OpenTradeItemPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenTradeItemPacket decode(FriendlyByteBuf buffer) {
        return new OpenTradeItemPacket();
    }

    public static void handle(OpenTradeItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                TradeItemOpener.open(player);
            }
        });
        context.setPacketHandled(true);
    }
}
