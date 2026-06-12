package com.nogeon.economyland.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class SyncReforgeStatusPacket {
    private final Component status;

    public SyncReforgeStatusPacket(Component status) {
        this.status = status;
    }

    public static void send(ServerPlayer player, Component status) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncReforgeStatusPacket(status));
    }

    public static void encode(SyncReforgeStatusPacket packet, FriendlyByteBuf buffer) {
        buffer.writeComponent(packet.status != null ? packet.status : Component.empty());
    }

    public static SyncReforgeStatusPacket decode(FriendlyByteBuf buffer) {
        return new SyncReforgeStatusPacket(buffer.readComponent());
    }

    public static void handle(SyncReforgeStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof com.nogeon.economyland.client.ReforgeScreen reforgeScreen) {
                reforgeScreen.setStatus(packet.status);
            }
        }));
        context.setPacketHandled(true);
    }
}
