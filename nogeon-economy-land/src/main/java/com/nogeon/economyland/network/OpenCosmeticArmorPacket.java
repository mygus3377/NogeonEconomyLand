package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.CosmeticArmorOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenCosmeticArmorPacket {
    public static void encode(OpenCosmeticArmorPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenCosmeticArmorPacket decode(FriendlyByteBuf buffer) {
        return new OpenCosmeticArmorPacket();
    }

    public static void handle(OpenCosmeticArmorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                CosmeticArmorOpener.open(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
