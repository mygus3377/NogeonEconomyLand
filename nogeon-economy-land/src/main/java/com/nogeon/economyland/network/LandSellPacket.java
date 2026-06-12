package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.LandHomeOpener;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class LandSellPacket {
    private final int landId;

    public LandSellPacket(int landId) {
        this.landId = landId;
    }

    public static void encode(LandSellPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.landId);
    }

    public static LandSellPacket decode(FriendlyByteBuf buffer) {
        return new LandSellPacket(buffer.readVarInt());
    }

    public static void handle(LandSellPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            EconomyState state = EconomyState.get(player.server);
            long refund = state.sellLand(player.getUUID(), packet.landId);
            if (refund < 0L) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.sell_failed"), false);
                return;
            }
            SyncCreditsPacket.send(player, state.profile(player.getUUID()).credits());
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.land.sold", refund), false);
            LandHomeOpener.open(player);
        });
        context.setPacketHandled(true);
    }
}