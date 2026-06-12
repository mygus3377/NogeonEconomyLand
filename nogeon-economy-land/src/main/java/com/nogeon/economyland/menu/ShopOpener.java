package com.nogeon.economyland.menu;

import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.shop.ShopItemProtection;
import com.nogeon.economyland.shop.Shops;
import com.nogeon.economyland.shop.DynamicPriceLogic;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.TraderShopState;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.JobProgress;
import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.SkillNodeStat;
import com.nogeon.economyland.network.BuyShopItemPacket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkHooks;

public final class ShopOpener {
    private static final int MAX_DYNAMIC_DELIVERY_LINES = 96;
    private static final int MAX_EXT_SYNC_ITEMS = 108;

    private ShopOpener() {
    }

    public static void open(ServerPlayer player, TraderKind kind) {
        open(player, kind, "");
    }

    public static void open(ServerPlayer player, TraderKind kind, String traderDatabaseId) {
        if (!kind.supportsInventoryShop() || kind == TraderKind.SMITH) {
            TraderActionOpener.open(player, kind, traderDatabaseId);
            return;
        }
        List<ShopEntry> entries = entriesFor(player, kind, traderDatabaseId);
        openShop(player, kind, traderDatabaseId, entries);
    }

    public static void openGeneral(ServerPlayer player) {
        open(player, TraderKind.GENERAL);
    }

    public static List<ShopEntry> entriesFor(ServerPlayer player, TraderKind kind) {
        return entriesFor(player, kind, "");
    }

    public static List<ShopEntry> entriesFor(ServerPlayer player, TraderKind kind, String traderDatabaseId) {
        EconomyState state = EconomyState.get(player.server);
        return TraderShopState.get(player.server).shopEntries(state, kind, traderDatabaseId);
    }

    public static void openShop(ServerPlayer player, TraderKind kind, String traderDatabaseId, List<ShopEntry> entries) {
        EconomyState state = EconomyState.get(player.server);
        TraderShopState traderState = TraderShopState.get(player.server);
        traderState.refreshShopDay(state, player.server.overworld().getDayTime() / 24000L);
        
        PlayerProfile profile = state.profile(player.getUUID());
        JobType deliveryJob = com.nogeon.economyland.network.BuyShopItemPacket.deliveryJob(kind);
        long bonusPercent = 0;
        if (deliveryJob != null) {
            JobProgress job = profile.job(deliveryJob);
            bonusPercent = job.bonusPercent(SkillNodeStat.DELIVERY_PRICE);
        }

        List<ShopLine> lines = new ArrayList<>();
        Set<net.minecraft.world.item.Item> fixedItems = new HashSet<>();

        for (ShopEntry entry : entries) {
            lines.add(new ShopLine(kind.id(), entry.id(), entry.stack(), traderState.adjustedPrice(kind, traderDatabaseId, entry, false), traderState.remaining(state, traderDatabaseId, entry), false));
        }
        for (ShopEntry entry : traderState.deliveryEntries(state, kind, traderDatabaseId)) {
            long basePrice = traderState.adjustedDeliveryPrice(kind, traderDatabaseId, entry);
            long totalPaid = basePrice + (basePrice * bonusPercent / 100L);
            lines.add(new ShopLine(kind.id(), entry.id(), entry.stack(), totalPaid, -1, true));
            fixedItems.add(entry.stack().getItem());
        }

        // Dynamic mod-compatible deliveries based on player inventory, storage, and backpacks.
        Set<String> dynamicIds = new HashSet<>();
        boolean dynamicLimitReached = addPriorityDynamicLines(player, kind, traderDatabaseId, profile, lines, dynamicIds, fixedItems, bonusPercent);
        
        // 1. 일반 인벤토리 수집
        if (!dynamicLimitReached) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (!invStack.isEmpty() && addDynamicLine(player, kind, traderDatabaseId, lines, dynamicIds, fixedItems, invStack, bonusPercent)) {
                    dynamicLimitReached = true;
                    break;
                }
            }
        }
        
        // 2. 확장 보관함 수집
        ItemStack[] extItems = com.nogeon.economyland.player.ExtendedInventoryDelivery.load(profile.extInventoryData());
        int unlockedSlots = Math.min(270, Math.max(0, profile.inventoryExtLevel() * 9));
        if (!dynamicLimitReached) {
            for (int slot = 0; slot < unlockedSlots; slot++) {
                ItemStack extStack = extItems[slot];
                if (!extStack.isEmpty() && addDynamicLine(player, kind, traderDatabaseId, lines, dynamicIds, fixedItems, extStack, bonusPercent)) {
                    dynamicLimitReached = true;
                    break;
                }
            }
        }
        
        // 3. 배낭 내부 수집
        if (!dynamicLimitReached) {
            for (ItemStack backpack : com.nogeon.economyland.player.ExtendedInventoryDelivery.findAllBackpacks(player)) {
                var cap = backpack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
                if (cap.isPresent()) {
                    net.minecraftforge.items.IItemHandler handler = cap.orElse(null);
                    if (handler != null) {
                        for (int slot = 0; slot < handler.getSlots(); slot++) {
                            ItemStack bagStack = handler.getStackInSlot(slot);
                            if (!bagStack.isEmpty() && addDynamicLine(player, kind, traderDatabaseId, lines, dynamicIds, fixedItems, bagStack, bonusPercent)) {
                                dynamicLimitReached = true;
                                break;
                            }
                        }
                        if (dynamicLimitReached) {
                            break;
                        }
                    }
                }
            }
        }

        // 수집된 모든 후보군을 대상으로 dynamic 등록 처리
        if (dynamicLimitReached) {
            player.displayClientMessage(Component.literal("Too many sellable items; showing the first " + MAX_DYNAMIC_DELIVERY_LINES + " entries."), false);
        }

        net.minecraft.nbt.CompoundTag extNbt = filteredExtInventoryNbt(kind, profile.extInventoryData(), profile.inventoryExtLevel());
        extNbt.putInt("inventoryExtLevel", profile.inventoryExtLevel());

        net.minecraft.nbt.CompoundTag minimalNbt = new net.minecraft.nbt.CompoundTag();
        minimalNbt.putInt("inventoryExtLevel", profile.inventoryExtLevel());

        // Create an empty lines snapshot for client to open screen immediately without 32KB buffer overflow.
        ShopMenu snapshotForClient = new ShopMenu(0, traderDatabaseId, new ArrayList<>(), minimalNbt);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new ShopMenu(containerId, traderDatabaseId, lines, minimalNbt),
            Component.translatable("screen.nogeon_economy_land." + kind.id() + "_shop")
        ), (FriendlyByteBuf buffer) -> snapshotForClient.write(buffer));

        // Sync full NBT through a custom network channel packet (safe from 32KB openGui buffer limit)
        com.nogeon.economyland.network.SyncExtendedInventoryNbtPacket.send(player, extNbt);

        // Sync actual shop lines safely through a custom channel packet
        com.nogeon.economyland.network.SyncShopLinesPacket.send(player, lines);
    }

    private static boolean addPriorityDynamicLines(ServerPlayer player, TraderKind kind, String traderDatabaseId, PlayerProfile profile,
        List<ShopLine> lines, Set<String> dynamicIds, Set<net.minecraft.world.item.Item> fixedItems, long bonusPercent) {
        if (kind != TraderKind.MINER) {
            return false;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isPriorityMinerItem(stack) && addDynamicLine(player, kind, traderDatabaseId, lines, dynamicIds, fixedItems, stack, bonusPercent)) {
                return true;
            }
        }

        ItemStack[] extItems = com.nogeon.economyland.player.ExtendedInventoryDelivery.load(profile.extInventoryData());
        int unlockedSlots = Math.min(270, Math.max(0, profile.inventoryExtLevel() * 9));
        for (int slot = 0; slot < unlockedSlots; slot++) {
            ItemStack stack = extItems[slot];
            if (isPriorityMinerItem(stack) && addDynamicLine(player, kind, traderDatabaseId, lines, dynamicIds, fixedItems, stack, bonusPercent)) {
                return true;
            }
        }

        for (ItemStack backpack : com.nogeon.economyland.player.ExtendedInventoryDelivery.findAllBackpacks(player)) {
            var cap = backpack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
            if (cap.isPresent()) {
                net.minecraftforge.items.IItemHandler handler = cap.orElse(null);
                if (handler != null) {
                    for (int slot = 0; slot < handler.getSlots(); slot++) {
                        ItemStack stack = handler.getStackInSlot(slot);
                        if (isPriorityMinerItem(stack) && addDynamicLine(player, kind, traderDatabaseId, lines, dynamicIds, fixedItems, stack, bonusPercent)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isPriorityMinerItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(Items.DIAMOND)
            || stack.is(Items.DIAMOND_BLOCK)
            || stack.is(Items.EMERALD)
            || stack.is(Items.EMERALD_BLOCK)
            || stack.is(Items.ANCIENT_DEBRIS)
            || stack.is(Items.NETHERITE_SCRAP)
            || stack.is(Items.NETHERITE_INGOT);
    }

    private static boolean addDynamicLine(ServerPlayer player, TraderKind kind, String traderDatabaseId, List<ShopLine> lines,
        Set<String> dynamicIds, Set<net.minecraft.world.item.Item> fixedItems, ItemStack stack, long bonusPercent) {
        if (fixedItems.contains(stack.getItem())) {
            return false;
        }
        if (dynamicIds.size() >= MAX_DYNAMIC_DELIVERY_LINES || ShopItemProtection.isSellBlocked(stack) || !DynamicPriceLogic.shouldAccept(kind, stack)) {
            return dynamicIds.size() >= MAX_DYNAMIC_DELIVERY_LINES;
        }
        String dynamicId = dynamicId(stack, kind);
        if (!dynamicIds.add(dynamicId)) {
            return false;
        }

        TraderShopState traderState = TraderShopState.get(player.server);
        long basePrice = Math.round(DynamicPriceLogic.calculatePrice(kind, stack) * 1.8D);
        long totalPaid = basePrice + (basePrice * bonusPercent / 100L);
        int currentSat = traderState.currentSaturation(kind, traderDatabaseId, stack);
        int maxSat = traderState.maxSaturation(stack);
        lines.add(new ShopLine(kind.id(), dynamicId, stack.copy(), totalPaid, -1, true, currentSat, maxSat));
        return dynamicIds.size() >= MAX_DYNAMIC_DELIVERY_LINES;
    }

    private static String dynamicId(ItemStack stack, TraderKind kind) {
        boolean ignoreNbt = (kind == TraderKind.CHEF || kind == TraderKind.CROP || kind == TraderKind.FISHER || kind == TraderKind.MINER || kind == TraderKind.HUNTER || kind == TraderKind.GENERAL);
        String nbtSuffix = (!ignoreNbt && stack.hasTag()) ? ":" + Integer.toHexString(stack.getTag().hashCode()) : "";
        return "dynamic:" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()) + nbtSuffix;
    }

    private static CompoundTag filteredExtInventoryNbt(TraderKind kind, CompoundTag source, int inventoryExtLevel) {
        CompoundTag filtered = new CompoundTag();
        filtered.putInt("inventoryExtLevel", inventoryExtLevel);
        ListTag items = new ListTag();
        if (source != null && source.contains("Items", Tag.TAG_LIST)) {
            ListTag sourceItems = source.getList("Items", Tag.TAG_COMPOUND);
            for (int index = 0; index < sourceItems.size() && items.size() < MAX_EXT_SYNC_ITEMS; index++) {
                CompoundTag itemTag = sourceItems.getCompound(index);
                ItemStack stack = ItemStack.of(itemTag);
                if (stack.isEmpty() || ShopItemProtection.isSellBlocked(stack) || !DynamicPriceLogic.shouldAccept(kind, stack)) {
                    continue;
                }
                items.add(itemTag.copy());
            }
        }
        filtered.put("Items", items);
        return filtered;
    }

    private static void openComingSoon(ServerPlayer player, TraderKind kind) {
        player.displayClientMessage(Component.translatable("message.nogeon_economy_land.trader_soon",
            Component.translatable(kind.translationKey())), false);
    }
}
