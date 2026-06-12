package com.nogeon.economyland.item;

import com.nogeon.economyland.player.PlayerProfile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;

public final class ReforgeService {
    public static final String REFORGE_TAG = "NoGeonReforgeSlots";
    public static final String BALANCE_VERSION_TAG = "NoGeonReforgeBalanceVersion";
    public static final int BALANCE_VERSION = 4;
    public static final int MAX_SLOTS = 3;
    public static final long BASE_ROLL_COST = 2000L;
    private static final long[] SLOT_UNLOCK_COSTS = {100000L, 250000L, 500000L};

    public enum Rarity {
        COMMON(ChatFormatting.GRAY, 0.50, 1.0, 100L),
        RARE(ChatFormatting.BLUE, 0.30, 1.8, 250L),
        UNIQUE(ChatFormatting.LIGHT_PURPLE, 0.15, 3.2, 600L),
        LEGENDARY(ChatFormatting.GOLD, 0.05, 5.5, 1500L);

        public final ChatFormatting color;
        public final double weight;
        public final double statMultiplier;
        public final long lockPenalty;

        Rarity(ChatFormatting color, double weight, double statMultiplier, long lockPenalty) {
            this.color = color;
            this.weight = weight;
            this.statMultiplier = statMultiplier;
            this.lockPenalty = lockPenalty;
        }

        public static Rarity roll(RandomSource random) {
            double r = random.nextDouble();
            double cumulative = 0;
            for (Rarity rarity : values()) {
                cumulative += rarity.weight;
                if (r < cumulative) return rarity;
            }
            return COMMON;
        }

        public static Rarity safe(String name) {
            if (name != null && !name.isBlank()) {
                try {
                    return valueOf(name);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return COMMON;
        }
    }

    public static record Modifier(String id, Attribute attribute, double baseValue, String translationKey, String descriptionKey) {}

    public static final List<Modifier> MELEE_MODS = List.of(
        new Modifier("melee_damage", null, 0.08, "reforge.mod.melee_damage", "reforge.desc.melee_damage"),
        new Modifier("melee_speed", Attributes.ATTACK_SPEED, 0.07, "reforge.mod.melee_speed", "reforge.desc.melee_speed"),
        new Modifier("crit_chance", null, 0.06, "reforge.mod.crit_chance", "reforge.desc.crit_chance"),
        new Modifier("knockback", Attributes.ATTACK_KNOCKBACK, 0.25, "reforge.mod.knockback", "reforge.desc.knockback"),
        new Modifier("lifesteal", null, 0.03, "reforge.mod.lifesteal", "reforge.desc.lifesteal"),
        new Modifier("execution", null, 0.08, "reforge.mod.execution", "reforge.desc.execution"),
        new Modifier("stun", null, 0.035, "reforge.mod.stun", "reforge.desc.stun"),
        new Modifier("piercing", null, 0.07, "reforge.mod.piercing", "reforge.desc.piercing")
    );
 
    public static final List<Modifier> ARMOR_MODS = List.of(
        new Modifier("armor", Attributes.ARMOR, 0.03, "reforge.mod.armor", "reforge.desc.armor"),
        new Modifier("toughness", Attributes.ARMOR_TOUGHNESS, 0.03, "reforge.mod.toughness", "reforge.desc.toughness"),
        new Modifier("max_health", Attributes.MAX_HEALTH, 0.035, "reforge.mod.max_health", "reforge.desc.max_health"),
        new Modifier("movement_speed", Attributes.MOVEMENT_SPEED, 0.01, "reforge.mod.movement_speed", "reforge.desc.movement_speed"),
        new Modifier("reflect", null, 0.02, "reforge.mod.reflect", "reforge.desc.reflect"),
        new Modifier("vitality", null, 0.03, "reforge.mod.vitality", "reforge.desc.vitality"),
        new Modifier("evasion", null, 0.015, "reforge.mod.evasion", "reforge.desc.evasion")
    );
 
    public static final List<Modifier> RANGE_MODS = List.of(
        new Modifier("projectile_damage", null, 0.06, "reforge.mod.projectile_damage", "reforge.desc.projectile_damage"),
        new Modifier("reload_mastery", null, 0.05, "reforge.mod.reload_mastery", "reforge.desc.reload_mastery"),
        new Modifier("rapid_fire", null, 0.04, "reforge.mod.rapid_fire", "reforge.desc.rapid_fire"),
        new Modifier("recoil_control", null, 0.06, "reforge.mod.recoil_control", "reforge.desc.recoil_control"),
        new Modifier("piercing", null, 0.05, "reforge.mod.piercing", "reforge.desc.piercing"),
        new Modifier("crit_chance", null, 0.05, "reforge.mod.crit_chance", "reforge.desc.crit_chance"),
        new Modifier("lifesteal", null, 0.02, "reforge.mod.lifesteal", "reforge.desc.lifesteal"),
        new Modifier("execution", null, 0.05, "reforge.mod.execution", "reforge.desc.execution"),
        new Modifier("stun", null, 0.025, "reforge.mod.stun", "reforge.desc.stun"),
        new Modifier("luck", Attributes.LUCK, 0.04, "reforge.mod.luck", "reforge.desc.luck")
    );

    public static final List<Modifier> BOW_MODS = List.of(
        new Modifier("projectile_damage", null, 0.06, "reforge.mod.projectile_damage", "reforge.desc.projectile_damage"),
        new Modifier("rapid_fire", null, 0.04, "reforge.mod.rapid_fire", "reforge.desc.rapid_fire"),
        new Modifier("draw_speed", null, 0.05, "reforge.mod.draw_speed", "reforge.desc.draw_speed"),
        new Modifier("arrow_velocity", null, 0.06, "reforge.mod.arrow_velocity", "reforge.desc.arrow_velocity"),
        new Modifier("piercing", null, 0.05, "reforge.mod.piercing", "reforge.desc.piercing"),
        new Modifier("crit_chance", null, 0.06, "reforge.mod.crit_chance", "reforge.desc.crit_chance"),
        new Modifier("lifesteal", null, 0.025, "reforge.mod.lifesteal", "reforge.desc.lifesteal"),
        new Modifier("execution", null, 0.06, "reforge.mod.execution", "reforge.desc.execution"),
        new Modifier("stun", null, 0.03, "reforge.mod.stun", "reforge.desc.stun"),
        new Modifier("luck", Attributes.LUCK, 0.05, "reforge.mod.luck", "reforge.desc.luck")
    );

    // Pickaxe Reforge Pool: Common + High-tier Mineral/Gem options
    private static final List<Modifier> PICKAXE_MODS = List.of(
        new Modifier("mining_speed", null, 0.06, "reforge.mod.mining_speed", "reforge.desc.mining_speed"),
        new Modifier("durability_thrift", null, 0.05, "reforge.mod.durability_thrift", "reforge.desc.durability_thrift"),
        new Modifier("double_drop", null, 0.04, "reforge.mod.double_drop", "reforge.desc.double_drop"),
        new Modifier("mineral_finder", null, 0.05, "reforge.mod.mineral_finder", "reforge.desc.mineral_finder"),
        new Modifier("gemstone_hunter", null, 0.06, "reforge.mod.gemstone_hunter", "reforge.desc.gemstone_hunter")
    );

    // Shovel Reforge Pool: Common + Dirt/Sand/Clay Treasure options
    private static final List<Modifier> SHOVEL_MODS = List.of(
        new Modifier("mining_speed", null, 0.06, "reforge.mod.mining_speed", "reforge.desc.mining_speed"),
        new Modifier("durability_thrift", null, 0.05, "reforge.mod.durability_thrift", "reforge.desc.durability_thrift"),
        new Modifier("double_drop", null, 0.04, "reforge.mod.double_drop", "reforge.desc.double_drop"),
        new Modifier("treasure_digger", null, 0.04, "reforge.mod.treasure_digger", "reforge.desc.treasure_digger"),
        new Modifier("clay_master", null, 0.08, "reforge.mod.clay_master", "reforge.desc.clay_master")
    );

    // Axe Reforge Pool: Common + Logging speed & Buff options
    private static final List<Modifier> AXE_MODS = List.of(
        new Modifier("mining_speed", null, 0.06, "reforge.mod.mining_speed", "reforge.desc.mining_speed"),
        new Modifier("durability_thrift", null, 0.05, "reforge.mod.durability_thrift", "reforge.desc.durability_thrift"),
        new Modifier("double_drop", null, 0.04, "reforge.mod.double_drop", "reforge.desc.double_drop"),
        new Modifier("lumberjack", null, 0.06, "reforge.mod.lumberjack", "reforge.desc.lumberjack"),
        new Modifier("tree_sap", null, 0.05, "reforge.mod.tree_sap", "reforge.desc.tree_sap")
    );

    // Hoe Reforge Pool: Common + Farming Yield & Auto-seed options
    public static final List<Modifier> HOE_MODS = List.of(
        new Modifier("mining_speed", null, 0.06, "reforge.mod.mining_speed", "reforge.desc.mining_speed"),
        new Modifier("durability_thrift", null, 0.05, "reforge.mod.durability_thrift", "reforge.desc.durability_thrift"),
        new Modifier("double_drop", null, 0.04, "reforge.mod.double_drop", "reforge.desc.double_drop"),
        new Modifier("bountiful_harvest", null, 0.08, "reforge.mod.bountiful_harvest", "reforge.desc.bountiful_harvest"),
        new Modifier("replanting", null, 0.15, "reforge.mod.replanting", "reforge.desc.replanting")
    );

    // Fishing Rod Reforge Pool: Custom fishing modifiers
    public static final List<Modifier> FISHING_ROD_MODS = List.of(
        new Modifier("fishing_speed", null, 0.08, "reforge.mod.fishing_speed", "reforge.desc.fishing_speed"),
        new Modifier("durability_thrift", null, 0.05, "reforge.mod.durability_thrift", "reforge.desc.durability_thrift"),
        new Modifier("double_catch", null, 0.04, "reforge.mod.double_catch", "reforge.desc.double_catch"),
        new Modifier("rare_fish_finder", null, 0.05, "reforge.mod.rare_fish_finder", "reforge.desc.rare_fish_finder"),
        new Modifier("treasure_hunter", null, 0.05, "reforge.mod.treasure_hunter", "reforge.desc.treasure_hunter")
    );

    private static Attribute getAttributeSafe(String namespace, String path) {
        try {
            return net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(new net.minecraft.resources.ResourceLocation(namespace, path));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static final List<Modifier> MAGIC_MODS = List.of(
        new Modifier("spell_power", getAttributeSafe("irons_spellbooks", "spell_power"), 0.05, "reforge.mod.spell_power", "reforge.desc.spell_power"),
        new Modifier("mana_regen", getAttributeSafe("irons_spellbooks", "mana_regen"), 0.08, "reforge.mod.mana_regen", "reforge.desc.mana_regen"),
        new Modifier("max_mana", getAttributeSafe("irons_spellbooks", "max_mana"), 0.08, "reforge.mod.max_mana", "reforge.desc.max_mana"),
        new Modifier("cooldown_reduction", getAttributeSafe("irons_spellbooks", "cooldown_reduction"), 0.05, "reforge.mod.cooldown_reduction", "reforge.desc.cooldown_reduction"),
        new Modifier("cast_time_reduction", getAttributeSafe("irons_spellbooks", "cast_time_reduction"), 0.06, "reforge.mod.cast_time_reduction", "reforge.desc.cast_time_reduction")
    );

    public static final List<Modifier> CURIO_MODS = List.of(
        new Modifier("curio_movement_speed", Attributes.MOVEMENT_SPEED, 0.005, "reforge.mod.movement_speed", "reforge.desc.movement_speed"),
        new Modifier("curio_max_health", Attributes.MAX_HEALTH, 0.01, "reforge.mod.max_health", "reforge.desc.max_health"),
        new Modifier("curio_attack_damage", Attributes.ATTACK_DAMAGE, 0.01, "reforge.mod.melee_damage", "reforge.desc.melee_damage"),
        new Modifier("curio_attack_speed", Attributes.ATTACK_SPEED, 0.01, "reforge.mod.melee_speed", "reforge.desc.melee_speed"),
        new Modifier("curio_armor", Attributes.ARMOR, 0.008, "reforge.mod.armor", "reforge.desc.armor"),
        new Modifier("curio_toughness", Attributes.ARMOR_TOUGHNESS, 0.008, "reforge.mod.toughness", "reforge.desc.toughness"),
        new Modifier("curio_knockback_resistance", Attributes.KNOCKBACK_RESISTANCE, 0.02, "reforge.mod.knockback_resistance", "reforge.desc.knockback_resistance"),
        new Modifier("curio_luck", Attributes.LUCK, 0.015, "reforge.mod.luck", "reforge.desc.luck"),
        new Modifier("curio_spell_power", getAttributeSafe("irons_spellbooks", "spell_power"), 0.015, "reforge.mod.spell_power", "reforge.desc.spell_power"),
        new Modifier("curio_mana_regen", getAttributeSafe("irons_spellbooks", "mana_regen"), 0.02, "reforge.mod.mana_regen", "reforge.desc.mana_regen"),
        new Modifier("curio_cooldown_reduction", getAttributeSafe("irons_spellbooks", "cooldown_reduction"), 0.01, "reforge.mod.cooldown_reduction", "reforge.desc.cooldown_reduction"),
        new Modifier("curio_spell_crit_chance", getAttributeSafe("irons_spellbooks", "spell_crit_chance"), 0.015, "reforge.mod.spell_crit_chance", "reforge.desc.spell_crit_chance"),
        new Modifier("curio_mana_cost", getAttributeSafe("irons_spellbooks", "mana_cost"), -0.015, "reforge.mod.mana_cost", "reforge.desc.mana_cost")
    );

    private ReforgeService() {}

    public static List<Modifier> getPool(ItemStack stack) {
        if (stack.isEmpty()) return List.of();
        
        if (SmithingService.isCurio(stack)) return CURIO_MODS;
        
        var item = stack.getItem();
        if (GunCatalog.isRegisteredGun(item)) return RANGE_MODS;
        if (item instanceof ArmorItem) return ARMOR_MODS;
        if (item instanceof BowItem || item instanceof CrossbowItem) return BOW_MODS;
        
        // Tool pools prioritized over weapon pools (since Axe is also a MeleeWeapon)
        if (item instanceof PickaxeItem) return PICKAXE_MODS;
        if (item instanceof ShovelItem) return SHOVEL_MODS;
        if (item instanceof AxeItem) return AXE_MODS;
        if (item instanceof HoeItem) return HOE_MODS;
        if (item instanceof net.minecraft.world.item.FishingRodItem) return FISHING_ROD_MODS;
        
        if (SmithingService.isMagicEquipment(stack)) return MAGIC_MODS;
        if (SmithingService.isMeleeWeapon(stack)) return MELEE_MODS;
        return List.of();
    }

    public static int normalizeSelectedSlot(ServerPlayer player, int slot) {
        if (slot >= 0 && slot < player.getInventory().getContainerSize() && !getPool(player.getInventory().getItem(slot)).isEmpty()) {
            return slot;
        }
        for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
            if (!getPool(player.getInventory().getItem(index)).isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    public static UUID reforgeUuid(EquipmentSlot slot, int index) {
        String seed = "nogeon_economy_land:reforge/" + slot.getName() + "/" + clampSlot(index);
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private static int clampSlot(int index) {
        return Math.max(0, Math.min(MAX_SLOTS - 1, index));
    }

    public static int getUnlockedCount(ItemStack stack) {
        return stack.getOrCreateTag().getList(REFORGE_TAG, Tag.TAG_COMPOUND).size();
    }

    public static boolean migrateBalance(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        CompoundTag root = stack.getTag();
        if (root == null || root.getInt(BALANCE_VERSION_TAG) >= BALANCE_VERSION || !root.contains(REFORGE_TAG, Tag.TAG_LIST)) {
            return false;
        }

        boolean changed = false;
        ListTag list = root.getList(REFORGE_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slot = list.getCompound(i);
            String modId = slot.getString("ModifierId");
            if (modId.isBlank() || "none".equals(modId)) {
                continue;
            }
            Modifier mod = findModifier(stack, modId);
            if (mod == null) {
                continue;
            }

            Rarity rarity = Rarity.safe(slot.getString("Rarity"));
            double expected = mod.baseValue() * rarity.statMultiplier;
            double min = expected * 0.8D;
            double max = expected * 1.2D;
            double value = slot.getDouble("Value");
            double migrated = Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : expected;
            if (Math.abs(migrated - value) > 0.0000001D) {
                slot.putDouble("Value", migrated);
                changed = true;
            }
        }

        root.putInt(BALANCE_VERSION_TAG, BALANCE_VERSION);
        return true;
    }

    private static Modifier findModifier(ItemStack stack, String modifierId) {
        for (Modifier mod : getPool(stack)) {
            if (mod.id().equals(modifierId)) {
                return mod;
            }
        }
        for (List<Modifier> pool : List.of(MELEE_MODS, ARMOR_MODS, RANGE_MODS, BOW_MODS, PICKAXE_MODS, SHOVEL_MODS, AXE_MODS, HOE_MODS, FISHING_ROD_MODS, MAGIC_MODS, CURIO_MODS)) {
            for (Modifier mod : pool) {
                if (mod.id().equals(modifierId)) {
                    return mod;
                }
            }
        }
        return null;
    }

    public static long getRollCost(ItemStack stack) {
        long cost = BASE_ROLL_COST;
        ListTag list = stack.getOrCreateTag().getList(REFORGE_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (tag.getBoolean("Locked")) {
                cost += Rarity.safe(tag.getString("Rarity")).lockPenalty;
            }
        }
        return cost;
    }

    public static long getUnlockCost(ItemStack stack) {
        int count = Math.min(getUnlockedCount(stack), SLOT_UNLOCK_COSTS.length - 1);
        return SLOT_UNLOCK_COSTS[count];
    }

    public static Component tryUnlock(ServerPlayer player, PlayerProfile profile, ItemStack stack) {
        if (stack.getCount() > 1) {
            return Component.literal("§c재련/해제할 장비는 1개씩만 올려둘 수 있습니다.");
        }
        int count = getUnlockedCount(stack);
        if (count >= MAX_SLOTS) return Component.translatable("message.nogeon_economy_land.reforge.all_slots_unlocked").withStyle(ChatFormatting.RED);
        if (getPool(stack).isEmpty()) return Component.translatable("message.nogeon_economy_land.reforge.invalid_item").withStyle(ChatFormatting.RED);
        
        long unlockCost = getUnlockCost(stack);
        if (!profile.spendCredits(unlockCost)) return Component.translatable("message.nogeon_economy_land.reforge.no_money", unlockCost).withStyle(ChatFormatting.RED);
        
        ListTag list = stack.getOrCreateTag().getList(REFORGE_TAG, Tag.TAG_COMPOUND);
        CompoundTag newSlot = new CompoundTag();
        newSlot.putInt("SlotIndex", count);
        newSlot.putString("ModifierId", "none");
        newSlot.putDouble("Value", 0);
        newSlot.putString("Rarity", Rarity.COMMON.name());
        newSlot.putBoolean("Locked", false);
        list.add(newSlot);
        CompoundTag root = stack.getOrCreateTag();
        root.put(REFORGE_TAG, list);
        root.putInt(BALANCE_VERSION_TAG, BALANCE_VERSION);
        
        player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0F, 1.2F);
        return Component.translatable("message.nogeon_economy_land.reforge.slot_unlocked", count + 1).withStyle(ChatFormatting.GREEN);
    }

    public static Component tryRoll(ServerPlayer player, PlayerProfile profile, ItemStack stack) {
        return tryRoll(player, profile, stack, false);
    }

    public static Component tryRoll(ServerPlayer player, PlayerProfile profile, ItemStack stack, boolean silent) {
        if (stack.getCount() > 1) {
            return Component.literal("§c재련/해제할 장비는 1개씩만 올려둘 수 있습니다.");
        }
        int count = getUnlockedCount(stack);
        if (count <= 0) return Component.translatable("message.nogeon_economy_land.reforge.unlock_first").withStyle(ChatFormatting.RED);
        
        long cost = getRollCost(stack);
        if (!profile.spendCredits(cost)) return Component.translatable("message.nogeon_economy_land.reforge.no_money", cost).withStyle(ChatFormatting.RED);
        
        List<Modifier> pool = getPool(stack);
        RandomSource random = player.getRandom();
        ListTag list = stack.getOrCreateTag().getList(REFORGE_TAG, Tag.TAG_COMPOUND);
        boolean hasHighTier = false;

        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (!tag.getBoolean("Locked")) {
                Rarity rarity = Rarity.roll(random);
                Modifier mod = pool.get(random.nextInt(pool.size()));
                
                // Variable Range: Base * Rarity * (0.8 ~ 1.2 variance)
                double variance = 0.8 + random.nextDouble() * 0.4;
                double val = mod.baseValue * rarity.statMultiplier * variance;
                
                tag.putString("ModifierId", mod.id);
                tag.putDouble("Value", val);
                tag.putString("Rarity", rarity.name());
                stack.getOrCreateTag().putInt(BALANCE_VERSION_TAG, BALANCE_VERSION);
                
                if (rarity == Rarity.UNIQUE || rarity == Rarity.LEGENDARY) hasHighTier = true;
            }
        }
        
        if (!silent) {
            if (hasHighTier) {
                playHighTierEffect(player);
            } else {
                player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 1.1F);
            }
        }
        
        return Component.translatable("message.nogeon_economy_land.reforge.complete").withStyle(ChatFormatting.GOLD);
    }

    public static void toggleLock(ItemStack stack, int slotIndex) {
        ListTag list = stack.getOrCreateTag().getList(REFORGE_TAG, Tag.TAG_COMPOUND);
        if (slotIndex >= 0 && slotIndex < list.size()) {
            CompoundTag tag = list.getCompound(slotIndex);
            tag.putBoolean("Locked", !tag.getBoolean("Locked"));
        }
    }

    private static void playHighTierEffect(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.5, player.getZ(), 20, 0.5, 0.5, 0.5, 0.15);
        }
    }
}
