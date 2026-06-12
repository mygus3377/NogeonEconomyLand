package com.nogeon.economyland.network;

import com.nogeon.economyland.client.ClientMinerData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class SyncMinerAbilityPacket {
    private final boolean minerBodyActive;
    private final boolean minerEyeActive;
    private final int minerEyeRadius;

    public SyncMinerAbilityPacket(boolean minerBodyActive, boolean minerEyeActive, int minerEyeRadius) {
        this.minerBodyActive = minerBodyActive;
        this.minerEyeActive = minerEyeActive;
        this.minerEyeRadius = minerEyeRadius;
    }

    public static void encode(SyncMinerAbilityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.minerBodyActive);
        buffer.writeBoolean(packet.minerEyeActive);
        buffer.writeInt(packet.minerEyeRadius);
    }

    public static SyncMinerAbilityPacket decode(FriendlyByteBuf buffer) {
        return new SyncMinerAbilityPacket(buffer.readBoolean(), buffer.readBoolean(), buffer.readInt());
    }

    public static void handle(SyncMinerAbilityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                boolean prevActive = ClientMinerData.minerEyeActive();
                ClientMinerData.setMinerBodyActive(packet.minerBodyActive);
                ClientMinerData.setMinerEyeRadius(packet.minerEyeRadius);
                ClientMinerData.setMinerEyeActive(packet.minerEyeActive);
                
                if (prevActive != packet.minerEyeActive) {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.levelRenderer != null) {
                        mc.levelRenderer.allChanged();
                    }
                }
            });
        });
        context.setPacketHandled(true);
    }
}
