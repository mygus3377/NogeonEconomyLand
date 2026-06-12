package com.nogeon.economyland.state;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.item.GunCatalog;
import com.nogeon.economyland.shop.GachaDefaultRewards;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.shop.Shops;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class TraderShopState extends SavedData {
    private static final String STATE_ID = NoGeonEconomyLand.MOD_ID + "_trader_shops";
    private static final int GUN_SHOP_AMMO_REVISION = 3;
    public static final int DELIVERY_PAYOUT_MULTIPLIER = 2;
    public static final int PURCHASE_PRICE_MULTIPLIER = 4;

    private final Map<String, List<ShopEntry>> customShopEntries = new HashMap<>();
    private final Map<String, List<ShopEntry>> customDeliveryEntries = new HashMap<>();
    private final Map<String, Integer> marketSaturation = new HashMap<>();
    private final Map<String, Integer> shopPurchases = new HashMap<>();
    private long shopDay = -1L;
    private int gunShopAmmoRevision;

    public static TraderShopState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(TraderShopState::fromNbt, TraderShopState::new, STATE_ID);
    }

    public List<ShopEntry> shopEntries(EconomyState baseState, TraderKind kind, String traderDatabaseId) {
        if (traderDatabaseId == null || traderDatabaseId.isBlank()) {
            return baseState.shopEntries(kind);
        }
        if (kind == TraderKind.GUN) {
            migrateGunShopEntries(baseState);
        }
        List<ShopEntry> entries = customShopEntries.computeIfAbsent(scopeKey(kind, traderDatabaseId), ignored -> seedShopEntries(baseState, kind));
        if (normalizeEntries(kind, entries)) {
            setDirty();
        }
        return entries;
    }

    public List<ShopEntry> deliveryEntries(EconomyState baseState, TraderKind kind, String traderDatabaseId) {
        if (traderDatabaseId == null || traderDatabaseId.isBlank()) {
            return baseState.deliveryEntries(kind);
        }
        List<ShopEntry> entries = customDeliveryEntries.computeIfAbsent(scopeKey(kind, traderDatabaseId), ignored -> seedDeliveryEntries(baseState, kind));
        if (normalizeDeliveryEntries(kind, entries)) {
            setDirty();
        }
        return entries;
    }

    public void addOrReplaceShopEntry(EconomyState baseState, TraderKind kind, String traderDatabaseId, ShopEntry entry) {
        if (traderDatabaseId == null || traderDatabaseId.isBlank()) {
            baseState.addOrReplaceShopEntry(kind, entry);
            return;
        }
        List<ShopEntry> entries = shopEntries(baseState, kind, traderDatabaseId);
        entries.removeIf(existing -> existing.id().equals(entry.id()));
        entries.add(entry);
        setDirty();
    }

    public void addOrReplaceDeliveryEntry(EconomyState baseState, TraderKind kind, String traderDatabaseId, ShopEntry entry) {
        if (traderDatabaseId == null || traderDatabaseId.isBlank()) {
            baseState.addOrReplaceDeliveryEntry(kind, entry);
            return;
        }
        List<ShopEntry> entries = deliveryEntries(baseState, kind, traderDatabaseId);
        entries.removeIf(existing -> existing.id().equals(entry.id()));
        entries.add(entry);
        setDirty();
    }

    public void removeShopEntry(EconomyState baseState, TraderKind kind, String traderDatabaseId, String entryId, boolean delivery) {
        if (traderDatabaseId == null || traderDatabaseId.isBlank()) {
            baseState.removeShopEntry(kind, entryId, delivery);
            return;
        }
        (delivery ? deliveryEntries(baseState, kind, traderDatabaseId) : shopEntries(baseState, kind, traderDatabaseId)).removeIf(existing -> existing.id().equals(entryId));
        shopPurchases.remove(purchaseKey(traderDatabaseId, entryId));
        setDirty();
    }

    public void resetShopEntries(EconomyState baseState, TraderKind kind, String traderDatabaseId, boolean delivery) {
        if (traderDatabaseId == null || traderDatabaseId.isBlank()) {
            baseState.resetShopEntries(kind, delivery);
            return;
        }
        if (delivery) {
            customDeliveryEntries.put(scopeKey(kind, traderDatabaseId), seedDeliveryEntries(baseState, kind));
        } else {
            customShopEntries.put(scopeKey(kind, traderDatabaseId), seedShopEntries(baseState, kind));
        }
        setDirty();
    }

    public static final String GLOBAL_GACHA_POOL = "global_gacha_pool";

    public List<ShopEntry> gachaRewardEntries(EconomyState baseState, String categoryId) {
        List<ShopEntry> entries = shopEntries(baseState, TraderKind.GACHA, GLOBAL_GACHA_POOL);
        String prefix = categoryId + ":";
        if ("gun_bow".equals(categoryId) && migrateGachaGunRewards(entries)) {
            setDirty();
        }
        if (!hasCategoryEntries(entries, prefix) && appendMissingEntries(entries, GachaDefaultRewards.entries(categoryId))) {
            setDirty();
        }
        List<ShopEntry> result = new ArrayList<>();
        for (ShopEntry entry : entries) {
            if (entry.id().startsWith(prefix) || ("item".equals(categoryId) && isGlobalLegacyGachaRewardId(entry.id()))) {
                result.add(entry);
            }
        }
        return result;
    }

    private boolean migrateGachaGunRewards(List<ShopEntry> entries) {
        boolean changed = entries.removeIf(entry -> {
            if (!entry.id().startsWith("gun_bow:")) {
                return false;
            }
            if (isTaczBaseGunWithoutId(entry.stack())) {
                return true;
            }
            ItemStack stack = entry.stack();
            if (stack.isEmpty()) {
                return false;
            }
            Item item = stack.getItem();
            if (GunCatalog.isRegisteredAmmo(item)) {
                return true;
            }
            net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) {
                String token = id.toString().toLowerCase(java.util.Locale.ROOT);
                if (containsAny(token, "ammo", "bullet", "magazine", "scope", "sight", "grip", "stock", "barrel", "suppressor", "muzzle", "attachment", "rocket")) {
                    return true;
                }
            }
            return false;
        });
        if (appendMissingEntries(entries, GachaDefaultRewards.entries("gun_bow"))) {
            changed = true;
        }
        return changed;
    }

    private static boolean containsAny(String token, String... needles) {
        for (String needle : needles) {
            if (token.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTaczBaseGunWithoutId(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null
            && "tacz".equals(id.getNamespace())
            && "modern_kinetic_gun".equals(id.getPath())
            && (!stack.hasTag() || !stack.getTag().contains("GunId", net.minecraft.nbt.Tag.TAG_STRING));
    }

    private boolean hasCategoryEntries(List<ShopEntry> entries, String prefix) {
        for (ShopEntry entry : entries) {
            if (entry.id().startsWith(prefix)) return true;
        }
        return false;
    }

    public List<ShopEntry> gachaRewardEntries(EconomyState baseState, String traderDatabaseId, String actionId, String categoryId) {
        return gachaRewardEntries(baseState, categoryId);
    }

    public void addOrReplaceGachaReward(EconomyState baseState, String categoryId, ShopEntry entry) {
        List<ShopEntry> entries = shopEntries(baseState, TraderKind.GACHA, GLOBAL_GACHA_POOL);
        entries.removeIf(existing -> existing.id().equals(entry.id()));
        entries.add(entry);
        setDirty();
    }

    public ShopEntry gachaRewardEntry(EconomyState baseState, String entryId) {
        if (entryId == null || entryId.isBlank()) {
            return null;
        }
        for (ShopEntry entry : shopEntries(baseState, TraderKind.GACHA, GLOBAL_GACHA_POOL)) {
            if (entry.id().equals(entryId)) {
                return entry;
            }
        }
        return null;
    }

    public void removeGachaReward(EconomyState baseState, String entryId) {
        shopEntries(baseState, TraderKind.GACHA, GLOBAL_GACHA_POOL).removeIf(existing -> existing.id().equals(entryId));
        setDirty();
    }

    public void resetGachaRewards(EconomyState baseState, String categoryId) {
        String prefix = categoryId + ":";
        shopEntries(baseState, TraderKind.GACHA, GLOBAL_GACHA_POOL).removeIf(entry -> entry.id().startsWith(prefix)
            || ("item".equals(categoryId) && isGlobalLegacyGachaRewardId(entry.id())));
        setDirty();
    }

    public void addMissingGachaRewards(EconomyState baseState, String categoryId) {
        String prefix = categoryId + ":";
        List<ShopEntry> entries = shopEntries(baseState, TraderKind.GACHA, GLOBAL_GACHA_POOL);
        if (appendMissingEntries(entries, GachaDefaultRewards.entries(categoryId))) {
            setDirty();
        }
    }

    public void refreshShopDay(EconomyState baseState, long day) {
        baseState.refreshShopDay(day);
        if (shopDay != day) {
            shopDay = day;
            shopPurchases.clear();
            setDirty();
        }
    }

    public long adjustedPrice(TraderKind kind, String traderDatabaseId, ShopEntry entry, boolean delivery) {
        int percent = demandPercent(kind, traderDatabaseId, entry.id(), delivery);
        long price = Math.max(1L, entry.price() * percent / 100L);
        return delivery ? price : price * PURCHASE_PRICE_MULTIPLIER;
    }

    public long adjustedNormalSellPrice(TraderKind kind, String traderDatabaseId, ShopEntry entry) {
        return Math.round(adjustedPrice(kind, traderDatabaseId, entry, true) * 2.4D);
    }

    public long adjustedDeliveryPrice(TraderKind kind, String traderDatabaseId, ShopEntry entry) {
        return Math.round(adjustedPrice(kind, traderDatabaseId, entry, true) * 1.8D);
    }

    public String demandSpeechSuffix(TraderKind kind, String traderDatabaseId, long day) {
        int buyPercent = demandPercent(kind, traderDatabaseId, "speech_buy", false);
        int deliveryPercent = demandPercent(kind, traderDatabaseId, "speech_delivery", true);
        if (deliveryPercent >= 118) {
            return "demand_high";
        }
        if (buyPercent <= 90) {
            return "demand_sale";
        }
        return "idle";
    }

    public int remaining(EconomyState baseState, String traderDatabaseId, ShopEntry entry) {
        if (traderDatabaseId == null || traderDatabaseId.isBlank()) {
            return baseState.remaining(entry);
        }
        return Math.max(0, entry.dailyLimit() - shopPurchases.getOrDefault(purchaseKey(traderDatabaseId, entry.id()), 0));
    }

    public boolean recordPurchase(EconomyState baseState, String traderDatabaseId, ShopEntry entry, int quantity) {
        if (traderDatabaseId == null || traderDatabaseId.isBlank()) {
            return baseState.recordPurchase(entry, quantity);
        }
        int bought = shopPurchases.getOrDefault(purchaseKey(traderDatabaseId, entry.id()), 0);
        if (quantity <= 0 || bought + quantity > entry.dailyLimit()) {
            return false;
        }
        shopPurchases.put(purchaseKey(traderDatabaseId, entry.id()), bought + quantity);
        setDirty();
        return true;
    }

    public void recordDelivery(TraderKind kind, String traderDatabaseId, ItemStack stack, long paidPrice) {
        String scope = scopeKey(kind, traderDatabaseId);
        String entryId = "delivered:" + BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().replace(':', '_');
        String satKey = scope + "|" + entryId;
        
        int currentSat = marketSaturation.getOrDefault(satKey, 0);
        int addedCount = stack.getCount();
        marketSaturation.put(satKey, currentSat + addedCount);

        int threshold = saturationThreshold(stack);
        if (currentSat + addedCount >= threshold) {
            List<ShopEntry> entries = customShopEntries.computeIfAbsent(scope, ignored -> new ArrayList<>());
            long buyPrice = Math.max(1L, paidPrice / Math.max(1, stack.getCount()));
            ShopEntry existing = null;
            for (ShopEntry entry : entries) {
                if (entry.id().equals(entryId) && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(entry.stack(), stack, kind)) {
                    existing = entry;
                    break;
                }
            }
            if (existing != null) {
                entries.remove(existing);
                entries.add(new ShopEntry(entryId, existing.stack(), buyPrice, existing.dailyLimit() + 1));
            } else {
                entries.add(new ShopEntry(entryId, stack.copyWithCount(1), buyPrice, 1));
            }
            marketSaturation.put(satKey, (currentSat + addedCount) - threshold);
        }
        setDirty();
    }

    private int saturationThreshold(ItemStack stack) {
        String rarity = stack.getRarity() == null ? "" : stack.getRarity().name();
        return switch (rarity) {
            case "EPIC" -> 2;
            case "RARE" -> 8;
            case "UNCOMMON" -> 32;
            default -> 128;
        };
    }

    public int currentSaturation(TraderKind kind, String traderDatabaseId, ItemStack stack) {
        String entryId = "delivered:" + BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().replace(':', '_');
        return marketSaturation.getOrDefault(scopeKey(kind, traderDatabaseId) + "|" + entryId, 0);
    }

    public int maxSaturation(ItemStack stack) {
        return saturationThreshold(stack);
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag shopNbt = new CompoundTag();
        shopNbt.putLong("day", shopDay);
        shopNbt.putInt("gunShopAmmoRevision", gunShopAmmoRevision);

        CompoundTag satNbt = new CompoundTag();
        for (Map.Entry<String, Integer> entry : marketSaturation.entrySet()) {
            satNbt.putInt(entry.getKey(), entry.getValue());
        }
        shopNbt.put("saturation", satNbt);

        CompoundTag customNbt = new CompoundTag();
        for (Map.Entry<String, List<ShopEntry>> shopEntry : customShopEntries.entrySet()) {
            CompoundTag kindNbt = new CompoundTag();
            int customIndex = 0;
            for (ShopEntry entry : shopEntry.getValue()) {
                kindNbt.put(String.valueOf(customIndex++), entry.toNbt());
            }
            kindNbt.putInt("entryCount", customIndex);
            customNbt.put(shopEntry.getKey(), kindNbt);
        }
        shopNbt.put("customEntries", customNbt);

        CompoundTag deliveryNbt = new CompoundTag();
        for (Map.Entry<String, List<ShopEntry>> shopEntry : customDeliveryEntries.entrySet()) {
            CompoundTag kindNbt = new CompoundTag();
            int customIndex = 0;
            for (ShopEntry entry : shopEntry.getValue()) {
                kindNbt.put(String.valueOf(customIndex++), entry.toNbt());
            }
            kindNbt.putInt("entryCount", customIndex);
            deliveryNbt.put(shopEntry.getKey(), kindNbt);
        }
        shopNbt.put("customDeliveries", deliveryNbt);

        CompoundTag purchasesNbt = new CompoundTag();
        for (Map.Entry<String, Integer> entry : shopPurchases.entrySet()) {
            purchasesNbt.putInt(entry.getKey(), entry.getValue());
        }
        shopNbt.put("purchases", purchasesNbt);

        nbt.put("traderShop", shopNbt);
        return nbt;
    }

    public static TraderShopState fromNbt(CompoundTag nbt) {
        TraderShopState state = new TraderShopState();
        CompoundTag shopNbt = nbt.getCompound("traderShop");
        state.shopDay = shopNbt.getLong("day");
        state.gunShopAmmoRevision = shopNbt.getInt("gunShopAmmoRevision");

        CompoundTag satNbt = shopNbt.getCompound("saturation");
        for (String key : satNbt.getAllKeys()) {
            state.marketSaturation.put(key, satNbt.getInt(key));
        }

        CompoundTag customNbt = shopNbt.getCompound("customEntries");
        for (String kindId : customNbt.getAllKeys()) {
            CompoundTag kindNbt = customNbt.getCompound(kindId);
            int customEntryCount = kindNbt.getInt("entryCount");
            List<ShopEntry> entries = new ArrayList<>();
            for (int i = 0; i < customEntryCount; i++) {
                entries.add(ShopEntry.fromNbt(kindNbt.getCompound(String.valueOf(i))));
            }
            state.customShopEntries.put(kindId, entries);
        }

        CompoundTag deliveryNbt = shopNbt.getCompound("customDeliveries");
        for (String kindId : deliveryNbt.getAllKeys()) {
            CompoundTag kindNbt = deliveryNbt.getCompound(kindId);
            int customEntryCount = kindNbt.getInt("entryCount");
            List<ShopEntry> entries = new ArrayList<>();
            for (int i = 0; i < customEntryCount; i++) {
                entries.add(ShopEntry.fromNbt(kindNbt.getCompound(String.valueOf(i))));
            }
            state.customDeliveryEntries.put(kindId, entries);
        }

        CompoundTag purchasesNbt = shopNbt.getCompound("purchases");
        for (String key : purchasesNbt.getAllKeys()) {
            state.shopPurchases.put(key, purchasesNbt.getInt(key));
        }
        return state;
    }

    private boolean normalizeEntries(TraderKind kind, List<ShopEntry> entries) {
        return switch (kind) {
            case GUN -> normalizeGunEntries(entries);
            case POTION, SMITH -> appendMissingEntries(entries, Shops.defaults(kind));
            default -> false;
        };
    }

    private boolean normalizeGunEntries(List<ShopEntry> entries) {
        return false;
    }

    private boolean normalizeDeliveryEntries(TraderKind kind, List<ShopEntry> entries) {
        if (kind == TraderKind.GUN) {
            boolean changed = false;
            List<ShopEntry> defaults = Shops.deliveries(TraderKind.GUN);
            for (ShopEntry def : defaults) {
                boolean found = false;
                for (int i = 0; i < entries.size(); i++) {
                    ShopEntry existing = entries.get(i);
                    if (existing.id().equals(def.id())) {
                        found = true;
                        if (existing.stack().getCount() != def.stack().getCount() || existing.price() != def.price()) {
                            entries.set(i, new ShopEntry(def.id(), def.stack().copy(), def.price(), existing.dailyLimit()));
                            changed = true;
                        }
                        break;
                    }
                }
                if (!found) {
                    entries.add(new ShopEntry(def.id(), def.stack().copy(), def.price(), def.dailyLimit()));
                    changed = true;
                }
            }
            return changed;
        }
        if (kind == TraderKind.CROP || kind == TraderKind.FISHER || kind == TraderKind.MINER || kind == TraderKind.CHEF || kind == TraderKind.HUNTER || kind == TraderKind.SMITH || kind == TraderKind.ENGINEER) {
            List<ShopEntry> defaults = Shops.deliveries(kind);
            boolean changed = appendMissingEntries(entries, defaults);
            if (kind == TraderKind.ENGINEER) {
                changed |= entries.removeIf(entry -> isLegacyEngineerFixedDelivery(entry.id()));
            }
            if (kind == TraderKind.SMITH || kind == TraderKind.ENGINEER) {
                changed |= syncEnhancementGemDeliveries(entries, defaults);
            }
            return changed;
        }
        return false;
    }

    private boolean syncEnhancementGemDeliveries(List<ShopEntry> entries, List<ShopEntry> defaults) {
        boolean changed = false;
        for (int i = 0; i < entries.size(); i++) {
            ShopEntry entry = entries.get(i);
            if (!isEnhancementGemDelivery(entry.id())) {
                continue;
            }
            ShopEntry def = Shops.find(defaults, entry.id());
            if (def != null && entry.price() != def.price()) {
                entries.set(i, new ShopEntry(entry.id(), entry.stack().copy(), def.price(), entry.dailyLimit()));
                changed = true;
            }
        }
        return changed;
    }

    private boolean isEnhancementGemDelivery(String id) {
        return "deliver_cracked_enhancement_gem".equals(id)
            || "deliver_split_enhancement_gem".equals(id)
            || "deliver_flawed_enhancement_gem".equals(id)
            || "deliver_enhancement_gem".equals(id)
            || "deliver_flawless_enhancement_gem".equals(id)
            || "deliver_perfect_enhancement_gem".equals(id);
    }

    private boolean isLegacyEngineerFixedDelivery(String id) {
        return "deliver_andesite_alloy".equals(id)
            || "deliver_copper_casing".equals(id)
            || "deliver_brass_casing".equals(id)
            || "deliver_electron_tube".equals(id)
            || "deliver_precision_mechanism".equals(id)
            || "deliver_raw_zinc".equals(id)
            || "deliver_zinc_ingot".equals(id)
            || "deliver_brass_ingot".equals(id)
            || "deliver_rose_quartz".equals(id)
            || "deliver_polished_rose_quartz".equals(id);
    }

    private boolean appendMissingEntries(List<ShopEntry> entries, List<ShopEntry> defaults) {
        boolean changed = false;
        Set<String> existingIds = new LinkedHashSet<>();
        for (ShopEntry entry : entries) {
            existingIds.add(entry.id());
        }
        for (ShopEntry entry : defaults) {
            if (existingIds.add(entry.id())) {
                entries.add(entry);
                changed = true;
            }
        }
        return changed;
    }

    private boolean isAllowedGunShopItem(Item item) {
        return GunCatalog.isRegisteredAmmo(item) || item == Items.ARROW || item == Items.FIREWORK_ROCKET;
    }

    private List<ShopEntry> seedShopEntries(EconomyState baseState, TraderKind kind) {
        if (kind == TraderKind.GACHA) {
            return new ArrayList<>();
        }
        if (kind == TraderKind.GENERAL) {
            return copyEntries(baseState.generalShopEntries());
        }
        return copyEntries(baseState.shopEntries(kind));
    }

    private void migrateGunShopEntries(EconomyState baseState) {
        if (gunShopAmmoRevision >= GUN_SHOP_AMMO_REVISION) {
            return;
        }
        for (Map.Entry<String, List<ShopEntry>> entry : customShopEntries.entrySet()) {
            if (entry.getKey().endsWith("|" + TraderKind.GUN.id())) {
                entry.setValue(seedShopEntries(baseState, TraderKind.GUN));
            }
        }
        shopPurchases.entrySet().removeIf(entry -> entry.getKey().contains("|ammo_"));
        gunShopAmmoRevision = GUN_SHOP_AMMO_REVISION;
        setDirty();
    }

    public static String gachaPrefix(String actionId) {
        return (actionId == null || actionId.isBlank() ? "gacha_basic" : actionId) + ":";
    }

    public static String gachaPrefix(String actionId, String categoryId) {
        String normalizedCategory = categoryId == null || categoryId.isBlank() ? "item" : categoryId;
        return gachaPrefix(actionId) + normalizedCategory + ":";
    }

    public static String globalGachaPrefix(String categoryId) {
        return (categoryId == null || categoryId.isBlank() ? "item" : categoryId) + ":";
    }

    private boolean isGlobalLegacyGachaRewardId(String entryId) {
        return entryId.startsWith("gacha_basic:") || entryId.startsWith("gacha_middle:")
            || entryId.startsWith("gacha_high:") || entryId.startsWith("gacha_legend:");
    }

    private boolean isLegacyGachaRewardId(String actionId, String entryId) {
        String prefix = gachaPrefix(actionId);
        if (!entryId.startsWith(prefix)) {
            return false;
        }
        String remainder = entryId.substring(prefix.length());
        return !(remainder.startsWith("weapon:") || remainder.startsWith("armor:")
            || remainder.startsWith("item:") || remainder.startsWith("gun:"));
    }

    private List<ShopEntry> seedDeliveryEntries(EconomyState baseState, TraderKind kind) {
        return copyEntries(baseState.deliveryEntries(kind));
    }

    private List<ShopEntry> copyEntries(List<ShopEntry> entries) {
        List<ShopEntry> copy = new ArrayList<>(entries.size());
        for (ShopEntry entry : entries) {
            copy.add(new ShopEntry(entry.id(), entry.stack(), entry.price(), entry.dailyLimit()));
        }
        return copy;
    }

    private String scopeKey(TraderKind kind, String traderDatabaseId) {
        return traderDatabaseId + "|" + kind.id();
    }

    private String purchaseKey(String traderDatabaseId, String entryId) {
        return traderDatabaseId + "|" + entryId;
    }

    private int demandPercent(TraderKind kind, String traderDatabaseId, String entryId, boolean delivery) {
        String scope = (traderDatabaseId == null || traderDatabaseId.isBlank()) ? "global" : traderDatabaseId;
        int hash = Math.abs((shopDay + "|" + kind.id() + "|" + scope + "|" + entryId + "|" + delivery).hashCode());
        int range = delivery ? 66 : 41;
        int minimum = delivery ? 75 : 80;
        return minimum + hash % range;
    }
}
