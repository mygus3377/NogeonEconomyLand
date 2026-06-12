package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.SkillsOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenSkillsPacket {
    public static void encode(OpenSkillsPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenSkillsPacket decode(FriendlyByteBuf buffer) {
        return new OpenSkillsPacket();
    }

    public static void handle(OpenSkillsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                SkillsOpener.open(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
