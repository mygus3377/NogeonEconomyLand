package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.HelpOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenHelpPacket {
    public static void encode(OpenHelpPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenHelpPacket decode(FriendlyByteBuf buffer) {
        return new OpenHelpPacket();
    }

    public static void handle(OpenHelpPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                HelpOpener.open(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
