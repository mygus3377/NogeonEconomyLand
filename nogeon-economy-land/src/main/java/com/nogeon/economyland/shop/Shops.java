package com.nogeon.economyland.shop;

import java.util.ArrayList;
import java.util.List;
import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.item.GunCatalog;
import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.state.EconomyState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

public final class Shops {
    public static final List<ShopEntry> GENERAL = List.of(
        new ShopEntry("bread", Items.BREAD, 1, 25, 64),
        new ShopEntry("torch", Items.TORCH, 1, 5, 128),
        new ShopEntry("white_bed", Items.WHITE_BED, 1, 1800, 16),
        new ShopEntry("baked_potato", Items.BAKED_POTATO, 1, 20, 64),
        new ShopEntry("steak", Items.COOKED_BEEF, 1, 80, 48),
        new ShopEntry("bucket", Items.BUCKET, 1, 1800, 32),
        new ShopEntry("water_bucket", Items.WATER_BUCKET, 1, 3200, 24),
        new ShopEntry("arrow", Items.ARROW, 1, 5, 128),
        new ShopEntry("shield", Items.SHIELD, 1, 2500, 16),
        new ShopEntry("ladder", Items.LADDER, 1, 10, 96),
        new ShopEntry("inventory_keep_scroll", ModItems.INVENTORY_KEEP_SCROLL.get(), 1, 30000, 16),
        new ShopEntry("shady_wizard_spawner", ModItems.SHADY_WIZARD_SPAWNER.get(), 1, 150000, 4)
    );
    private static final List<ShopEntry> CROP = List.of(
        new ShopEntry("crop_wheat_seeds", Items.WHEAT_SEEDS, 1, 40, 128),
        new ShopEntry("crop_carrot", Items.CARROT, 1, 75, 96),
        new ShopEntry("crop_potato", Items.POTATO, 1, 75, 96),
        new ShopEntry("crop_oak_sapling", Items.OAK_SAPLING, 1, 230, 64),
        new ShopEntry("crop_sugar", Items.SUGAR, 1, 90, 96),
        new ShopEntry("crop_wooden_hoe", Items.WOODEN_HOE, 1, 450, 24),
        new ShopEntry("crop_stone_hoe", Items.STONE_HOE, 1, 750, 24),
        new ShopEntry("crop_iron_hoe", Items.IRON_HOE, 1, 2600, 12)
    );
    private static final List<ShopEntry> FISHER = List.of(
        new ShopEntry("fisher_fishing_rod", Items.FISHING_ROD, 1, 1800, 32),
        new ShopEntry("fisher_string", Items.STRING, 1, 90, 96),
        new ShopEntry("fisher_water_bucket", Items.WATER_BUCKET, 1, 3200, 24),
        new ShopEntry("fisher_oak_boat", Items.OAK_BOAT, 1, 1200, 32),
        new ShopEntry("fisher_torch", Items.TORCH, 1, 35, 128),
        new ShopEntry("fisher_lantern", Items.LANTERN, 1, 450, 48)
    );
    private static final List<ShopEntry> MINER = List.of(
        new ShopEntry("miner_wooden_pickaxe", Items.WOODEN_PICKAXE, 1, 500, 32),
        new ShopEntry("miner_stone_pickaxe", Items.STONE_PICKAXE, 1, 850, 32),
        new ShopEntry("miner_iron_pickaxe", Items.IRON_PICKAXE, 1, 3200, 16),
        new ShopEntry("miner_wooden_shovel", Items.WOODEN_SHOVEL, 1, 350, 32),
        new ShopEntry("miner_stone_shovel", Items.STONE_SHOVEL, 1, 650, 32),
        new ShopEntry("miner_iron_shovel", Items.IRON_SHOVEL, 1, 2400, 16),
        new ShopEntry("miner_wooden_axe", Items.WOODEN_AXE, 1, 550, 32),
        new ShopEntry("miner_stone_axe", Items.STONE_AXE, 1, 900, 32),
        new ShopEntry("miner_iron_axe", Items.IRON_AXE, 1, 3300, 16)
    );
    private static final List<ShopEntry> CHEF = List.of(
        new ShopEntry("chef_bread", Items.BREAD, 1, 30, 64),
        new ShopEntry("chef_baked_potato", Items.BAKED_POTATO, 1, 25, 64),
        new ShopEntry("chef_cooked_chicken", Items.COOKED_CHICKEN, 1, 650, 48),
        new ShopEntry("chef_steak", Items.COOKED_BEEF, 1, 100, 48),
        new ShopEntry("chef_mushroom_stew", Items.MUSHROOM_STEW, 1, 1800, 32),
        new ShopEntry("chef_pumpkin_pie", Items.PUMPKIN_PIE, 1, 1100, 32),
        new ShopEntry("chef_cake", Items.CAKE, 1, 9000, 8)
    );
    private static final List<ShopEntry> POTION = List.of(
        potionEntry("potion_healing", Potions.HEALING, 850),
        potionEntry("potion_strong_healing", Potions.STRONG_HEALING, 2200),
        potionEntry("potion_regeneration", Potions.REGENERATION, 1600),
        potionEntry("potion_strong_regeneration", Potions.STRONG_REGENERATION, 4200),
        potionEntry("potion_swiftness", Potions.SWIFTNESS, 1100),
        potionEntry("potion_long_swiftness", Potions.LONG_SWIFTNESS, 2500),
        potionEntry("potion_strength", Potions.STRENGTH, 1800),
        potionEntry("potion_long_strength", Potions.LONG_STRENGTH, 3600),
        potionEntry("potion_night_vision", Potions.NIGHT_VISION, 1200),
        potionEntry("potion_long_night_vision", Potions.LONG_NIGHT_VISION, 2800),
        potionEntry("potion_fire_resistance", Potions.FIRE_RESISTANCE, 1700),
        potionEntry("potion_long_fire_resistance", Potions.LONG_FIRE_RESISTANCE, 3900),
        potionEntry("potion_water_breathing", Potions.WATER_BREATHING, 1500),
        potionEntry("potion_long_water_breathing", Potions.LONG_WATER_BREATHING, 3200),
        potionEntry("potion_invisibility", Potions.INVISIBILITY, 2200),
        potionEntry("potion_long_invisibility", Potions.LONG_INVISIBILITY, 4300),
        potionEntry("potion_slow_falling", Potions.SLOW_FALLING, 1700),
        potionEntry("potion_long_slow_falling", Potions.LONG_SLOW_FALLING, 3400),
        splashPotionEntry("splash_healing", Potions.HEALING, 1250),
        splashPotionEntry("splash_strong_healing", Potions.STRONG_HEALING, 2900),
        splashPotionEntry("splash_swiftness", Potions.SWIFTNESS, 1600),
        splashPotionEntry("splash_strength", Potions.STRENGTH, 2400),
        splashPotionEntry("splash_fire_resistance", Potions.FIRE_RESISTANCE, 2200),
        lingeringPotionEntry("lingering_regeneration", Potions.REGENERATION, 5400),
        lingeringPotionEntry("lingering_swiftness", Potions.SWIFTNESS, 4600)
    );
    private static final List<ShopEntry> CROP_DELIVERIES = List.of(
        new ShopEntry("deliver_wheat", Items.WHEAT, 64, 80, 0),
        new ShopEntry("deliver_carrot", Items.CARROT, 64, 105, 0),
        new ShopEntry("deliver_potato", Items.POTATO, 64, 105, 0),
        new ShopEntry("deliver_beetroot", Items.BEETROOT, 64, 120, 0)
    );
    private static final List<ShopEntry> FISHER_DELIVERIES = List.of(
        new ShopEntry("deliver_cod", Items.COD, 64, 220, 0),
        new ShopEntry("deliver_salmon", Items.SALMON, 64, 300, 0),
        new ShopEntry("deliver_tropical_fish", Items.TROPICAL_FISH, 64, 850, 0),
        new ShopEntry("deliver_pufferfish", Items.PUFFERFISH, 64, 1050, 0)
    );
    private static final List<ShopEntry> MINER_DELIVERIES = List.of(
        new ShopEntry("deliver_coal", Items.COAL, 64, 130, 0),
        new ShopEntry("deliver_raw_copper", Items.RAW_COPPER, 64, 145, 0),
        new ShopEntry("deliver_raw_iron", Items.RAW_IRON, 64, 450, 0),
        new ShopEntry("deliver_redstone", Items.REDSTONE, 64, 200, 0)
    );
    private static final List<ShopEntry> CHEF_DELIVERIES = List.of(
        new ShopEntry("deliver_bread", Items.BREAD, 64, 300, 0),
        new ShopEntry("deliver_baked_potato", Items.BAKED_POTATO, 64, 340, 0),
        new ShopEntry("deliver_cooked_chicken", Items.COOKED_CHICKEN, 64, 720, 0),
        new ShopEntry("deliver_cooked_beef", Items.COOKED_BEEF, 64, 850, 0)
    );
    private static final List<ShopEntry> HUNTER = List.of(
        new ShopEntry("hunter_bow", Items.BOW, 1, 1500, 32),
        new ShopEntry("hunter_crossbow", Items.CROSSBOW, 1, 2000, 24),
        new ShopEntry("hunter_arrow", Items.ARROW, 1, 45, 256),
        new ShopEntry("hunter_spectral_arrow", Items.SPECTRAL_ARROW, 1, 120, 128),
        new ShopEntry("hunter_leather_helmet", Items.LEATHER_HELMET, 1, 800, 16),
        new ShopEntry("hunter_leather_chestplate", Items.LEATHER_CHESTPLATE, 1, 1200, 16),
        new ShopEntry("hunter_leather_leggings", Items.LEATHER_LEGGINGS, 1, 1000, 16),
        new ShopEntry("hunter_leather_boots", Items.LEATHER_BOOTS, 1, 700, 16),
        new ShopEntry("hunter_cooked_rabbit", Items.COOKED_RABBIT, 1, 400, 64)
    );
    private static final List<ShopEntry> HUNTER_DELIVERIES = List.of(
        new ShopEntry("deliver_rotten_flesh", Items.ROTTEN_FLESH, 64, 40, 0),
        new ShopEntry("deliver_bone", Items.BONE, 64, 60, 0),
        new ShopEntry("deliver_gunpowder", Items.GUNPOWDER, 64, 115, 0),
        new ShopEntry("deliver_spider_eye", Items.SPIDER_EYE, 1, 2, 0),
        new ShopEntry("deliver_string", Items.STRING, 64, 75, 0),
        new ShopEntry("deliver_ender_pearl", Items.ENDER_PEARL, 16, 550, 0)
    );

    private static final List<ShopEntry> SMITH = List.of(
        new ShopEntry("smith_anvil", Items.ANVIL, 1, 8000, 2)
    );

    private static final List<ShopEntry> SMITH_DELIVERIES = List.of(
        new ShopEntry("deliver_coal", Items.COAL, 64, 90, 0),
        new ShopEntry("deliver_raw_copper", Items.RAW_COPPER, 64, 100, 0),
        new ShopEntry("deliver_raw_iron", Items.RAW_IRON, 64, 325, 0),
        new ShopEntry("deliver_raw_gold", Items.RAW_GOLD, 64, 650, 0),
        new ShopEntry("deliver_netherite_scrap", Items.NETHERITE_SCRAP, 1, 15000, 0),
        new ShopEntry("deliver_cracked_enhancement_gem", ModItems.CRACKED_ENHANCEMENT_GEM.get(), 1, 9000, 0),
        new ShopEntry("deliver_split_enhancement_gem", ModItems.SPLIT_ENHANCEMENT_GEM.get(), 1, 40000, 0),
        new ShopEntry("deliver_flawed_enhancement_gem", ModItems.FLAWED_ENHANCEMENT_GEM.get(), 1, 100000, 0),
        new ShopEntry("deliver_enhancement_gem", ModItems.ENHANCEMENT_GEM.get(), 1, 850000, 0),
        new ShopEntry("deliver_flawless_enhancement_gem", ModItems.FLAWLESS_ENHANCEMENT_GEM.get(), 1, 4000000, 0),
        new ShopEntry("deliver_perfect_enhancement_gem", ModItems.PERFECT_ENHANCEMENT_GEM.get(), 1, 15000000, 0)
    );

    private static List<ShopEntry> cachedEngineerShop = null;
    private static List<ShopEntry> cachedEngineerDeliveries = null;

    public static synchronized List<ShopEntry> getEngineerShop() {
        if (cachedEngineerShop == null) {
            List<ShopEntry> list = new ArrayList<>();
            addIfPresent(list, "engineer_wrench", "create:wrench", 1, 3000, 16);
            addIfPresent(list, "engineer_goggles", "create:goggles", 1, 5000, 16);
            addIfPresent(list, "engineer_cogwheel", "create:cogwheel", 1, 200, 64);
            addIfPresent(list, "engineer_large_cogwheel", "create:large_cogwheel", 1, 400, 64);
            addIfPresent(list, "engineer_shaft", "create:shaft", 1, 100, 128);
            addIfPresent(list, "engineer_belt_connector", "create:belt_connector", 1, 150, 64);
            addIfPresent(list, "engineer_gearbox", "create:gearbox", 1, 800, 32);
            addIfPresent(list, "engineer_hand_crank", "create:hand_crank", 1, 300, 16);
            cachedEngineerShop = List.copyOf(list);
        }
        return cachedEngineerShop;
    }

    public static synchronized List<ShopEntry> getEngineerDeliveries() {
        if (cachedEngineerDeliveries == null) {
            List<ShopEntry> list = new ArrayList<>();
            list.add(new ShopEntry("deliver_cracked_enhancement_gem", ModItems.CRACKED_ENHANCEMENT_GEM.get(), 1, 9000, 0));
            list.add(new ShopEntry("deliver_split_enhancement_gem", ModItems.SPLIT_ENHANCEMENT_GEM.get(), 1, 40000, 0));
            list.add(new ShopEntry("deliver_flawed_enhancement_gem", ModItems.FLAWED_ENHANCEMENT_GEM.get(), 1, 100000, 0));
            list.add(new ShopEntry("deliver_enhancement_gem", ModItems.ENHANCEMENT_GEM.get(), 1, 850000, 0));
            list.add(new ShopEntry("deliver_flawless_enhancement_gem", ModItems.FLAWLESS_ENHANCEMENT_GEM.get(), 1, 4000000, 0));
            list.add(new ShopEntry("deliver_perfect_enhancement_gem", ModItems.PERFECT_ENHANCEMENT_GEM.get(), 1, 15000000, 0));

            cachedEngineerDeliveries = List.copyOf(list);
        }
        return cachedEngineerDeliveries;
    }

    private static void addIfPresent(List<ShopEntry> list, String id, String registryName, int count, long price, int dailyLimit) {
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(registryName);
        if (rl != null) {
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
            if (item != net.minecraft.world.item.Items.AIR) {
                list.add(new ShopEntry(id, item, count, price, dailyLimit));
            }
        }
    }

    private Shops() {
    }

    public static List<ShopEntry> defaults() {
        return new ArrayList<>(GENERAL);
    }

    public static List<ShopEntry> defaults(TraderKind kind) {
        return switch (kind) {
            case GENERAL -> defaults();
            case CROP -> new ArrayList<>(CROP);
            case FISHER -> new ArrayList<>(FISHER);
            case MINER -> new ArrayList<>(MINER);
            case CHEF -> new ArrayList<>(CHEF);
            case POTION -> new ArrayList<>(POTION);
            case GUN -> new ArrayList<>(GunCatalog.shopEntries());
            case HUNTER -> new ArrayList<>(HUNTER);
            case SMITH -> new ArrayList<>(SMITH);
            case ENGINEER -> new ArrayList<>(getEngineerShop());
            default -> new ArrayList<>();
        };
    }

    public static List<ShopEntry> entries(TraderKind kind, EconomyState state) {
        return state.shopEntries(kind);
    }

    public static List<ShopEntry> entries(TraderKind kind, EconomyState state, String traderDatabaseId) {
        return state.shopEntries(kind);
    }

    public static List<ShopEntry> deliveries(TraderKind kind) {
        return defaultDeliveries(kind);
    }

    public static List<ShopEntry> defaultDeliveries(TraderKind kind) {
        return switch (kind) {
            case CROP -> new ArrayList<>(CROP_DELIVERIES);
            case FISHER -> new ArrayList<>(FISHER_DELIVERIES);
            case MINER -> new ArrayList<>(MINER_DELIVERIES);
            case CHEF -> new ArrayList<>(CHEF_DELIVERIES);
            case HUNTER -> {
                List<ShopEntry> list = new ArrayList<>(HUNTER_DELIVERIES);
                addIfPresent(list, "deliver_iceandfire_ash", "iceandfire:ash", 1, 1, 0);
                addIfPresent(list, "deliver_supplementaries_ash", "supplementaries:ash", 1, 1, 0);
                addIfPresent(list, "deliver_dread_shard", "iceandfire:dread_shard", 1, 15, 0);
                yield list;
            }
            case GUN -> new ArrayList<>(GunCatalog.shopEntries());
            case SMITH -> new ArrayList<>(SMITH_DELIVERIES);
            case ENGINEER -> new ArrayList<>(getEngineerDeliveries());
            default -> new ArrayList<>();
        };
    }

    public static ShopEntry find(List<ShopEntry> entries, String id) {
        for (ShopEntry entry : entries) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    private static ShopEntry potionEntry(String id, net.minecraft.world.item.alchemy.Potion potion, long price) {
        ItemStack stack = PotionUtils.setPotion(new ItemStack(Items.POTION), potion);
        return new ShopEntry(id, stack, price, 24);
    }

    private static ShopEntry splashPotionEntry(String id, net.minecraft.world.item.alchemy.Potion potion, long price) {
        ItemStack stack = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), potion);
        return new ShopEntry(id, stack, price, 24);
    }

    private static ShopEntry lingeringPotionEntry(String id, net.minecraft.world.item.alchemy.Potion potion, long price) {
        ItemStack stack = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), potion);
        return new ShopEntry(id, stack, price, 24);
    }
}
