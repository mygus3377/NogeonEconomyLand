package com.nogeon.economyland.network;

import com.nogeon.economyland.client.ClientHunterData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class SyncHunterAbilityPacket {
    private final boolean hunterSenseActive;
    private final int hunterSenseRadius;
    private final String hunterPreyMarkedUUID;

    public SyncHunterAbilityPacket(boolean hunterSenseActive, int hunterSenseRadius, String hunterPreyMarkedUUID) {
        this.hunterSenseActive = hunterSenseActive;
        this.hunterSenseRadius = hunterSenseRadius;
        this.hunterPreyMarkedUUID = hunterPreyMarkedUUID == null ? "" : hunterPreyMarkedUUID;
    }

    public static void encode(SyncHunterAbilityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.hunterSenseActive);
        buffer.writeInt(packet.hunterSenseRadius);
        buffer.writeUtf(packet.hunterPreyMarkedUUID);
    }

    public static SyncHunterAbilityPacket decode(FriendlyByteBuf buffer) {
        return new SyncHunterAbilityPacket(buffer.readBoolean(), buffer.readInt(), buffer.readUtf(256));
    }

    public static void handle(SyncHunterAbilityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientHunterData.setHunterSenseActive(packet.hunterSenseActive);
                ClientHunterData.setHunterSenseRadius(packet.hunterSenseRadius);
                ClientHunterData.setHunterPreyMarkedUUID(packet.hunterPreyMarkedUUID);
            });
        });
        context.setPacketHandled(true);
    }
}
