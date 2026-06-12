package com.nogeon.economyland.network;

import com.nogeon.economyland.player.HomeTeleportService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class SpawnReturnPacket {
    public static void encode(SpawnReturnPacket packet, FriendlyByteBuf buffer) {
    }

    public static SpawnReturnPacket decode(FriendlyByteBuf buffer) {
        return new SpawnReturnPacket();
    }

    public static void handle(SpawnReturnPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.closeContainer();
                HomeTeleportService.requestSpawn(player);
            }
        });
        context.setPacketHandled(true);
    }
}
