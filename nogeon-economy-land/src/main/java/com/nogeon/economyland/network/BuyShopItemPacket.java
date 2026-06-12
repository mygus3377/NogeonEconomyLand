package com.nogeon.economyland.network;

import com.nogeon.economyland.player.JobProgress;
import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.SkillNodeStat;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.job.JobEvents;
import com.nogeon.economyland.menu.ShopOpener;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.shop.ShopItemProtection;
import com.nogeon.economyland.shop.Shops;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.TraderShopState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public final class BuyShopItemPacket {
    private final String kindId;
    private final String traderDatabaseId;
    private final String entryId;
    private final boolean delivery;
    private final int quantity;
    private final boolean normalSell;

    public BuyShopItemPacket(String kindId, String traderDatabaseId, String entryId, boolean delivery, int quantity) {
        this(kindId, traderDatabaseId, entryId, delivery, quantity, false);
    }

    public BuyShopItemPacket(String kindId, String traderDatabaseId, String entryId, boolean delivery, int quantity, boolean normalSell) {
        this.kindId = kindId;
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.entryId = entryId;
        this.delivery = delivery;
        this.quantity = Math.max(1, quantity);
        this.normalSell = normalSell;
    }

    public static void encode(BuyShopItemPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.kindId);
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeUtf(packet.entryId);
        buffer.writeBoolean(packet.delivery);
        buffer.writeVarInt(packet.quantity);
        buffer.writeBoolean(packet.normalSell);
    }

    public static BuyShopItemPacket decode(FriendlyByteBuf buffer) {
        return new BuyShopItemPacket(
            buffer.readUtf(),
            buffer.readUtf(),
            buffer.readUtf(),
            buffer.readBoolean(),
            buffer.readVarInt(),
            buffer.readBoolean()
        );
    }

    public static void handle(BuyShopItemPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            EconomyState state = EconomyState.get(player.server);
            TraderShopState traderState = TraderShopState.get(player.server);
            TraderKind kind = TraderKind.byId(packet.kindId);
            ShopEntry entry = Shops.find(packet.delivery
                ? traderState.deliveryEntries(state, kind, packet.traderDatabaseId)
                : ShopOpener.entriesFor(player, kind, packet.traderDatabaseId), packet.entryId);
            
            if (entry == null && packet.delivery && packet.entryId.startsWith("dynamic:")) {
                entry = resolveDynamicEntry(player, kind, packet.entryId);
            }

            if (entry == null) {
                return;
            }

            traderState.refreshShopDay(state, player.server.overworld().getDayTime() / 24000L);
            PlayerProfile profile = state.profile(player.getUUID());
            if (packet.delivery) {
                deliverItem(player, profile, state, kind, packet.traderDatabaseId, entry, packet.quantity, packet.normalSell);
            } else if (traderState.remaining(state, packet.traderDatabaseId, entry) < packet.quantity) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.shop.sold_out"), false);
            } else if (!profile.spendCredits(traderState.adjustedPrice(kind, packet.traderDatabaseId, entry, false) * packet.quantity)) {
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.shop.no_money"), false);
            } else {
                traderState.recordPurchase(state, packet.traderDatabaseId, entry, packet.quantity);
                ItemStack stack = entry.stack();
                stack.setCount(entry.count() * packet.quantity);
                ShopItemProtection.markPurchased(stack);
                ExtendedInventoryDelivery.giveOrDrop(player, stack);
                SyncCreditsPacket.send(player, profile.credits());
                state.setDirty();
            }
            if (kind == TraderKind.SMITH) {
                java.util.List<com.nogeon.economyland.shop.ShopEntry> entries = com.nogeon.economyland.menu.ShopOpener.entriesFor(player, TraderKind.SMITH, packet.traderDatabaseId);
                com.nogeon.economyland.menu.ShopOpener.openShop(player, TraderKind.SMITH, packet.traderDatabaseId, entries);
            } else if (kind == TraderKind.HUNTER) {
                // 사냥꾼은 supportsInventoryShop()이 false이므로(우클릭 시 선택창을 띄우게 함),
                // 상점 거래 후 리프레시할 때는 선택창으로 튕겨나가지 않고 상점을 다이렉트로 재오픈하여 동기화해줍니다!
                java.util.List<com.nogeon.economyland.shop.ShopEntry> entries = com.nogeon.economyland.menu.ShopOpener.entriesFor(player, TraderKind.HUNTER, packet.traderDatabaseId);
                com.nogeon.economyland.menu.ShopOpener.openShop(player, TraderKind.HUNTER, packet.traderDatabaseId, entries);
            } else {
                ShopOpener.open(player, kind, packet.traderDatabaseId);
            }
        });
        context.setPacketHandled(true);
    }

    private static ShopEntry resolveDynamicEntry(ServerPlayer player, TraderKind kind, String entryId) {
        // 1. 일반 인벤토리 스캔 (장착 장비/보조손 제외)
        for (int i = 0; i < 36; i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            ShopEntry entry = checkDynamicMatch(invStack, kind, entryId);
            if (entry != null) return entry;
        }
        
        // 2. 확장 보관함 스캔
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        ItemStack[] extItems = ExtendedInventoryDelivery.load(profile.extInventoryData());
        int unlockedSlots = Math.min(270, Math.max(0, profile.inventoryExtLevel() * 9));
        for (int slot = 0; slot < unlockedSlots; slot++) {
            ItemStack stack = extItems[slot];
            ShopEntry entry = checkDynamicMatch(stack, kind, entryId);
            if (entry != null) return entry;
        }

        // 3. 배낭(Backpack) 내부 스캔
        for (ItemStack backpack : ExtendedInventoryDelivery.findAllBackpacks(player)) {
            var cap = backpack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
            if (cap.isPresent()) {
                net.minecraftforge.items.IItemHandler handler = cap.orElse(null);
                if (handler != null) {
                    for (int slot = 0; slot < handler.getSlots(); slot++) {
                        ItemStack stack = handler.getStackInSlot(slot);
                        ShopEntry entry = checkDynamicMatch(stack, kind, entryId);
                        if (entry != null) return entry;
                    }
                }
            }
        }
        
        return null;
    }

    private static ShopEntry checkDynamicMatch(ItemStack stack, TraderKind kind, String entryId) {
        if (!stack.isEmpty() && !ShopItemProtection.isSellBlocked(stack) && com.nogeon.economyland.shop.DynamicPriceLogic.shouldAccept(kind, stack)) {
            boolean ignoreNbt = (kind == TraderKind.CHEF || kind == TraderKind.CROP || kind == TraderKind.FISHER || kind == TraderKind.MINER || kind == TraderKind.HUNTER || kind == TraderKind.GENERAL);
            String nbtSuffix = (!ignoreNbt && stack.hasTag()) ? ":" + Integer.toHexString(stack.getTag().hashCode()) : "";
            String dynamicId = "dynamic:" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()) + nbtSuffix;
            if (dynamicId.equals(entryId)) {
                long basePrice = com.nogeon.economyland.shop.DynamicPriceLogic.calculatePrice(kind, stack) * TraderShopState.DELIVERY_PAYOUT_MULTIPLIER;
                return new ShopEntry(entryId, stack.copy(), basePrice, 0);
            }
        }
        return null;
    }

    private static void deliverItem(ServerPlayer player, PlayerProfile profile, EconomyState state, TraderKind kind, String traderDatabaseId, ShopEntry entry, int quantity, boolean normalSell) {
        int unitCount = normalSell ? 1 : entry.count();
        int remaining = quantity * unitCount;
        
        // 인벤토리 + 보관함 + 배낭 통합 수량 조사
        int totalAvailable = com.nogeon.economyland.player.ExtendedInventoryDelivery.countAllOwned(player, entry.stack(), kind);
        if (totalAvailable < remaining) {
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.delivery.locked_or_missing"), false);
            return;
        }

        int toRemove = quantity * unitCount;
        int totalItems = toRemove;
        int shopPurchasedCount = 0;
        
        // 인벤토리 내 샵 구매 마킹 아이템 카운팅 (장착 장비/보조손 36~40 슬롯 제외)
        int tempToRemove = toRemove;
        for (int slot = 0; slot < 36 && tempToRemove > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && !ShopItemProtection.isSellBlocked(stack) && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, entry.stack(), kind)) {
                int removed = Math.min(tempToRemove, stack.getCount());
                if (ShopItemProtection.isShopPurchased(stack)) {
                    shopPurchasedCount += removed;
                }
                tempToRemove -= removed;
            }
        }

        // 플레이어 가방 내의 nogeon_plus_grade 가 붙은 농산물 수량 조사
        int plusCount = 0;
        JobType deliveryJob = deliveryJob(kind);
        if (deliveryJob == JobType.FARMER) {
            // 인벤토리 스캔 (장착 장비/보조손 제외)
            for (int i = 0; i < 36; i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (!invStack.isEmpty() && invStack.getItem() == entry.stack().getItem() && invStack.hasTag() && invStack.getTag().getBoolean("nogeon_plus_grade")) {
                    plusCount += invStack.getCount();
                }
            }
            // 확장 보관함 스캔
            ItemStack[] extItems = ExtendedInventoryDelivery.load(profile.extInventoryData());
            int unlockedSlots = Math.min(270, Math.max(0, profile.inventoryExtLevel() * 9));
            for (int slot = 0; slot < unlockedSlots; slot++) {
                ItemStack invStack = extItems[slot];
                if (invStack != null && !invStack.isEmpty() && invStack.getItem() == entry.stack().getItem() && invStack.hasTag() && invStack.getTag().getBoolean("nogeon_plus_grade")) {
                    plusCount += invStack.getCount();
                }
            }
            // 배낭 스캔
            for (ItemStack backpack : ExtendedInventoryDelivery.findAllBackpacks(player)) {
                var cap = backpack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER);
                if (cap.isPresent()) {
                    net.minecraftforge.items.IItemHandler handler = cap.orElse(null);
                    if (handler != null) {
                        for (int slot = 0; slot < handler.getSlots(); slot++) {
                            ItemStack invStack = handler.getStackInSlot(slot);
                            if (!invStack.isEmpty() && invStack.getItem() == entry.stack().getItem() && invStack.hasTag() && invStack.getTag().getBoolean("nogeon_plus_grade")) {
                                plusCount += invStack.getCount();
                            }
                        }
                    }
                }
            }
            plusCount = Math.min(plusCount, toRemove);
        }

        // 통합 차감 (인벤토리 -> 보관함 -> 배낭)
        com.nogeon.economyland.player.ExtendedInventoryDelivery.consumeAllOwned(player, entry.stack(), toRemove, kind);

        long base;
        if (normalSell) {
            base = TraderShopState.get(player.server).adjustedNormalSellPrice(kind, traderDatabaseId, entry) * quantity;
            if (entry.id().startsWith("dynamic:")) {
                base = Math.round(com.nogeon.economyland.shop.DynamicPriceLogic.calculatePrice(kind, entry.stack()) * 2.4D * quantity);
            } else {
                int bundleSize = Math.max(1, entry.count());
                base = Math.max(1L, Math.round((double) base / bundleSize));
                if (totalItems > 0 && shopPurchasedCount > 0) {
                    double multiplier = (double) (totalItems - (shopPurchasedCount * 0.98D)) / totalItems;
                    base = Math.round(base * multiplier);
                }
            }
        } else {
            base = TraderShopState.get(player.server).adjustedDeliveryPrice(kind, traderDatabaseId, entry) * quantity;
            if (entry.id().startsWith("dynamic:")) {
                base = Math.round(com.nogeon.economyland.shop.DynamicPriceLogic.calculatePrice(kind, entry.stack()) * 1.8D * entry.count() * quantity);
            } else if (totalItems > 0 && shopPurchasedCount > 0) {
                double multiplier = (double) (totalItems - (shopPurchasedCount * 0.98D)) / totalItems;
                base = Math.round(base * multiplier);
            }
        }
        
        long bonus = 0;
        if (deliveryJob != null) {
            JobProgress job = profile.job(deliveryJob);
            bonus = base * job.bonusPercent(SkillNodeStat.DELIVERY_PRICE) / 100L;
        }
        long paid = base + bonus;
        
        // 고급 부산물 가치 2배 보정
        if (plusCount > 0) {
            double plusMultiplier = 1.0D + ((double) plusCount / toRemove);
            paid = Math.round(paid * plusMultiplier);
            player.displayClientMessage(Component.literal("§e✨ [대지의 기적] §f고급 농산물(§e+§f 등급) §a" + plusCount + "개§f가 2배 가격으로 판매되었습니다!"), false);
        }
        
        profile.addCredits(paid);
        if (!normalSell && deliveryJob != null) {
            JobEvents.grantJobExp(player, deliveryJob, deliveryExp(deliveryJob, toRemove, paid));
        }
        
        // Dynamic market: add to trader stock (only for bulk delivery)
        if (!normalSell && traderDatabaseId != null && !traderDatabaseId.isBlank()) {
            if (!com.nogeon.economyland.item.SmithingService.canEnhance(entry.stack())) {
                TraderShopState.get(player.server).recordDelivery(kind, traderDatabaseId, entry.stack().copyWithCount(toRemove), base);
            }
        }

        SyncCreditsPacket.send(player, profile.credits());
        state.setDirty();
        if (normalSell) {
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.sell.paid", paid), false);
        } else {
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.delivery.paid", paid), false);
        }
        
        // 실시간 인벤토리 갱신 패킷 강제 전송
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        SyncPlayerInventoryPacket.send(player);
    }

    public static JobType deliveryJob(TraderKind kind) {
        return switch (kind) {
            case CROP -> JobType.FARMER;
            case FISHER -> JobType.FISHER;
            case MINER -> JobType.MINER;
            case CHEF -> JobType.COOK;
            case HUNTER -> JobType.HUNTER;
            case SMITH -> JobType.MINER; // Miner job for smith deliveries
            case ENGINEER -> JobType.ENGINEER;
            default -> null;
        };
    }

    private static int deliveryExp(JobType job, int itemCount, long paid) {
        int perItem = switch (job) {
            case FISHER -> 6;
            case HUNTER -> 5;
            case COOK -> 4;
            case FARMER -> 2;
            case MINER -> 1;
            case ENGINEER -> 3;
        };
        int valueBonus = (int) Math.min(300L, Math.max(0L, paid / 5000L));
        return Math.min(1200, Math.max(5, itemCount * perItem + valueBonus));
    }
}
