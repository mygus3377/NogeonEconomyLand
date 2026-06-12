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

public final class AdminShopResetPacket {
    private final String kindId;
    private final String traderDatabaseId;
    private final boolean delivery;

    public AdminShopResetPacket(String kindId, String traderDatabaseId, boolean delivery) {
        this.kindId = kindId;
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.delivery = delivery;
    }

    public static void encode(AdminShopResetPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.kindId);
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeBoolean(packet.delivery);
    }

    public static AdminShopResetPacket decode(FriendlyByteBuf buffer) {
        return new AdminShopResetPacket(buffer.readUtf(), buffer.readUtf(), buffer.readBoolean());
    }

    public static void handle(AdminShopResetPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
            TraderShopState.get(player.server).resetShopEntries(state, kind, packet.traderDatabaseId, packet.delivery);
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.admin.shop_reset"), false);
            AdminShopOpener.open(player, kind, packet.traderDatabaseId);
        });
        context.setPacketHandled(true);
    }
}
