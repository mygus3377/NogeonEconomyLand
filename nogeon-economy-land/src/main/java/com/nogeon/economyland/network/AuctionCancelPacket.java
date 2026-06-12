package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.AuctionOpener;
import com.nogeon.economyland.state.AuctionState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class AuctionCancelPacket {
    private final int auctionId;

    public AuctionCancelPacket(int auctionId) {
        this.auctionId = auctionId;
    }

    public static void encode(AuctionCancelPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.auctionId);
    }

    public static AuctionCancelPacket decode(FriendlyByteBuf buffer) {
        return new AuctionCancelPacket(buffer.readVarInt());
    }

    public static void handle(AuctionCancelPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            String result = AuctionState.get(player.server).cancelAuction(player, packet.auctionId);
            if (result != null) {
                player.displayClientMessage(Component.translatable(result), false);
            }
            AuctionOpener.open(player);
        });
        context.setPacketHandled(true);
    }
}