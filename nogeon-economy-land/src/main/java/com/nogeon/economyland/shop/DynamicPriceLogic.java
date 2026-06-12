package com.nogeon.economyland.shop;

import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.item.GunCatalog;
import com.nogeon.economyland.item.SmithingService;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.Tags;

public final class DynamicPriceLogic {

    public static boolean isSameItemForTrade(ItemStack stack, ItemStack target, TraderKind kind) {
        if (stack.isEmpty() || target.isEmpty()) return false;
        if (stack.getItem() != target.getItem()) return false;

        if (kind == TraderKind.CHEF || kind == TraderKind.CROP || kind == TraderKind.FISHER || kind == TraderKind.MINER || kind == TraderKind.HUNTER || kind == TraderKind.GENERAL) {
            return true;
        }

        return ItemStack.isSameItemSameTags(stack, target);
    }

    public static boolean isSameItemForTrade(ItemStack stack, ItemStack target, String kindId) {
        return isSameItemForTrade(stack, target, TraderKind.byId(kindId));
    }

    public static boolean shouldAccept(TraderKind kind, ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath().toLowerCase();
        boolean isFood = item.getFoodProperties() != null;

        return switch (kind) {
            case HUNTER -> true;
            case FISHER -> isFish(stack, item, path);
            case CHEF -> isCookedFood(stack, item, path);
            case MINER -> isMineral(stack, item, path) && !isFood;
            case CROP -> isFarmProduct(stack, item, path) || (isFood && !isCookedFood(stack, item, path) && !isFish(stack, item, path));
            case POTION -> isPotionRelated(stack, item, path);
            case GUN -> isGunRelated(stack, item, path);
            case SMITH -> isEquipment(stack, item, path);
            case ENGINEER -> isEngineerRelated(stack, item, path, id);
            case GENERAL -> true;
            default -> false;
        };
    }

    private static boolean isMonsterDrop(ItemStack stack, Item item, String path) {
        if (stack.is(ItemTags.MUSIC_DISCS) || stack.is(Tags.Items.DUSTS)) return true;
        if (stack.is(ItemTags.PIGLIN_LOVED)) return true;
        return path.contains("flesh") || path.contains("bone") || path.contains("eye") || 
               path.contains("gunpowder") || path.contains("essence") || path.contains("heart") ||
               path.contains("fragment") || path.contains("tear") || path.contains("nether_star") ||
               path.contains("membrane") || path.contains("shard") || path.contains("crystal") ||
               path.contains("hide") || path.contains("foot") || path.contains("scute") ||
               path.contains("shulker_shell") || path.contains("totem") || path.contains("skull") || 
               path.contains("head") || path.contains("slime_ball") || path.contains("magma_cream");
    }

    private static boolean isFish(ItemStack stack, Item item, String path) {
        if (stack.is(ItemTags.FISHES)) return true;
        return path.contains("fish") || path.contains("cod") || path.contains("salmon") || 
               path.contains("puffer") || path.contains("tropical") || path.contains("shrimp") ||
               path.contains("crab") || path.contains("lobster") || path.contains("roe") ||
               path.contains("clam") || path.contains("oyster") || path.contains("shellfish");
    }

    private static boolean isCookedFood(ItemStack stack, Item item, String path) {
        if (isChefBeverage(path) || isPreparedKitchenFood(path)) return true;

        FoodProperties food = item.getFoodProperties();
        if (food == null) return false;
        if (path.contains("cooked") || path.contains("baked") || path.contains("fried") || 
            path.contains("grilled") || path.contains("stew") || path.contains("soup") ||
            path.contains("pie") || path.contains("cake") || path.contains("burger") ||
            path.contains("sandwich") || path.contains("pasta") || path.contains("meal") ||
            path.contains("bread") || path.contains("toast") || path.contains("cookie") ||
            path.contains("juice") || path.contains("tea") || path.contains("coffee") ||
            path.contains("feast") || path.contains("delight") || path.contains("platter") ||
            path.contains("dumpling") || path.contains("croissant") || path.contains("tart") ||
            path.contains("donut") || path.contains("doughnut") || path.contains("pastry") ||
            path.contains("scone") || path.contains("muffin") || path.contains("cupcake") ||
            path.contains("pudding") || path.contains("jelly") || path.contains("chocolate") ||
            path.contains("taco") || path.contains("pizza") || path.contains("ramen") ||
            path.contains("noodle") || path.contains("rice") || path.contains("curry") ||
            path.contains("sushi") || path.contains("wrap") || path.contains("salad") ||
            path.contains("stuffed") || path.contains("roast") || path.contains("skewer") ||
            path.contains("kebab") || path.contains("kabob") || path.contains("potage") ||
            path.contains("chowder") || path.contains("broth") || path.contains("porridge") ||
            path.contains("pancake") || path.contains("waffle") || path.contains("crepe") ||
            path.contains("custard") || path.contains("ice_cream") || path.contains("icecream") || path.contains("sorbet") || path.contains("gelato") || path.contains("sherbet") ||
            path.contains("yogurt") || path.contains("cheese") || path.contains("butter") ||
            path.contains("bacon") || path.contains("sausage") || path.contains("ham") ||
            path.contains("meatball") || path.contains("patty") || path.contains("omelet") ||
            path.contains("scramble") || path.contains("quiche") || path.contains("frittata") ||
            path.contains("lasagna") || path.contains("spaghetti") || path.contains("burrito") ||
            path.contains("quesadilla") || path.contains("egg_roll") || path.contains("spring_roll")) return true;
        
        return food.getNutrition() >= 6 || food.getSaturationModifier() >= 0.6F;
    }

    private static boolean isChefBeverage(String path) {
        if (path.contains("bottle") || path.contains("vial") || path.contains("glass") || path.contains("jar") || path.contains("mug") || path.contains("cup")) {
            return false;
        }
        return path.contains("wine") || path.contains("juice") || path.contains("cider") ||
               path.contains("mead") || path.contains("beer") || path.contains("ale") ||
               path.contains("liquor") || path.contains("whiskey") || path.contains("vodka") ||
               path.contains("sake") || path.contains("tea") || path.contains("coffee") ||
               path.contains("drink") || path.contains("beverage") || path.contains("alcohol") ||
               path.contains("cocktail") || path.contains("latte") || path.contains("cappuccino") ||
               path.contains("espresso") || path.contains("smoothie") || path.contains("soda");
    }

    private static boolean isPreparedKitchenFood(String path) {
        return path.contains("fried") || path.contains("grilled") || path.contains("roast") ||
               path.contains("skillet") || path.contains("pan") || path.contains("omelet") ||
               path.contains("pasta") || path.contains("stew") || path.contains("soup") ||
               path.contains("meal") || path.contains("delight") || path.contains("feast") ||
               path.contains("platter") || path.contains("sandwich") || path.contains("burger");
    }

    private static boolean isDelightFood(ResourceLocation id, Item item) {
        String ns = id.getNamespace().toLowerCase(Locale.ROOT);
        boolean isDelightMod = ns.contains("delight") || ns.contains("farm") || ns.contains("charm") || 
                               ns.contains("vinery") || ns.contains("brewery") || ns.contains("candlelight") ||
                               ns.contains("brewin");
        if (!isDelightMod) return false;

        String path = id.getPath().toLowerCase();
        
        // 1차 농작물 및 원재료 제외 필터링 (씨앗, 단순 식재료 등)
        if (path.contains("seed") || path.contains("sapling") || path.contains("wild_") ||
            path.contains("cabbage") || path.contains("tomato") || path.contains("onion") ||
            path.contains("lettuce") || path.contains("barley") || path.contains("flax") ||
            path.contains("rice_panicle") || path.contains("straw") || path.contains("canvas") ||
            path.contains("root") || path.contains("beetroot") || path.contains("potato") ||
            path.contains("carrot") || path.contains("wheat") || path.contains("apple") ||
            path.contains("berry") || path.contains("berries") || path.contains("leaves") ||
            path.contains("leaf") || path.contains("flower") || path.contains("petal")) {
            return false;
        }

        // 조리되지 않은 육류/해산물 원재료 제외
        if ((path.contains("raw") || path.contains("mutton") || path.contains("beef") || 
             path.contains("porkchop") || path.contains("chicken") || path.contains("cod") || 
             path.contains("salmon") || path.contains("bacon") || path.contains("ham")) 
            && !path.contains("cooked") && !path.contains("smoked") && !path.contains("baked")
            && !path.contains("grilled") && !path.contains("fried")) {
            return false;
        }

        // 영양가가 4 미만인 가벼운 재료는 명시적인 요리 키워드가 없을 경우 제외
        net.minecraft.world.food.FoodProperties food = item.getFoodProperties();
        if (food != null) {
            if (food.getNutrition() < 4) {
                boolean isCooked = path.contains("cooked") || path.contains("baked") || path.contains("fried") || 
                                   path.contains("grilled") || path.contains("stew") || path.contains("soup") ||
                                   path.contains("pie") || path.contains("cake") || path.contains("burger") ||
                                   path.contains("sandwich") || path.contains("pasta") || path.contains("meal") ||
                                   path.contains("bread") || path.contains("toast") || path.contains("cookie") ||
                                   path.contains("juice") || path.contains("tea") || path.contains("coffee") ||
                                   path.contains("salad") || path.contains("spaghetti") || path.contains("noodle") ||
                                   path.contains("curry") || path.contains("dumpling") || path.contains("cheese") ||
                                   path.contains("butter") || path.contains("sauce") || path.contains("jam");
                if (!isCooked) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isMineral(ItemStack stack, Item item, String path) {
        if (isEquipment(stack, item, path)) return false;
        if (stack.is(Tags.Items.ORES) || stack.is(Tags.Items.INGOTS) || stack.is(Tags.Items.GEMS) || 
            stack.is(Tags.Items.NUGGETS) || stack.is(Tags.Items.RAW_MATERIALS)) return true;
        
        if (path.contains("sugar") || path.contains("flour") || path.contains("salt") || path.contains("spice")) return false;
        
        return path.contains("ore") || path.contains("ingot") || path.contains("gem") || 
               (path.contains("crystal") && !path.contains("sugar")) || 
               path.contains("diamond") || path.contains("emerald") ||
               path.contains("ruby") || path.contains("sapphire") || path.contains("amethyst") ||
               path.contains("quartz") || path.contains("coal") || 
               (path.contains("dust") && !path.contains("flour")) || 
               path.contains("nugget") || path.contains("raw_") || path.contains("alloy");
    }

    private static boolean isFarmProduct(ItemStack stack, Item item, String path) {
        if (stack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS) || stack.is(ItemTags.FLOWERS) || 
            stack.is(ItemTags.SAPLINGS) || stack.is(ItemTags.LEAVES)) return true;
        
        return path.contains("seed") || path.contains("wheat") || path.contains("carrot") || 
               path.contains("potato") || path.contains("beetroot") || path.contains("crop") ||
               path.contains("berry") || path.contains("fruit") || path.contains("leaf") ||
               path.contains("sapling") || path.contains("sugar") || path.contains("honey") ||
               path.contains("herb") || path.contains("vegetable") || path.contains("mushroom") ||
               path.contains("flower") || path.contains("petal");
    }

    private static boolean isPotionRelated(ItemStack stack, Item item, String path) {
        if (item instanceof PotionItem || item == Items.GLASS_BOTTLE || item == Items.BREWING_STAND) return true;
        return path.contains("potion") || path.contains("elixir") || path.contains("vial") ||
               path.contains("bottle") || path.contains("brewing") || path.contains("wart") ||
               path.contains("fermented") || path.contains("ghast_tear") || path.contains("blaze_powder");
    }

    private static boolean isGunRelated(ItemStack stack, Item item, String path) {
        if (GunCatalog.isRegisteredGun(item) || GunCatalog.isRegisteredAmmo(item)) return true;
        return path.contains("gun") || path.contains("ammo") || path.contains("bullet") ||
               path.contains("shell") || path.contains("magazine") || path.contains("pistol") ||
               path.contains("rifle") || path.contains("sniper") || path.contains("shotgun") ||
               path.contains("scope") || path.contains("barrel") || path.contains("stock");
    }

    private static boolean isEquipment(ItemStack stack, Item item, String path) {
        return SmithingService.canEnhance(stack) || 
               item instanceof SwordItem || 
               item instanceof DiggerItem || 
               item instanceof ArmorItem ||
               item instanceof BowItem ||
               item instanceof CrossbowItem ||
               item instanceof TridentItem ||
               item instanceof ShieldItem ||
               path.contains("bow") || 
               path.contains("shield") || 
               path.contains("helmet") || 
               path.contains("chestplate") || 
               path.contains("leggings") || 
               path.contains("boots") ||
               path.contains("wand") ||
               path.contains("staff") ||
               path.contains("stave") ||
               path.contains("scepter") ||
               path.contains("spellblade") ||
               path.contains("spell_book") ||
               path.contains("spellbook") ||
               path.contains("chakram") ||
               path.contains("boomerang") ||
               path.contains("throwing") ||
               path.contains("void_core") ||
               path.contains("mystic");
    }

    private static boolean isEngineerRelated(ItemStack stack, Item item, String path, ResourceLocation id) {
        String namespace = id.getNamespace();
        if ("create".equals(namespace)) {
            if (path.contains("raw_") || path.contains("ore") || path.contains("block") || path.contains("ingot") || path.contains("nugget")) {
                return false;
            }
            return true;
        }
        if ("apotheosis".equals(namespace) && path.contains("gem")) {
            return true;
        }
        if (path.contains("enhancement_gem")) {
            return true;
        }
        if (path.contains("zinc") || path.contains("brass") || path.contains("copper") || path.contains("iron") || path.contains("gold") || path.contains("rose_quartz") || path.contains("quartz")) {
            if (path.contains("ingot") || path.contains("raw_") || path.contains("block") || path.contains("ore") || path.contains("dust") || path.contains("nugget")) {
                return false;
            }
            return true;
        }
        return false;
    }

    private static boolean isMagicOrThrownWeapon(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
        return id.contains("wand")
            || id.contains("staff")
            || id.contains("stave")
            || id.contains("scepter")
            || id.contains("spellblade")
            || id.contains("spell_book")
            || id.contains("spellbook")
            || id.contains("chakram")
            || id.contains("boomerang")
            || id.contains("throwing");
    }

    public static long calculatePrice(TraderKind kind, ItemStack stack) {
        if (kind == TraderKind.HUNTER) {
            TraderKind dedicated = findDedicatedTrader(stack);
            long dedicatedPrice = calculatePriceInternal(dedicated, stack);
            return Math.max(1L, Math.round(dedicatedPrice * 0.70D));
        }
        return calculatePriceInternal(kind, stack);
    }

    private static TraderKind findDedicatedTrader(ItemStack stack) {
        if (stack.isEmpty()) return TraderKind.GENERAL;
        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath().toLowerCase();
        boolean isFood = item.getFoodProperties() != null;

        if (isMonsterDrop(stack, item, path) && !isFood) {
            return TraderKind.HUNTER;
        }
        if (isFish(stack, item, path)) {
            return TraderKind.FISHER;
        }
        if (isCookedFood(stack, item, path)) {
            return TraderKind.CHEF;
        }
        if (isMineral(stack, item, path) && !isFood) {
            return TraderKind.MINER;
        }
        if (isFarmProduct(stack, item, path) || (isFood && !isCookedFood(stack, item, path) && !isFish(stack, item, path))) {
            return TraderKind.CROP;
        }
        if (isPotionRelated(stack, item, path)) {
            return TraderKind.POTION;
        }
        if (isGunRelated(stack, item, path)) {
            return TraderKind.GUN;
        }
        if (isEquipment(stack, item, path)) {
            return TraderKind.SMITH;
        }
        if (isEngineerRelated(stack, item, path, id)) {
            return TraderKind.ENGINEER;
        }
        return TraderKind.GENERAL;
    }

    public static long calculatePriceInternal(TraderKind kind, ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath().toLowerCase();
        String namespace = id.getNamespace();
        Item item = stack.getItem();

        long baseValue = getBasePrice(kind, stack);
        
        // If it's equipment, calculate base value based on material count
        if (isEquipment(stack, item, path)) {
            int count = getIngredientCount(item);
            long materialPrice = getMaterialUnitPrice(path);
            
            if (path.contains("netherite")) {
                // Netherite = Diamond Base + 1 Ingot + Upgrade Template cost (approx 5000)
                baseValue = (getMaterialUnitPrice("diamond") * count) + getMaterialUnitPrice("netherite") + 5000;
            } else {
                baseValue = materialPrice * count;
            }
        }

        double rarityMultiplier = rarityMultiplier(stack.getRarity());
        if ("apotheosis".equals(namespace) && path.contains("gem")) {
            rarityMultiplier = getApotheosisGemMultiplier(stack);
        }

        int enchantCount = EnchantmentHelper.getEnchantments(stack).size();
        int totalLevel = EnchantmentHelper.getEnchantments(stack).values().stream().mapToInt(Integer::intValue).sum();
        double effectBonus = enchantmentValueMultiplier(stack, item, path, enchantCount, totalLevel);
        
        int enhanceLevel = SmithingService.level(stack);
        double enhanceBonus = 1.0 + (enhanceLevel * 0.15) + (enhanceLevel >= 10 ? (enhanceLevel - 9) * 0.5 : 0.0);
        
        double reforgeBonus = 1.0;
        if (stack.hasTag() && stack.getTag().contains(com.nogeon.economyland.item.ReforgeService.REFORGE_TAG, net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag list = stack.getTag().getList(com.nogeon.economyland.item.ReforgeService.REFORGE_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND);
            int limit = Math.min(list.size(), com.nogeon.economyland.item.ReforgeService.MAX_SLOTS);
            for (int i = 0; i < limit; i++) {
                net.minecraft.nbt.CompoundTag tag = list.getCompound(i);
                String modId = tag.getString("ModifierId");
                if ("none".equals(modId) || modId.isEmpty()) {
                    reforgeBonus += 0.15;
                } else {
                    String rarityStr = tag.getString("Rarity");
                    com.nogeon.economyland.item.ReforgeService.Rarity rarity = com.nogeon.economyland.item.ReforgeService.Rarity.safe(rarityStr);
                    double rarityScale = 0.25;
                    switch (rarity) {
                        case COMMON: rarityScale = 0.25; break;
                        case RARE: rarityScale = 0.50; break;
                        case UNIQUE: rarityScale = 1.00; break;
                        case LEGENDARY: rarityScale = 2.00; break;
                    }
                    reforgeBonus += rarityScale;
                }
            }
        }
        
        double modMultiplier = "minecraft".equals(namespace) ? 1.0 : 1.8;

        int tagCount = 0;
        try { tagCount = (int) BuiltInRegistries.ITEM.getHolderOrThrow(BuiltInRegistries.ITEM.getResourceKey(stack.getItem()).get()).tags().count(); } catch (Exception ignored) {}
        double complexityBonus = 1.0 + (tagCount * 0.1);

        double typeBonus = 1.0;
        if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || item instanceof SwordItem || GunCatalog.isRegisteredGun(item)) {
            typeBonus = 1.5; // Adjusted since material count is now factored in
        } else if (isMagicOrThrownWeapon(stack)) {
            typeBonus = 2.2;
        }

        double materialMultiplier = 1.0;
        if (isEquipment(stack, item, path)) {
            if (path.contains("netherite")) materialMultiplier = 2.0;
            else if (path.contains("diamond")) materialMultiplier = 1.5;
            else if (path.contains("void") || path.contains("mystic")) materialMultiplier = 3.2;
        }

        double foodBonus = 1.0;
        FoodProperties food = item.getFoodProperties();
        if (food != null) {
            double nutritionFactor = food.getNutrition();
            double nutritionBonus = (nutritionFactor * 0.15) + (nutritionFactor * nutritionFactor * 0.18);
            double saturationBonus = food.getSaturationModifier() * 4.0;
            foodBonus = 1.0 + nutritionBonus + saturationBonus;
            
            if (path.contains("feast") || path.contains("platter") || path.contains("meal") || path.contains("delight")) foodBonus *= 4.5;
            else if (path.contains("stew") || path.contains("sandwich") || path.contains("burger")) foodBonus *= 2.5;
        }
        if (isChefBeverage(path)) {
            foodBonus *= path.contains("wine") ? 3.0 : 1.8;
        }

        double jobBonus = 1.0;
        if (kind == TraderKind.GUN) jobBonus = 2.4; 
        else if (kind == TraderKind.SMITH) jobBonus = 2.2;
        else if (kind == TraderKind.MINER) jobBonus = 1.6;
        else if (kind == TraderKind.HUNTER) jobBonus = 2.8;
        else if (kind == TraderKind.FISHER) jobBonus = 3.2;
        else if (kind == TraderKind.CHEF) jobBonus = 4.0;
        else if (kind == TraderKind.CROP) jobBonus = 2.8;
        else if (kind == TraderKind.ENGINEER) jobBonus = 7.0;
        else if (kind == TraderKind.GENERAL) jobBonus = 0.8;

        // Special handling for Enchanted Golden Apple to prevent price explosion
        if (item == Items.ENCHANTED_GOLDEN_APPLE) {
            foodBonus = 1.2;
            jobBonus = Math.min(jobBonus, 2.0);
        }

        boolean equipment = isEquipment(stack, item, path);
        double totalMultiplier = rarityMultiplier * effectBonus * enhanceBonus * reforgeBonus * modMultiplier * complexityBonus * typeBonus * materialMultiplier * foodBonus * jobBonus;
        long price;
        if (equipment) {
            double equipmentJobBonus = switch (kind) {
                case SMITH -> 1.15D;
                case GUN, HUNTER -> 1.10D;
                case GENERAL -> 0.80D;
                default -> 1.0D;
            };
            double equipmentTypeBonus = typeBonus > 1.0D ? 1.20D : 1.0D;
            double equipmentModBonus = "minecraft".equals(namespace) ? 1.0D : 1.35D;
            double equipmentMultiplier = rarityMultiplier * effectBonus * enhanceBonus * reforgeBonus * equipmentModBonus * equipmentTypeBonus * equipmentJobBonus;
            price = Math.max(1L, Math.round(baseValue * equipmentMultiplier));
        } else {
            price = Math.max(1L, Math.round(baseValue * totalMultiplier / 3.5D));
        }
        
        // Add value of socketed gems
        if (equipment) {
            List<dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance> gems = com.nogeon.economyland.item.SocketUpgradeService.gems(stack);
            for (dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance gem : gems) {
                if (gem.isValid() && !gem.gemStack().isEmpty()) {
                    price += calculatePrice(kind, gem.gemStack());
                }
            }
        }

        if (ShopItemProtection.isShopPurchased(stack)) {
            price = Math.max(1L, price / 50);
        }
        return price;
    }

    private static double getApotheosisGemMultiplier(ItemStack stack) {
        if (!stack.hasTag()) return 1.0;
        String rarity = stack.getTag().getString("rarity");
        if (rarity.isEmpty() && stack.getTag().contains("affix_data", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            rarity = stack.getTag().getCompound("affix_data").getString("rarity");
        }
        if (rarity.isEmpty()) return 1.0;
        
        if (rarity.contains("mythic")) return 150.0;     // Rainbow (approx 200k+ credits)
        if (rarity.contains("ancient")) return 100.0;    // Ancient/Mythical variants
        if (rarity.contains("legendary")) return 50.0;   // Orange (approx 70k+ credits)
        if (rarity.contains("epic")) return 20.0;        // Pink (approx 28k+ credits)
        if (rarity.contains("rare")) return 8.0;         // Blue (approx 11k+ credits)
        if (rarity.contains("uncommon")) return 3.5;     // Green (approx 5k+ credits)
        return 1.5; // White
    }

    private static double enchantmentValueMultiplier(ItemStack stack, Item item, String path, int enchantCount, int totalLevel) {
        if (enchantCount <= 0 || totalLevel <= 0) {
            return 1.0D;
        }
        double rawBonus = enchantCount * 0.12D + Math.sqrt(totalLevel) * 0.18D;
        double maxBonus = isEquipment(stack, item, path) ? 1.25D : 0.60D;
        return 1.0D + Math.min(maxBonus, rawBonus);
    }

    private static int getIngredientCount(Item item) {
        if (item instanceof ArmorItem armor) {
            return switch (armor.getEquipmentSlot()) {
                case HEAD -> 5;
                case CHEST -> 8;
                case LEGS -> 7;
                case FEET -> 4;
                default -> 1;
            };
        }
        if (item instanceof net.minecraft.world.item.PickaxeItem || item instanceof net.minecraft.world.item.AxeItem) return 3;
        if (item instanceof net.minecraft.world.item.SwordItem || item instanceof net.minecraft.world.item.HoeItem) return 2;
        if (item instanceof net.minecraft.world.item.ShovelItem) return 1;
        return 1;
    }

    private static long getMaterialUnitPrice(String path) {
        if (path.contains("netherite")) return 10000;
        if (path.contains("void")) return 12000;
        if (path.contains("mystic")) return 10000;
        if (path.contains("diamond")) return 3000;
        if (path.contains("emerald")) return 2000;
        if (path.contains("gold")) return 500;
        if (path.contains("iron")) return 300;
        if (path.contains("steel")) return 800;
        return 150;
    }

    private static double rarityMultiplier(Rarity rarity) {
        if (rarity == null) return 1.0;
        String name = rarity.name();
        if ("MYSTIC".equals(name) || "MYTHIC".equals(name) || "MYTHICAL".equals(name)) return 14.0;
        if ("LEGENDARY".equals(name)) return 12.0;
        if ("EPIC".equals(name)) return 10.0;
        if ("RARE".equals(name)) return 4.5;
        if ("UNCOMMON".equals(name)) return 2.0;
        return 1.0;
    }

    private static long getBasePrice(TraderKind kind, ItemStack stack) {
        Item item = stack.getItem();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath().toLowerCase();

        // 0. 식기 및 빈 용기류 기초 가격 저렴하게 강제 조정
        if (path.contains("mug") || path.contains("cup") || path.contains("bowl") || path.equals("glass_bottle") || path.contains("empty_bottle")) {
            return 50;
        }

        // Create 모드 및 공학 부품 기초 가격 설정 (가격 역전 및 밸런싱 수정 - 가격 상향 조정)
        if (path.contains("precision_mechanism")) return 7500;
        if (path.contains("obsidian_plate") || path.contains("steel_plate") || path.contains("obsidian_sheet") || path.contains("steel_sheet")) return 3750;
        if (path.contains("golden_sheet") || path.contains("golden_plate")) return 3000;
        if (path.contains("brass_sheet") || path.contains("brass_plate")) return 1800;
        if (path.contains("gantry_shaft")) return 1500;
        if (path.contains("iron_sheet") || path.contains("iron_plate")) return 1200;
        if (path.contains("electron_tube")) return 900;
        if (path.contains("copper_sheet") || path.contains("copper_plate")) return 600;
        if (path.contains("mechanical_arm")) return 9000;
        if (path.contains("rotation_speed_controller")) return 12000;
        if (path.contains("steam_engine")) return 15000;

        // 0. 총기 및 총알 가격 연동 (고정가 버그 해결)
        if (com.nogeon.economyland.item.GunCatalog.isRegisteredGun(item)) {
            return Math.max(100L, com.nogeon.economyland.item.GunCatalog.gunPrice(item) / 6);
        }
        if (com.nogeon.economyland.item.GunCatalog.isRegisteredAmmo(item)) {
            return Math.max(10L, com.nogeon.economyland.item.GunCatalog.ammoPrice(item) / 6);
        }

        // 0.5. 강화의 보석류 기초가격 대폭 상향조정
        if (item == com.nogeon.economyland.item.ModItems.PERFECT_ENHANCEMENT_GEM.get()) return 15000000;
        if (item == com.nogeon.economyland.item.ModItems.FLAWLESS_ENHANCEMENT_GEM.get()) return 4000000;
        if (item == com.nogeon.economyland.item.ModItems.ENHANCEMENT_GEM.get()) return 850000;
        if (item == com.nogeon.economyland.item.ModItems.FLAWED_ENHANCEMENT_GEM.get()) return 100000;
        if (item == com.nogeon.economyland.item.ModItems.SPLIT_ENHANCEMENT_GEM.get()) return 40000;
        if (item == com.nogeon.economyland.item.ModItems.CRACKED_ENHANCEMENT_GEM.get()) return 9000;

        // 1. 최고가 귀중품 / 전리품
        if (item == Items.DRAGON_EGG) return 500000;
        if (item == Items.NETHER_STAR) return 600000;
        if (item == Items.ELYTRA) return 100000;
        if (item == Items.TOTEM_OF_UNDYING) return 60000;
        if (item == Items.ENCHANTED_GOLDEN_APPLE) return 20000;
        if (item == Items.HEART_OF_THE_SEA) return 30000;
        
        // 2. 압축 블록류 (9배 재료 압축)
        if (item == Items.NETHERITE_BLOCK) return 90000;
        if (item == Items.DIAMOND_BLOCK) return 27000;
        if (item == Items.EMERALD_BLOCK) return 18000;
        if (item == Items.GOLD_BLOCK || item == Items.RAW_GOLD_BLOCK) return 9000;
        if (item == Items.IRON_BLOCK || item == Items.RAW_IRON_BLOCK) return 2700;
        if (item == Items.LAPIS_BLOCK) return 1080;
        if (item == Items.COPPER_BLOCK || item == Items.RAW_COPPER_BLOCK || item == Items.REDSTONE_BLOCK) return 900;
        
        // 3. 희귀 및 가공 고급 자원
        if (item == Items.NETHERITE_INGOT) return 10000;
        if (item == Items.WITHER_SKELETON_SKULL) return 50000;
        if (item == Items.ANCIENT_DEBRIS) return 7000;
        if (item == Items.DRAGON_BREATH) return 10000;
        if (item == Items.DIAMOND || ("apotheosis".equals(id.getNamespace()) && path.contains("gem"))) return 3000;
        if (item == Items.NETHERITE_SCRAP) return 2500;
        if (item == Items.EMERALD) return 2000;
        if (item == Items.SHULKER_SHELL) return 15000;
        if (item == Items.GHAST_TEAR) return 12000;
        if (item == Items.SPONGE) return 1000;

        // [신설] 몬스터 전리품 대형 버프
        if (item == Items.ROTTEN_FLESH) return 400;
        if (item == Items.BONE) return 800;
        if (item == Items.GUNPOWDER) return 1000;
        if (item == Items.LEATHER || item == Items.STRING) return 900;
        if (item == Items.SPIDER_EYE) return 700;
        if (item == Items.FEATHER) return 600;
        if (item == Items.INK_SAC || item == Items.GLOW_INK_SAC || item == Items.RABBIT_HIDE) return 800;
        if (item == Items.ENDER_PEARL) return 3000;
        if (item == Items.BLAZE_ROD || item == Items.PHANTOM_MEMBRANE) return 3500;
        if (item == Items.SLIME_BALL || item == Items.MAGMA_CREAM) return 2000;
        if (item == Items.PRISMARINE_SHARD || item == Items.PRISMARINE_CRYSTALS) return 1200;
        if (item == Items.RABBIT_FOOT) return 2500;
        if (item == Items.SCUTE) return 15000;
        if (item == Items.SADDLE || item == Items.NAUTILUS_SHELL) return 10000;

        if (isChefBeverage(path)) {
            if (path.contains("wine")) return 2500;
            if (path.contains("mead") || path.contains("beer") || path.contains("cider")) return 1600;
            return 900;
        }
        
        // 4. 일반 광물 및 가공품
        if (item == Items.GOLD_INGOT || item == Items.RAW_GOLD) return 1000;
        if (item == Items.IRON_INGOT || item == Items.RAW_IRON) return 300;
        if (item == Items.LAPIS_LAZULI) return 120;
        if (item == Items.RAW_COPPER || item == Items.COPPER_INGOT) return 100;
        
        // 5. 음식류 기본값
        if (item.getFoodProperties() != null) {
            if (isDelightFood(id, item)) {
                return 1200;
            }
            return 250;
        }
        
        // 6. 기초 자원 대량 어뷰징 방지 분기
        if (item == Items.COBBLESTONE || item == Items.DIRT || item == Items.STONE || item == Items.NETHERRACK 
            || item == Items.SAND || item == Items.GRAVEL || item == Items.DEEPSLATE || item == Items.ANDESITE 
            || item == Items.GRANITE || item == Items.DIORITE || item == Items.BLACKSTONE || item == Items.BASALT 
            || item == Items.TUFF || item == Items.SOUL_SAND || item == Items.SOUL_SOIL || item == Items.RED_SAND
            || path.equals("cobblestone") || path.equals("dirt") || path.equals("stone") || path.equals("netherrack")
            || path.equals("sand") || path.equals("gravel") || path.equals("deepslate") || path.equals("andesite")
            || path.equals("granite") || path.equals("diorite") || path.equals("blackstone") || path.equals("basalt")
            || path.equals("tuff")) {
            return 30;
        }
        
        // 7. 가공 목재 planks
        if (path.endsWith("_planks") || path.equals("planks")) {
            return 50;
        }
        
        // 8. 광석류
        if (stack.is(Tags.Items.ORES) || path.contains("ore")) {
            return 150;
        }
        
        // 그 외 일반 블록/아이템 기본값
        return 180;
    }
}
