package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.GachaCategory;
import com.nogeon.economyland.menu.GachaRewardAdminOpener;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class GachaRewardAdminCategoryPacket {
    private final String traderDatabaseId;
    private final String categoryId;
    private final int page;

    public GachaRewardAdminCategoryPacket(String traderDatabaseId, String categoryId) {
        this(traderDatabaseId, categoryId, 0);
    }

    public GachaRewardAdminCategoryPacket(String traderDatabaseId, String categoryId, int page) {
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.categoryId = GachaCategory.byId(categoryId).id();
        this.page = Math.max(0, page);
    }

    public static void encode(GachaRewardAdminCategoryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeUtf(packet.categoryId);
        buffer.writeVarInt(packet.page);
    }

    public static GachaRewardAdminCategoryPacket decode(FriendlyByteBuf buffer) {
        return new GachaRewardAdminCategoryPacket(buffer.readUtf(), buffer.readUtf(), buffer.readVarInt());
    }

    public static void handle(GachaRewardAdminCategoryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }
            GachaRewardAdminOpener.open(player, packet.traderDatabaseId, packet.categoryId, packet.page);
        });
        context.setPacketHandled(true);
    }
}
