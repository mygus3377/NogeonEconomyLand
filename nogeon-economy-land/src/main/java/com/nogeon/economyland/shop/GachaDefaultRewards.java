package com.nogeon.economyland.shop;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.item.GunCatalog;
import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.menu.GachaCategory;
import com.nogeon.economyland.state.TraderShopState;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TridentItem;

public final class GachaDefaultRewards {
    private GachaDefaultRewards() {
    }

    public static List<ShopEntry> entries(String categoryId) {
        GachaCategory category = GachaCategory.byId(categoryId);
        List<ShopEntry> entries = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        switch (category) {
            case WEAPON -> addWeaponDefaults(entries, seen);
            case ARMOR -> addArmorDefaults(entries, seen);
            case ITEM -> addItemDefaults(entries, seen);
            case GUN_BOW -> addGunBowDefaults(entries, seen);
        }
        addModdedDefaults(entries, seen, category);
        return entries;
    }

    private static void addWeaponDefaults(List<ShopEntry> entries, Set<String> seen) {
        add(entries, seen, "weapon", Items.WOODEN_SWORD, 1, 42, 0, false);
        add(entries, seen, "weapon", Items.STONE_SWORD, 1, 38, 0, false);
        add(entries, seen, "weapon", Items.STONE_AXE, 1, 34, 0, false);
        add(entries, seen, "weapon", Items.IRON_SWORD, 1, 30, 1, false);
        add(entries, seen, "weapon", Items.IRON_AXE, 1, 26, 1, false);
        add(entries, seen, "weapon", Items.GOLDEN_SWORD, 1, 20, 1, false);
        add(entries, seen, "weapon", Items.DIAMOND_SWORD, 1, 16, 2, true);
        add(entries, seen, "weapon", Items.DIAMOND_AXE, 1, 14, 2, true);
        add(entries, seen, "weapon", Items.NETHERITE_SWORD, 1, 6, 3, true);
        add(entries, seen, "weapon", Items.NETHERITE_AXE, 1, 5, 3, true);
        add(entries, seen, "weapon", Items.TRIDENT, 1, 4, 3, true);
    }

    private static void addArmorDefaults(List<ShopEntry> entries, Set<String> seen) {
        add(entries, seen, "armor", Items.LEATHER_HELMET, 1, 34, 0, false);
        add(entries, seen, "armor", Items.LEATHER_CHESTPLATE, 1, 30, 0, false);
        add(entries, seen, "armor", Items.CHAINMAIL_CHESTPLATE, 1, 24, 0, false);
        add(entries, seen, "armor", Items.IRON_HELMET, 1, 28, 1, false);
        add(entries, seen, "armor", Items.IRON_CHESTPLATE, 1, 24, 1, false);
        add(entries, seen, "armor", Items.IRON_LEGGINGS, 1, 24, 1, false);
        add(entries, seen, "armor", Items.DIAMOND_HELMET, 1, 14, 2, true);
        add(entries, seen, "armor", Items.DIAMOND_CHESTPLATE, 1, 12, 2, true);
        add(entries, seen, "armor", Items.DIAMOND_LEGGINGS, 1, 12, 2, true);
        add(entries, seen, "armor", Items.NETHERITE_HELMET, 1, 6, 3, true);
        add(entries, seen, "armor", Items.NETHERITE_CHESTPLATE, 1, 5, 3, true);
        add(entries, seen, "armor", Items.NETHERITE_LEGGINGS, 1, 5, 3, true);
        add(entries, seen, "armor", Items.ELYTRA, 1, 3, 3, true);
    }

    private static void addItemDefaults(List<ShopEntry> entries, Set<String> seen) {
        add(entries, seen, "item", Items.IRON_INGOT, 16, 48, 0, false);
        add(entries, seen, "item", Items.REDSTONE, 24, 44, 0, false);
        add(entries, seen, "item", Items.LAPIS_LAZULI, 24, 44, 0, false);
        add(entries, seen, "item", Items.GOLD_INGOT, 12, 32, 1, false);
        add(entries, seen, "item", Items.EMERALD, 8, 28, 1, false);
        add(entries, seen, "item", Items.EXPERIENCE_BOTTLE, 12, 26, 1, false);
        add(entries, seen, "item", Items.DIAMOND, 6, 16, 2, true);
        add(entries, seen, "item", Items.ENDER_PEARL, 8, 18, 2, true);
        add(entries, seen, "item", Items.GOLDEN_APPLE, 3, 14, 2, true);
        add(entries, seen, "item", Items.ENCHANTED_GOLDEN_APPLE, 1, 5, 3, true);
        add(entries, seen, "item", Items.TOTEM_OF_UNDYING, 1, 4, 3, true);
        add(entries, seen, "item", Items.NETHER_STAR, 1, 2, 3, true);
        add(entries, seen, "item", ModItems.PORTAL_SCROLL.get(), 1, 3, 3, true);
    }

    private static void addGunBowDefaults(List<ShopEntry> entries, Set<String> seen) {
        add(entries, seen, "gun_bow", Items.ARROW, 32, 46, 0, false);
        add(entries, seen, "gun_bow", Items.FIREWORK_ROCKET, 12, 36, 0, false);
        add(entries, seen, "gun_bow", Items.BOW, 1, 32, 1, false);
        add(entries, seen, "gun_bow", Items.CROSSBOW, 1, 22, 2, true);
        add(entries, seen, "gun_bow", Items.TRIDENT, 1, 5, 3, true);
        for (int band = 0; band <= 3; band++) {
            for (ItemStack stack : GunCatalog.gachaStacks(band)) {
                add(entries, seen, "gun_bow", stack, weightForBand(band), band, band >= 2);
            }
        }
    }

    private static void addModdedDefaults(List<ShopEntry> entries, Set<String> seen, GachaCategory category) {
        int added = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!isExternalItem(item, id) || isTaczBaseGunItem(id) || !matchesCategory(item, id, category)) {
                continue;
            }
            int rarity = rarityFor(item, id, category);
            add(entries, seen, category.id(), item, countFor(item, category, rarity), weightForBand(rarity), rarity, rarity >= 2);
            added++;
            if (added >= 80) {
                return;
            }
        }
    }

    private static boolean matchesCategory(Item item, ResourceLocation id, GachaCategory category) {
        ItemStack stack = new ItemStack(item);
        String token = id.toString().toLowerCase(Locale.ROOT);
        return switch (category) {
            case WEAPON -> stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || item instanceof TridentItem
                || containsAny(token, "sword", "blade", "dagger", "spear", "katana", "halberd", "scythe", "mace", "rapier");
            case ARMOR -> item instanceof ArmorItem || item instanceof ElytraItem;
            case GUN_BOW -> {
                boolean matchesBase = item instanceof BowItem || item instanceof CrossbowItem || GunCatalog.isRegisteredGun(item)
                    || containsAny(token, "tacz", "gun", "rifle", "pistol", "shotgun", "smg", "sniper");
                if (!matchesBase) {
                    yield false;
                }
                if (containsAny(token, "ammo", "bullet", "magazine", "scope", "sight", "grip", "stock", "barrel", "suppressor", "muzzle", "attachment", "rocket")) {
                    yield false;
                }
                if (GunCatalog.isRegisteredAmmo(item)) {
                    yield false;
                }
                yield true;
            }
            case ITEM -> !matchesCategory(item, id, GachaCategory.WEAPON)
                && !matchesCategory(item, id, GachaCategory.ARMOR)
                && !matchesCategory(item, id, GachaCategory.GUN_BOW)
                && (item.getMaxStackSize() > 1 || containsAny(token, "gem", "ingot", "crystal", "essence", "fragment", "core", "rune", "ticket", "scroll"));
        };
    }

    private static int rarityFor(Item item, ResourceLocation id, GachaCategory category) {
        String token = id.toString().toLowerCase(Locale.ROOT);
        int rarity = switch (safeRarityName(new ItemStack(item))) {
            case "EPIC" -> 3;
            case "RARE" -> 2;
            case "UNCOMMON" -> 1;
            default -> 0;
        };
        if (category == GachaCategory.ARMOR) {
            rarity = Math.max(rarity, armorRarityFor(item, token));
        }
        if (containsAny(token, "netherite", "legend", "mythic", "unique", "artifact", "50bmg", "sniper")) {
            rarity = Math.max(rarity, 3);
        } else if (containsAny(token, "diamond", "epic", "rare", "rifle", "shotgun", "scope")) {
            rarity = Math.max(rarity, 2);
        } else if (containsAny(token, "iron", "gold", "uncommon", "pistol", "smg", "magazine", "grip")) {
            rarity = Math.max(rarity, 1);
        }
        if (category == GachaCategory.ITEM && item.getMaxStackSize() >= 32) {
            rarity = Math.min(rarity, 2);
        }
        return Math.max(0, Math.min(3, rarity));
    }

    private static int armorRarityFor(Item item, String token) {
        if (item instanceof ElytraItem) {
            return 3;
        }
        if (!(item instanceof ArmorItem armorItem)) {
            return 0;
        }

        double score = armorItem.getDefense() + armorItem.getToughness() * 1.5D;
        ItemStack stack = new ItemStack(item);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            for (AttributeModifier modifier : stack.getAttributeModifiers(slot).get(Attributes.ARMOR)) {
                score += Math.abs(modifier.getAmount()) * 0.8D;
            }
            for (AttributeModifier modifier : stack.getAttributeModifiers(slot).get(Attributes.ARMOR_TOUGHNESS)) {
                score += Math.abs(modifier.getAmount()) * 1.2D;
            }
            for (AttributeModifier modifier : stack.getAttributeModifiers(slot).get(Attributes.KNOCKBACK_RESISTANCE)) {
                score += Math.abs(modifier.getAmount()) * 8.0D;
            }
            int extraModifierCount = 0;
            for (AttributeModifier ignored : stack.getAttributeModifiers(slot).values()) {
                extraModifierCount++;
            }
            score += Math.max(0, extraModifierCount - 2) * 1.5D;
        }

        if (containsAny(token, "legend", "mythic", "ancient", "artifact", "dragon", "netherite")) {
            score += 5.0D;
        } else if (containsAny(token, "diamond", "obsidian", "platinum", "titanium")) {
            score += 3.0D;
        } else if (containsAny(token, "iron", "steel", "gold")) {
            score += 1.5D;
        }

        if (score >= 13.0D) {
            return 3;
        }
        if (score >= 8.0D) {
            return 2;
        }
        if (score >= 4.0D) {
            return 1;
        }
        return 0;
    }

    private static String safeRarityName(ItemStack stack) {
        return stack.getRarity() == null ? "" : stack.getRarity().name();
    }

    private static int countFor(Item item, GachaCategory category, int rarity) {
        if (category != GachaCategory.ITEM && category != GachaCategory.GUN_BOW) {
            return 1;
        }
        if (item.getMaxStackSize() == 1) {
            return 1;
        }
        int count = switch (rarity) {
            case 3 -> 2;
            case 2 -> 4;
            case 1 -> 8;
            default -> 16;
        };
        return Math.max(1, Math.min(item.getMaxStackSize(), count));
    }

    private static int weightForBand(int band) {
        return switch (band) {
            case 3 -> 5;
            case 2 -> 14;
            case 1 -> 28;
            default -> 44;
        };
    }

    private static void add(List<ShopEntry> entries, Set<String> seen, String categoryId, Item item, int count, long weight, int rarity, boolean jackpot) {
        if (item == Items.AIR) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || !seen.add(id.toString())) {
            return;
        }
        ItemStack stack = item.getDefaultInstance();
        if (stack.isEmpty()) {
            stack = new ItemStack(item);
        } else {
            stack = stack.copy();
        }
        stack.setCount(Math.max(1, Math.min(stack.getMaxStackSize(), count)));
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("NoGeonGachaRarity", Math.max(0, Math.min(3, rarity)));
        tag.putBoolean("NoGeonGachaJackpot", jackpot);
        entries.add(new ShopEntry(TraderShopState.globalGachaPrefix(categoryId) + sanitize(id), stack, Math.max(1L, weight), 0));
    }

    private static void add(List<ShopEntry> entries, Set<String> seen, String categoryId, ItemStack stack, long weight, int rarity, boolean jackpot) {
        if (stack.isEmpty() || stack.getItem() == Items.AIR) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return;
        }
        String uniqueId = id.toString();
        if (stack.hasTag() && stack.getTag().contains("GunId", Tag.TAG_STRING)) {
            uniqueId += "/" + stack.getTag().getString("GunId");
        }
        if (!seen.add(uniqueId)) {
            return;
        }
        ItemStack reward = stack.copy();
        reward.setCount(Math.max(1, Math.min(reward.getMaxStackSize(), reward.getCount())));
        CompoundTag tag = reward.getOrCreateTag();
        tag.putInt("NoGeonGachaRarity", Math.max(0, Math.min(3, rarity)));
        tag.putBoolean("NoGeonGachaJackpot", jackpot);
        entries.add(new ShopEntry(TraderShopState.globalGachaPrefix(categoryId) + uniqueId.replace(':', '_').replace('/', '_'), reward, Math.max(1L, weight), 0));
    }

    private static boolean isExternalItem(Item item, ResourceLocation id) {
        if (id == null || item == Items.AIR || item instanceof SpawnEggItem || item instanceof BlockItem) {
            return false;
        }
        String namespace = id.getNamespace();
        return !ResourceLocation.DEFAULT_NAMESPACE.equals(namespace) && !NoGeonEconomyLand.MOD_ID.equals(namespace);
    }

    private static boolean containsAny(String token, String... needles) {
        for (String needle : needles) {
            if (token.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String sanitize(ResourceLocation id) {
        return id.toString().replace(':', '_').replace('/', '_');
    }

    private static boolean isTaczBaseGunItem(ResourceLocation id) {
        return "tacz".equals(id.getNamespace()) && "modern_kinetic_gun".equals(id.getPath());
    }
}
