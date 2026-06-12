package com.nogeon.economyland.network;

import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class GachaClaimStoredPacket {
    public static void encode(GachaClaimStoredPacket packet, FriendlyByteBuf buffer) {
    }

    public static GachaClaimStoredPacket decode(FriendlyByteBuf buffer) {
        return new GachaClaimStoredPacket();
    }

    public static void handle(GachaClaimStoredPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            int claimed = EconomyState.get(player.server).claimGachaRewards(player);
            player.displayClientMessage(Component.literal(claimed > 0
                ? "가챠 보관함에서 " + claimed + "개를 회수했습니다."
                : "가챠 보관함에 회수할 보상이 없거나 인벤토리가 가득 찼습니다."), false);
        });
        context.setPacketHandled(true);
    }
}
