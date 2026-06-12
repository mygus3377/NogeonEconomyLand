package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.GachaRewardAdminOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.TraderShopState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class GachaRewardResetPacket {
    private final String traderDatabaseId;
    private final String actionId;
    private final String categoryId;

    public GachaRewardResetPacket(String traderDatabaseId, String actionId) {
        this(traderDatabaseId, actionId, "item");
    }

    public GachaRewardResetPacket(String traderDatabaseId, String actionId, String categoryId) {
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.actionId = actionId == null || actionId.isBlank() ? "gacha_basic" : actionId;
        this.categoryId = categoryId == null || categoryId.isBlank() ? "item" : categoryId;
    }

    public static void encode(GachaRewardResetPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeUtf(packet.actionId);
        buffer.writeUtf(packet.categoryId);
    }

    public static GachaRewardResetPacket decode(FriendlyByteBuf buffer) {
        return new GachaRewardResetPacket(buffer.readUtf(), buffer.readUtf(), buffer.readUtf());
    }

    public static void handle(GachaRewardResetPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }
            TraderShopState.get(player.server).resetGachaRewards(EconomyState.get(player.server), packet.categoryId);
            GachaRewardAdminOpener.open(player, packet.traderDatabaseId, packet.categoryId);
        });
        context.setPacketHandled(true);
    }
}
