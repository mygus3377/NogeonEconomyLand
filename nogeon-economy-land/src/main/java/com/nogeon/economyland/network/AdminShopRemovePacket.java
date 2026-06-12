package com.nogeon.economyland.network;

import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.menu.AdminShopOpener;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.TraderShopState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class AdminShopRemovePacket {
    private final String kindId;
    private final String traderDatabaseId;
    private final String entryId;
    private final boolean delivery;

    public AdminShopRemovePacket(String kindId, String traderDatabaseId, String entryId, boolean delivery) {
        this.kindId = kindId;
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.entryId = entryId;
        this.delivery = delivery;
    }

    public static void encode(AdminShopRemovePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.kindId);
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeUtf(packet.entryId);
        buffer.writeBoolean(packet.delivery);
    }

    public static AdminShopRemovePacket decode(FriendlyByteBuf buffer) {
        return new AdminShopRemovePacket(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(AdminShopRemovePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }

            TraderKind kind = TraderKind.byId(packet.kindId);
            if (!kind.supportsInventoryShop() && kind != TraderKind.GACHA) {
                return;
            }
            EconomyState state = EconomyState.get(player.server);
            TraderShopState.get(player.server).removeShopEntry(state, kind, packet.traderDatabaseId, packet.entryId, packet.delivery);
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.admin.item_removed"), false);
            AdminShopOpener.open(player, kind, packet.traderDatabaseId);
        });
        context.setPacketHandled(true);
    }
}
