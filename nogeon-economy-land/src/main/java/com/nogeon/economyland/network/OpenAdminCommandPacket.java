package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.AdminCommandOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenAdminCommandPacket {
    public static void encode(OpenAdminCommandPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenAdminCommandPacket decode(FriendlyByteBuf buffer) {
        return new OpenAdminCommandPacket();
    }

    public static void handle(OpenAdminCommandPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                AdminCommandOpener.open(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
