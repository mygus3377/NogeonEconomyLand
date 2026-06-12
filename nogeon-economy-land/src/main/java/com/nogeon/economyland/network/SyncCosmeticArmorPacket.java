package com.nogeon.economyland.network;

import com.nogeon.economyland.client.ClientCosmeticArmorData;
import com.nogeon.economyland.player.PlayerProfile;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class SyncCosmeticArmorPacket {
    private final UUID playerId;
    private final boolean visible;
    private final ItemStack[] stacks;

    public SyncCosmeticArmorPacket(UUID playerId, boolean visible, ItemStack[] stacks) {
        this.playerId = playerId;
        this.visible = visible;
        this.stacks = stacks;
    }

    public static void send(ServerPlayer target, UUID playerId, PlayerProfile profile) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), fromProfile(playerId, profile));
    }

    public static void broadcast(MinecraftServer server, UUID playerId, PlayerProfile profile) {
        SyncCosmeticArmorPacket packet = fromProfile(playerId, profile);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    private static SyncCosmeticArmorPacket fromProfile(UUID playerId, PlayerProfile profile) {
        ItemStack[] stacks = new ItemStack[PlayerProfile.COSMETIC_ARMOR_SLOTS];
        for (int i = 0; i < stacks.length; i++) {
            stacks[i] = profile.cosmeticArmor(i).copy();
        }
        return new SyncCosmeticArmorPacket(playerId, profile.cosmeticArmorVisible(), stacks);
    }

    public static void encode(SyncCosmeticArmorPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.visible);
        buffer.writeVarInt(packet.stacks.length);
        for (ItemStack stack : packet.stacks) {
            buffer.writeItem(stack);
        }
    }

    public static SyncCosmeticArmorPacket decode(FriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        boolean visible = buffer.readBoolean();
        int size = Math.max(0, buffer.readVarInt());
        ItemStack[] stacks = new ItemStack[PlayerProfile.COSMETIC_ARMOR_SLOTS];
        for (int i = 0; i < PlayerProfile.COSMETIC_ARMOR_SLOTS; i++) {
            stacks[i] = i < size ? buffer.readItem() : ItemStack.EMPTY;
        }
        for (int i = PlayerProfile.COSMETIC_ARMOR_SLOTS; i < size; i++) {
            buffer.readItem();
        }
        return new SyncCosmeticArmorPacket(playerId, visible, stacks);
    }

    public static void handle(SyncCosmeticArmorPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
            ClientCosmeticArmorData.set(packet.playerId, packet.visible, packet.stacks)
        ));
        context.setPacketHandled(true);
    }
}
