package com.nogeon.economyland.network;

import com.nogeon.economyland.land.LandRegion;
import com.nogeon.economyland.menu.LandHomeOpener;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.network.NetworkEvent;

public final class LandKickPacket {
    private final int landId;
    private final String playerName;

    public LandKickPacket(int landId, String playerName) {
        this.landId = landId;
        this.playerName = playerName;
    }

    public static void encode(LandKickPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.landId);
        buffer.writeUtf(packet.playerName);
    }

    public static LandKickPacket decode(FriendlyByteBuf buffer) {
        return new LandKickPacket(buffer.readVarInt(), buffer.readUtf());
    }

    public static void handle(LandKickPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer owner = context.getSender();
            if (owner == null) {
                return;
            }

            EconomyState state = EconomyState.get(owner.server);
            LandRegion land = null;
            for (LandRegion candidate : state.lands()) {
                if (candidate.id() == packet.landId && candidate.owner().equals(owner.getUUID())) {
                    land = candidate;
                    break;
                }
            }
            if (land == null) {
                owner.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.kick_failed"), false);
                return;
            }

            ServerPlayer target = null;
            for (ServerPlayer player : owner.server.getPlayerList().getPlayers()) {
                if (player.getGameProfile().getName().equalsIgnoreCase(packet.playerName)) {
                    target = player;
                    break;
                }
            }
            if (target == null || target.getUUID().equals(owner.getUUID())) {
                owner.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.player_not_online"), false);
                return;
            }
            if (!land.contains(target.level().dimension(), target.blockPosition())) {
                owner.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.player_not_inside"), false);
                return;
            }

            BlockPos destination = findKickPosition(target.serverLevel(), land, target.blockPosition());
            target.teleportTo(target.serverLevel(), destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D,
                target.getYRot(), target.getXRot());
            owner.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.kicked", target.getName()), false);
            target.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.kicked_by_owner", owner.getName()), false);
            LandHomeOpener.open(owner);
        });
        context.setPacketHandled(true);
    }

    private static BlockPos findKickPosition(ServerLevel level, LandRegion land, BlockPos current) {
        int westX = land.min().getX() - 1;
        int eastX = land.max().getX() + 1;
        int northZ = land.min().getZ() - 1;
        int southZ = land.max().getZ() + 1;

        double westDistance = Math.abs(current.getX() - land.min().getX());
        double eastDistance = Math.abs(current.getX() - land.max().getX());
        double northDistance = Math.abs(current.getZ() - land.min().getZ());
        double southDistance = Math.abs(current.getZ() - land.max().getZ());

        int destinationX = current.getX();
        int destinationZ = current.getZ();
        double best = westDistance;
        destinationX = westX;
        destinationZ = clamp(current.getZ(), land.min().getZ(), land.max().getZ());

        if (eastDistance < best) {
            best = eastDistance;
            destinationX = eastX;
            destinationZ = clamp(current.getZ(), land.min().getZ(), land.max().getZ());
        }
        if (northDistance < best) {
            best = northDistance;
            destinationX = clamp(current.getX(), land.min().getX(), land.max().getX());
            destinationZ = northZ;
        }
        if (southDistance < best) {
            destinationX = clamp(current.getX(), land.min().getX(), land.max().getX());
            destinationZ = southZ;
        }

        int destinationY = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, destinationX, destinationZ) + 1, land.max().getY() + 1);
        return new BlockPos(destinationX, destinationY, destinationZ);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}