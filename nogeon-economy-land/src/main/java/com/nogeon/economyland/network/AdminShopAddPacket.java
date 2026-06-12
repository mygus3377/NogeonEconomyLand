package com.nogeon.economyland.network;

import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.menu.AdminShopOpener;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.TraderShopState;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public final class AdminShopAddPacket {
    private final String kindId;
    private final String traderDatabaseId;
    private final String entryId;
    private final ItemStack stack;
    private final long price;
    private final int dailyLimit;
    private final boolean delivery;

    public AdminShopAddPacket(String kindId, String traderDatabaseId, String entryId, ItemStack stack, long price, int dailyLimit, boolean delivery) {
        this.kindId = kindId;
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.entryId = entryId == null ? "" : entryId;
        this.stack = stack.copy();
        this.price = price;
        this.dailyLimit = dailyLimit;
        this.delivery = delivery;
    }

    public static void encode(AdminShopAddPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.kindId);
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeUtf(packet.entryId);
        buffer.writeItem(packet.stack);
        buffer.writeLong(packet.price);
        buffer.writeVarInt(packet.dailyLimit);
        buffer.writeBoolean(packet.delivery);
    }

    public static AdminShopAddPacket decode(FriendlyByteBuf buffer) {
        return new AdminShopAddPacket(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readItem(), buffer.readLong(), buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(AdminShopAddPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2)) {
                return;
            }

            if (packet.stack.isEmpty()) {
                player.displayClientMessage(Component.translatable("command.nogeon_economy_land.admin.empty_hand"), false);
                return;
            }

            TraderKind kind = TraderKind.byId(packet.kindId);
            if (!kind.supportsInventoryShop() && kind != TraderKind.GACHA) {
                return;
            }
            ItemStack normalized = packet.stack.copy();
            normalized.setCount(Math.max(1, Math.min(normalized.getMaxStackSize(), normalized.getCount())));
            String entryId = packet.entryId.isBlank() ? createEntryId(kind, normalized, packet.delivery) : packet.entryId;
            EconomyState state = EconomyState.get(player.server);
            TraderShopState traderState = TraderShopState.get(player.server);
            ShopEntry entry = new ShopEntry(entryId, normalized, Math.max(1L, packet.price), packet.delivery ? 0 : Math.max(1, packet.dailyLimit));
            if (packet.delivery) {
                traderState.addOrReplaceDeliveryEntry(state, kind, packet.traderDatabaseId, entry);
            } else {
                traderState.addOrReplaceShopEntry(state, kind, packet.traderDatabaseId, entry);
            }
            player.displayClientMessage(Component.translatable("command.nogeon_economy_land.admin.shop_added",
                normalized.getHoverName(), normalized.getCount(), packet.price, packet.dailyLimit), false);
            AdminShopOpener.open(player, kind, packet.traderDatabaseId);
        });
        context.setPacketHandled(true);
    }

    private static String createEntryId(TraderKind kind, ItemStack stack, boolean delivery) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().replace(':', '_').replace('/', '_');
        String tagSuffix = stack.hasTag() ? "_" + Integer.toUnsignedString(stack.getTag().toString().hashCode()) : "";
        return (delivery ? "sell_" : "buy_") + kind.id() + "_" + itemId + tagSuffix;
    }
}
