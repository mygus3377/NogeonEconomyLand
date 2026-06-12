package com.nogeon.economyland.item;

import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketedGems;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public final class SocketUpgradeService {
    public static final int MAX_SOCKETS = 6;
    private static final long[] SOCKET_COSTS = {100000L, 250000L, 500000L, 1000000L, 2000000L, 4000000L};
    private static final long[] GEM_REMOVE_COSTS = {50000L, 100000L, 200000L, 400000L, 800000L, 1500000L};

    private SocketUpgradeService() {
    }

    public static int normalizeSelectedSlot(ServerPlayer player, int slot) {
        if (isSelectable(player, slot)) {
            return slot;
        }
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            if (isSelectable(player, index)) {
                return index;
            }
        }
        return -1;
    }

    public static boolean canUpgrade(ItemStack stack) {
        return !stack.isEmpty() && !LootCategory.forItem(stack).isNone() && sockets(stack) < MAX_SOCKETS;
    }

    public static int sockets(ItemStack stack) {
        return stack.isEmpty() ? 0 : SocketHelper.getSockets(stack);
    }

    public static long cost(ItemStack stack) {
        int sockets = Math.min(sockets(stack), SOCKET_COSTS.length - 1);
        return SOCKET_COSTS[sockets];
    }

    public static List<GemInstance> gems(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        return SocketHelper.getGems(stack).gems();
    }

    public static boolean hasRemovableGem(ItemStack stack, int gemIndex) {
        GemInstance gem = gemAt(stack, gemIndex);
        return gem != null && gem.isValid() && !gem.gemStack().isEmpty();
    }

    public static long removeCost(ItemStack stack, int gemIndex) {
        GemInstance gem = gemAt(stack, gemIndex);
        if (gem == null || !gem.isValid()) {
            return 0L;
        }
        int ordinal = Math.max(0, rarityOrdinal(gem));
        int index = Math.min(ordinal, GEM_REMOVE_COSTS.length - 1);
        return GEM_REMOVE_COSTS[index];
    }

    public static int gemColor(GemInstance gem) {
        try {
            Object rarity = rarity(gem);
            Object color = rarity == null ? null : rarity.getClass().getMethod("getColor").invoke(rarity);
            if (color != null) {
                return 0xFF000000 | (Integer) color.getClass().getMethod("getValue").invoke(color);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 0xFF64B5F6;
    }

    public static boolean isRainbowGem(GemInstance gem) {
        return rarityOrdinal(gem) >= GEM_REMOVE_COSTS.length - 1;
    }

    public static Component tryUpgrade(ServerPlayer player, PlayerProfile profile, ItemStack stack) {
        if (stack.isEmpty() || LootCategory.forItem(stack).isNone()) {
            return Component.translatable("message.nogeon_economy_land.socket.invalid_item").withStyle(ChatFormatting.RED);
        }
        int sockets = sockets(stack);
        if (sockets >= MAX_SOCKETS) {
            return Component.translatable("message.nogeon_economy_land.socket.max").withStyle(ChatFormatting.GOLD);
        }
        long cost = cost(stack);
        if (!profile.spendCredits(cost)) {
            return Component.translatable("message.nogeon_economy_land.socket.no_money", cost).withStyle(ChatFormatting.RED);
        }
        SocketHelper.setSockets(stack, sockets + 1);
        player.level().playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return Component.translatable("message.nogeon_economy_land.socket.complete", sockets + 1).withStyle(ChatFormatting.GREEN);
    }

    public static Component tryRemoveGem(ServerPlayer player, PlayerProfile profile, ItemStack stack, int gemIndex) {
        if (stack.isEmpty() || LootCategory.forItem(stack).isNone()) {
            return Component.literal("소켓 보석을 뺄 수 없는 아이템입니다.").withStyle(ChatFormatting.RED);
        }
        GemInstance gem = gemAt(stack, gemIndex);
        if (gem == null || !gem.isValid() || gem.gemStack().isEmpty()) {
            return Component.literal("제거할 소켓 보석을 선택하세요.").withStyle(ChatFormatting.RED);
        }
        long cost = removeCost(stack, gemIndex);
        if (!profile.spendCredits(cost)) {
            return Component.literal("크레딧이 부족합니다: " + cost).withStyle(ChatFormatting.RED);
        }

        List<GemInstance> updated = new ArrayList<>(SocketHelper.getGems(stack).gems());
        if (gemIndex < 0 || gemIndex >= updated.size()) {
            return Component.literal("소켓 보석 위치가 올바르지 않습니다.").withStyle(ChatFormatting.RED);
        }
        ItemStack removed = gem.gemStack().copy();
        if (removed.hasTag()) {
            removed.getOrCreateTag().remove("uuids");
        }
        updated.set(gemIndex, GemInstance.EMPTY);
        SocketHelper.setGems(stack, new SocketedGems(updated));
        ExtendedInventoryDelivery.giveOrDrop(player, removed);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return Component.literal("소켓 보석을 제거했습니다. -" + cost + " C").withStyle(ChatFormatting.GREEN);
    }

    private static GemInstance gemAt(ItemStack stack, int gemIndex) {
        if (gemIndex < 0 || stack.isEmpty()) {
            return null;
        }
        List<GemInstance> gems = SocketHelper.getGems(stack).gems();
        return gemIndex < gems.size() ? gems.get(gemIndex) : null;
    }

    private static int rarityOrdinal(GemInstance gem) {
        try {
            Object rarity = rarity(gem);
            return rarity == null ? 0 : (Integer) rarity.getClass().getMethod("ordinal").invoke(rarity);
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }

    private static Object rarity(GemInstance gem) throws ReflectiveOperationException {
        if (gem == null) {
            return null;
        }
        Object holder = gem.getClass().getMethod("rarity").invoke(gem);
        return holder == null ? null : holder.getClass().getMethod("get").invoke(holder);
    }

    private static boolean isSelectable(ServerPlayer player, int slot) {
        return slot >= 0
            && slot < player.getInventory().getContainerSize()
            && !player.getInventory().getItem(slot).isEmpty()
            && !LootCategory.forItem(player.getInventory().getItem(slot)).isNone();
    }
}
