package com.nogeon.economyland.network;

import com.nogeon.economyland.client.ClientFisherData;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public final class SyncFisherDataPacket {
    private final int flowGauge;
    private final BlockPos hotspotPos;
    private final double hotspotRadius;
    private final Map<BlockPos, Double> fisheryZones;

    public SyncFisherDataPacket(int flowGauge, BlockPos hotspotPos, double hotspotRadius, Map<BlockPos, Double> fisheryZones) {
        this.flowGauge = flowGauge;
        this.hotspotPos = hotspotPos;
        this.hotspotRadius = hotspotRadius;
        this.fisheryZones = fisheryZones;
    }

    public SyncFisherDataPacket(int flowGauge) {
        this(flowGauge, null, 0.0D, new HashMap<>());
    }

    public static void encode(SyncFisherDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.flowGauge);
        
        if (packet.hotspotPos != null) {
            buffer.writeBoolean(true);
            buffer.writeBlockPos(packet.hotspotPos);
            buffer.writeDouble(packet.hotspotRadius);
        } else {
            buffer.writeBoolean(false);
        }
        
        buffer.writeInt(packet.fisheryZones.size());
        for (Map.Entry<BlockPos, Double> entry : packet.fisheryZones.entrySet()) {
            buffer.writeBlockPos(entry.getKey());
            buffer.writeDouble(entry.getValue());
        }
    }

    public static SyncFisherDataPacket decode(FriendlyByteBuf buffer) {
        int flowGauge = buffer.readInt();
        
        BlockPos hotspotPos = null;
        double hotspotRadius = 0.0D;
        if (buffer.readBoolean()) {
            hotspotPos = buffer.readBlockPos();
            hotspotRadius = buffer.readDouble();
        }
        
        int size = buffer.readInt();
        Map<BlockPos, Double> fisheryZones = new HashMap<>();
        for (int i = 0; i < size; i++) {
            BlockPos pos = buffer.readBlockPos();
            double radius = buffer.readDouble();
            fisheryZones.put(pos, radius);
        }
        
        return new SyncFisherDataPacket(flowGauge, hotspotPos, hotspotRadius, fisheryZones);
    }

    public static void handle(SyncFisherDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientFisherData.setFlowGauge(packet.flowGauge);
                ClientFisherData.setHotspotPos(packet.hotspotPos);
                ClientFisherData.setHotspotRadius(packet.hotspotRadius);
                ClientFisherData.updateFisheryZones(packet.fisheryZones);
            });
        });
        context.setPacketHandled(true);
    }
}

