package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.AuctionOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenAuctionPacket {
    public static void encode(OpenAuctionPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenAuctionPacket decode(FriendlyByteBuf buffer) {
        return new OpenAuctionPacket();
    }

    public static void handle(OpenAuctionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                AuctionOpener.open(sender);
            }
        });
        context.setPacketHandled(true);
    }
}