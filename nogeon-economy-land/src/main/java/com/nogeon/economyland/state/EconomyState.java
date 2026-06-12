package com.nogeon.economyland.state;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.network.SyncCreditsPacket;
import com.nogeon.economyland.land.LandFlag;
import com.nogeon.economyland.land.LandRegion;
import com.nogeon.economyland.land.LandSelection;
import com.nogeon.economyland.land.LandPermission;
import com.nogeon.economyland.land.LandType;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.shop.Shops;
import com.nogeon.economyland.trade.TradeSession;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public final class EconomyState extends SavedData {
    private static final String STATE_ID = NoGeonEconomyLand.MOD_ID + "_state";
    private static final int DAILY_LOTTERY_LIMIT = 10;
    private static final int GUN_SHOP_AMMO_REVISION = 2;
    private static final long BASE_LOTTERY_JACKPOT_1 = 500_000_000L;
    private static final long BASE_LOTTERY_JACKPOT_2 = 100_000_000L;
    private final Map<UUID, PlayerProfile> profiles = new HashMap<>();
    private final Map<String, Integer> shopPurchases = new HashMap<>();
    private final List<ShopEntry> generalShopEntries = new ArrayList<>();
    private final Map<String, List<ShopEntry>> customShopEntries = new HashMap<>();
    private final Map<String, List<ShopEntry>> customDeliveryEntries = new HashMap<>();
    private final List<LandRegion> lands = new ArrayList<>();
    private final Map<UUID, String> knownPlayers = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> pendingItemRewards = new HashMap<>();
    private final Map<UUID, List<ItemStack>> pendingGachaRewards = new HashMap<>();
    private final Set<String> playerPlacedResourceBlocks = new HashSet<>();
    private final List<UUID> lotteryEntries = new ArrayList<>();
    private final Map<UUID, UUID> tradeRequests = new HashMap<>();
    private final Map<UUID, TradeSession> tradeSessions = new HashMap<>();
    private final List<String> tradeLogs = new ArrayList<>();
    private final Map<UUID, HighLowSession> highLowSessions = new HashMap<>();
    private long shopDay = -1;
    private long lastLotteryDrawDay = -1;
    private long lotteryJackpot1 = BASE_LOTTERY_JACKPOT_1;
    private long lotteryJackpot2 = BASE_LOTTERY_JACKPOT_2;
    private int nextLandId = 1;
    private int gunShopAmmoRevision;

    public static EconomyState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(EconomyState::fromNbt, EconomyState::new, STATE_ID);
    }

    public PlayerProfile profile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, ignored -> new PlayerProfile());
    }

    public int resetAllJobProgress() {
        for (PlayerProfile profile : profiles.values()) {
            profile.resetAllJobProgress();
        }
        setDirty();
        return profiles.size();
    }

    public void markPlayerPlacedResourceBlock(ResourceKey<Level> dimension, BlockPos pos) {
        playerPlacedResourceBlocks.add(blockKey(dimension, pos));
        setDirty();
    }

    public boolean isPlayerPlacedResourceBlock(ResourceKey<Level> dimension, BlockPos pos) {
        return playerPlacedResourceBlocks.contains(blockKey(dimension, pos));
    }

    private static String blockKey(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + "|" + pos.asLong();
    }

    public void rememberPlayer(UUID uuid, String name) {
        knownPlayers.put(uuid, name);
        setDirty();
    }

    public Map<UUID, String> knownPlayers() {
        return knownPlayers;
    }

    public String knownPlayerName(UUID playerId) {
        return knownPlayers.getOrDefault(playerId, playerId.toString());
    }

    public UUID findKnownPlayer(String name) {
        for (Map.Entry<UUID, String> entry : knownPlayers.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public List<ShopEntry> generalShopEntries() {
        if (generalShopEntries.isEmpty()) {
            generalShopEntries.addAll(Shops.defaults());
            setDirty();
        }
        return generalShopEntries;
    }

    public List<ShopEntry> shopEntries(TraderKind kind) {
        if (kind == TraderKind.GENERAL) {
            return generalShopEntries();
        }
        if (kind == TraderKind.GUN) {
            migrateGunShopEntries();
        }
        List<ShopEntry> entries = customShopEntries.get(kind.id());
        if (entries == null) {
            entries = Shops.defaults(kind);
            customShopEntries.put(kind.id(), entries);
            setDirty();
        } else {
            List<ShopEntry> defaults = Shops.defaults(kind);
            boolean modified = false;
            for (ShopEntry def : defaults) {
                if (Shops.find(entries, def.id()) == null) {
                    entries.add(def);
                    modified = true;
                }
            }
            if (modified) {
                setDirty();
            }
        }
        return entries;
    }

    public List<ShopEntry> deliveryEntries(TraderKind kind) {
        List<ShopEntry> entries = customDeliveryEntries.get(kind.id());
        if (entries == null) {
            entries = Shops.defaultDeliveries(kind);
            customDeliveryEntries.put(kind.id(), entries);
            setDirty();
        } else {
            List<ShopEntry> defaults = Shops.defaultDeliveries(kind);
            boolean modified = false;
            for (ShopEntry def : defaults) {
                if (Shops.find(entries, def.id()) == null) {
                    entries.add(def);
                    modified = true;
                }
            }
            if (modified) {
                setDirty();
            }
        }
        if (syncDeliveryPrices(kind, entries)) {
            setDirty();
        }
        return entries;
    }

    private static boolean syncDeliveryPrices(TraderKind kind, List<ShopEntry> entries) {
        if (!shouldSyncDeliveryPrices(kind)) {
            return false;
        }
        boolean changed = false;
        if (kind == TraderKind.SMITH) {
            changed |= entries.removeIf(entry -> "deliver_diamond".equals(entry.id()));
        }
        if (kind == TraderKind.ENGINEER) {
            changed |= entries.removeIf(entry -> isLegacyEngineerFixedDelivery(entry.id()));
        }
        List<ShopEntry> defaults = Shops.defaultDeliveries(kind);
        for (int i = 0; i < entries.size(); i++) {
            ShopEntry entry = entries.get(i);
            if (!shouldSyncDeliveryEntry(kind, entry.id())) {
                continue;
            }
            ShopEntry def = Shops.find(defaults, entry.id());
            if (def != null && (entry.price() != def.price() || entry.stack().getCount() != def.stack().getCount())) {
                entries.set(i, new ShopEntry(entry.id(), def.stack().copy(), def.price(), entry.dailyLimit()));
                changed = true;
            }
        }
        return changed;
    }

    private static boolean shouldSyncDeliveryPrices(TraderKind kind) {
        return kind == TraderKind.CROP
            || kind == TraderKind.FISHER
            || kind == TraderKind.MINER
            || kind == TraderKind.CHEF
            || kind == TraderKind.HUNTER
            || kind == TraderKind.SMITH
            || kind == TraderKind.ENGINEER;
    }

    private static boolean shouldSyncDeliveryEntry(TraderKind kind, String id) {
        return switch (kind) {
            case CROP, FISHER, MINER, CHEF, HUNTER -> id.startsWith("deliver_");
            case SMITH, ENGINEER -> isEnhancementGemDelivery(id);
            default -> false;
        };
    }

    private static boolean isEnhancementGemDelivery(String id) {
        return "deliver_cracked_enhancement_gem".equals(id)
            || "deliver_split_enhancement_gem".equals(id)
            || "deliver_flawed_enhancement_gem".equals(id)
            || "deliver_enhancement_gem".equals(id)
            || "deliver_flawless_enhancement_gem".equals(id)
            || "deliver_perfect_enhancement_gem".equals(id);
    }

    private static boolean isLegacyEngineerFixedDelivery(String id) {
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

    public void addOrReplaceGeneralShopEntry(ShopEntry entry) {
        generalShopEntries().removeIf(existing -> existing.id().equals(entry.id()));
        generalShopEntries.add(entry);
        setDirty();
    }

    public void addOrReplaceShopEntry(TraderKind kind, ShopEntry entry) {
        List<ShopEntry> entries = shopEntries(kind);
        entries.removeIf(existing -> existing.id().equals(entry.id()));
        entries.add(entry);
        setDirty();
    }

    public long lotteryJackpot1() {
        return lotteryJackpot1;
    }

    public long lotteryJackpot2() {
        return lotteryJackpot2;
    }

    public void incrementJackpots(int ticketCount) {
        if (ticketCount <= 0) return;
        lotteryJackpot1 += ticketCount * 500L;
        lotteryJackpot2 += ticketCount * 200L;
        setDirty();
    }

    public void resetJackpot1() {
        lotteryJackpot1 = BASE_LOTTERY_JACKPOT_1;
        setDirty();
    }

    public void resetJackpot2() {
        lotteryJackpot2 = BASE_LOTTERY_JACKPOT_2;
        setDirty();
    }

    public void addOrReplaceDeliveryEntry(TraderKind kind, ShopEntry entry) {
        List<ShopEntry> entries = deliveryEntries(kind);
        entries.removeIf(existing -> existing.id().equals(entry.id()));
        entries.add(entry);
        setDirty();
    }

    public void removeShopEntry(TraderKind kind, String entryId, boolean delivery) {
        (delivery ? deliveryEntries(kind) : shopEntries(kind)).removeIf(existing -> existing.id().equals(entryId));
        shopPurchases.remove(entryId);
        setDirty();
    }

    public void resetShopEntries(TraderKind kind, boolean delivery) {
        if (delivery) {
            customDeliveryEntries.put(kind.id(), Shops.defaultDeliveries(kind));
        } else if (kind == TraderKind.GENERAL) {
            generalShopEntries.clear();
            generalShopEntries.addAll(Shops.defaults());
        } else {
            customShopEntries.put(kind.id(), Shops.defaults(kind));
        }
        setDirty();
    }

    public void refreshShopDay(long day) {
        if (shopDay != day) {
            shopDay = day;
            shopPurchases.clear();
            setDirty();
        }
    }

    public int remaining(ShopEntry entry) {
        return Math.max(0, entry.dailyLimit() - shopPurchases.getOrDefault(entry.id(), 0));
    }

    public boolean recordPurchase(ShopEntry entry) {
        int bought = shopPurchases.getOrDefault(entry.id(), 0);
        if (bought >= entry.dailyLimit()) {
            return false;
        }
        shopPurchases.put(entry.id(), bought + 1);
        setDirty();
        return true;
    }

    public boolean recordPurchase(ShopEntry entry, int quantity) {
        int bought = shopPurchases.getOrDefault(entry.id(), 0);
        if (quantity <= 0 || bought + quantity > entry.dailyLimit()) {
            return false;
        }
        shopPurchases.put(entry.id(), bought + quantity);
        setDirty();
        return true;
    }

    public int lotteryTicketsRemaining(UUID playerId) {
        int count = 0;
        for (UUID entry : lotteryEntries) {
            if (entry.equals(playerId)) {
                count++;
            }
        }
        return Math.max(0, DAILY_LOTTERY_LIMIT - count);
    }

    public boolean recordLotteryEntry(ServerPlayer player) {
        if (lotteryTicketsRemaining(player.getUUID()) <= 0) {
            return false;
        }
        rememberPlayer(player.getUUID(), player.getGameProfile().getName());
        lotteryEntries.add(player.getUUID());
        setDirty();
        return true;
    }

    public long lastLotteryDrawDay() {
        return lastLotteryDrawDay;
    }

    public List<UUID> consumeLotteryEntries(long drawDay) {
        List<UUID> entries = List.copyOf(lotteryEntries);
        lotteryEntries.clear();
        lastLotteryDrawDay = drawDay;
        setDirty();
        return entries;
    }

    public void queueItemReward(UUID playerId, Item item, int count) {
        if (count <= 0) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null) {
            return;
        }
        pendingItemRewards.computeIfAbsent(playerId, ignored -> new LinkedHashMap<>()).merge(itemId.toString(), count, Integer::sum);
        setDirty();
    }

    public void deliverPendingRewards(ServerPlayer player) {
        Map<String, Integer> rewards = pendingItemRewards.remove(player.getUUID());
        if (rewards == null || rewards.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> reward : rewards.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(reward.getKey()));
            if (item == Items.AIR || reward.getValue() <= 0) {
                continue;
            }
            ItemStack stack = new ItemStack(item, reward.getValue());
            ItemStack remainder = ExtendedInventoryDelivery.giveRemainder(player, stack);
            if (!remainder.isEmpty()) {
                queueItemReward(player.getUUID(), item, remainder.getCount());
            }
        }
        player.displayClientMessage(Component.translatable("message.nogeon_economy_land.reward.delivered"), false);
        setDirty();
    }

    public void queueGachaReward(UUID playerId, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        pendingGachaRewards.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(stack.copy());
        setDirty();
    }

    public int pendingGachaRewardCount(UUID playerId) {
        return pendingGachaRewards.getOrDefault(playerId, List.of()).size();
    }

    public List<ItemStack> pendingGachaRewards(UUID playerId) {
        List<ItemStack> rewards = pendingGachaRewards.getOrDefault(playerId, List.of());
        List<ItemStack> copy = new ArrayList<>(rewards.size());
        for (ItemStack stack : rewards) {
            copy.add(stack.copy());
        }
        return copy;
    }

    public boolean claimGachaReward(ServerPlayer player, int index) {
        List<ItemStack> rewards = pendingGachaRewards.get(player.getUUID());
        if (rewards == null || index < 0 || index >= rewards.size()) {
            return false;
        }
        ItemStack remainder = ExtendedInventoryDelivery.giveRemainder(player, rewards.get(index));
        if (!remainder.isEmpty()) {
            rewards.set(index, remainder);
            setDirty();
            return false;
        }
        rewards.remove(index);
        if (rewards.isEmpty()) {
            pendingGachaRewards.remove(player.getUUID());
        }
        setDirty();
        return true;
    }

    public int claimGachaRewards(ServerPlayer player) {
        List<ItemStack> rewards = pendingGachaRewards.remove(player.getUUID());
        if (rewards == null || rewards.isEmpty()) {
            return 0;
        }
        int claimed = 0;
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack reward : rewards) {
            ItemStack remainder = ExtendedInventoryDelivery.giveRemainder(player, reward);
            if (remainder.isEmpty()) {
                claimed++;
            } else {
                remaining.add(remainder);
            }
        }
        if (!remaining.isEmpty()) {
            pendingGachaRewards.put(player.getUUID(), remaining);
        }
        setDirty();
        return claimed;
    }

    public List<LandRegion> lands() {
        return lands;
    }

    public List<LandRegion> landsOf(UUID owner) {
        List<LandRegion> result = new ArrayList<>();
        for (LandRegion land : lands) {
            if (land.owner().equals(owner)) {
                result.add(land);
            }
        }
        return result;
    }

    public boolean isInsideAnyOwnedLand(UUID owner, ResourceKey<Level> world, net.minecraft.core.BlockPos pos) {
        for (LandRegion land : lands) {
            if (land.owner().equals(owner) && land.contains(world, pos)) {
                return true;
            }
        }
        return false;
    }

    public boolean isHomeSaveAllowed(UUID owner, ResourceKey<Level> world, net.minecraft.core.BlockPos pos) {
        LandRegion land = landColumnAt(world, pos);
        return land != null && land.owner().equals(owner);
    }

    public LandRegion landColumnAt(ResourceKey<Level> world, net.minecraft.core.BlockPos pos) {
        for (LandRegion land : lands) {
            if (land.containsColumn(world, pos)) {
                return land;
            }
        }
        return null;
    }

    public LandRegion landAt(ResourceKey<Level> world, net.minecraft.core.BlockPos pos) {
        for (LandRegion land : lands) {
            if (land.contains(world, pos)) {
                return land;
            }
        }
        return null;
    }

    public boolean overlaps(ResourceKey<Level> world, net.minecraft.core.BlockPos min, net.minecraft.core.BlockPos max) {
        for (LandRegion land : lands) {
            if (!land.world().equals(world)) {
                continue;
            }
            boolean separated = max.getX() < land.min().getX() || min.getX() > land.max().getX()
                || max.getY() < land.min().getY() || min.getY() > land.max().getY()
                || max.getZ() < land.min().getZ() || min.getZ() > land.max().getZ();
            if (!separated) {
                return true;
            }
        }
        return false;
    }

    public boolean selectionOverlaps(LandSelection selection) {
        for (LandSelection.Cuboid cuboid : selection.cuboids()) {
            if (overlaps(selection.world(), cuboid.min(), cuboid.max())) {
                return true;
            }
        }
        return false;
    }

    public LandRegion addLand(UUID owner, ResourceKey<Level> world, LandType type, net.minecraft.core.BlockPos min, net.minecraft.core.BlockPos max, long purchasePricePerBlock, String memo) {
        String landName = memo;
        if (landName == null || landName.trim().isEmpty() || landName.startsWith("land_type.nogeon_economy_land")) {
            String typeKor = switch (type) {
                case BASIC -> "기본";
                case NORMAL -> "일반";
                case INDUSTRIAL -> "산업";
                case ADMIN -> "관리자";
            };
            landName = "[" + typeKor + "] 토지 #" + nextLandId;
        }
        LandRegion land = new LandRegion(nextLandId++, owner, world, type, min, max, purchasePricePerBlock, landName, new HashMap<>(), new EnumMap<>(LandFlag.class));
        lands.add(land);
        setDirty();
        return land;
    }

    public void addLandSelection(UUID owner, LandSelection selection, long purchasePricePerBlock, String memo) {
        for (LandSelection.Cuboid cuboid : selection.cuboids()) {
            addLand(owner, selection.world(), selection.type(), cuboid.min(), cuboid.max(), purchasePricePerBlock, memo);
        }
    }

    public boolean hasAdminLand() {
        for (LandRegion land : lands) {
            if (land.type() == LandType.ADMIN) {
                return true;
            }
        }
        return false;
    }

    public List<LandRegion> adminLands() {
        List<LandRegion> result = new ArrayList<>();
        for (LandRegion land : lands) {
            if (land.type() == LandType.ADMIN) {
                result.add(land);
            }
        }
        return result;
    }

    public boolean removeAdminLand() {
        boolean removed = lands.removeIf(land -> land.type() == LandType.ADMIN);
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public boolean canOwnLand(UUID owner, LandType type) {
        return type != LandType.INDUSTRIAL || profile(owner).socialClass().canBuyIndustrialLand();
    }

    public long sellLand(UUID owner, int landId) {
        LandRegion land = landById(landId);
        if (land == null || !land.owner().equals(owner)) {
            return -1L;
        }
        // 매각 시 구매했던 가격의 90%만 환급 (매각 수수료 10%)
        long refund = (long) (land.blocks() * land.purchasePricePerBlock() * 0.9);
        lands.remove(land);
        profile(owner).addCredits(refund);
        setDirty();
        return refund;
    }

    public LandRegion landById(int id) {
        for (LandRegion land : lands) {
            if (land.id() == id) {
                return land;
            }
        }
        return null;
    }

    public boolean setLandPermission(UUID owner, int landId, UUID target, LandPermission permission) {
        LandRegion land = landById(landId);
        if (land == null || !land.owner().equals(owner) || land.owner().equals(target)) {
            return false;
        }
        if (permission == LandPermission.NONE) {
            land.permissions().remove(target);
        } else {
            land.permissions().put(target, permission);
        }
        setDirty();
        return true;
    }

    public boolean setLandFlag(UUID owner, int landId, LandFlag flag, boolean value) {
        LandRegion land = landById(landId);
        if (land == null || !land.owner().equals(owner)) {
            return false;
        }
        land.setFlag(flag, value);
        setDirty();
        return true;
    }

    public boolean setAdminLandFlag(int landId, LandFlag flag, boolean value) {
        LandRegion land = landById(landId);
        if (land == null || land.type() != LandType.ADMIN) {
            return false;
        }
        land.setFlag(flag, value);
        setDirty();
        return true;
    }

    private void transferLandOwnership(UUID fromOwner, UUID toOwner, int landId) {
        for (int index = 0; index < lands.size(); index++) {
            LandRegion land = lands.get(index);
            if (land.id() != landId || !land.owner().equals(fromOwner)) {
                continue;
            }
            lands.set(index, new LandRegion(land.id(), toOwner, land.world(), land.type(), land.min(), land.max(), land.purchasePricePerBlock(), land.memo(), new HashMap<>(), new EnumMap<>(land.flags())));
            return;
        }
    }

    public List<ServerPlayer> nearbyPlayers(ServerPlayer player, double radius) {
        List<ServerPlayer> result = new ArrayList<>();
        double maxDistance = radius * radius;
        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (other != player && other.level() == player.level() && other.distanceToSqr(player) <= maxDistance) {
                result.add(other);
            }
        }
        return result;
    }

    public boolean canTradeTogether(ServerPlayer first, ServerPlayer second) {
        return first.level() == second.level() && first.distanceToSqr(second) <= 64.0D;
    }

    public boolean requestTrade(ServerPlayer sender, ServerPlayer target) {
        if (tradeSession(sender.getUUID()) != null || tradeSession(target.getUUID()) != null || tradeRequests.containsKey(target.getUUID())) {
            return false;
        }
        tradeRequests.put(target.getUUID(), sender.getUUID());
        return true;
    }

    public void clearTradeRequest(UUID responder) {
        tradeRequests.remove(responder);
    }

    public TradeSession acceptTrade(ServerPlayer responder, ServerPlayer requester) {
        UUID expectedRequester = tradeRequests.get(responder.getUUID());
        if (expectedRequester == null || !expectedRequester.equals(requester.getUUID())) {
            return null;
        }
        TradeSession session = new TradeSession(requester.getUUID(), responder.getUUID());
        tradeSessions.put(requester.getUUID(), session);
        tradeSessions.put(responder.getUUID(), session);
        tradeRequests.remove(responder.getUUID());
        return session;
    }

    public TradeSession tradeSession(UUID playerId) {
        return tradeSessions.get(playerId);
    }

    public ServerPlayer partnerPlayer(ServerPlayer player) {
        TradeSession session = tradeSession(player.getUUID());
        if (session == null) {
            return null;
        }
        return player.server.getPlayerList().getPlayer(session.partner(player.getUUID()));
    }

    public void cancelTrade(UUID playerId) {
        cancelTrade(playerId, "cancel");
    }

    public void cancelTrade(UUID playerId, String reason) {
        TradeSession session = tradeSession(playerId);
        if (session == null) {
            return;
        }
        appendTradeLog("cancel:" + reason + ":" + session.firstPlayer() + ":" + session.secondPlayer());
        tradeSessions.remove(session.firstPlayer());
        tradeSessions.remove(session.secondPlayer());
        setDirty();
    }

    public boolean setTradeCredits(ServerPlayer player, long credits) {
        TradeSession session = tradeSession(player.getUUID());
        PlayerProfile profile = profile(player.getUUID());
        if (session == null || credits < 0 || credits > profile.credits()) {
            return false;
        }
        session.setCredits(player.getUUID(), credits);
        return true;
    }

    public boolean addHeldTradeOffer(ServerPlayer player) {
        TradeSession session = tradeSession(player.getUUID());
        ItemStack stack = player.getMainHandItem();
        if (session == null || stack.isEmpty()) {
            return false;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        session.addOffer(player.getUUID(), itemId, stack.getCount());
        return true;
    }

    public List<ItemStack> tradeOfferItems(ServerPlayer player) {
        Map<String, ItemStack> previews = new LinkedHashMap<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            collectTradeItems(player.getInventory().getItem(slot), previews);
        }
        return new ArrayList<>(previews.values());
    }

    public boolean addInventoryTradeOffer(ServerPlayer player, String itemId) {
        TradeSession session = tradeSession(player.getUUID());
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (session == null || id == null) {
            return false;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        int available = accessibleItemCount(player, item) - session.offers(player.getUUID()).getOrDefault(itemId, 0);
        if (item == Items.AIR || available <= 0) {
            return false;
        }
        session.addOffer(player.getUUID(), itemId, available);
        return true;
    }

    public String addLandTradeOffer(ServerPlayer player, int landId) {
        TradeSession session = tradeSession(player.getUUID());
        if (session == null) {
            return "message.nogeon_economy_land.trade.failed";
        }
        LandRegion land = landById(landId);
        if (land == null || !land.owner().equals(player.getUUID())) {
            return "message.nogeon_economy_land.trade.land_invalid";
        }
        if (session.landOffers(player.getUUID()).contains(landId)) {
            return "message.nogeon_economy_land.trade.land_duplicate";
        }
        UUID partnerId = session.partner(player.getUUID());
        if (!canOwnLand(partnerId, land.type())) {
            return "message.nogeon_economy_land.trade.land_target_restricted";
        }
        session.addLandOffer(player.getUUID(), landId);
        return null;
    }

    public void clearTradeOffer(ServerPlayer player) {
        TradeSession session = tradeSession(player.getUUID());
        if (session != null) {
            session.clearOffers(player.getUUID());
        }
    }

    public HighLowSession startHighLow(ServerPlayer player, long stake) {
        HighLowSession session = new HighLowSession(player.getUUID(), stake);
        session.start(player);
        highLowSessions.put(player.getUUID(), session);
        return session;
    }

    public HighLowSession highLowSession(UUID playerId) {
        return highLowSessions.get(playerId);
    }

    public long finishHighLow(ServerPlayer player, boolean awardPayout) {
        HighLowSession session = highLowSessions.remove(player.getUUID());
        if (session == null) {
            return 0L;
        }
        long reward = awardPayout ? session.payout() : 0L;
        if (reward > 0L) {
            PlayerProfile profile = profile(player.getUUID());
            profile.addCredits(reward);
            SyncCreditsPacket.send(player, profile.credits());
            setDirty();
        }
        return reward;
    }

    public void toggleTradeReady(ServerPlayer player) {
        TradeSession session = tradeSession(player.getUUID());
        if (session != null) {
            session.toggleReady(player.getUUID());
        }
    }

    public void confirmTrade(ServerPlayer player) {
        TradeSession session = tradeSession(player.getUUID());
        if (session != null && session.canConfirm(player.getUUID())) {
            session.confirm(player.getUUID());
        }
    }

    public boolean finalizeTrade(MinecraftServer server, UUID playerId) {
        TradeSession session = tradeSession(playerId);
        if (session == null || !session.fullyConfirmed()) {
            return false;
        }
        ServerPlayer first = server.getPlayerList().getPlayer(session.firstPlayer());
        ServerPlayer second = server.getPlayerList().getPlayer(session.secondPlayer());
        if (first == null || second == null) {
            return false;
        }
        PlayerProfile firstProfile = profile(first.getUUID());
        PlayerProfile secondProfile = profile(second.getUUID());
        if (firstProfile.credits() < session.credits(first.getUUID()) || secondProfile.credits() < session.credits(second.getUUID())) {
            return false;
        }
        if (!hasItems(first, session.offers(first.getUUID())) || !hasItems(second, session.offers(second.getUUID()))) {
            return false;
        }
        if (!canTransferOfferedLands(session)) {
            return false;
        }
        moveItems(first, second, session.offers(first.getUUID()));
        moveItems(second, first, session.offers(second.getUUID()));
        firstProfile.spendCredits(session.credits(first.getUUID()));
        secondProfile.addCredits(session.credits(first.getUUID()));
        secondProfile.spendCredits(session.credits(second.getUUID()));
        firstProfile.addCredits(session.credits(second.getUUID()));
        transferOfferedLands(session);
        appendTradeLog("complete:" + session.firstPlayer() + ":" + session.secondPlayer()
            + ":c1=" + session.credits(first.getUUID()) + ":c2=" + session.credits(second.getUUID()));
        tradeSessions.remove(session.firstPlayer());
        tradeSessions.remove(session.secondPlayer());
        setDirty();
        return true;
    }

    private boolean canTransferOfferedLands(TradeSession session) {
        return canTransferOfferedLands(session.firstPlayer(), session.secondPlayer(), session.landOffers(session.firstPlayer()))
            && canTransferOfferedLands(session.secondPlayer(), session.firstPlayer(), session.landOffers(session.secondPlayer()));
    }

    private boolean canTransferOfferedLands(UUID fromOwner, UUID toOwner, java.util.Set<Integer> landIds) {
        for (Integer landId : landIds) {
            if (landId == null) {
                return false;
            }
            LandRegion land = landById(landId);
            if (land == null || !land.owner().equals(fromOwner) || !canOwnLand(toOwner, land.type())) {
                return false;
            }
        }
        return true;
    }

    private void transferOfferedLands(TradeSession session) {
        transferOfferedLands(session.firstPlayer(), session.secondPlayer(), session.landOffers(session.firstPlayer()));
        transferOfferedLands(session.secondPlayer(), session.firstPlayer(), session.landOffers(session.secondPlayer()));
    }

    private void transferOfferedLands(UUID fromOwner, UUID toOwner, java.util.Set<Integer> landIds) {
        for (Integer landId : new java.util.LinkedHashSet<>(landIds)) {
            transferLandOwnership(fromOwner, toOwner, landId);
        }
    }

    private static boolean hasItems(ServerPlayer player, Map<String, Integer> offers) {
        for (Map.Entry<String, Integer> offer : offers.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(offer.getKey()));
            if (accessibleItemCount(player, item) < offer.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static void moveItems(ServerPlayer from, ServerPlayer to, Map<String, Integer> offers) {
        for (Map.Entry<String, Integer> offer : offers.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(offer.getKey()));
            extractTradeItems(from, to, item, offer.getValue());
        }
    }

    private static void collectTradeItems(ItemStack stack, Map<String, ItemStack> previews) {
        if (stack.isEmpty()) {
            return;
        }
        addTradePreview(stack, previews);
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(stack.getOrCreateTagElement("BlockEntityTag"), items);
            for (ItemStack nested : items) {
                collectTradeItems(nested, previews);
            }
            return;
        }
        stack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> collectTradeItems(handler, previews));
    }

    private static void collectTradeItems(IItemHandler handler, Map<String, ItemStack> previews) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            collectTradeItems(handler.getStackInSlot(slot), previews);
        }
    }

    private static void addTradePreview(ItemStack stack, Map<String, ItemStack> previews) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null || stack.isEmpty()) {
            return;
        }
        ItemStack preview = previews.get(id.toString());
        if (preview == null) {
            previews.put(id.toString(), stack.copy());
        } else {
            preview.setCount(Math.min(Integer.MAX_VALUE, preview.getCount() + stack.getCount()));
        }
    }

    private static int accessibleItemCount(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            count += accessibleItemCount(player.getInventory().getItem(slot), item);
        }
        return count;
    }

    private static int accessibleItemCount(ItemStack stack, Item item) {
        if (stack.isEmpty()) {
            return 0;
        }
        int count = stack.is(item) ? stack.getCount() : 0;
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(stack.getOrCreateTagElement("BlockEntityTag"), items);
            for (ItemStack nested : items) {
                count += accessibleItemCount(nested, item);
            }
            return count;
        }
        IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler != null) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                count += accessibleItemCount(handler.getStackInSlot(slot), item);
            }
        }
        return count;
    }

    private static int extractTradeItems(ServerPlayer from, ServerPlayer to, Item item, int remaining) {
        for (int slot = 0; slot < from.getInventory().getContainerSize() && remaining > 0; slot++) {
            remaining = extractTradeItems(from.getInventory().getItem(slot), to, item, remaining);
        }
        return remaining;
    }

    private static int extractTradeItems(ItemStack stack, ServerPlayer to, Item item, int remaining) {
        if (stack.isEmpty() || remaining <= 0) {
            return remaining;
        }
        if (stack.is(item)) {
            int moved = Math.min(remaining, stack.getCount());
            giveTradeStack(to, stack.split(moved));
            remaining -= moved;
        }
        if (remaining <= 0) {
            return 0;
        }
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
            CompoundTag blockEntityTag = stack.getOrCreateTagElement("BlockEntityTag");
            ContainerHelper.loadAllItems(blockEntityTag, items);
            for (ItemStack nested : items) {
                remaining = extractTradeItems(nested, to, item, remaining);
            }
            ContainerHelper.saveAllItems(blockEntityTag, items);
            return remaining;
        }
        IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (handler != null) {
            for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                ItemStack nested = handler.getStackInSlot(slot);
                if (!nested.is(item)) {
                    continue;
                }
                ItemStack extracted = handler.extractItem(slot, remaining, false);
                remaining -= extracted.getCount();
                giveTradeStack(to, extracted);
            }
        }
        return remaining;
    }

    private static void giveTradeStack(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty()) {
            ExtendedInventoryDelivery.giveOrDrop(player, stack);
        }
    }

    private void appendTradeLog(String line) {
        tradeLogs.add(System.currentTimeMillis() + ":" + line);
        while (tradeLogs.size() > 100) {
            tradeLogs.remove(0);
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag profilesNbt = new CompoundTag();
        for (Map.Entry<UUID, PlayerProfile> entry : profiles.entrySet()) {
            profilesNbt.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        nbt.put("profiles", profilesNbt);
        CompoundTag knownPlayersNbt = new CompoundTag();
        for (Map.Entry<UUID, String> entry : knownPlayers.entrySet()) {
            knownPlayersNbt.putString(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("knownPlayers", knownPlayersNbt);
        CompoundTag shopNbt = new CompoundTag();
        shopNbt.putLong("day", shopDay);
        shopNbt.putInt("gunShopAmmoRevision", gunShopAmmoRevision);
        CompoundTag entriesNbt = new CompoundTag();
        int index = 0;
        for (ShopEntry entry : generalShopEntries()) {
            entriesNbt.put(String.valueOf(index++), entry.toNbt());
        }
        shopNbt.putInt("entryCount", index);
        shopNbt.put("entries", entriesNbt);
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
        nbt.put("shop", shopNbt);
        CompoundTag lotteryNbt = new CompoundTag();
        lotteryNbt.putLong("lastDrawDay", lastLotteryDrawDay);
        lotteryNbt.putLong("jackpot1", lotteryJackpot1);
        lotteryNbt.putLong("jackpot2", lotteryJackpot2);
        lotteryNbt.putInt("entryCount", lotteryEntries.size());
        for (int i = 0; i < lotteryEntries.size(); i++) {
            lotteryNbt.putString(String.valueOf(i), lotteryEntries.get(i).toString());
        }
        nbt.put("lottery", lotteryNbt);
        CompoundTag pendingRewardsNbt = new CompoundTag();
        for (Map.Entry<UUID, Map<String, Integer>> rewardEntry : pendingItemRewards.entrySet()) {
            CompoundTag playerRewardsNbt = new CompoundTag();
            for (Map.Entry<String, Integer> itemReward : rewardEntry.getValue().entrySet()) {
                playerRewardsNbt.putInt(itemReward.getKey(), itemReward.getValue());
            }
            pendingRewardsNbt.put(rewardEntry.getKey().toString(), playerRewardsNbt);
        }
        nbt.put("pendingRewards", pendingRewardsNbt);
        CompoundTag pendingGachaNbt = new CompoundTag();
        for (Map.Entry<UUID, List<ItemStack>> rewardEntry : pendingGachaRewards.entrySet()) {
            ListTag stacksNbt = new ListTag();
            for (ItemStack stack : rewardEntry.getValue()) {
                stacksNbt.add(stack.save(new CompoundTag()));
            }
            pendingGachaNbt.put(rewardEntry.getKey().toString(), stacksNbt);
        }
        nbt.put("pendingGachaRewards", pendingGachaNbt);

        CompoundTag placedBlocksNbt = new CompoundTag();
        int placedIndex = 0;
        for (String key : playerPlacedResourceBlocks) {
            placedBlocksNbt.putString(String.valueOf(placedIndex++), key);
        }
        placedBlocksNbt.putInt("count", placedIndex);
        nbt.put("playerPlacedResourceBlocks", placedBlocksNbt);

        CompoundTag tradeLogsNbt = new CompoundTag();
        tradeLogsNbt.putInt("count", tradeLogs.size());
        for (int i = 0; i < tradeLogs.size(); i++) {
            tradeLogsNbt.putString(String.valueOf(i), tradeLogs.get(i));
        }
        nbt.put("tradeLogs", tradeLogsNbt);

        CompoundTag landsNbt = new CompoundTag();
        int landIndex = 0;
        for (LandRegion land : lands) {
            landsNbt.put(String.valueOf(landIndex++), land.toNbt());
        }
        landsNbt.putInt("count", landIndex);
        landsNbt.putInt("nextId", nextLandId);
        nbt.put("lands", landsNbt);
        return nbt;
    }

    public static EconomyState fromNbt(CompoundTag nbt) {
        EconomyState state = new EconomyState();
        CompoundTag profilesNbt = nbt.getCompound("profiles");
        for (String key : profilesNbt.getAllKeys()) {
            state.profiles.put(UUID.fromString(key), PlayerProfile.fromNbt(profilesNbt.getCompound(key)));
        }
        CompoundTag knownPlayersNbt = nbt.getCompound("knownPlayers");
        for (String key : knownPlayersNbt.getAllKeys()) {
            state.knownPlayers.put(UUID.fromString(key), knownPlayersNbt.getString(key));
        }
        CompoundTag shopNbt = nbt.getCompound("shop");
        state.shopDay = shopNbt.getLong("day");
        state.gunShopAmmoRevision = shopNbt.getInt("gunShopAmmoRevision");
        CompoundTag entriesNbt = shopNbt.getCompound("entries");
        int entryCount = shopNbt.getInt("entryCount");
        for (int i = 0; i < entryCount; i++) {
            state.generalShopEntries.add(ShopEntry.fromNbt(entriesNbt.getCompound(String.valueOf(i))));
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
        CompoundTag lotteryNbt = nbt.getCompound("lottery");
        state.lastLotteryDrawDay = lotteryNbt.getLong("lastDrawDay");
        state.lotteryJackpot1 = lotteryNbt.contains("jackpot1") ? lotteryNbt.getLong("jackpot1") : BASE_LOTTERY_JACKPOT_1;
        state.lotteryJackpot2 = lotteryNbt.contains("jackpot2") ? lotteryNbt.getLong("jackpot2") : BASE_LOTTERY_JACKPOT_2;
        state.lotteryJackpot1 = Math.max(state.lotteryJackpot1, BASE_LOTTERY_JACKPOT_1);
        state.lotteryJackpot2 = Math.max(state.lotteryJackpot2, BASE_LOTTERY_JACKPOT_2);
        int lotteryEntryCount = lotteryNbt.getInt("entryCount");
        for (int i = 0; i < lotteryEntryCount; i++) {
            String playerId = lotteryNbt.getString(String.valueOf(i));
            if (!playerId.isBlank()) {
                state.lotteryEntries.add(UUID.fromString(playerId));
            }
        }
        CompoundTag pendingRewardsNbt = nbt.getCompound("pendingRewards");
        for (String key : pendingRewardsNbt.getAllKeys()) {
            CompoundTag playerRewardsNbt = pendingRewardsNbt.getCompound(key);
            Map<String, Integer> rewards = new LinkedHashMap<>();
            for (String itemId : playerRewardsNbt.getAllKeys()) {
                rewards.put(itemId, playerRewardsNbt.getInt(itemId));
            }
            if (!rewards.isEmpty()) {
                state.pendingItemRewards.put(UUID.fromString(key), rewards);
            }
        }
        CompoundTag pendingGachaNbt = nbt.getCompound("pendingGachaRewards");
        for (String key : pendingGachaNbt.getAllKeys()) {
            ListTag stacksNbt = pendingGachaNbt.getList(key, 10);
            List<ItemStack> rewards = new ArrayList<>();
            for (int i = 0; i < stacksNbt.size(); i++) {
                ItemStack stack = ItemStack.of(stacksNbt.getCompound(i));
                if (!stack.isEmpty()) {
                    rewards.add(stack);
                }
            }
            if (!rewards.isEmpty()) {
                state.pendingGachaRewards.put(UUID.fromString(key), rewards);
            }
        }

        CompoundTag placedBlocksNbt = nbt.getCompound("playerPlacedResourceBlocks");
        int placedCount = placedBlocksNbt.getInt("count");
        for (int i = 0; i < placedCount; i++) {
            String key = placedBlocksNbt.getString(String.valueOf(i));
            if (!key.isBlank()) {
                state.playerPlacedResourceBlocks.add(key);
            }
        }

        CompoundTag tradeLogsNbt = nbt.getCompound("tradeLogs");
        int tradeLogCount = tradeLogsNbt.getInt("count");
        for (int i = 0; i < tradeLogCount; i++) {
            String line = tradeLogsNbt.getString(String.valueOf(i));
            if (!line.isBlank()) {
                state.tradeLogs.add(line);
            }
        }

        CompoundTag landsNbt = nbt.getCompound("lands");
        state.nextLandId = Math.max(1, landsNbt.getInt("nextId"));
        int landCount = landsNbt.getInt("count");
        for (int i = 0; i < landCount; i++) {
            state.lands.add(LandRegion.fromNbt(landsNbt.getCompound(String.valueOf(i))));
        }
        return state;
    }

    private void migrateGunShopEntries() {
        if (gunShopAmmoRevision >= GUN_SHOP_AMMO_REVISION) {
            return;
        }
        customShopEntries.put(TraderKind.GUN.id(), Shops.defaults(TraderKind.GUN));
        shopPurchases.keySet().removeIf(key -> key.startsWith("ammo_"));
        gunShopAmmoRevision = GUN_SHOP_AMMO_REVISION;
        setDirty();
    }
}
