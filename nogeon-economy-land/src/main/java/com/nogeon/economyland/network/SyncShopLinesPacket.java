package com.nogeon.economyland.network;

import com.nogeon.economyland.client.ClientPacketHandler;
import com.nogeon.economyland.menu.ShopLine;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public final class SyncShopLinesPacket {
    private final List<ShopLine> lines;

    public SyncShopLinesPacket(List<ShopLine> lines) {
        this.lines = lines;
    }

    public static void send(ServerPlayer player, List<ShopLine> lines) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncShopLinesPacket(lines));
    }

    public static void encode(SyncShopLinesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.lines.size());
        for (ShopLine line : packet.lines) {
            buffer.writeUtf(line.kindId());
            buffer.writeUtf(line.id());
            buffer.writeItem(line.stack());
            buffer.writeLong(line.price());
            buffer.writeVarInt(line.remaining());
            buffer.writeBoolean(line.delivery());
            buffer.writeVarInt(line.currentSaturation());
            buffer.writeVarInt(line.maxSaturation());
        }
    }

    public static SyncShopLinesPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<ShopLine> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(new ShopLine(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readItem(),
                buffer.readLong(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt()
            ));
        }
        return new SyncShopLinesPacket(lines);
    }

    public static void handle(SyncShopLinesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncShopLines(packet.lines)));
        context.setPacketHandled(true);
    }
}
