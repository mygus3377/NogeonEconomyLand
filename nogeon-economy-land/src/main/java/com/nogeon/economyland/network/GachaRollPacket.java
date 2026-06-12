package com.nogeon.economyland.network;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.item.GunCatalog;
import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.menu.GachaCategory;
import com.nogeon.economyland.menu.GachaOpener;
import com.nogeon.economyland.menu.TraderActionLine;
import com.nogeon.economyland.menu.TraderActionOpener;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.GachaRewardResult;
import com.nogeon.economyland.state.TraderShopState;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.shop.ShopItemProtection;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

public final class GachaRollPacket {
    private final String actionId;
    private final String traderDatabaseId;
    private final String categoryId;
    private final int count;

    public GachaRollPacket(String actionId, String categoryId, int count) {
        this(actionId, "", categoryId, count);
    }

    public GachaRollPacket(String actionId, String traderDatabaseId, String categoryId, int count) {
        this.actionId = actionId;
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.categoryId = categoryId;
        this.count = count;
    }

    public static void encode(GachaRollPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.actionId);
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeUtf(packet.categoryId);
        buffer.writeVarInt(packet.count);
    }

    public static GachaRollPacket decode(FriendlyByteBuf buffer) {
        return new GachaRollPacket(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readVarInt());
    }

    public static void handle(GachaRollPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || packet.count < 1 || packet.count > 10) {
                return;
            }

            TraderActionLine line = findLine(packet.actionId);
            if (line == null) {
                return;
            }

            GachaCategory category = GachaCategory.byId(packet.categoryId);
            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            List<ShopEntry> customRewards = customRewards(state, player, packet.traderDatabaseId, packet.actionId, category.id());
            if ("gacha_legend".equals(packet.actionId) && rewardsOfRarity(customRewards, 3).isEmpty()) {
                player.displayClientMessage(Component.literal("이 카테고리에는 전설 등급 보상이 없습니다. 보상풀에서 등급 3 항목을 추가하세요."), false);
            }
            if (customRewards.isEmpty()) {
                player.displayClientMessage(Component.literal("이 가챠 등급의 보상풀이 비어 있습니다."), false);
                GachaOpener.open(player, packet.traderDatabaseId, packet.actionId, category.id(), line.price(), packet.count);
                return;
            }
            if (!payForRolls(player, profile, packet.actionId, line.price(), packet.count)) {
                GachaOpener.open(player, packet.traderDatabaseId, packet.actionId, category.id(), line.price(), packet.count);
                return;
            }

            List<GachaRewardResult> results = new ArrayList<>(packet.count);
            for (int index = 0; index < packet.count; index++) {
                GachaRewardResult result = rollCustom(player.getRandom(), packet.actionId, customRewards);
                results.add(result);
                give(player, state, result.stack());
            }

            GachaRewardResult featuredJackpot = featuredJackpot(results);
            UUID celebrationToken = featuredJackpot != null
                ? GachaCelebratePacket.queue(player, featuredJackpot.stack())
                : null;

            SyncCreditsPacket.send(player, profile.credits());
            state.setDirty();
            GachaOpener.open(player, packet.traderDatabaseId, packet.actionId, category.id(), line.price(), packet.count, List.copyOf(results), celebrationToken);
        });
        context.setPacketHandled(true);
    }

    private static List<ShopEntry> customRewards(EconomyState state, ServerPlayer player, String traderDatabaseId, String actionId, String categoryId) {
        return TraderShopState.get(player.server).gachaRewardEntries(state, categoryId);
    }

    private static GachaRewardResult rollCustom(RandomSource random, String actionId, List<ShopEntry> rewards) {
        int rarity = rollRarity(random, actionId);
        List<ShopEntry> candidates = rewardsOfRarity(rewards, rarity);
        
        // Fallback if no items for target rarity
        if (candidates.isEmpty()) {
            for (int r = rarity - 1; r >= 0; r--) {
                for (ShopEntry entry : rewards) {
                    if (entry.stack().getOrCreateTag().getInt("NoGeonGachaRarity") == r) {
                        candidates.add(entry);
                    }
                }
                if (!candidates.isEmpty()) break;
            }
        }
        if (candidates.isEmpty()) {
            candidates = rewards;
        }

        long totalWeight = 0L;
        for (ShopEntry entry : candidates) {
            totalWeight += Math.max(1L, entry.price());
        }
        
        long roll = totalWeight <= 0L ? 0L : nextLong(random, totalWeight);
        for (ShopEntry entry : candidates) {
            roll -= Math.max(1L, entry.price());
            if (roll < 0L) {
                ItemStack stack = entry.stack();
                int actualRarity = stack.getOrCreateTag().getInt("NoGeonGachaRarity");
                boolean jackpot = stack.getOrCreateTag().getBoolean("NoGeonGachaJackpot");
                return GachaRewardResult.of(withApotheosisSockets(stack, actualRarity), actualRarity, jackpot);
            }
        }
        
        ItemStack fallback = candidates.get(0).stack();
        int fallbackRarity = fallback.getOrCreateTag().getInt("NoGeonGachaRarity");
        return GachaRewardResult.of(withApotheosisSockets(fallback, fallbackRarity), fallbackRarity, fallback.getOrCreateTag().getBoolean("NoGeonGachaJackpot"));
    }

    private static List<ShopEntry> rewardsOfRarity(List<ShopEntry> rewards, int rarity) {
        List<ShopEntry> candidates = new ArrayList<>();
        for (ShopEntry entry : rewards) {
            if (rewardRarity(entry) == rarity) {
                candidates.add(entry);
            }
        }
        return candidates;
    }

    private static int rewardRarity(ShopEntry entry) {
        return Math.max(0, Math.min(3, entry.stack().getOrCreateTag().getInt("NoGeonGachaRarity")));
    }

    private static int rollRarity(RandomSource random, String actionId) {
        int roll = random.nextInt(1000); // Using 1000 for 0.1% precision
        return switch (actionId) {
            case "gacha_basic" -> roll < 250 ? 1 : 0; // 75:25
            case "gacha_middle" -> roll < 100 ? 2 : (roll < 400 ? 1 : 0); // 60:30:10
            case "gacha_high" -> roll < 30 ? 3 : (roll < 300 ? 2 : 1); // 70:27:3
            case "gacha_legend" -> roll < 100 ? 3 : 2; // 90:10 -> 90% Epic, 10% Legend
            default -> 0;
        };
    }

    private static long nextLong(RandomSource random, long bound) {
        return Math.floorMod(random.nextLong(), bound);
    }

    private static TraderActionLine findLine(String actionId) {
        for (TraderActionLine line : TraderActionOpener.lines(TraderKind.GACHA)) {
            if (line.actionId().equals(actionId)) {
                return line;
            }
        }
        return null;
    }

    private static GachaRewardResult featuredJackpot(List<GachaRewardResult> results) {
        GachaRewardResult featured = null;
        for (GachaRewardResult result : results) {
            if (!result.jackpot()) {
                continue;
            }
            if (featured == null || result.rarity() > featured.rarity()) {
                featured = result;
            }
        }
        return featured;
    }

    private static boolean payForRolls(ServerPlayer player, PlayerProfile profile, String actionId, long pricePerRoll, int count) {
        Item ticket = gachaTicket(actionId);
        int ticketsUsed = consumeItems(player, ticket, count);
        int remainingRolls = count - ticketsUsed;
        long creditCost = remainingRolls * pricePerRoll;
        if (creditCost > 0L && !profile.spendCredits(creditCost)) {
            refund(player, ticket, ticketsUsed);
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.shop.no_money"), false);
            return false;
        }
        if (ticketsUsed > 0) {
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.gacha.ticket_used_count", ticket.getDescription(), ticketsUsed), false);
        }
        return true;
    }

    private static int consumeItems(ServerPlayer player, Item item, int count) {
        int removed = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && removed < count; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item) || stack.isEmpty()) {
                continue;
            }
            int used = Math.min(count - removed, stack.getCount());
            stack.shrink(used);
            removed += used;
        }
        return removed;
    }

    private static Item gachaTicket(String actionId) {
        return switch (actionId) {
            case "gacha_middle" -> ModItems.MIDDLE_GACHA_TICKET.get();
            case "gacha_high" -> ModItems.HIGH_GACHA_TICKET.get();
            case "gacha_legend" -> ModItems.LEGEND_GACHA_TICKET.get();
            default -> ModItems.BASIC_GACHA_TICKET.get();
        };
    }

    private static GachaRewardResult roll(ServerPlayer player, String actionId, GachaCategory category) {
        RandomSource random = player.getRandom();
        int boost = switch (actionId) {
            case "gacha_middle" -> 12;
            case "gacha_high" -> 28;
            case "gacha_legend" -> 55;
            default -> 0;
        };
        int roll = random.nextInt(100) + boost;
        if (roll >= 120) {
            return pickReward(random, category, 3);
        }
        if (roll >= 85) {
            return pickReward(random, category, 2);
        }
        if (roll >= 55) {
            return pickReward(random, category, 1);
        }
        return pickReward(random, category, 0);
    }

    private static GachaRewardResult pickReward(RandomSource random, GachaCategory category, int band) {
        RewardOption[] options = switch (category) {
            case WEAPON -> weaponRewards(band);
            case ARMOR -> armorRewards(band);
            case ITEM -> itemRewards(band);
            case GUN_BOW -> gunRewards(band);
        };
        RewardOption selected = options[random.nextInt(options.length)];
        return GachaRewardResult.of(selected.item(), selected.count(), selected.rarity(), selected.jackpot());
    }

    private static RewardOption[] gunRewards(int band) {
        List<Item> dynamicItems = GunCatalog.gachaItems(band);
        if (!dynamicItems.isEmpty()) {
            RewardOption[] options = new RewardOption[dynamicItems.size()];
            for (int index = 0; index < dynamicItems.size(); index++) {
                options[index] = new RewardOption(dynamicItems.get(index), 1, band, band >= 2);
            }
            return options;
        }
        return switch (band) {
            case 3 -> new RewardOption[] {
                new RewardOption(Items.CROSSBOW, 1, 3, true),
                new RewardOption(Items.TRIDENT, 1, 3, true)
            };
            case 2 -> new RewardOption[] {
                new RewardOption(Items.CROSSBOW, 1, 2, true),
                new RewardOption(Items.BOW, 1, 2, true)
            };
            case 1 -> new RewardOption[] {
                new RewardOption(Items.CROSSBOW, 1, 1, false),
                new RewardOption(Items.ARROW, 16, 1, false)
            };
            default -> new RewardOption[] {
                new RewardOption(Items.ARROW, 24, 0, false),
                new RewardOption(Items.FIREWORK_ROCKET, 12, 0, false)
            };
        };
    }

    private static RewardOption[] weaponRewards(int band) {
        return mergeRewards(switch (band) {
            case 3 -> new RewardOption[] {
                new RewardOption(Items.NETHERITE_SWORD, 1, 3, true),
                new RewardOption(Items.NETHERITE_AXE, 1, 3, true),
                new RewardOption(Items.TRIDENT, 1, 3, true)
            };
            case 2 -> new RewardOption[] {
                new RewardOption(Items.DIAMOND_SWORD, 1, 2, true),
                new RewardOption(Items.DIAMOND_AXE, 1, 2, true),
                new RewardOption(Items.CROSSBOW, 1, 2, true)
            };
            case 1 -> new RewardOption[] {
                new RewardOption(Items.IRON_SWORD, 1, 1, false),
                new RewardOption(Items.BOW, 1, 1, false),
                new RewardOption(Items.IRON_AXE, 1, 1, false)
            };
            default -> new RewardOption[] {
                new RewardOption(Items.STONE_SWORD, 1, 0, false),
                new RewardOption(Items.STONE_AXE, 1, 0, false),
                new RewardOption(Items.BOW, 1, 0, false)
            };
        }, moddedRewards(GachaCategory.WEAPON, band));
    }

    private static RewardOption[] armorRewards(int band) {
        return mergeRewards(switch (band) {
            case 3 -> new RewardOption[] {
                new RewardOption(Items.NETHERITE_CHESTPLATE, 1, 3, true),
                new RewardOption(Items.NETHERITE_LEGGINGS, 1, 3, true),
                new RewardOption(Items.ELYTRA, 1, 3, true)
            };
            case 2 -> new RewardOption[] {
                new RewardOption(Items.DIAMOND_CHESTPLATE, 1, 2, true),
                new RewardOption(Items.DIAMOND_LEGGINGS, 1, 2, true),
                new RewardOption(Items.DIAMOND_HELMET, 1, 2, true)
            };
            case 1 -> new RewardOption[] {
                new RewardOption(Items.IRON_CHESTPLATE, 1, 1, false),
                new RewardOption(Items.IRON_LEGGINGS, 1, 1, false),
                new RewardOption(Items.IRON_HELMET, 1, 1, false)
            };
            default -> new RewardOption[] {
                new RewardOption(Items.CHAINMAIL_CHESTPLATE, 1, 0, false),
                new RewardOption(Items.CHAINMAIL_LEGGINGS, 1, 0, false),
                new RewardOption(Items.IRON_BOOTS, 1, 0, false)
            };
        }, moddedRewards(GachaCategory.ARMOR, band));
    }

    private static RewardOption[] itemRewards(int band) {
        return mergeRewards(switch (band) {
            case 3 -> new RewardOption[] {
                new RewardOption(Items.NETHER_STAR, 1, 3, true),
                new RewardOption(Items.TOTEM_OF_UNDYING, 2, 3, true),
                new RewardOption(Items.ENCHANTED_GOLDEN_APPLE, 2, 3, true)
            };
            case 2 -> new RewardOption[] {
                new RewardOption(Items.TOTEM_OF_UNDYING, 1, 2, true),
                new RewardOption(Items.DIAMOND, 8, 2, true),
                new RewardOption(Items.ENDER_PEARL, 12, 2, true)
            };
            case 1 -> new RewardOption[] {
                new RewardOption(Items.EMERALD, 8, 1, false),
                new RewardOption(Items.EXPERIENCE_BOTTLE, 12, 1, false),
                new RewardOption(Items.GOLD_INGOT, 16, 1, false)
            };
            default -> new RewardOption[] {
                new RewardOption(Items.IRON_INGOT, 16, 0, false),
                new RewardOption(Items.REDSTONE, 24, 0, false),
                new RewardOption(Items.LAPIS_LAZULI, 24, 0, false)
            };
        }, moddedRewards(GachaCategory.ITEM, band));
    }

    private static RewardOption[] mergeRewards(RewardOption[] base, List<RewardOption> extras) {
        if (extras.isEmpty()) {
            return base;
        }
        RewardOption[] merged = new RewardOption[base.length + extras.size()];
        System.arraycopy(base, 0, merged, 0, base.length);
        for (int index = 0; index < extras.size(); index++) {
            merged[base.length + index] = extras.get(index);
        }
        return merged;
    }

    private static List<RewardOption> moddedRewards(GachaCategory category, int band) {
        List<RewardOption> rewards = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!isEligibleModItem(item, category)) {
                continue;
            }
            int itemBand = rewardBand(item, category);
            if (itemBand != band) {
                continue;
            }
            rewards.add(new RewardOption(item, rewardCount(item, category, band), band, band >= 2));
        }
        return rewards;
    }

    private static boolean isEligibleModItem(Item item, GachaCategory category) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
        String namespace = id.getNamespace();
        if (namespace.equals(ResourceLocation.DEFAULT_NAMESPACE) || namespace.equals(NoGeonEconomyLand.MOD_ID)) {
            return false;
        }
        if (item == Items.AIR || item instanceof BlockItem || item instanceof SpawnEggItem) {
            return false;
        }
        return switch (category) {
            case WEAPON -> isWeapon(item);
            case ARMOR -> isArmor(item);
            case ITEM -> !isWeapon(item) && !isArmor(item);
            case GUN_BOW -> false;
        };
    }

    private static boolean isWeapon(Item item) {
        ItemStack stack = new ItemStack(item);
        return stack.is(ItemTags.SWORDS)
            || stack.is(ItemTags.AXES)
            || item instanceof BowItem
            || item instanceof CrossbowItem
            || item instanceof TridentItem;
    }

    private static boolean isArmor(Item item) {
        return item instanceof ArmorItem || item instanceof ElytraItem;
    }

    private static ItemStack withApotheosisSockets(ItemStack reward, int rarity) {
        ItemStack stack = reward.copy();
        if (!ModList.get().isLoaded("apotheosis") || !isGachaEquipment(stack.getItem())) {
            return stack;
        }
        CompoundTag affixData = stack.getOrCreateTagElement("affix_data");
        if (affixData.getInt("sockets") <= 0) {
            affixData.putInt("sockets", Math.max(1, Math.min(4, rarity + 1)));
        }
        return stack;
    }

    private static boolean isGachaEquipment(Item item) {
        return isWeapon(item) || isArmor(item);
    }

    private static int rewardBand(Item item, GachaCategory category) {
        return switch (category) {
            case WEAPON -> weaponBand(item);
            case ARMOR -> armorBand(item);
            case ITEM -> rarityBand(item);
            case GUN_BOW -> rarityBand(item);
        };
    }

    private static int weaponBand(Item item) {
        ItemStack stack = rewardStack(item, 1);
        int durability = stack.getMaxDamage();
        int rarityBand = rarityBand(item);
        if (rarityBand >= 3 || durability >= 1800 || item instanceof TridentItem) {
            return 3;
        }
        if (rarityBand >= 2 || durability >= 1000) {
            return 2;
        }
        if (rarityBand >= 1 || durability >= 350) {
            return 1;
        }
        return 0;
    }

    private static int armorBand(Item item) {
        if (item instanceof ElytraItem) {
            return 3;
        }
        int rarityBand = rarityBand(item);
        if (item instanceof ArmorItem armorItem) {
            int defense = armorItem.getDefense();
            if (rarityBand >= 3 || defense >= 8) {
                return 3;
            }
            if (rarityBand >= 2 || defense >= 6) {
                return 2;
            }
            if (rarityBand >= 1 || defense >= 3) {
                return 1;
            }
        }
        return rarityBand;
    }

    private static int rarityBand(Item item) {
        Rarity rarity = rewardStack(item, 1).getRarity();
        if (rarity == null) {
            return 0;
        }
        return switch (rarity.name()) {
            case "EPIC" -> 3;
            case "RARE" -> 2;
            case "UNCOMMON" -> 1;
            default -> 0;
        };
    }

    private static int rewardCount(Item item, GachaCategory category, int band) {
        if (category != GachaCategory.ITEM) {
            return 1;
        }
        ItemStack stack = rewardStack(item, 1);
        if (stack.isDamageableItem()) {
            return 1;
        }
        int suggested = switch (band) {
            case 3 -> 2;
            case 2 -> 4;
            case 1 -> 8;
            default -> 16;
        };
        return Math.max(1, Math.min(item.getMaxStackSize(), suggested));
    }

    private static Item rewardItem(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return Items.BARRIER;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? Items.BARRIER : item;
    }

    private static void give(ServerPlayer player, EconomyState state, ItemStack reward) {
        ItemStack stack = reward.copy();
        ShopItemProtection.markPurchased(stack);
        ItemStack remainder = ExtendedInventoryDelivery.giveRemainder(player, stack);
        if (!remainder.isEmpty()) {
            state.queueGachaReward(player.getUUID(), remainder);
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.gacha.stored_overflow"), false);
        }
    }

    private static void refund(ServerPlayer player, Item item, int count) {
        if (count <= 0) {
            return;
        }
        ItemStack stack = rewardStack(item, count);
        ExtendedInventoryDelivery.giveOrDrop(player, stack);
    }

    private static ItemStack rewardStack(Item item, int count) {
        ItemStack stack = item.getDefaultInstance();
        if (stack.isEmpty()) {
            stack = new ItemStack(item);
        } else {
            stack = stack.copy();
        }
        stack.setCount(Math.max(1, count));
        return stack;
    }

    private record RewardOption(Item item, int count, int rarity, boolean jackpot) {
    }
}
