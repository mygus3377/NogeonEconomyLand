package com.nogeon.economyland.network;

import com.nogeon.economyland.client.ClientPacketHandler;
import com.nogeon.economyland.land.LandSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class SyncLandSelectionPacket {
    private final boolean active;
    private final String typeId;
    private final ResourceLocation dimensionId;
    private final List<CuboidData> cuboids;
    private final BlockPos pendingFirst;
    private final boolean pendingAdditive;

    private SyncLandSelectionPacket(boolean active, String typeId, ResourceLocation dimensionId, List<CuboidData> cuboids, BlockPos pendingFirst, boolean pendingAdditive) {
        this.active = active;
        this.typeId = typeId;
        this.dimensionId = dimensionId;
        this.cuboids = cuboids;
        this.pendingFirst = pendingFirst;
        this.pendingAdditive = pendingAdditive;
    }

    public static void send(ServerPlayer player, LandSelection selection, BlockPos pendingFirst, boolean pendingAdditive) {
        List<CuboidData> cuboids = new ArrayList<>();
        for (LandSelection.Cuboid cuboid : selection.cuboids()) {
            cuboids.add(new CuboidData(cuboid.first(), cuboid.second(), cuboid.additive()));
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncLandSelectionPacket(
            true,
            selection.type().id(),
            selection.world().location(),
            cuboids,
            pendingFirst,
            pendingAdditive
        ));
    }

    public static void clear(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncLandSelectionPacket(false, "", null, List.of(), null, true));
    }

    public static void encode(SyncLandSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        if (!packet.active) {
            return;
        }
        buffer.writeUtf(packet.typeId);
        buffer.writeResourceLocation(packet.dimensionId);
        buffer.writeVarInt(packet.cuboids.size());
        for (CuboidData cuboid : packet.cuboids) {
            buffer.writeBlockPos(cuboid.first);
            buffer.writeBlockPos(cuboid.second);
            buffer.writeBoolean(cuboid.additive);
        }
        buffer.writeBoolean(packet.pendingFirst != null);
        if (packet.pendingFirst != null) {
            buffer.writeBlockPos(packet.pendingFirst);
            buffer.writeBoolean(packet.pendingAdditive);
        }
    }

    public static SyncLandSelectionPacket decode(FriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        if (!active) {
            return new SyncLandSelectionPacket(false, "", null, List.of(), null, true);
        }
        String typeId = buffer.readUtf();
        ResourceLocation dimensionId = buffer.readResourceLocation();
        int cuboidCount = buffer.readVarInt();
        List<CuboidData> cuboids = new ArrayList<>(cuboidCount);
        for (int i = 0; i < cuboidCount; i++) {
            cuboids.add(new CuboidData(buffer.readBlockPos(), buffer.readBlockPos(), buffer.readBoolean()));
        }
        boolean hasPending = buffer.readBoolean();
        BlockPos pendingFirst = hasPending ? buffer.readBlockPos() : null;
        boolean pendingAdditive = hasPending && buffer.readBoolean();
        return new SyncLandSelectionPacket(true, typeId, dimensionId, cuboids, pendingFirst, pendingAdditive);
    }

    public static void handle(SyncLandSelectionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            ClientPacketHandler.handleSyncLandSelection(packet.active, packet.typeId, packet.dimensionId, packet.cuboids, packet.pendingFirst, packet.pendingAdditive)
        ));
        context.setPacketHandled(true);
    }

    public record CuboidData(BlockPos first, BlockPos second, boolean additive) {}
}
