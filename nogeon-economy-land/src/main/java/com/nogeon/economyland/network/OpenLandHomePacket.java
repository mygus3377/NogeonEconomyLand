package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.LandHomeOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenLandHomePacket {
    public static void encode(OpenLandHomePacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenLandHomePacket decode(FriendlyByteBuf buffer) {
        return new OpenLandHomePacket();
    }

    public static void handle(OpenLandHomePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                LandHomeOpener.open(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
