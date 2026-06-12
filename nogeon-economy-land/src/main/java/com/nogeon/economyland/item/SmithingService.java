package com.nogeon.economyland.item;

import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Ingredient;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.shop.ShopItemProtection;

public final class SmithingService {
    public static final int MAX_LEVEL = 20;
    public static final int SHOP_PURCHASE_PRICE_MULTIPLIER = 4;

    private static final String LEVEL_TAG = "NoGeonEnhanceLevel";
    private static final String MODIFIER_PREFIX = "nogeon.smith.";
    private static final String LAST_SOURCED_LEVEL_TAG = "NoGeonEnhanceLastApplied";
    private static final java.util.Map<UUID, Long> LAST_ENHANCE_TIME = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long[] ENHANCEMENT_COSTS = {
        0L,
        800L, 1_200L, 2_000L, 3_200L, 5_000L,
        8_500L, 13_500L, 21_000L, 34_000L, 55_000L,
        90_000L, 150_000L, 250_000L, 420_000L, 680_000L,
        1_250_000L, 2_050_000L, 3_500_000L, 5_500_000L, 8_000_000L
    };
    public static final UUID WEAPON_DAMAGE_UUID = UUID.fromString("54e8e03d-6ef6-4f0d-91b5-83973218a31e");
    public static final UUID WEAPON_FLAT_DAMAGE_UUID = UUID.fromString("7cb8241d-3f89-4d42-96cf-4e8512a5ec2f");
    public static final UUID WEAPON_SPEED_UUID = UUID.fromString("91cbf8db-6548-4379-a66d-53340966f1b0");
    public static final UUID[] ARMOR_UUIDS = new UUID[] {
        UUID.fromString("697eaf9e-04b2-4d6e-bf4f-1ef4dc67ae73"),
        UUID.fromString("3dbe6d96-4df7-445a-a6d9-995702d002b4"),
        UUID.fromString("952ef3f1-2424-4160-b429-2dd0f8312ee9"),
        UUID.fromString("cfc58ce2-1788-45f2-b858-1b00f699a6ef")
    };
    public static final UUID[] TOUGHNESS_UUIDS = new UUID[] {
        UUID.fromString("44d361f5-76d5-4ec2-8731-a5d033862d27"),
        UUID.fromString("a49ac985-35e7-4348-8d57-334a82b226d2"),
        UUID.fromString("92292a5b-995c-4417-88ff-1aa7418e8d9e"),
        UUID.fromString("22e3c477-7403-43db-8648-b4617ee539a6")
    };
    public static final UUID[] MAX_HEALTH_UUIDS = new UUID[] {
        UUID.fromString("8742ac9e-04b2-4d6e-bf4f-1ef4dc67ae73"),
        UUID.fromString("2dbe6d96-4df7-445a-a6d9-995702d002b4"),
        UUID.fromString("852ef3f1-2424-4160-b429-2dd0f8312ee9"),
        UUID.fromString("dfc58ce2-1788-45f2-b858-1b00f699a6ef")
    };
    private static final List<ShopEntry> SHOP_ITEMS = List.of(
        new ShopEntry("smith_wooden_sword", Items.WOODEN_SWORD, 1, 180, 0),
        new ShopEntry("smith_stone_sword", Items.STONE_SWORD, 1, 420, 0),
        new ShopEntry("smith_iron_sword", Items.IRON_SWORD, 1, 1900, 0),
        new ShopEntry("smith_wooden_axe", Items.WOODEN_AXE, 1, 220, 0),
        new ShopEntry("smith_stone_axe", Items.STONE_AXE, 1, 520, 0),
        new ShopEntry("smith_iron_axe", Items.IRON_AXE, 1, 2100, 0),
        new ShopEntry("smith_bow", Items.BOW, 1, 1400, 0),
        new ShopEntry("smith_crossbow", Items.CROSSBOW, 1, 3400, 0),
        new ShopEntry("smith_shield", Items.SHIELD, 1, 2200, 0),
        new ShopEntry("smith_arrow_bundle", Items.ARROW, 16, 850, 0)
    );

    private SmithingService() {
    }

    public static boolean isCurio(ItemStack stack) {
        if (stack.isEmpty()) return false;
        
        // 1. 키워드 기반 자동 장신구 판정 (선글라스, 안경, 고글 등 장신구류 추가)
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        if (itemId.contains("ring")
            || itemId.contains("bracelet")
            || itemId.contains("necklace")
            || itemId.contains("amulet")
            || itemId.contains("belt")
            || itemId.contains("charm")
            || itemId.contains("pendant")
            || itemId.contains("circlet")
            || itemId.contains("glove")
            || itemId.contains("bauble")
            || itemId.contains("artifact")
            || itemId.contains("trinket")
            || itemId.contains("sunglasses")
            || itemId.contains("goggles")
            || itemId.contains("glasses")
            || itemId.contains("mask")
            || itemId.contains("eyepatch")
            || itemId.contains("crown")
            || itemId.contains("tiara")
            || itemId.contains("visage")
            || itemId.contains("monocle")
            || itemId.contains("earring")) {
            
            return true;
        }

        // 2. Curios API 기반 판정
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object helper = curiosApiClass.getMethod("getCuriosHelper").invoke(null);
            Object curioOpt = helper.getClass()
                .getMethod("getCurio", ItemStack.class)
                .invoke(helper, stack);
            if (curioOpt instanceof java.util.Optional<?> opt) {
                if (opt.isPresent()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean canEnhance(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return isArmor(item) || isWeapon(item, stack) || isTool(item) || isCurio(stack);
    }

    public static boolean canSmith(ItemStack stack) {
        return canEnhance(stack) || canRepair(stack) || canDeconstruct(stack);
    }

    public static boolean canRepair(ItemStack stack) {
        return !stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged() && (isArmor(stack.getItem()) || isWeapon(stack.getItem(), stack));
    }

    public static boolean canDeconstruct(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Only allow modded items or equipment for deconstruction to prevent generic item abuse
        return isArmor(stack.getItem()) || isWeapon(stack.getItem(), stack) || isTool(stack.getItem()) || isCurio(stack);
    }

    public static List<ShopEntry> shopItems() {
        return SHOP_ITEMS;
    }

    public static ShopEntry findShopItem(String entryId) {
        for (ShopEntry entry : SHOP_ITEMS) {
            if (entry.id().equals(entryId)) {
                return entry;
            }
        }
        return null;
    }

    public static long shopPrice(ShopEntry entry) {
        return entry.price() * SHOP_PURCHASE_PRICE_MULTIPLIER;
    }

    public static int defaultSelectedSlot(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (canSmith(stack)) {
                return slot;
            }
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (!player.getInventory().getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    public static int normalizeSelectedSlot(ServerPlayer player, int slot) {
        if (slot >= 0 && slot < player.getInventory().getContainerSize() && !player.getInventory().getItem(slot).isEmpty()) {
            return slot;
        }
        return defaultSelectedSlot(player);
    }

    public static ItemStack stackForSlot(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return player.getInventory().getItem(slot);
    }

    public static int level(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return Math.max(0, stack.getOrCreateTag().getInt(LEVEL_TAG));
    }

    public static int nextLevel(ItemStack stack) {
        return Math.min(MAX_LEVEL, level(stack) + 1);
    }

    public static long price(ItemStack stack) {
        int targetLevel = nextLevel(stack);
        long base = ENHANCEMENT_COSTS[Math.max(1, Math.min(MAX_LEVEL, targetLevel))];
        double itemScale = materialMultiplier(stack) * rarityMultiplier(stack.getRarity()) * statMultiplier(stack) * 0.35D;
        itemScale = Math.max(0.75D, Math.min(2.25D, itemScale));
        return Math.max(base, Math.round(base * itemScale));
    }

    public static long repairPrice(ItemStack stack) {
        if (!canRepair(stack)) {
            return 0L;
        }
        double damageRatio = (double) stack.getDamageValue() / Math.max(1, stack.getMaxDamage());
        double materialMultiplier = materialMultiplier(stack);
        double rarityMultiplier = rarityMultiplier(stack.getRarity());
        double modMultiplier = GunCatalog.isRegisteredGun(stack.getItem()) || isModdedItem(stack) ? 1.45D : 1.0D;
        double durabilityFactor = Math.max(180.0D, stack.getMaxDamage() * 1.4D);
        return Math.max(120L, Math.round(damageRatio * durabilityFactor * materialMultiplier * rarityMultiplier * modMultiplier));
    }

    public static int successPercent(ItemStack stack) {
        return (int) Math.round(successChance(nextLevel(stack)) * 100.0D);
    }

    public static int resetPercent(ItemStack stack) {
        return (int) Math.round(resetChance(nextLevel(stack)) * 100.0D);
    }

    public static boolean canDowngrade(ItemStack stack) {
        return nextLevel(stack) >= 7;
    }

    public static Component defaultStatus(ItemStack stack) {
        if (stack.isEmpty()) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_empty").withStyle(ChatFormatting.GRAY);
        }
        if (!canSmith(stack)) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_invalid").withStyle(ChatFormatting.RED);
        }
        if (canRepair(stack) && (!canEnhance(stack) || level(stack) >= MAX_LEVEL)) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_repair_ready").withStyle(ChatFormatting.YELLOW);
        }
        if (level(stack) >= MAX_LEVEL) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_max").withStyle(ChatFormatting.GOLD);
        }
        return Component.translatable("gui.nogeon_economy_land.smith_status_ready").withStyle(ChatFormatting.AQUA);
    }

    public static Component tryEnhance(ServerPlayer player, PlayerProfile profile, int slot) {
        return tryEnhance(player, profile, slot, 0);
    }

    public static Component tryEnhance(ServerPlayer player, PlayerProfile profile, int slot, int gemTier) {
        long now = System.currentTimeMillis();
        long lastTime = LAST_ENHANCE_TIME.getOrDefault(player.getUUID(), 0L);
        if (now - lastTime < 300L) {
            return Component.literal("§c강화 요청이 너무 빠릅니다. 잠시 후 시도해주세요.");
        }
        LAST_ENHANCE_TIME.put(player.getUUID(), now);

        ItemStack stack = stackForSlot(player, slot);
        if (stack.isEmpty()) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_empty").withStyle(ChatFormatting.GRAY);
        }
        if (stack.getCount() > 1) {
            return Component.literal("§c강화/수리/분해할 장비는 1개씩만 올려둘 수 있습니다.");
        }
        if (!canEnhance(stack)) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_invalid").withStyle(ChatFormatting.RED);
        }
        int currentLevel = level(stack);
        if (currentLevel >= MAX_LEVEL) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_max").withStyle(ChatFormatting.GOLD);
        }

        int targetLevel = currentLevel + 1;
        RandomSource random = player.getRandom();
        if (gemTier > 0 && countEnhancementGem(player, gemTier) <= 0) {
            return Component.literal("\u00a7c\uc120\ud0dd\ud55c \uac15\ud654\uc758 \ubcf4\uc11d\uc774 \uc778\ubca4\ud1a0\ub9ac\uc5d0 \uc5c6\uc2b5\ub2c8\ub2e4.");
        }
        long price = price(stack);
        if (!profile.spendCredits(price)) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_no_money", price).withStyle(ChatFormatting.RED);
        }

        profile.incrementEnhanceAttempts();
        profile.addEnhanceSpent(price);

        int consumedGemTier = consumeEnhancementGem(player, gemTier);
        boolean perfectGem = consumedGemTier >= 6;
        double gemBonus = enhancementGemEffectiveBonus(consumedGemTier, targetLevel) / 100.0D;
        if (perfectGem || random.nextDouble() < Math.min(1.0D, successChance(targetLevel) + gemBonus)) {
            applyEnhancement(stack, targetLevel);
            profile.trackHighestEnhanceLevel(targetLevel);
            playSuccessEffects(player, targetLevel, stack);
            return Component.translatable("gui.nogeon_economy_land.smith_status_success", targetLevel).withStyle(ChatFormatting.GREEN);
        }

        if (random.nextDouble() < resetChance(targetLevel)) {
            if (profile.consumeEnhancementResetProtectionCharge()) {
                EconomyState.get(player.server).setDirty();
                player.displayClientMessage(Component.literal("§d[초기화 방지] 초기화 방지권을 소모하여 장비 초기화를 면제했습니다!"), true);
            } else {
                applyEnhancement(stack, 0);
                profile.incrementEnhanceFails();
                playFailureEffects(player, true);
                return Component.translatable("gui.nogeon_economy_land.smith_status_reset").withStyle(ChatFormatting.DARK_RED);
            }
        }

        if (targetLevel >= 7 && currentLevel > 0) {
            if (consumeDowngradeProtectionScroll(player, targetLevel)) {
                playFailureEffects(player, false);
                return Component.translatable("gui.nogeon_economy_land.smith_status_guarded").withStyle(ChatFormatting.AQUA);
            }
            int downgradedLevel = Math.max(0, currentLevel - downgradeAmount(targetLevel, random));
            applyEnhancement(stack, downgradedLevel);
            profile.incrementEnhanceFails();
            playFailureEffects(player, false);
            return Component.translatable("gui.nogeon_economy_land.smith_status_fail_drop", downgradedLevel).withStyle(ChatFormatting.YELLOW);
        }

        profile.incrementEnhanceFails();
        playFailureEffects(player, false);
        return Component.translatable("gui.nogeon_economy_land.smith_status_fail_hold").withStyle(ChatFormatting.RED);
    }

    public static int enhancementGemBonus(int tier) {
        return switch (tier) {
            case 1 -> 5;
            case 2 -> 10;
            case 3 -> 15;
            case 4 -> 25;
            case 5 -> 40;
            case 6 -> 100;
            default -> 0;
        };
    }

    public static int enhancementGemEffectiveBonus(int tier, int targetLevel) {
        if (tier <= 0) {
            return 0;
        }
        if (tier >= 6) {
            return 100;
        }
        return Math.max(1, (int) Math.round(enhancementGemBonus(tier) * enhancementGemEfficiencyPercent(targetLevel) / 100.0D));
    }

    public static int enhancementGemEfficiencyPercent(int targetLevel) {
        if (targetLevel <= 5) {
            return 100;
        }
        if (targetLevel <= 10) {
            return 80;
        }
        if (targetLevel <= 15) {
            return 60;
        }
        if (targetLevel <= 17) {
            return 40;
        }
        return 25;
    }

    public static ItemStack enhancementGemStack(int tier) {
        return switch (tier) {
            case 1 -> new ItemStack(ModItems.CRACKED_ENHANCEMENT_GEM.get());
            case 2 -> new ItemStack(ModItems.SPLIT_ENHANCEMENT_GEM.get());
            case 3 -> new ItemStack(ModItems.FLAWED_ENHANCEMENT_GEM.get());
            case 4 -> new ItemStack(ModItems.ENHANCEMENT_GEM.get());
            case 5 -> new ItemStack(ModItems.FLAWLESS_ENHANCEMENT_GEM.get());
            case 6 -> new ItemStack(ModItems.PERFECT_ENHANCEMENT_GEM.get());
            default -> ItemStack.EMPTY;
        };
    }

    public static int countEnhancementGem(net.minecraft.world.entity.player.Player player, int tier) {
        ItemStack target = enhancementGemStack(tier);
        if (target.isEmpty()) return 0;
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItem(stack, target)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int consumeEnhancementGem(ServerPlayer player, int tier) {
        ItemStack target = enhancementGemStack(tier);
        if (target.isEmpty()) return 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItem(stack, target)) {
                stack.shrink(1);
                return tier;
            }
        }
        return 0;
    }

    public static Component tryRepair(ServerPlayer player, PlayerProfile profile, int slot) {
        ItemStack stack = stackForSlot(player, slot);
        if (stack.isEmpty()) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_empty").withStyle(ChatFormatting.GRAY);
        }
        if (stack.getCount() > 1) {
            return Component.literal("§c강화/수리/분해할 장비는 1개씩만 올려둘 수 있습니다.");
        }
        if (!canSmith(stack)) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_invalid").withStyle(ChatFormatting.RED);
        }
        if (!canRepair(stack)) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_repair_full").withStyle(ChatFormatting.RED);
        }

        long repairPrice = repairPrice(stack);
        if (!profile.spendCredits(repairPrice)) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_no_money", repairPrice).withStyle(ChatFormatting.RED);
        }

        stack.setDamageValue(0);
        playRepairEffects(player);
        return Component.translatable("gui.nogeon_economy_land.smith_status_repaired", repairPrice).withStyle(ChatFormatting.GREEN);
    }

    public static Component tryDeconstruct(ServerPlayer player, PlayerProfile profile, int slot) {
        ItemStack stack = stackForSlot(player, slot);
        if (stack.isEmpty()) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_empty").withStyle(ChatFormatting.GRAY);
        }
        if (stack.getCount() > 1) {
            return Component.literal("§c강화/수리/분해할 장비는 1개씩만 올려둘 수 있습니다.");
        }
        if (!canDeconstruct(stack)) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_deconstruct_impossible").withStyle(ChatFormatting.RED);
        }

        Recipe<?> recipe = player.serverLevel().getRecipeManager().getRecipes().stream()
            .filter(r -> r.getType() == RecipeType.CRAFTING && r.getResultItem(player.serverLevel().registryAccess()).is(stack.getItem()))
            .findFirst().orElse(null);

        if (recipe == null) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_deconstruct_impossible").withStyle(ChatFormatting.RED);
        }

        RandomSource random = player.getRandom();
        double roll = random.nextDouble();
        boolean greatSuccess = roll < 0.10D; // 10% chance
        boolean success = roll < 0.80D; // 70% chance for normal success (0.1 ~ 0.8)

        if (!success && !greatSuccess) {
            stack.shrink(1);
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 0.8F);
            return Component.translatable("gui.nogeon_economy_land.smith_deconstruct_fail").withStyle(ChatFormatting.RED);
        }

        float recoveryRate = greatSuccess ? 1.0F : 0.6F;
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] possibleItems = ingredient.getItems();
            if (possibleItems.length > 0) {
                ItemStack ingredientStack = possibleItems[0].copy();
                int count = Math.max(1, Math.round(ingredientStack.getCount() * recoveryRate));
                if (random.nextFloat() > recoveryRate && !greatSuccess) {
                   // Add a bit of randomness to count if not great success
                   count = Math.max(0, count - 1);
                }
                if (count > 0) {
                    ingredientStack.setCount(count);
                    ExtendedInventoryDelivery.giveOrDrop(player, ingredientStack);
                }
            }
        }

        stack.shrink(1);
        if (greatSuccess) {
            playSuccessEffects(player, 10, stack);
            return Component.translatable("gui.nogeon_economy_land.smith_deconstruct_great_success").withStyle(ChatFormatting.GOLD);
        } else {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
            return Component.translatable("gui.nogeon_economy_land.smith_deconstruct_success").withStyle(ChatFormatting.GREEN);
        }
    }

    public static Component tryBuy(ServerPlayer player, PlayerProfile profile, String entryId) {
        ShopEntry entry = findShopItem(entryId);
        if (entry == null) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_invalid").withStyle(ChatFormatting.RED);
        }
        long price = shopPrice(entry);
        if (!profile.spendCredits(price)) {
            return Component.translatable("gui.nogeon_economy_land.smith_status_no_money", price).withStyle(ChatFormatting.RED);
        }
        ItemStack stack = entry.stack();
        ShopItemProtection.markPurchased(stack);
        ExtendedInventoryDelivery.giveOrDrop(player, stack);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.9F, 1.15F);
        return Component.translatable("gui.nogeon_economy_land.smith_status_bought", stack.getHoverName(), price).withStyle(ChatFormatting.GOLD);
    }

    public static Component tryUseScroll(ServerPlayer player, int targetLevel) {
        return tryUseScroll(player, targetLevel, defaultSelectedSlot(player));
    }

    public static Component tryUseScroll(ServerPlayer player, int targetLevel, int selectedSlot) {
        int slot = normalizeSelectedSlot(player, selectedSlot);
        ItemStack stack = stackForSlot(player, slot);
        if (stack.isEmpty() || !canEnhance(stack)) {
            return Component.literal("강화할 수 있는 장비가 없습니다.").withStyle(ChatFormatting.RED);
        }
        int currentLevel = level(stack);
        if (currentLevel >= targetLevel) {
            return Component.literal("이미 +" + targetLevel + " 이상인 장비입니다.").withStyle(ChatFormatting.YELLOW);
        }
        if (!consumeItem(player, scrollItem(targetLevel))) {
            return Component.literal("+" + targetLevel + " 강화권이 없습니다.").withStyle(ChatFormatting.RED);
        }
        applyEnhancement(stack, targetLevel);
        playSuccessEffects(player, targetLevel, stack);
        return Component.literal("강화권 사용 완료: ").append(stack.getHoverName()).append(Component.literal(" +" + targetLevel)).withStyle(ChatFormatting.GREEN);
    }

    public static Component tryUseBoostScroll(ServerPlayer player) {
        return tryUseScroll(player, 7);
    }

    public static boolean isArmor(Item item) {
        return item instanceof ArmorItem || item instanceof ElytraItem;
    }

    public static boolean isArmor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isCurio(stack)) return false;
        return isArmor(stack.getItem());
    }

    public static boolean isMagicEquipment(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isCurio(stack)) return false; // 장신구는 마법 장비로 취급하지 않음
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
        String namespace = stack.getItem().builtInRegistryHolder().key().location().getNamespace().toLowerCase(Locale.ROOT);
        
        if ("irons_spellbooks".equals(namespace) || "traveloptics".equals(namespace) || "iceandfire".equals(namespace)) {
            return itemId.contains("spellbook")
                || itemId.contains("spell_book")
                || itemId.contains("wand")
                || itemId.contains("staff")
                || itemId.contains("stave")
                || itemId.contains("scroll")
                || itemId.contains("tome")
                || itemId.contains("magic");
        }
        return false;
    }

    public static boolean isWeapon(Item item, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isCurio(stack)) return false; // 장신구는 무기로 취급하지 않음
        return stack.is(ItemTags.SWORDS)
            || stack.is(ItemTags.AXES)
            || item instanceof BowItem
            || item instanceof CrossbowItem
            || item instanceof TridentItem
            || item instanceof ShieldItem
            || hasMainHandAttackDamage(stack)
            || GunCatalog.isRegisteredGun(item)
            || isMagicEquipment(stack);
    }

    public static boolean isTool(Item item) {
        return item instanceof PickaxeItem
            || item instanceof ShovelItem
            || item instanceof AxeItem
            || item instanceof HoeItem
            || item instanceof net.minecraft.world.item.FishingRodItem;
    }

    private static double successChance(int targetLevel) {
        return switch (targetLevel) {
            case 1 -> 0.95D;
            case 2 -> 0.90D;
            case 3 -> 0.85D;
            case 4 -> 0.80D;
            case 5 -> 0.75D;
            case 6 -> 0.68D;
            case 7 -> 0.62D;
            case 8 -> 0.56D;
            case 9 -> 0.50D;
            case 10 -> 0.44D;
            case 11 -> 0.36D;
            case 12 -> 0.30D;
            case 13 -> 0.24D;
            case 14 -> 0.18D;
            case 15 -> 0.13D;
            case 16 -> 0.10D;
            case 17 -> 0.065D;
            case 18 -> 0.04D;
            case 19 -> 0.022D;
            default -> 0.012D;
        };
    }

    private static double resetChance(int targetLevel) {
        return switch (targetLevel) {
            case 16 -> 0.005D;
            case 17 -> 0.01D;
            case 18 -> 0.02D;
            case 19 -> 0.03D;
            case 20 -> 0.04D;
            default -> 0.0D;
        };
    }

    private static void applyEnhancement(ItemStack stack, int level) {
        CompoundTag tag = stack.getOrCreateTag();
        if (level <= 0) {
            tag.remove(LEVEL_TAG);
            tag.remove(LAST_SOURCED_LEVEL_TAG);
        } else {
            tag.putInt(LEVEL_TAG, level);
            tag.putInt(LAST_SOURCED_LEVEL_TAG, level);
        }
        removeSmithModifiers(tag);
    }

    private static void removeSmithModifiers(CompoundTag tag) {
        if (!tag.contains("AttributeModifiers", Tag.TAG_LIST)) {
            return;
        }
        ListTag modifiers = tag.getList("AttributeModifiers", Tag.TAG_COMPOUND);
        ListTag filtered = new ListTag();
        for (int index = 0; index < modifiers.size(); index++) {
            CompoundTag modifier = modifiers.getCompound(index);
            if (!modifier.getString("Name").startsWith(MODIFIER_PREFIX)) {
                filtered.add(modifier.copy());
            }
        }
        if (filtered.isEmpty()) {
            tag.remove("AttributeModifiers");
            return;
        }
        tag.put("AttributeModifiers", filtered);
    }

    public static float damageMultiplier(ItemStack stack) {
        int level = level(stack);
        if (level <= 0) {
            return 1.0F;
        }
        return (float) (1.0D + levelCurve(level, 0.055D, 0.075D, 0.095D, 0.125D));
    }

    public static double weaponDamageBonus(int level) {
        return levelCurve(level, 0.07D, 0.10D, 0.13D, 0.18D);
    }

    public static double weaponFlatDamageBonus(int level) {
        return levelCurve(level, 0.25D, 0.40D, 0.60D, 0.85D);
    }

    public static double weaponSpeedBonus(int level) {
        return levelCurve(level, 0.02D, 0.03D, 0.04D, 0.055D);
    }

    public static double armorBonus(int level) {
        return levelCurve(level, 0.028D, 0.035D, 0.042D, 0.060D);
    }

    public static double toughnessBonus(int level) {
        return levelCurve(level, 0.025D, 0.030D, 0.035D, 0.050D);
    }

    public static double maxHealthBonus(int level) {
        return levelCurve(level, 0.016D, 0.020D, 0.024D, 0.035D);
    }

    private static double levelCurve(int level, double early, double mid, double high, double endgame) {
        int clamped = Math.max(0, level);
        double value = Math.min(clamped, 5) * early;
        if (clamped > 5) {
            value += Math.min(clamped - 5, 5) * mid;
        }
        if (clamped > 10) {
            value += Math.min(clamped - 10, 5) * high;
        }
        if (clamped > 15) {
            value += (clamped - 15) * endgame;
        }
        return value;
    }

    public static Component displayName(ItemStack stack) {
        int level = level(stack);
        if (level <= 0) {
            return stack.getHoverName();
        }
        return Component.literal("+" + level + " ").withStyle(ChatFormatting.GOLD).append(stack.getHoverName().copy().withStyle(ChatFormatting.RESET));
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS)
            || stack.is(ItemTags.AXES)
            || stack.getItem() instanceof TridentItem
            || hasMainHandAttackDamage(stack)
            || hasMeleeWeaponName(stack);
    }

    private static boolean hasMainHandAttackDamage(ItemStack stack) {
        return stack.getItem().getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE);
    }

    private static boolean hasMeleeWeaponName(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
        return id.contains("sword")
            || id.contains("axe")
            || id.contains("spear")
            || id.contains("lance")
            || id.contains("glaive")
            || id.contains("halberd")
            || id.contains("scythe")
            || id.contains("katana")
            || id.contains("dagger")
            || id.contains("claymore")
            || id.contains("greatsword")
            || id.contains("longsword")
            || id.contains("twinblade")
            || id.contains("rapier")
            || id.contains("cutlass")
            || id.contains("greathammer")
            || id.contains("greataxe")
            || id.contains("machete")
            || id.contains("knife");
    }

    private static boolean isModdedItem(ItemStack stack) {
        return !"minecraft".equals(stack.getItem().builtInRegistryHolder().key().location().getNamespace());
    }

    private static double rarityMultiplier(Rarity rarity) {
        if (rarity == null) {
            return 1.0D;
        }
        return switch (rarity.name()) {
            case "EPIC" -> 1.8D;
            case "RARE" -> 1.45D;
            case "UNCOMMON" -> 1.2D;
            default -> 1.0D;
        };
    }

    private static double materialMultiplier(ItemStack stack) {
        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        if (itemId.contains("netherite")) {
            return 2.4D;
        }
        if (itemId.contains("diamond")) {
            return 1.9D;
        }
        if (itemId.contains("iron") || stack.getItem() instanceof ShieldItem || stack.getItem() instanceof CrossbowItem) {
            return 1.45D;
        }
        if (itemId.contains("gold")) {
            return 1.15D;
        }
        if (itemId.contains("stone") || stack.getItem() instanceof BowItem) {
            return 1.0D;
        }
        if (stack.getItem() instanceof TridentItem || stack.getItem() instanceof ElytraItem) {
            return 2.0D;
        }
        return 0.85D;
    }

    private static double statMultiplier(ItemStack stack) {
        double durability = stack.isDamageableItem() ? Math.min(1.1D, stack.getMaxDamage() / 1200.0D) : 0.0D;
        double attack = 0.0D;
        for (AttributeModifier modifier : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)) {
            attack += Math.max(0.0D, modifier.getAmount());
        }
        return 1.0D + durability + Math.min(0.9D, attack / 12.0D);
    }

    private static boolean consumeProtectionScroll(ServerPlayer player) {
        EconomyState state = EconomyState.get(player.server);
        boolean consumed = state.profile(player.getUUID()).consumeEnhancementGuardCharge();
        if (consumed) {
            state.setDirty();
        }
        return consumed;
    }

    private static boolean consumeDowngradeProtectionScroll(ServerPlayer player, int targetLevel) {
        EconomyState state = EconomyState.get(player.server);
        if (state.profile(player.getUUID()).consumeEnhancementDowngradeCharge(targetLevel)) {
            state.setDirty();
            return true;
        }
        return false;
    }

    private static int downgradeAmount(int targetLevel, RandomSource random) {
        if (targetLevel <= 10) {
            return 1;
        }
        if (targetLevel <= 15) {
            return random.nextDouble() < 0.12D ? 2 : 1;
        }
        if (targetLevel <= 17) {
            return random.nextDouble() < 0.35D ? 2 : 1;
        }
        return random.nextDouble() < 0.35D ? 3 : 2;
    }

    private static boolean consumeItem(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    public static Item scrollItem(int targetLevel) {
        return switch (targetLevel) {
            case 1 -> ModItems.ENHANCEMENT_SCROLL_1.get();
            case 2 -> ModItems.ENHANCEMENT_SCROLL_2.get();
            case 3 -> ModItems.ENHANCEMENT_SCROLL_3.get();
            case 4 -> ModItems.ENHANCEMENT_SCROLL_4.get();
            case 5 -> ModItems.ENHANCEMENT_SCROLL_5.get();
            case 6 -> ModItems.ENHANCEMENT_SCROLL_6.get();
            case 7 -> ModItems.ENHANCEMENT_SCROLL_7.get();
            case 8 -> ModItems.ENHANCEMENT_SCROLL_8.get();
            case 9 -> ModItems.ENHANCEMENT_SCROLL_9.get();
            case 10 -> ModItems.ENHANCEMENT_SCROLL_10.get();
            case 11 -> ModItems.ENHANCEMENT_SCROLL_11.get();
            case 12 -> ModItems.ENHANCEMENT_SCROLL_12.get();
            case 13 -> ModItems.ENHANCEMENT_SCROLL_13.get();
            case 14 -> ModItems.ENHANCEMENT_SCROLL_14.get();
            default -> ModItems.ENHANCEMENT_SCROLL_15.get();
        };
    }

    private static void playSuccessEffects(ServerPlayer player, int targetLevel, ItemStack stack) {
        boolean highTier = targetLevel >= 10;
        
        if (targetLevel < 15) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, highTier ? 1.2F : 1.05F);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, highTier ? 0.8F : 0.55F, highTier ? 1.2F : 1.45F);
            player.serverLevel().sendParticles(highTier ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0D, player.getZ(), highTier ? 28 : 18, 0.35D, 0.45D, 0.35D, 0.03D);
            player.serverLevel().sendParticles(highTier ? ParticleTypes.END_ROD : ParticleTypes.WAX_ON,
                player.getX(), player.getY() + 1.15D, player.getZ(), highTier ? 18 : 10, 0.28D, 0.3D, 0.28D, 0.01D);
            return;
        }

        String playerName = player.getScoreboardName();
        String itemName = stack.isEmpty() ? "장비" : stack.getHoverName().getString();
        
        String chatMsgPrefix = "§e§l[★강화 대성공★] ";
        String chatMsgBody = "";
        
        String titleStr = "";
        String subtitleStr = "§f강화자: §7" + playerName + " §f| §7" + itemName;
        
        int totemCount = 50;
        int dragonBreathCount = 0;
        int portalCount = 0;
        int explosionCount = 0;
        int sonicBoomCount = 0;
        
        java.util.List<SoundEventPlay> soundsToPlay = new java.util.ArrayList<>();
        
        if (targetLevel == 15) {
            chatMsgBody = "§a" + playerName + "§f님이 §d" + itemName + "§f +15강 강화에 성공했습니다!";
            titleStr = "§e+15 강화 성공";
            subtitleStr = "§6[전설] 등급의 장비 탄생";
            totemCount = 60;
            soundsToPlay.add(new SoundEventPlay(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2F, 1.0F));
            soundsToPlay.add(new SoundEventPlay(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.1F));
        } else if (targetLevel == 16) {
            chatMsgBody = "§b" + playerName + "§f님이 §b" + itemName + "§f +16강 강화에 성공했습니다!";
            titleStr = "§b+16 강화 성공";
            subtitleStr = "§f[신화] 등급의 장비 탄생";
            totemCount = 90;
            dragonBreathCount = 40;
            soundsToPlay.add(new SoundEventPlay(SoundEvents.ENDER_DRAGON_GROWL, 1.0F, 1.4F));
            soundsToPlay.add(new SoundEventPlay(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.8F, 1.5F));
        } else if (targetLevel == 17) {
            chatMsgBody = "§d" + playerName + "§f님이 §d" + itemName + "§f +17강 강화에 성공했습니다!";
            titleStr = "§d+17 강화 성공";
            subtitleStr = "§f한계를 넘어선 절대적인 장비";
            totemCount = 130;
            dragonBreathCount = 60;
            portalCount = 40;
            soundsToPlay.add(new SoundEventPlay(SoundEvents.ENDER_DRAGON_GROWL, 1.0F, 1.1F));
            soundsToPlay.add(new SoundEventPlay(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F, 1.3F));
        } else if (targetLevel == 18) {
            chatMsgBody = "§e" + playerName + "§f님이 §6" + itemName + "§f +18강 강화에 성공했습니다!";
            titleStr = "§e+18 강화 성공";
            subtitleStr = "§f불멸의 힘이 깃든 장비";
            totemCount = 180;
            dragonBreathCount = 80;
            portalCount = 70;
            explosionCount = 4;
            soundsToPlay.add(new SoundEventPlay(SoundEvents.ENDER_DRAGON_GROWL, 1.2F, 0.9F));
            soundsToPlay.add(new SoundEventPlay(SoundEvents.WITHER_SPAWN, 0.8F, 1.3F));
            soundsToPlay.add(new SoundEventPlay(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.0F, 1.1F));
        } else if (targetLevel == 19) {
            chatMsgBody = "§6" + playerName + "§f님이 §c" + itemName + "§f +19강 강화에 성공했습니다!";
            titleStr = "§6+19 강화 성공";
            subtitleStr = "§f파멸적인 위력의 장비";
            totemCount = 240;
            dragonBreathCount = 110;
            portalCount = 110;
            explosionCount = 8;
            soundsToPlay.add(new SoundEventPlay(SoundEvents.ENDER_DRAGON_DEATH, 1.0F, 1.2F));
            soundsToPlay.add(new SoundEventPlay(SoundEvents.WITHER_SPAWN, 1.0F, 1.0F));
            soundsToPlay.add(new SoundEventPlay(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2F, 0.9F));
        } else {
            chatMsgPrefix = "§6§l[★최대 강화 성공★] ";
            chatMsgBody = "§b§l" + playerName + "§f§l님이 장비 강화의 최종 정점인 §e§l+20강§f§l 달성에 성공했습니다!";
            titleStr = "§6⚡ +20 강화 성공 [최대] ⚡";
            subtitleStr = "§e§l장비 강화의 최종 경지에 도달했습니다.";
            totemCount = 350;
            dragonBreathCount = 160;
            portalCount = 160;
            explosionCount = 15;
            sonicBoomCount = 8;
            soundsToPlay.add(new SoundEventPlay(SoundEvents.ENDER_DRAGON_DEATH, 1.2F, 0.75F));
            soundsToPlay.add(new SoundEventPlay(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.5F, 0.7F));
            soundsToPlay.add(new SoundEventPlay(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.5F, 0.8F));
            soundsToPlay.add(new SoundEventPlay(SoundEvents.WITHER_DEATH, 1.0F, 0.9F));
        }

        Component broadcastMessage = Component.literal(chatMsgPrefix + chatMsgBody);
        player.server.getPlayerList().broadcastSystemMessage(broadcastMessage, false);

        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
            for (SoundEventPlay soundPlay : soundsToPlay) {
                p.playNotifySound(soundPlay.sound, SoundSource.RECORDS, soundPlay.volume, soundPlay.pitch);
            }
        }

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        
        if (totemCount > 0) {
            player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, px, py + 1.0D, pz, totemCount, 0.5D, 0.7D, 0.5D, 0.05D);
        }
        if (dragonBreathCount > 0) {
            player.serverLevel().sendParticles(ParticleTypes.DRAGON_BREATH, px, py + 1.0D, pz, dragonBreathCount, 0.4D, 0.6D, 0.4D, 0.02D);
        }
        if (portalCount > 0) {
            player.serverLevel().sendParticles(ParticleTypes.PORTAL, px, py + 1.0D, pz, portalCount, 0.5D, 0.7D, 0.5D, 0.08D);
        }
        if (explosionCount > 0) {
            player.serverLevel().sendParticles(ParticleTypes.EXPLOSION_EMITTER, px, py + 1.2D, pz, explosionCount, 0.8D, 0.8D, 0.8D, 0.0D);
        }
        if (sonicBoomCount > 0) {
            player.serverLevel().sendParticles(ParticleTypes.SONIC_BOOM, px, py + 1.2D, pz, sonicBoomCount, 0.6D, 0.6D, 0.6D, 0.0D);
        }
        
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, px, py + 1.15D, pz, 30, 0.35D, 0.4D, 0.35D, 0.02D);
        player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py + 1.0D, pz, 40, 0.5D, 0.5D, 0.5D, 0.1D);
    }

    private static class SoundEventPlay {
        final net.minecraft.sounds.SoundEvent sound;
        final float volume;
        final float pitch;

        SoundEventPlay(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    private static void playRepairEffects(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.9F, 1.1F);
        player.serverLevel().sendParticles(ParticleTypes.WAX_ON,
            player.getX(), player.getY() + 1.0D, player.getZ(), 12, 0.3D, 0.25D, 0.3D, 0.02D);
    }

    private static void playFailureEffects(ServerPlayer player, boolean catastrophic) {
        player.level().playSound(null, player.blockPosition(), catastrophic ? SoundEvents.GENERIC_EXPLODE : SoundEvents.ANVIL_BREAK,
            SoundSource.PLAYERS, catastrophic ? 0.7F : 0.65F, catastrophic ? 1.15F : 0.9F);
        player.serverLevel().sendParticles(catastrophic ? ParticleTypes.SMOKE : ParticleTypes.CRIT,
            player.getX(), player.getY() + 1.0D, player.getZ(), catastrophic ? 18 : 10, 0.25D, 0.35D, 0.25D, 0.02D);
    }
}
