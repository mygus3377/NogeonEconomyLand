package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.GachaRewardAdminOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.TraderShopState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class GachaRewardRemovePacket {
    private final String traderDatabaseId;
    private final String entryId;

    public GachaRewardRemovePacket(String traderDatabaseId, String entryId) {
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.entryId = entryId == null ? "" : entryId;
    }

    public static void encode(GachaRewardRemovePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeUtf(packet.entryId);
    }

    public static GachaRewardRemovePacket decode(FriendlyByteBuf buffer) {
        return new GachaRewardRemovePacket(buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(GachaRewardRemovePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2) || packet.entryId.isBlank()) {
                return;
            }
            TraderShopState.get(player.server).removeGachaReward(EconomyState.get(player.server), packet.entryId);
            GachaRewardAdminOpener.open(player, packet.traderDatabaseId, categoryFromEntryId(packet.entryId));
        });
        context.setPacketHandled(true);
    }

    private static String categoryFromEntryId(String entryId) {
        int separator = entryId.indexOf(':');
        return separator <= 0 ? "item" : entryId.substring(0, separator);
    }
}
