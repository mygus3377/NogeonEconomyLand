package com.nogeon.economyland.item;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.shop.ShopEntry;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

public final class GunCatalog {
    private static final List<String> TIER_1_AMMO_IDS = List.of(
        "tacz:9mm",
        "tacz:45acp",
        "tacz:762x25",
        "tacz:357mag",
        "tacz:50ae",
        "tacz:57x28",
        "tacz:46x30",
        "tacz:12g"
    );

    private static final List<String> HANDGUN_KEYWORDS = List.of("pistol", "revolver", "handgun", "sidearm");
    private static final List<String> GUN_KEYWORDS = List.of("gun", "pistol", "revolver", "rifle", "smg", "shotgun", "carbine", "sniper");
    private static final List<String> AMMO_KEYWORDS = List.of("ammo", "bullet", "shell", "cartridge", "round", "mag", "magazine");
    private static final List<String> CALIBER_KEYWORDS = List.of("9mm", "45acp", "556", "762", "12g", "308", "50bmg", "57");
    private static final List<String> EXCLUDED_KEYWORDS = List.of("attachment", "scope", "sight", "stock", "barrel", "grip", "suppressor", "silencer", "skin", "crate", "box", "case", "blueprint", "part", "kit");
    private static final List<String> INVALID_GUN_KEYWORDS = List.of("blaster", "launcher", "bazooka", "rocket", "cannon");

    private static boolean initialized;
    private static List<Item> cachedGuns = List.of();
    private static List<Item> cachedAmmo = List.of();

    private GunCatalog() {
    }

    public static List<ShopEntry> shopEntries() {
        ensureScanned();
        List<ShopEntry> taczEntries = taczAmmoShopEntries();
        if (!taczEntries.isEmpty()) {
            return taczEntries;
        }
        if (cachedAmmo.isEmpty()) {
            return fallbackAmmoShopEntries();
        }

        List<ShopEntry> entries = new ArrayList<>();
        for (int index = 0; index < cachedAmmo.size(); index++) {
            Item ammo = cachedAmmo.get(index);
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(ammo);
            entries.add(new ShopEntry("ammo_" + sanitize(id), new ItemStack(ammo, ammoBundleSize(ammo)), ammoPrice(ammo), 32));
        }
        return entries;
    }

    private static List<ShopEntry> taczAmmoShopEntries() {
        Item ammoItem = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("tacz:ammo"));
        if (ammoItem == Items.AIR) {
            return List.of();
        }

        List<Map.Entry<ResourceLocation, CommonAmmoIndex>> ammoIndexes = new ArrayList<>(TimelessAPI.getAllCommonAmmoIndex());
        ammoIndexes.sort(Map.Entry.comparingByKey());
        List<ShopEntry> entries = new ArrayList<>(ammoIndexes.size());
        for (Map.Entry<ResourceLocation, CommonAmmoIndex> ammoIndex : ammoIndexes) {
            String ammoIdStr = ammoIndex.getKey().toString();
            if (!isTier1Ammo(ammoIdStr)) {
                continue;
            }
            ItemStack stack = new ItemStack(ammoItem);
            IAmmo ammo = IAmmo.getIAmmoOrNull(stack);
            if (ammo == null) {
                continue;
            }
            ammo.setAmmoId(stack, ammoIndex.getKey());
            stack.setCount(Math.max(1, Math.min(stack.getMaxStackSize(), ammoIndex.getValue().getStackSize())));
            entries.add(new ShopEntry("ammo_tacz_" + sanitize(ammoIndex.getKey()), stack, ammoPrice(ammoItem), 32));
        }
        return entries;
    }

    public static List<Item> gachaItems(int band) {
        ensureScanned();
        if (cachedGuns.isEmpty()) {
            return List.of();
        }
        int size = cachedGuns.size();
        int bandThreeEnd = Math.max(1, (int) Math.ceil(size * 0.2D));
        int bandTwoEnd = Math.max(bandThreeEnd + 1, (int) Math.ceil(size * 0.5D));
        int bandOneEnd = Math.max(bandTwoEnd + 1, (int) Math.ceil(size * 0.8D));
        return switch (band) {
            case 3 -> slice(0, bandThreeEnd, size);
            case 2 -> slice(bandThreeEnd, bandTwoEnd, size);
            case 1 -> slice(bandTwoEnd, bandOneEnd, size);
            default -> slice(bandOneEnd, size, size);
        };
    }

    public static List<ItemStack> gachaStacks(int band) {
        List<ItemStack> taczGuns = taczGachaStacks();
        if (!taczGuns.isEmpty()) {
            int size = taczGuns.size();
            int bandThreeEnd = Math.max(1, (int) Math.ceil(size * 0.2D));
            int bandTwoEnd = Math.max(bandThreeEnd + 1, (int) Math.ceil(size * 0.5D));
            int bandOneEnd = Math.max(bandTwoEnd + 1, (int) Math.ceil(size * 0.8D));
            return switch (band) {
                case 3 -> stackSlice(taczGuns, 0, bandThreeEnd, size);
                case 2 -> stackSlice(taczGuns, bandThreeEnd, bandTwoEnd, size);
                case 1 -> stackSlice(taczGuns, bandTwoEnd, bandOneEnd, size);
                default -> stackSlice(taczGuns, bandOneEnd, size, size);
            };
        }

        List<Item> items = gachaItems(band);
        List<ItemStack> stacks = new ArrayList<>(items.size());
        for (Item item : items) {
            stacks.add(new ItemStack(item));
        }
        return stacks;
    }

    public static boolean isRegisteredGun(Item item) {
        ensureScanned();
        return cachedGuns.contains(item);
    }

    public static boolean isRegisteredAmmo(Item item) {
        ensureScanned();
        return cachedAmmo.contains(item);
    }

    private static List<Item> slice(int startInclusive, int endExclusive, int size) {
        if (startInclusive >= size) {
            return List.of(cachedGuns.get(size - 1));
        }
        int safeEnd = Math.max(startInclusive + 1, Math.min(size, endExclusive));
        return cachedGuns.subList(startInclusive, safeEnd);
    }

    private static List<ItemStack> stackSlice(List<ItemStack> stacks, int startInclusive, int endExclusive, int size) {
        if (startInclusive >= size) {
            return List.of(stacks.get(size - 1).copy());
        }
        int safeEnd = Math.max(startInclusive + 1, Math.min(size, endExclusive));
        List<ItemStack> slice = new ArrayList<>(safeEnd - startInclusive);
        for (int index = startInclusive; index < safeEnd; index++) {
            slice.add(stacks.get(index).copy());
        }
        return slice;
    }

    private static List<ItemStack> taczGachaStacks() {
        Item gunItem = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("tacz:modern_kinetic_gun"));
        if (gunItem == Items.AIR) {
            return List.of();
        }

        List<Map.Entry<ResourceLocation, CommonGunIndex>> gunIndexes = new ArrayList<>(TimelessAPI.getAllCommonGunIndex());
        gunIndexes.sort(Comparator.comparingInt((Map.Entry<ResourceLocation, CommonGunIndex> entry) -> entry.getValue().getSort())
            .reversed()
            .thenComparing(entry -> entry.getKey().toString()));

        List<ItemStack> stacks = new ArrayList<>(gunIndexes.size());
        for (Map.Entry<ResourceLocation, CommonGunIndex> gunIndex : gunIndexes) {
            ItemStack stack = new ItemStack(gunItem);
            IGun gun = IGun.getIGunOrNull(stack);
            if (gun == null) {
                continue;
            }
            ResourceLocation gunId = gunIndex.getKey();
            gun.setGunId(stack, gunId);
            gun.setGunDisplayId(stack, gunId);
            String nameKey = gunIndex.getValue().getPojo().getName();
            if (nameKey != null && !nameKey.isBlank()) {
                stack.setHoverName(Component.translatable(nameKey));
            }
            stacks.add(stack);
        }
        return stacks;
    }

    private static void ensureScanned() {
        if (initialized) {
            return;
        }
        List<Item> guns = new ArrayList<>();
        List<Item> ammo = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!isExternalItem(item, id)) {
                continue;
            }
            if (isGun(item, id)) {
                guns.add(item);
            } else if (isAmmo(item, id)) {
                ammo.add(item);
            }
        }
        guns.sort(Comparator.comparingInt(GunCatalog::gunScore).reversed().thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        ammo.sort(Comparator.comparingInt(GunCatalog::ammoScore).reversed().thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        cachedGuns = List.copyOf(guns);
        cachedAmmo = List.copyOf(ammo);
        initialized = true;
    }

    private static boolean isExternalItem(Item item, ResourceLocation id) {
        if (id == null || item == Items.AIR || item instanceof SpawnEggItem || item instanceof BlockItem) {
            return false;
        }
        String namespace = id.getNamespace();
        return !ResourceLocation.DEFAULT_NAMESPACE.equals(namespace) && !NoGeonEconomyLand.MOD_ID.equals(namespace);
    }

    private static boolean isGun(Item item, ResourceLocation id) {
        String token = token(id);
        if (containsAny(token, EXCLUDED_KEYWORDS) || containsAny(token, INVALID_GUN_KEYWORDS) || item.getMaxStackSize() != 1) {
            return false;
        }
        if (containsAny(token, HANDGUN_KEYWORDS) || containsAny(token, GUN_KEYWORDS)) {
            return true;
        }
        return likelyGunNamespace(id) && new ItemStack(item).isDamageableItem();
    }

    private static boolean isAmmo(Item item, ResourceLocation id) {
        String token = token(id);
        if (containsAny(token, EXCLUDED_KEYWORDS) || item.getMaxStackSize() <= 1) {
            return false;
        }
        return containsAny(token, AMMO_KEYWORDS) || (likelyGunNamespace(id) && containsAny(token, CALIBER_KEYWORDS));
    }

    private static boolean likelyGunNamespace(ResourceLocation id) {
        String namespace = id.getNamespace().toLowerCase(Locale.ROOT);
        return namespace.contains("gun") || namespace.contains("weapon") || namespace.contains("firearm") || namespace.contains("tacz");
    }

    private static boolean isHandgun(Item item) {
        return containsAny(token(BuiltInRegistries.ITEM.getKey(item)), HANDGUN_KEYWORDS);
    }

    private static int gunScore(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String token = token(id);
        int score = new ItemStack(item).getMaxDamage();
        if (containsAny(token, List.of("sniper", "anti_material", "50bmg"))) {
            score += 140;
        }
        if (containsAny(token, List.of("rifle", "battle_rifle", "dmr"))) {
            score += 110;
        }
        if (containsAny(token, List.of("shotgun", "smg", "carbine"))) {
            score += 90;
        }
        if (containsAny(token, HANDGUN_KEYWORDS)) {
            score += 70;
        }
        return score;
    }

    private static int ammoScore(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String token = token(id);
        int score = item.getMaxStackSize();
        if (containsAny(token, CALIBER_KEYWORDS)) {
            score += 40;
        }
        if (containsAny(token, AMMO_KEYWORDS)) {
            score += 20;
        }
        return score;
    }

    public static long gunPrice(Item item) {
        int score = gunScore(item);
        if (score >= 200) {
            return 26000L;
        }
        if (score >= 140) {
            return 18000L;
        }
        if (score >= 90) {
            return 12000L;
        }
        return 7800L;
    }

    public static long ammoPrice(Item item) {
        int score = ammoScore(item);
        return score >= 70 ? 70L : 40L;
    }

    private static int ammoBundleSize(Item item) {
        return 1;
    }

    private static boolean containsAny(String token, List<String> needles) {
        for (String needle : needles) {
            if (token.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String token(ResourceLocation id) {
        return id.toString().toLowerCase(Locale.ROOT);
    }

    private static String sanitize(ResourceLocation id) {
        return id.toString().replace(':', '_').replace('/', '_');
    }

    private static List<ShopEntry> fallbackAmmoShopEntries() {
        return List.of(
            new ShopEntry("ammo_fallback_arrow", Items.ARROW, 24, 900, 64),
            new ShopEntry("ammo_fallback_firework", Items.FIREWORK_ROCKET, 12, 1500, 32)
        );
    }

    private static boolean isTier1Ammo(String ammoId) {
        for (String t1 : TIER_1_AMMO_IDS) {
            if (ammoId.equals(t1) || ammoId.endsWith(":" + t1) || t1.endsWith(":" + ammoId)) {
                return true;
            }
        }
        return false;
    }
}
