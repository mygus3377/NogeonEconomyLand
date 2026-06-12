package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.TradeBrowserOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenTradeBrowserPacket {
    public static void encode(OpenTradeBrowserPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenTradeBrowserPacket decode(FriendlyByteBuf buffer) {
        return new OpenTradeBrowserPacket();
    }

    public static void handle(OpenTradeBrowserPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                TradeBrowserOpener.open(sender);
            }
        });
        context.setPacketHandled(true);
    }
}