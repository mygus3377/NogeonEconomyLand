package com.nogeon.economyland.network;

import com.nogeon.economyland.menu.GachaRewardAdminOpener;
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

public final class GachaRewardSavePacket {
    private final String traderDatabaseId;
    private final String actionId;
    private final String categoryId;
    private final String entryId;
    private final ItemStack stack;
    private final long weight;
    private final int rarity;
    private final boolean jackpot;

    public GachaRewardSavePacket(String traderDatabaseId, String actionId, String entryId, ItemStack stack, long weight) {
        this(traderDatabaseId, actionId, "item", entryId, stack, weight, 0, false);
    }

    public GachaRewardSavePacket(String traderDatabaseId, String actionId, String entryId, ItemStack stack, long weight, int rarity, boolean jackpot) {
        this(traderDatabaseId, actionId, "item", entryId, stack, weight, rarity, jackpot);
    }

    public GachaRewardSavePacket(String traderDatabaseId, String actionId, String categoryId, String entryId, ItemStack stack, long weight, int rarity, boolean jackpot) {
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.actionId = actionId == null || actionId.isBlank() ? "gacha_basic" : actionId;
        this.categoryId = categoryId == null || categoryId.isBlank() ? "item" : categoryId;
        this.entryId = entryId == null ? "" : entryId;
        this.stack = stack.copy();
        this.weight = weight;
        this.rarity = Math.max(0, Math.min(3, rarity));
        this.jackpot = jackpot;
    }

    public static void encode(GachaRewardSavePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeUtf(packet.actionId);
        buffer.writeUtf(packet.categoryId);
        buffer.writeUtf(packet.entryId);
        buffer.writeItem(packet.stack);
        buffer.writeLong(packet.weight);
        buffer.writeVarInt(packet.rarity);
        buffer.writeBoolean(packet.jackpot);
    }

    public static GachaRewardSavePacket decode(FriendlyByteBuf buffer) {
        return new GachaRewardSavePacket(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readItem(), buffer.readLong(), buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(GachaRewardSavePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !player.hasPermissions(2) || packet.stack.isEmpty()) {
                return;
            }
            EconomyState state = EconomyState.get(player.server);
            TraderShopState traderState = TraderShopState.get(player.server);
            ShopEntry existing = packet.entryId.isBlank() ? null : traderState.gachaRewardEntry(state, packet.entryId);
            ItemStack normalized = existing == null ? packet.stack.copy() : existing.stack().copy();
            normalized.setCount(Math.max(1, Math.min(normalized.getMaxStackSize(), packet.stack.getCount())));
            normalized.getOrCreateTag().putInt("NoGeonGachaRarity", packet.rarity);
            normalized.getOrCreateTag().putBoolean("NoGeonGachaJackpot", packet.jackpot);
            String entryId = packet.entryId.isBlank() ? createEntryId(packet.categoryId, normalized) : packet.entryId;
            traderState.addOrReplaceGachaReward(state, packet.categoryId,
                new ShopEntry(entryId, normalized, Math.max(1L, packet.weight), 0));
            player.displayClientMessage(Component.literal("가챠 보상 저장 (전역): ").append(normalized.getHoverName()), false);
            GachaRewardAdminOpener.open(player, packet.traderDatabaseId, packet.categoryId);
        });
        context.setPacketHandled(true);
    }

    private static String createEntryId(String categoryId, ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().replace(':', '_').replace('/', '_');
        String tagSuffix = stack.hasTag() ? "_" + Integer.toUnsignedString(stack.getTag().toString().hashCode()) : "";
        return TraderShopState.globalGachaPrefix(categoryId) + itemId + tagSuffix;
    }
}
