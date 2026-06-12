package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.AdminLandOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenAdminLandPacket {
    public static void encode(OpenAdminLandPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenAdminLandPacket decode(FriendlyByteBuf buffer) {
        return new OpenAdminLandPacket();
    }

    public static void handle(OpenAdminLandPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2)) {
                AdminLandOpener.open(player);
            }
        });
        context.setPacketHandled(true);
    }
}
