package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.GachaStorageOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenGachaStoragePacket {
    public static void encode(OpenGachaStoragePacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenGachaStoragePacket decode(FriendlyByteBuf buffer) {
        return new OpenGachaStoragePacket();
    }

    public static void handle(OpenGachaStoragePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                GachaStorageOpener.open(player);
            }
        });
        context.setPacketHandled(true);
    }
}
