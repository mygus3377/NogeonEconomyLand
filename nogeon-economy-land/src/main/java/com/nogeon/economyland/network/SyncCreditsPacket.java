package com.nogeon.economyland.network;

import com.nogeon.economyland.client.ClientWalletData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class SyncCreditsPacket {
    private final long credits;

    public SyncCreditsPacket(long credits) {
        this.credits = credits;
    }

    public static void send(ServerPlayer player, long credits) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncCreditsPacket(credits));
    }

    public static void encode(SyncCreditsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.credits);
    }

    public static SyncCreditsPacket decode(FriendlyByteBuf buffer) {
        return new SyncCreditsPacket(buffer.readLong());
    }

    public static void handle(SyncCreditsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWalletData.setCredits(packet.credits)));
        context.setPacketHandled(true);
    }
}
