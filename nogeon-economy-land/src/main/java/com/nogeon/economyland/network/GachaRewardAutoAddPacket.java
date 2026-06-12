package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.GachaCategory;
import com.nogeon.economyland.menu.GachaRewardAdminOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.TraderShopState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class GachaRewardAutoAddPacket {
    private final String traderDatabaseId;
    private final String categoryId;

    public GachaRewardAutoAddPacket(String traderDatabaseId, String categoryId) {
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.categoryId = GachaCategory.byId(categoryId).id();
    }

    public static void encode(GachaRewardAutoAddPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeUtf(packet.categoryId);
    }

    public static GachaRewardAutoAddPacket decode(FriendlyByteBuf buffer) {
        return new GachaRewardAutoAddPacket(buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(GachaRewardAutoAddPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }
            TraderShopState.get(player.server).addMissingGachaRewards(EconomyState.get(player.server), packet.categoryId);
            GachaRewardAdminOpener.open(player, packet.traderDatabaseId, packet.categoryId);
        });
        context.setPacketHandled(true);
    }
}
