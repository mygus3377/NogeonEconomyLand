package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.WalletOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenWalletPacket {
    public static void encode(OpenWalletPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenWalletPacket decode(FriendlyByteBuf buffer) {
        return new OpenWalletPacket();
    }

    public static void handle(OpenWalletPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                WalletOpener.open(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
