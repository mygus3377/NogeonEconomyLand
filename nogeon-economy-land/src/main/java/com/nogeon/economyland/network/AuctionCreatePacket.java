package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.AuctionOpener;
import com.nogeon.economyland.state.AuctionState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class AuctionCreatePacket {
    private final int slot;
    private final int count;
    private final long price;

    public AuctionCreatePacket(int slot, int count, long price) {
        this.slot = slot;
        this.count = count;
        this.price = price;
    }

    public static void encode(AuctionCreatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.slot);
        buffer.writeVarInt(packet.count);
        buffer.writeLong(packet.price);
    }

    public static AuctionCreatePacket decode(FriendlyByteBuf buffer) {
        return new AuctionCreatePacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readLong());
    }

    public static void handle(AuctionCreatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String result = AuctionState.get(player.server).listInventoryAuction(player, packet.slot, packet.count, packet.price);
            if (result != null) {
                player.displayClientMessage(Component.translatable(result), false);
            }
            AuctionOpener.open(player);
        });
        context.setPacketHandled(true);
    }
}