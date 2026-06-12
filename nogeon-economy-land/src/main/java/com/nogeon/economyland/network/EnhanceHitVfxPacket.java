package com.nogeon.economyland.network;

import com.nogeon.economyland.client.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class EnhanceHitVfxPacket {
    private final double x;
    private final double y;
    private final double z;
    private final double lookX;
    private final double lookZ;
    private final int level;

    public EnhanceHitVfxPacket(double x, double y, double z, double lookX, double lookZ, int level) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.lookX = lookX;
        this.lookZ = lookZ;
        this.level = level;
    }

    public static void encode(EnhanceHitVfxPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.x);
        buffer.writeDouble(packet.y);
        buffer.writeDouble(packet.z);
        buffer.writeDouble(packet.lookX);
        buffer.writeDouble(packet.lookZ);
        buffer.writeVarInt(packet.level);
    }

    public static EnhanceHitVfxPacket decode(FriendlyByteBuf buffer) {
        return new EnhanceHitVfxPacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt());
    }

    public static void handle(EnhanceHitVfxPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> ClientPacketHandler.handleEnhancedHitVfx(packet.x, packet.y, packet.z, packet.lookX, packet.lookZ, packet.level)));
        context.setPacketHandled(true);
    }
}
