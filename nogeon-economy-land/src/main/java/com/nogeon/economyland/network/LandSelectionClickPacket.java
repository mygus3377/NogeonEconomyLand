package com.nogeon.economyland.network;

import com.nogeon.economyland.land.LandEvents;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class LandSelectionClickPacket {
    private final boolean rightClick;
    private final BlockPos pos;

    public LandSelectionClickPacket(boolean rightClick, BlockPos pos) {
        this.rightClick = rightClick;
        this.pos = pos;
    }

    public static void encode(LandSelectionClickPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.rightClick);
        buffer.writeBoolean(packet.pos != null);
        if (packet.pos != null) {
            buffer.writeBlockPos(packet.pos);
        }
    }

    public static LandSelectionClickPacket decode(FriendlyByteBuf buffer) {
        boolean rightClick = buffer.readBoolean();
        BlockPos pos = buffer.readBoolean() ? buffer.readBlockPos() : null;
        return new LandSelectionClickPacket(rightClick, pos);
    }

    public static void handle(LandSelectionClickPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            LandEvents.handleDeedClick(player, packet.rightClick, packet.pos);
        });
        context.setPacketHandled(true);
    }
}