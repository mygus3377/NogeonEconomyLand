package com.nogeon.economyland.network;

import com.nogeon.economyland.client.ClientPacketHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class SyncExtendedInventoryNbtPacket {
    private final CompoundTag nbt;

    public SyncExtendedInventoryNbtPacket(CompoundTag nbt) {
        this.nbt = nbt;
    }

    public static void send(ServerPlayer player, CompoundTag nbt) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncExtendedInventoryNbtPacket(nbt));
    }

    public static void encode(SyncExtendedInventoryNbtPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.nbt);
    }

    public static SyncExtendedInventoryNbtPacket decode(FriendlyByteBuf buffer) {
        return new SyncExtendedInventoryNbtPacket(buffer.readNbt());
    }

    public static void handle(SyncExtendedInventoryNbtPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncExtendedInventoryNbt(packet.nbt)));
        context.setPacketHandled(true);
    }
}
