package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.JobChangeOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenJobChangePacket {
    private final String targetJobId;

    public OpenJobChangePacket(String targetJobId) {
        this.targetJobId = targetJobId == null ? "" : targetJobId;
    }

    public static void encode(OpenJobChangePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.targetJobId);
    }

    public static OpenJobChangePacket decode(FriendlyByteBuf buffer) {
        return new OpenJobChangePacket(buffer.readUtf());
    }

    public static void handle(OpenJobChangePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                JobChangeOpener.open(player, packet.targetJobId);
            }
        });
        context.setPacketHandled(true);
    }
}
