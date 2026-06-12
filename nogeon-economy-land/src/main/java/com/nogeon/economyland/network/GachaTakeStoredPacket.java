package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.GachaStorageOpener;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class GachaTakeStoredPacket {
    private final int index;

    public GachaTakeStoredPacket(int index) {
        this.index = index;
    }

    public static void encode(GachaTakeStoredPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.index);
    }

    public static GachaTakeStoredPacket decode(FriendlyByteBuf buffer) {
        return new GachaTakeStoredPacket(buffer.readVarInt());
    }

    public static void handle(GachaTakeStoredPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (!EconomyState.get(player.server).claimGachaReward(player, packet.index)) {
                player.displayClientMessage(Component.literal("인벤토리 공간이 부족하거나 보상이 없습니다."), false);
            }
            GachaStorageOpener.open(player);
        });
        context.setPacketHandled(true);
    }
}
