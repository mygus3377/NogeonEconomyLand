package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.ExtendedInventoryOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenExtendedInventoryPacket {
    public static void encode(OpenExtendedInventoryPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenExtendedInventoryPacket decode(FriendlyByteBuf buffer) {
        return new OpenExtendedInventoryPacket();
    }

    public static void handle(OpenExtendedInventoryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                ExtendedInventoryOpener.open(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
