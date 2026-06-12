package com.nogeon.economyland.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class OpenCompressionScreenPacket {
    private final int remainingCooldownticks;

    public OpenCompressionScreenPacket(int remainingCooldownticks) {
        this.remainingCooldownticks = remainingCooldownticks;
    }

    public static void encode(OpenCompressionScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.remainingCooldownticks);
    }

    public static OpenCompressionScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenCompressionScreenPacket(buffer.readInt());
    }

    public static void handle(OpenCompressionScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.nogeon.economyland.client.ClientPacketHandler.handleOpenCompression(packet.remainingCooldownticks);
            });
        });
        context.setPacketHandled(true);
    }
}
