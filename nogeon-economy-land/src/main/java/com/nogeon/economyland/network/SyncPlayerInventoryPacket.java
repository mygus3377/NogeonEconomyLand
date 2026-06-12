package com.nogeon.economyland.network;

import com.nogeon.economyland.client.ClientPacketHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class SyncPlayerInventoryPacket {
    private final List<ItemStack> stacks;

    public SyncPlayerInventoryPacket(List<ItemStack> stacks) {
        this.stacks = stacks;
    }

    public static void send(ServerPlayer player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            stacks.add(player.getInventory().getItem(slot).copy());
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerInventoryPacket(stacks));
    }

    public static void encode(SyncPlayerInventoryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.stacks.size());
        for (ItemStack stack : packet.stacks) {
            buffer.writeItem(stack);
        }
    }

    public static SyncPlayerInventoryPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<ItemStack> stacks = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            stacks.add(buffer.readItem());
        }
        return new SyncPlayerInventoryPacket(stacks);
    }

    public static void handle(SyncPlayerInventoryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncPlayerInventory(packet.stacks)));
        context.setPacketHandled(true);
    }
}
