package com.nogeon.economyland.item;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.job.JobEvents;
import com.nogeon.economyland.network.EnhanceHitVfxPacket;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.resources.ResourceLocation;

@Mod.EventBusSubscriber(modid = NoGeonEconomyLand.MOD_ID)
public final class SmithEvents {
    private static final ResourceLocation GUNSMITH_RPM = ResourceLocation.parse("gunsmithlib:rpm");
    private static final ResourceLocation PUFFISH_RANGED_RESISTANCE = ResourceLocation.parse("puffish_attributes:player.ranged_resistance");
    private static final double RANGED_RESISTANCE_SOFT_CAP = 50.0D;
    private static final double RANGED_RESISTANCE_HARD_CAP = 99.0D;
    private static final float RANGED_PVP_DAMAGE_FLOOR_MULTIPLIER = 0.01F;
    private static final float RANGED_PVP_MIN_DAMAGE = 0.5F;
    private static final float PVP_DAMAGE_FLOOR_MULTIPLIER = 0.01F;
    private static final float PVP_MIN_DAMAGE = 0.5F;
    private static final double PVP_ARMOR_PIERCE_CAP = 0.65D;
    private static final float PVE_DAMAGE_FLOOR_MULTIPLIER = 0.03F;
    private static final float PVE_STRONG_DAMAGE_FLOOR_MULTIPLIER = 0.10F;
    private static final float PVE_MIN_DAMAGE = 0.25F;
    private static final float PVE_STRONG_MIN_DAMAGE = 1.0F;
    private static final Map<UUID, RangedDamageSnapshot> RANGED_DAMAGE_SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, RangedDamageSnapshot> RANGED_DAMAGE_FLOORS = new HashMap<>();
    private static final Map<UUID, RangedDamageSnapshot> PVE_DAMAGE_SNAPSHOTS = new HashMap<>();
    private static final Map<UUID, Integer> LIFESTEAL_COOLDOWNS = new HashMap<>();
    private static final String PROJECTILE_DAMAGE_TAG = "NoGeonReforgeProjectileDamage";
    private static final String PROJECTILE_PIERCING_TAG = "NoGeonReforgeProjectilePiercing";

    private SmithEvents() {
    }

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        ItemStack bow = event.getBow();
        double drawSpeed = reforgeValue(bow, "draw_speed") + reforgeValue(bow, "rapid_fire");
        if (drawSpeed > 0) {
            event.setCharge((int) Math.min(Integer.MAX_VALUE, Math.round(event.getCharge() * (1.0D + drawSpeed))));
        }
    }

    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getOwner() instanceof LivingEntity owner)) {
            return;
        }
        ItemStack weapon = rangedWeapon(owner);
        if (weapon.isEmpty()) {
            return;
        }

        double velocity = reforgeValue(weapon, "arrow_velocity");
        if (velocity > 0) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.0D + velocity));
        }

        double projectileDamage = reforgeValue(weapon, "projectile_damage");
        if (projectileDamage > 0) {
            arrow.getPersistentData().putDouble(PROJECTILE_DAMAGE_TAG, projectileDamage);
        }
        double piercing = reforgeValue(weapon, "piercing");
        if (piercing > 0) {
            arrow.getPersistentData().putDouble(PROJECTILE_PIERCING_TAG, piercing);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rememberRangedPvpDamage(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        LivingEntity attacker = attacker(event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (victim instanceof net.minecraft.world.entity.player.Player
            && attacker instanceof net.minecraft.world.entity.player.Player
            && isRangedSource(event.getSource())) {
            RANGED_DAMAGE_SNAPSHOTS.put(victim.getUUID(), new RangedDamageSnapshot(event.getAmount(), victim.tickCount));
        }
    }

    @SubscribeEvent
    public static void onAttributeModifier(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        int level = SmithingService.level(stack);
        
        // Smithing Bonuses
        if (level > 0) {
            if (SmithingService.isArmor(stack)) {
                EquipmentSlot slot = stack.getItem() instanceof ArmorItem armorItem ? armorItem.getEquipmentSlot() : EquipmentSlot.CHEST;
                if (event.getSlotType() == slot) {
                    event.addModifier(Attributes.ARMOR, new AttributeModifier(SmithingService.ARMOR_UUIDS[slot.getIndex()], "nogeon.smith.armor", SmithingService.armorBonus(level), AttributeModifier.Operation.MULTIPLY_TOTAL));
                    event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(SmithingService.TOUGHNESS_UUIDS[slot.getIndex()], "nogeon.smith.toughness", SmithingService.toughnessBonus(level), AttributeModifier.Operation.MULTIPLY_TOTAL));
                    event.addModifier(Attributes.MAX_HEALTH, new AttributeModifier(SmithingService.MAX_HEALTH_UUIDS[slot.getIndex()], "nogeon.smith.max_health", SmithingService.maxHealthBonus(level), AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            } else if (SmithingService.isMeleeWeapon(stack)) {
                if (event.getSlotType() == EquipmentSlot.MAINHAND) {
                    event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(SmithingService.WEAPON_FLAT_DAMAGE_UUID, "nogeon.smith.flat_damage", SmithingService.weaponFlatDamageBonus(level), AttributeModifier.Operation.ADDITION));
                    event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(SmithingService.WEAPON_DAMAGE_UUID, "nogeon.smith.damage", SmithingService.weaponDamageBonus(level), AttributeModifier.Operation.MULTIPLY_TOTAL));
                    event.addModifier(Attributes.ATTACK_SPEED, new AttributeModifier(SmithingService.WEAPON_SPEED_UUID, "nogeon.smith.speed", SmithingService.weaponSpeedBonus(level), AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
            }
            
            if (SmithingService.isMagicEquipment(stack)) {
                if (event.getSlotType() == EquipmentSlot.MAINHAND || event.getSlotType() == EquipmentSlot.OFFHAND) {
                    Attribute spellPower = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(new net.minecraft.resources.ResourceLocation("irons_spellbooks", "spell_power"));
                    Attribute manaRegen = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(new net.minecraft.resources.ResourceLocation("irons_spellbooks", "mana_regen"));
                    Attribute maxMana = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(new net.minecraft.resources.ResourceLocation("irons_spellbooks", "max_mana"));
                    
                    if (spellPower != null) {
                        UUID uuid = UUID.nameUUIDFromBytes(("nogeon.smith.spell_power/" + event.getSlotType().getName()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        event.addModifier(spellPower, new AttributeModifier(uuid, "nogeon.smith.spell_power", level * 0.04D, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    }
                    if (manaRegen != null) {
                        UUID uuid = UUID.nameUUIDFromBytes(("nogeon.smith.mana_regen/" + event.getSlotType().getName()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        event.addModifier(manaRegen, new AttributeModifier(uuid, "nogeon.smith.mana_regen", level * 0.03D, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    }
                    if (maxMana != null) {
                        UUID uuid = UUID.nameUUIDFromBytes(("nogeon.smith.max_mana/" + event.getSlotType().getName()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        event.addModifier(maxMana, new AttributeModifier(uuid, "nogeon.smith.max_mana", level * 10.0D, AttributeModifier.Operation.ADDITION));
                    }
                }
            }
        }

        // Reforge Bonuses
        if (stack.hasTag() && stack.getTag().contains(ReforgeService.REFORGE_TAG, Tag.TAG_LIST)) {
            ListTag list = stack.getTag().getList(ReforgeService.REFORGE_TAG, Tag.TAG_COMPOUND);
            boolean isValidSlot = false;
            EquipmentSlot validSlot = EquipmentSlot.MAINHAND;
            
            if (SmithingService.isArmor(stack)) {
                validSlot = stack.getItem() instanceof ArmorItem armorItem ? armorItem.getEquipmentSlot() : EquipmentSlot.CHEST;
                isValidSlot = (event.getSlotType() == validSlot);
            } else if (SmithingService.isMagicEquipment(stack)) {
                validSlot = event.getSlotType();
                isValidSlot = (validSlot == EquipmentSlot.MAINHAND || validSlot == EquipmentSlot.OFFHAND);
            } else {
                validSlot = EquipmentSlot.MAINHAND;
                isValidSlot = (event.getSlotType() == validSlot);
            }

            if (isValidSlot) {
                List<ReforgeService.Modifier> pool = ReforgeService.getPool(stack);
                int limit = Math.min(list.size(), ReforgeService.MAX_SLOTS);
                for (int i = 0; i < limit; i++) {
                    CompoundTag tag = list.getCompound(i);
                    String modId = tag.getString("ModifierId");
                    double value = tag.getDouble("Value");
                    if (!modId.equals("none") && value > 0) {
                        for (ReforgeService.Modifier mod : pool) {
                            if (mod.id().equals(modId)) {
                                if (mod.attribute() != null) {
                                    event.addModifier(mod.attribute(), new AttributeModifier(ReforgeService.reforgeUuid(validSlot, i), "nogeon.reforge." + modId, value, AttributeModifier.Operation.MULTIPLY_TOTAL));
                                } else {
                                    addGunRangeModifier(event, stack, validSlot, i, modId, value);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCurioAttributeModifier(CurioAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        int level = SmithingService.level(stack);
        if (level > 0) {
            String slotIdentifier = event.getSlotContext().getIdentifier();
            
            // 1. Movement Speed (+0.3% * level)
            UUID speedUuid = UUID.nameUUIDFromBytes(("nogeon.smith.curio_speed/" + slotIdentifier).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            event.addModifier(Attributes.MOVEMENT_SPEED, new AttributeModifier(speedUuid, "nogeon.smith.curio_speed", level * 0.003D, AttributeModifier.Operation.MULTIPLY_TOTAL));

            // 2. Max Health (+0.5% * level)
            UUID hpUuid = UUID.nameUUIDFromBytes(("nogeon.smith.curio_hp/" + slotIdentifier).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            event.addModifier(Attributes.MAX_HEALTH, new AttributeModifier(hpUuid, "nogeon.smith.curio_hp", level * 0.005D, AttributeModifier.Operation.MULTIPLY_TOTAL));

            // 3. Spell Power (+0.5% * level)
            Attribute spellPower = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(new net.minecraft.resources.ResourceLocation("irons_spellbooks", "spell_power"));
            if (spellPower != null) {
                UUID spUuid = UUID.nameUUIDFromBytes(("nogeon.smith.curio_spell_power/" + slotIdentifier).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                event.addModifier(spellPower, new AttributeModifier(spUuid, "nogeon.smith.curio_spell_power", level * 0.005D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }

            // 4. Mana Regen (+0.4% * level)
            Attribute manaRegen = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.get(new net.minecraft.resources.ResourceLocation("irons_spellbooks", "mana_regen"));
            if (manaRegen != null) {
                UUID mrUuid = UUID.nameUUIDFromBytes(("nogeon.smith.curio_mana_regen/" + slotIdentifier).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                event.addModifier(manaRegen, new AttributeModifier(mrUuid, "nogeon.smith.curio_mana_regen", level * 0.004D, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // Reforge Bonuses for Curios
        if (stack.hasTag() && stack.getTag().contains(ReforgeService.REFORGE_TAG, Tag.TAG_LIST)) {
            ListTag list = stack.getTag().getList(ReforgeService.REFORGE_TAG, Tag.TAG_COMPOUND);
            String slotIdentifier = event.getSlotContext().getIdentifier();
            List<ReforgeService.Modifier> pool = ReforgeService.getPool(stack);
            int limit = Math.min(list.size(), ReforgeService.MAX_SLOTS);
            
            for (int i = 0; i < limit; i++) {
                CompoundTag tag = list.getCompound(i);
                String modId = tag.getString("ModifierId");
                double value = tag.getDouble("Value");
                if (!modId.equals("none") && value != 0) {
                    for (ReforgeService.Modifier mod : pool) {
                        if (mod.id().equals(modId)) {
                            if (mod.attribute() != null) {
                                String seed = "nogeon_economy_land:reforge/curio/" + slotIdentifier + "/" + i;
                                UUID uuid = UUID.nameUUIDFromBytes(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                event.addModifier(mod.attribute(), new AttributeModifier(uuid, "nogeon.reforge." + modId, value, AttributeModifier.Operation.MULTIPLY_TOTAL));
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void addGunRangeModifier(ItemAttributeModifierEvent event, ItemStack stack, EquipmentSlot slot, int index, String modId, double value) {
        if (!GunCatalog.isRegisteredGun(stack.getItem())) {
            return;
        }

        switch (modId) {
            case "rapid_fire" -> addOptionalModifier(event, GUNSMITH_RPM, slot, index, modId, value);
        }
    }

    private static void addOptionalModifier(ItemAttributeModifierEvent event, ResourceLocation attributeId, EquipmentSlot slot, int index, String name, double value) {
        addOptionalModifier(event, attributeId, slot, index, name, value, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    private static void addOptionalModifier(ItemAttributeModifierEvent event, ResourceLocation attributeId, EquipmentSlot slot, int index, String name, double value, AttributeModifier.Operation op) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute != null) {
            event.addModifier(attribute, new AttributeModifier(ReforgeService.reforgeUuid(slot, index), "nogeon.reforge." + name, value, op));
        }
    }

    public static double reforgeValue(ItemStack stack, String modifierId) {
        if (!stack.hasTag() || !stack.getTag().contains(ReforgeService.REFORGE_TAG, Tag.TAG_LIST)) {
            return 0;
        }

        double total = 0;
        ListTag list = stack.getTag().getList(ReforgeService.REFORGE_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            if (modifierId.equals(tag.getString("ModifierId"))) {
                total += tag.getDouble("Value");
            }
        }
        return total;
    }

    private static double projectileReforgeValue(Entity directEntity, String tagName) {
        if (directEntity == null) {
            return 0;
        }
        return Math.max(0.0D, directEntity.getPersistentData().getDouble(tagName));
    }

    private static ItemStack rangedWeapon(LivingEntity owner) {
        ItemStack mainHand = owner.getMainHandItem();
        if (isBowOrCrossbow(mainHand)) {
            return mainHand;
        }
        ItemStack offHand = owner.getOffhandItem();
        return isBowOrCrossbow(offHand) ? offHand : ItemStack.EMPTY;
    }

    private static boolean isBowOrCrossbow(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem);
    }

    // --- Tool Smithing: Mining Speed Boost Event ---
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !SmithingService.isTool(stack.getItem())) {
            return;
        }

        int level = SmithingService.level(stack);
        float multiplier = 1.0F + level * 0.15F; // +15% Mining Speed per enhance level

        double reforgeSpeed = reforgeValue(stack, "mining_speed");
        if (reforgeSpeed > 0) {
            multiplier += (float) reforgeSpeed; // Reforge speed addition
        }

        event.setNewSpeed(event.getOriginalSpeed() * multiplier);
    }

    // --- Tool Smithing: Break Block Special Reforge & Durability Events ---
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || player.level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !SmithingService.isTool(stack.getItem())) {
            return;
        }

        RandomSource random = player.getRandom();

        // 1. Durability Thrift (Smithing level * 5% + Reforge durability_thrift)
        int enhanceLevel = SmithingService.level(stack);
        double thriftChance = enhanceLevel * 0.05D;
        thriftChance += reforgeValue(stack, "durability_thrift");

        if (thriftChance > 0 && random.nextDouble() < thriftChance) {
            int currentDamage = stack.getDamageValue();
            if (currentDamage > 0) {
                stack.setDamageValue(currentDamage - 1);
            }
        }

        // 2. Reforge Common: Double Drop (double_drop)
        double doubleDropChance = reforgeValue(stack, "double_drop");
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        boolean playerPlacedResource = JobEvents.isPlayerPlacedResourceBlock(level, pos);
        boolean validToolMaterial = isValidToolMaterialForExtraDrop(state, stack.getItem());
        
        if (!playerPlacedResource && validToolMaterial && doubleDropChance > 0 && random.nextDouble() < doubleDropChance) {
            List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, stack);
            for (ItemStack drop : drops) {
                Block.popResource(level, pos, drop.copy());
            }
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, 1.3F);
        }

        // 3. Tool-specific Special Reforge Bonuses
        Item toolItem = stack.getItem();
        EconomyState economyState = EconomyState.get(level.getServer());
        PlayerProfile profile = economyState.profile(player.getUUID());

        // 3-1. Pickaxe Options
        if (toolItem instanceof PickaxeItem) {
            // mineral_finder
            double finderChance = reforgeValue(stack, "mineral_finder");
            boolean isOre = state.is(net.minecraftforge.common.Tags.Blocks.ORES);
            if (!playerPlacedResource && isOre && finderChance > 0 && random.nextDouble() < finderChance) {
                long earned = 100 + random.nextInt(401);
                profile.addCredits(earned);
                economyState.setDirty();
                player.displayClientMessage(Component.literal("§a[광물 탐지자] §f광맥을 성공적으로 채굴하여 §6" + earned + " 크레딧§f을 추가 획득했습니다!").withStyle(ChatFormatting.BOLD), true);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.2F);
            }

            // gemstone_hunter
            double hunterChance = reforgeValue(stack, "gemstone_hunter");
            boolean isGemOre = isGemstoneOre(state);
            if (!playerPlacedResource && isGemOre && hunterChance > 0 && random.nextDouble() < hunterChance) {
                long earned = 150 + random.nextInt(351);
                profile.addCredits(earned);
                economyState.setDirty();
                List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, stack);
                for (ItemStack drop : drops) {
                    Block.popResource(level, pos, drop.copy());
                }
                player.displayClientMessage(Component.literal("§d[보석 사냥꾼] §f희귀 보석맥 발견! 보석 추가 드롭 및 §6" + earned + " 크레딧§f을 획득했습니다!").withStyle(ChatFormatting.BOLD), true);
                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.5F);
            }
        }

        // 3-2. Shovel Options
        else if (toolItem instanceof ShovelItem) {
            // treasure_digger
            double diggerChance = reforgeValue(stack, "treasure_digger");
            boolean isDiggable = isDiggableBlock(state);
            if (!playerPlacedResource && isDiggable && diggerChance > 0 && random.nextDouble() < diggerChance) {
                giveTreasure(player, level, pos, profile, economyState);
                
                // 보물 발굴자 발동 시 흙/모래 블록 자체는 드롭되지 않고 소멸 처리 (무한 꼼수 방지)
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                event.setCanceled(true);
                
                if (!player.isCreative()) {
                    stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
                }
                return;
            }

            // clay_master
            double clayChance = reforgeValue(stack, "clay_master");
            boolean isClay = state.is(net.minecraft.world.level.block.Blocks.CLAY);
            if (!playerPlacedResource && isClay && clayChance > 0 && random.nextDouble() < clayChance) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1)); // Speed II for 4s
                int clayBalls = 4 + random.nextInt(5);
                Block.popResource(level, pos, new ItemStack(Items.CLAY_BALL, clayBalls));
                player.displayClientMessage(Component.literal("§b[점토 장인] 점토를 깔끔히 가공하여 추가 점토구와 신속 버프를 얻었습니다!"), true);
                level.playSound(null, pos, SoundEvents.DOLPHIN_PLAY, SoundSource.PLAYERS, 0.7F, 1.2F);
            }
        }

        // 3-3. Axe Options
        else if (toolItem instanceof AxeItem) {
            // lumberjack
            double lumberjackChance = reforgeValue(stack, "lumberjack");
            boolean isLog = state.is(BlockTags.LOGS);
            if (!playerPlacedResource && isLog && lumberjackChance > 0 && random.nextDouble() < lumberjackChance) {
                List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, stack);
                for (ItemStack drop : drops) {
                    Block.popResource(level, pos, drop.copy());
                }
                player.displayClientMessage(Component.literal("§e[벌목꾼] §f아름다운 도끼질로 인접 부가 원목을 추가 획득했습니다!"), true);
                level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 0.8F, 0.8F);
            }

            // tree_sap
            double sapChance = reforgeValue(stack, "tree_sap");
            if (isLog && sapChance > 0 && random.nextDouble() < sapChance) {
                int r = random.nextInt(3);
                MobEffectInstance effect = switch (r) {
                    case 0 -> new MobEffectInstance(MobEffects.REGENERATION, 120, 0); // Regeneration I (6s)
                    case 1 -> new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0); // Strength I (6s)
                    default -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 1); // Speed II (6s)
                };
                player.addEffect(effect);
                player.displayClientMessage(Component.literal("§a[수액 수확] §f숲의 자연 수액을 채취하여 강력한 버프 효과가 활성화되었습니다!"), true);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.6F, 1.2F);
            }
        }

        // 3-4. Hoe Options
        else if (toolItem instanceof HoeItem) {
            // bountiful_harvest
            double harvestChance = reforgeValue(stack, "bountiful_harvest");
            boolean isMaxCrop = isMaxAgeCrop(state);
            if (isMaxCrop && harvestChance > 0 && random.nextDouble() < harvestChance) {
                long earned = 50 + random.nextInt(151);
                profile.addCredits(earned);
                economyState.setDirty();
                List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, stack);
                for (ItemStack drop : drops) {
                    Block.popResource(level, pos, drop.copy());
                }
                player.displayClientMessage(Component.literal("§6[풍요로운 수확] §f풍성히 영근 작물을 완벽 수확하여 추가 수량과 §e" + earned + " 농업 장려금§f을 받았습니다!").withStyle(ChatFormatting.BOLD), true);
                level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.8F, 1.2F);
            }

            // replanting
            double replantChance = reforgeValue(stack, "replanting");
            if (isMaxCrop && replantChance > 0 && random.nextDouble() < replantChance) {
                Item seedItem = getSeedForCrop(state.getBlock());
                if (seedItem != null) {
                    boolean hasSeed = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack invStack = player.getInventory().getItem(i);
                        if (invStack.is(seedItem)) {
                            invStack.shrink(1);
                            hasSeed = true;
                            break;
                        }
                    }
                    if (hasSeed) {
                        BlockState defaultState = state.getBlock().defaultBlockState();
                        level.getServer().execute(() -> {
                            if (level.getBlockState(pos).isAir()) {
                                level.setBlockAndUpdate(pos, defaultState);
                                level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.5F, 1.0F);
                            }
                        });
                    }
                }
            }
        }
    }

    private static boolean isGemstoneOre(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase(Locale.ROOT);
        return id.contains("diamond_ore") 
            || id.contains("emerald_ore") 
            || id.contains("lapis_ore") 
            || id.contains("quartz_ore");
    }

    private static boolean isValidToolMaterialForExtraDrop(BlockState state, Item toolItem) {
        if (toolItem instanceof PickaxeItem) {
            String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            return state.is(net.minecraftforge.common.Tags.Blocks.ORES) || path.equals("ancient_debris");
        }
        if (toolItem instanceof ShovelItem) {
            return isDiggableBlock(state);
        }
        if (toolItem instanceof AxeItem) {
            return state.is(BlockTags.LOGS);
        }
        if (toolItem instanceof HoeItem) {
            return isMaxAgeCrop(state);
        }
        return false;
    }

    private static boolean isDiggableBlock(BlockState state) {
        return JobEvents.isDiggableMaterial(state);
    }

    private static boolean isMaxAgeCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        return false;
    }

    private static Item getSeedForCrop(Block block) {
        if (block == net.minecraft.world.level.block.Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (block == net.minecraft.world.level.block.Blocks.CARROTS) return Items.CARROT;
        if (block == net.minecraft.world.level.block.Blocks.POTATOES) return Items.POTATO;
        if (block == net.minecraft.world.level.block.Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
        if (block == net.minecraft.world.level.block.Blocks.NETHER_WART) return Items.NETHER_WART;
        return null;
    }

    private static void giveTreasure(Player player, ServerLevel level, BlockPos pos, PlayerProfile profile, EconomyState economyState) {
        RandomSource random = player.getRandom();
        double r = random.nextDouble();
        
        if (r < 0.70) {
            long earned = 100 + random.nextInt(401);
            profile.addCredits(earned);
            economyState.setDirty();
            player.displayClientMessage(Component.literal("§e[보물 발굴자] §f흙 속에 숨겨진 고대 크레딧 주머니를 발견하여 §6" + earned + " 크레딧§f을 획득했습니다!"), true);
            level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 1.1F);
        } else if (r < 0.935) {
            Item[] resources = {Items.GOLD_INGOT, Items.IRON_INGOT, Items.LAPIS_LAZULI, Items.COAL, Items.COPPER_INGOT, Items.REDSTONE};
            Item found = resources[random.nextInt(resources.length)];
            ItemStack foundStack = new ItemStack(found);
            Block.popResource(level, pos, foundStack.copy());
            player.displayClientMessage(Component.literal("\u00a76[\ubcf4\ubb3c \ubc1c\uad74\uc790] \u00a7f\ubc1c\uad74 \uc911 \ud759 \uc18d\uc5d0 \ubb3b\ud600\uc788\ub358 \uad11\ubb3c\uc790\uc6d0(\u00a7b")
                .append(foundStack.getHoverName())
                .append(Component.literal("\u00a7f)\uc744 \ubc1c\uad74\ud588\uc2b5\ub2c8\ub2e4!")), true);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        } else if (r < 0.9975) {
            Item found = random.nextBoolean() ? Items.DIAMOND : Items.EMERALD;
            ItemStack foundStack = new ItemStack(found);
            Block.popResource(level, pos, foundStack.copy());
            player.displayClientMessage(Component.literal("\u00a7d[\ubcf4\ubb3c \ubc1c\uad74\uc790] \u00a75\ub208\ubd80\uc2e0 \ud589\uc6b4! \u00a7f\ud759\ub354\ubbf8 \uc0ac\uc774\uc5d0\uc11c \ucc2c\ub780\ud55c \ubcf4\uc11d(\u00a7a")
                .append(foundStack.getHoverName())
                .append(Component.literal("\u00a7f)\uc744 \ubc1c\uad74\ud574\ub0c8\uc2b5\ub2c8\ub2e4!")), true);
            level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5F, 1.5F);
        } else {
            int targetLevel = treasureDowngradeScrollTarget(random);
            profile.addEnhancementDowngradeCharge(targetLevel, 1);
            economyState.setDirty();
            player.displayClientMessage(Component.literal("\u00a7b[\ubcf4\ubb3c \ubc1c\uad74\uc790] \u00a76\u00a7l\uc7ad\ud31f! \u00a7f\uace0\ub300 \ub300\uc7a5\uac04\uc758 \ud754\uc801\uc744 \ubc1c\uad74\ud574 ")
                .append(downgradeScrollName(targetLevel))
                .append(Component.literal("\u00a7f 1\uac1c\ub97c \ub4f1\ub85d\ud588\uc2b5\ub2c8\ub2e4!")).withStyle(ChatFormatting.BOLD), false);
            level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static int treasureDowngradeScrollTarget(net.minecraft.util.RandomSource random) {
        double r = random.nextDouble();
        if (r < 0.89) return 10;
        if (r < 0.99) return 15;
        if (r < 0.998) return 17;
        return 20;
    }

    private static Component downgradeScrollName(int targetLevel) {
        if (targetLevel <= 10) {
            return Component.literal("\u00a79\ud558\uae09 \uac15\ud654 \ud558\ub77d \ubc29\uc9c0\uad8c");
        }
        if (targetLevel <= 15) {
            return Component.literal("\u00a79\uc911\uae09 \uac15\ud654 \ud558\ub77d \ubc29\uc9c0\uad8c");
        }
        if (targetLevel <= 17) {
            return Component.literal("\u00a76\uc0c1\uae09 \uac15\ud654 \ud558\ub77d \ubc29\uc9c0\uad8c");
        }
        return Component.literal("\u00a7d\ucd5c\uc0c1\uae09 \uac15\ud654 \ud558\ub77d \ubc29\uc9c0\uad8c");
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        net.minecraft.world.entity.Entity sourceEntity = event.getSource().getEntity();
        net.minecraft.world.entity.Entity directEntity = event.getSource().getDirectEntity();
        if ((sourceEntity instanceof com.nogeon.economyland.entity.ScrapDroneEntity || directEntity instanceof com.nogeon.economyland.entity.ScrapDroneEntity)
            && (victim instanceof Player || victim instanceof com.nogeon.economyland.entity.ScrapDroneEntity)) {
            event.setCanceled(true);
            return;
        }
        LivingEntity attacker = attacker(sourceEntity, directEntity);
        boolean droneAttack = directEntity instanceof com.nogeon.economyland.entity.ScrapDroneEntity;
        if (victim instanceof net.minecraft.world.entity.player.Player
            && attacker != null
            && !(attacker instanceof net.minecraft.world.entity.player.Player)) {
            event.setAmount(event.getAmount() * pveThreatMultiplier(attacker));
            PVE_DAMAGE_SNAPSHOTS.put(victim.getUUID(), new RangedDamageSnapshot(event.getAmount(), victim.tickCount));
        }
        
        // --- Attacker Effects (Lifesteal, Smithing Bonus, Execution, Stun, Piercing) ---
        double projectileStoredDamage = projectileReforgeValue(event.getSource().getDirectEntity(), PROJECTILE_DAMAGE_TAG);
        if (projectileStoredDamage > 0) {
            event.setAmount((float) (event.getAmount() * (1.0D + projectileStoredDamage)));
        }
        double totalPiercing = projectileReforgeValue(event.getSource().getDirectEntity(), PROJECTILE_PIERCING_TAG);
        if (attacker != null && !droneAttack) {
            ItemStack weapon = attacker.getMainHandItem();
            
            // Smithing Damage Bonus
            if (SmithingService.canEnhance(weapon)) {
                int level = SmithingService.level(weapon);
                if (!SmithingService.isMeleeWeapon(weapon)) {
                    float multiplier = SmithingService.damageMultiplier(weapon);
                    if (multiplier > 1.0F) {
                        event.setAmount(event.getAmount() * multiplier);
                    }
                }
                if (level > 0 && !victim.level().isClientSide) {
                    ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> victim),
                        new EnhanceHitVfxPacket(
                            victim.getX(),
                            victim.getY() + victim.getBbHeight() * 0.58D,
                            victim.getZ(),
                            attacker.getLookAngle().x,
                            attacker.getLookAngle().z,
                            level
                        ));
                }
            }

            // Reforge: Special Effects
            if (weapon.hasTag() && weapon.getTag().contains(ReforgeService.REFORGE_TAG, Tag.TAG_LIST)) {
                ListTag list = weapon.getTag().getList(ReforgeService.REFORGE_TAG, Tag.TAG_COMPOUND);
                double totalLifesteal = 0;
                double totalExecution = 0;
                double totalStun = 0;
                double totalMeleeDamage = 0;
                double totalProjectileDamage = 0;
                
                double totalCritChance = 0;
                
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag tag = list.getCompound(i);
                    String modId = tag.getString("ModifierId");
                    double val = tag.getDouble("Value");
                    
                    switch (modId) {
                        case "lifesteal" -> totalLifesteal += val;
                        case "execution" -> totalExecution += val;
                        case "stun" -> totalStun += val;
                        case "melee_damage" -> totalMeleeDamage += val;
                        case "piercing" -> totalPiercing += val;
                        case "projectile_damage" -> totalProjectileDamage += val;
                        case "crit_chance" -> totalCritChance += val;
                    }
                }

                if (totalProjectileDamage > 0 && projectileStoredDamage <= 0 && isRangedDamage(weapon, event)) {
                    event.setAmount((float) (event.getAmount() * (1.0D + totalProjectileDamage)));
                }

                if (totalMeleeDamage > 0 && isMeleeDamage(weapon, event)) {
                    event.setAmount((float) (event.getAmount() * (1.0D + totalMeleeDamage)));
                }

                if (totalLifesteal > 0) {
                    int currentTick = attacker.tickCount;
                    int lastTick = LIFESTEAL_COOLDOWNS.getOrDefault(attacker.getUUID(), 0);
                    if (currentTick - lastTick >= 20) { // 20틱 = 1초 쿨타임
                        attacker.heal((float) (attacker.getMaxHealth() * totalLifesteal));
                        LIFESTEAL_COOLDOWNS.put(attacker.getUUID(), currentTick);
                    }
                }
                
                if (totalExecution > 0 && victim.getHealth() / victim.getMaxHealth() <= 0.35F) {
                    event.setAmount((float) (event.getAmount() * (1.0 + totalExecution)));
                }
                
                if (totalStun > 0 && attacker.getRandom().nextDouble() < totalStun) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4));
                    victim.level().playSound(null, victim.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.5F);
                }

                // Custom Critical Hit (합연산 - additive stacking)
                if (totalCritChance > 0 && attacker.getRandom().nextDouble() < Math.min(totalCritChance, 0.80D)) {
                    event.setAmount(event.getAmount() * 1.5F);
                    if (!victim.level().isClientSide) {
                        victim.level().playSound(null, victim.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                        if (victim.level() instanceof ServerLevel critLevel) {
                            critLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(), 15, 0.3, 0.3, 0.3, 0.2);
                        }
                    }
                }
                
            }
        }

        // --- Victim Effects (Reflect, Vitality, Evasion) ---
        if (!victim.level().isClientSide) {
            boolean playerVsPlayer = victim instanceof net.minecraft.world.entity.player.Player
                && attacker instanceof net.minecraft.world.entity.player.Player;
            double totalReflect = 0;
            double totalVitality = 0;
            double evasionMissChance = 1.0D;
            for (ItemStack armor : victim.getArmorSlots()) {
                if (armor.hasTag() && armor.getTag().contains(ReforgeService.REFORGE_TAG, Tag.TAG_LIST)) {
                    ListTag list = armor.getTag().getList(ReforgeService.REFORGE_TAG, Tag.TAG_COMPOUND);
                    for (int i = 0; i < list.size(); i++) {
                        CompoundTag tag = list.getCompound(i);
                        String modId = tag.getString("ModifierId");
                        double val = tag.getDouble("Value");
                        
                        switch (modId) {
                            case "reflect" -> totalReflect += val;
                            case "vitality" -> totalVitality += val;
                            case "evasion" -> evasionMissChance *= 1.0D - Math.min(0.95D, Math.max(0.0D, val));
                        }
                    }
                }
            }

            double totalEvasion = Math.min(0.40D, 1.0D - evasionMissChance);
            if (!playerVsPlayer && totalEvasion > 0 && victim.getRandom().nextDouble() < totalEvasion) {
                event.setCanceled(true);
                victim.level().playSound(null, victim.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 2.0F);
                return;
            }

            if (!playerVsPlayer && totalReflect > 0 && attacker != null && !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
                float reflectDamage = (float) (event.getAmount() * totalReflect);
                net.minecraft.server.MinecraftServer server = victim.level().getServer();
                if (server != null) {
                    server.tell(new net.minecraft.server.TickTask(server.getTickCount() + 1, () -> {
                        if (attacker.isAlive() && !victim.level().isClientSide) {
                            int oldInvulnerableTime = attacker.invulnerableTime;
                            attacker.invulnerableTime = 0;
                            boolean hurtSuccess = attacker.hurt(victim.level().damageSources().thorns(victim), reflectDamage);
                            if (hurtSuccess) {
                                attacker.invulnerableTime = 0;
                            } else {
                                attacker.invulnerableTime = oldInvulnerableTime;
                            }
                        }
                    }));
                }
            }

            if (!playerVsPlayer && totalVitality > 0) {
                victim.heal((float) (totalVitality * 2.0));
            }

            // PVP Damage Formula - Exponential Armor Decay
            if (victim instanceof net.minecraft.world.entity.player.Player && attacker instanceof net.minecraft.world.entity.player.Player) {
                float damage = event.getAmount();
                if (!Float.isFinite(damage) || damage <= 0.0F) {
                    event.setAmount(PVP_MIN_DAMAGE);
                    return;
                }
                double armor = victim.getArmorValue();
                double toughness = victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                double sourcePiercing = event.getSource().is(DamageTypeTags.BYPASSES_ARMOR) ? PVP_ARMOR_PIERCE_CAP : 0.0D;
                double armorPiercing = Math.min(PVP_ARMOR_PIERCE_CAP, Math.max(sourcePiercing, Math.max(0.0D, totalPiercing)));
                armor *= 1.0D - armorPiercing;
                toughness *= 1.0D - armorPiercing;
                double decay = 0.008D;
                if (isRangedSource(event.getSource())) {
                    decay *= rangedArmorDecayScale(rangedResistance(victim));
                }
                double multiplier = Math.exp(-decay * (armor + toughness));
                float adjusted = Double.isFinite(multiplier) ? (float) (damage * multiplier) : PVP_MIN_DAMAGE;
                event.setAmount(Math.max(minimumPvpDamage(damage), adjusted));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventRangedPvpImmunity(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        LivingEntity attacker = attacker(event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (!(victim instanceof net.minecraft.world.entity.player.Player)
            || !(attacker instanceof net.minecraft.world.entity.player.Player)
            || !isRangedSource(event.getSource())) {
            return;
        }

        RangedDamageSnapshot snapshot = RANGED_DAMAGE_SNAPSHOTS.remove(victim.getUUID());
        if (snapshot == null || victim.tickCount - snapshot.tickCount > 1 || snapshot.amount <= 0.0F) {
            return;
        }

        float floor = Math.max(RANGED_PVP_MIN_DAMAGE, snapshot.amount * RANGED_PVP_DAMAGE_FLOOR_MULTIPLIER);
        event.setAmount(Math.max(event.getAmount(), floor));
        RANGED_DAMAGE_FLOORS.put(victim.getUUID(), new RangedDamageSnapshot(floor, victim.tickCount));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void sanitizeFinalPvpDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        LivingEntity attacker = attacker(event.getSource().getEntity(), event.getSource().getDirectEntity());
        if (victim instanceof net.minecraft.world.entity.player.Player
            && attacker != null
            && !(attacker instanceof net.minecraft.world.entity.player.Player)) {
            sanitizeFinalPveDamage(event, victim, attacker);
            return;
        }
        if (!(victim instanceof net.minecraft.world.entity.player.Player)
            || !(attacker instanceof net.minecraft.world.entity.player.Player)) {
            return;
        }

        float amount = event.getAmount();
        if (!Float.isFinite(amount) || amount < 0.0F) {
            event.setAmount(PVP_MIN_DAMAGE);
            return;
        }

        float maxSafeDamage = Math.max(20.0F, victim.getMaxHealth() * 4.0F);
        amount = Math.min(amount, maxSafeDamage);
        amount = Math.max(amount, PVP_MIN_DAMAGE);

        if (isRangedSource(event.getSource())) {
            RangedDamageSnapshot floor = RANGED_DAMAGE_FLOORS.remove(victim.getUUID());
            if (floor != null && victim.tickCount - floor.tickCount <= 1) {
                amount = Math.max(amount, floor.amount);
            }
        }

        event.setAmount(Math.max(PVP_MIN_DAMAGE, amount));
    }

    private static void sanitizeFinalPveDamage(LivingDamageEvent event, LivingEntity victim, LivingEntity attacker) {
        float amount = event.getAmount();
        if (!Float.isFinite(amount) || amount < 0.0F) {
            amount = 0.0F;
        }

        RangedDamageSnapshot snapshot = PVE_DAMAGE_SNAPSHOTS.remove(victim.getUUID());
        float baseDamage = snapshot != null && victim.tickCount - snapshot.tickCount <= 1
            ? snapshot.amount
            : amount;
        float floor = minimumPveDamage(baseDamage, victim, attacker);
        if (amount > 0.0F || floor >= PVE_STRONG_MIN_DAMAGE) {
            event.setAmount(Math.max(amount, floor));
        }
    }

    private static float minimumPveDamage(float baseDamage, LivingEntity victim, LivingEntity attacker) {
        boolean strong = isStrongPveAttacker(attacker);
        float minimum = strong ? PVE_STRONG_MIN_DAMAGE : PVE_MIN_DAMAGE;
        float multiplier = strong ? PVE_STRONG_DAMAGE_FLOOR_MULTIPLIER : PVE_DAMAGE_FLOOR_MULTIPLIER;
        if (!Float.isFinite(baseDamage) || baseDamage <= 0.0F) {
            double attackDamage = attacker.getAttribute(Attributes.ATTACK_DAMAGE) == null
                ? 0.0D
                : attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
            baseDamage = (float) Math.max(attackDamage, 1.0D);
        }
        return Math.max(minimum, baseDamage * multiplier);
    }

    private static float pveThreatMultiplier(LivingEntity attacker) {
        if (!isStrongPveAttacker(attacker)) {
            return 1.0F;
        }
        double health = Math.max(80.0D, attacker.getMaxHealth());
        double healthScale = (Math.sqrt(health) - Math.sqrt(80.0D)) * 0.03D;
        double attackDamage = attacker.getAttribute(Attributes.ATTACK_DAMAGE) == null
            ? 0.0D
            : attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double lowAttackBonus = attackDamage > 0.0D && attackDamage < 8.0D ? (8.0D - attackDamage) * 0.06D : 0.0D;
        double bossBonus = isBossPveAttacker(attacker) ? 0.20D : 0.0D;
        return (float) Math.min(1.75D, 1.0D + healthScale + lowAttackBonus + bossBonus);
    }

    private static boolean isStrongPveAttacker(LivingEntity attacker) {
        double attackDamage = attacker.getAttribute(Attributes.ATTACK_DAMAGE) == null
            ? 0.0D
            : attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return isBossPveAttacker(attacker)
            || attacker.getMaxHealth() >= 80.0F
            || attackDamage >= 8.0D;
    }

    private static boolean isBossPveAttacker(LivingEntity attacker) {
        return attacker instanceof net.minecraft.world.entity.boss.wither.WitherBoss
            || attacker instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
    }

    private static float minimumPvpDamage(float baseDamage) {
        if (!Float.isFinite(baseDamage) || baseDamage <= 0.0F) {
            return PVP_MIN_DAMAGE;
        }
        return Math.max(PVP_MIN_DAMAGE, baseDamage * PVP_DAMAGE_FLOOR_MULTIPLIER);
    }

    private static LivingEntity attacker(Entity sourceEntity, Entity directEntity) {
        if (sourceEntity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }
        if (directEntity instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        return null;
    }

    private static boolean isRangedDamage(ItemStack weapon, LivingHurtEvent event) {
        if (GunCatalog.isRegisteredGun(weapon.getItem())) {
            return true;
        }
        if (event.getSource().is(com.tacz.guns.init.ModDamageTypes.BULLETS_TAG)) {
            return true;
        }
        return !SmithingService.isMeleeWeapon(weapon) && event.getSource().getDirectEntity() instanceof Projectile;
    }

    private static boolean isRangedSource(net.minecraft.world.damagesource.DamageSource source) {
        return source.is(com.tacz.guns.init.ModDamageTypes.BULLETS_TAG) || source.getDirectEntity() instanceof Projectile;
    }

    private static double rangedResistance(LivingEntity entity) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(PUFFISH_RANGED_RESISTANCE);
        if (attribute == null || entity.getAttribute(attribute) == null) {
            return 0.0D;
        }
        return Math.min(RANGED_RESISTANCE_HARD_CAP, entity.getAttributeValue(attribute));
    }

    private static double rangedArmorDecayScale(double rangedResistance) {
        if (rangedResistance <= RANGED_RESISTANCE_SOFT_CAP) {
            return 1.0D;
        }
        double progress = (rangedResistance - RANGED_RESISTANCE_SOFT_CAP) / (RANGED_RESISTANCE_HARD_CAP - RANGED_RESISTANCE_SOFT_CAP);
        return 1.0D - Math.min(1.0D, Math.max(0.0D, progress)) * 0.75D;
    }

    private static boolean isMeleeDamage(ItemStack weapon, LivingHurtEvent event) {
        return SmithingService.isMeleeWeapon(weapon) && !(event.getSource().getDirectEntity() instanceof Projectile);
    }

    private record RangedDamageSnapshot(float amount, int tickCount) {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        int level = SmithingService.level(stack);
        List<Component> tooltip = event.getToolTip();

        if (level > 0) {
            if (!tooltip.isEmpty()) {
                tooltip.set(0, SmithingService.displayName(stack));
            }
            if (SmithingService.isArmor(stack)) {
                tooltip.add(1, Component.literal("강화 체력 보너스: +").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(Math.round(SmithingService.maxHealthBonus(level) * 100.0D) + "%")));
                tooltip.add(2, Component.literal("강화 방어 보너스: +").withStyle(ChatFormatting.BLUE)
                    .append(Component.literal(Math.round(SmithingService.armorBonus(level) * 100.0D) + "%")));
            } else if (SmithingService.isTool(stack.getItem())) {
                tooltip.add(1, Component.literal("강화 채굴 속도: +").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(Math.round(level * 15.0F) + "%")));
                tooltip.add(2, Component.literal("강화 내구 절약: +").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(Math.round(level * 5.0F) + "%")));
            } else {
                tooltip.add(1, Component.translatable("gui.nogeon_economy_land.smith_tooltip_bonus", Math.round((SmithingService.damageMultiplier(stack) - 1.0F) * 100.0F)));
                if (SmithingService.isMeleeWeapon(stack)) {
                    tooltip.add(2, Component.literal("Enhance Attack Damage: +").withStyle(ChatFormatting.RED)
                        .append(Component.literal(String.format(Locale.ROOT, "%.1f", SmithingService.weaponFlatDamageBonus(level)))));
                }
            }
        }

        if (stack.hasTag() && stack.getTag().contains(ReforgeService.REFORGE_TAG, Tag.TAG_LIST)) {
            ListTag list = stack.getTag().getList(ReforgeService.REFORGE_TAG, Tag.TAG_COMPOUND);
            if (!list.isEmpty()) {
                tooltip.add(Component.empty());
                tooltip.add(Component.translatable("tooltip.nogeon_economy_land.reforge.header").withStyle(ChatFormatting.DARK_GRAY));
                
                boolean isShift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
                List<ReforgeService.Modifier> pool = ReforgeService.getPool(stack);

                int limit = Math.min(list.size(), ReforgeService.MAX_SLOTS);
                for (int i = 0; i < limit; i++) {
                    CompoundTag tag = list.getCompound(i);
                    String modId = tag.getString("ModifierId");
                    if (modId.equals("none")) {
                        tooltip.add(Component.translatable("tooltip.nogeon_economy_land.reforge.slot", i + 1).withStyle(ChatFormatting.GRAY)
                            .append(Component.translatable("tooltip.nogeon_economy_land.reforge.none").withStyle(ChatFormatting.DARK_GRAY)));
                    } else {
                        ReforgeService.Rarity rarity = ReforgeService.Rarity.safe(tag.getString("Rarity"));
                        double value = tag.getDouble("Value");
                        String sign = value >= 0 ? "+" : "";
                        
                        tooltip.add(Component.translatable("tooltip.nogeon_economy_land.reforge.slot", i + 1).withStyle(ChatFormatting.GRAY)
                            .append(Component.translatable("tooltip.nogeon_economy_land.reforge.rarity",
                                Component.translatable("gui.nogeon_economy_land.reforge_rarity." + rarity.name().toLowerCase(java.util.Locale.ROOT))).withStyle(rarity.color))
                            .append(Component.translatable("reforge.mod." + modId).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(" " + sign + Math.round(value * 100.0D) + "%").withStyle(rarity.color)));
                        
                        if (isShift) {
                            for (ReforgeService.Modifier mod : pool) {
                                if (mod.id().equals(modId)) {
                                    tooltip.add(Component.literal("  > ").withStyle(ChatFormatting.DARK_GRAY)
                                        .append(Component.translatable(mod.descriptionKey()).withStyle(ChatFormatting.GRAY)));
                                    break;
                                }
                            }
                        }
                    }
                }
                
                if (!isShift) {
                    tooltip.add(Component.translatable("tooltip.nogeon_economy_land.reforge.shift_hint").withStyle(ChatFormatting.DARK_AQUA));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickItemToEatWhenFull(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        if (!stack.isEmpty() && stack.isEdible()) {
            net.minecraft.world.food.FoodProperties food = stack.getFoodProperties(player);
            if (food != null && !food.canAlwaysEat()) {
                if (!player.canEat(false)) {
                    player.startUsingItem(event.getHand());
                    event.setCancellationResult(net.minecraft.world.InteractionResultHolder.consume(stack).getResult());
                    event.setCanceled(true);
                }
            }
        }
    }

    private static boolean isCreateMod(String id) {
        return id.startsWith("create:")
            || id.startsWith("create_dd:")
            || id.startsWith("createaddition:")
            || id.startsWith("create_new_age:")
            || id.startsWith("createdieselgenerators:")
            || id.startsWith("create_enchantment_industry:")
            || id.startsWith("create_hypertube:")
            || id.startsWith("create_sabers:")
            || id.startsWith("create_jetpack:");
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (event.player instanceof ServerPlayer player && player.tickCount % 100 == 0) {
            BlockPos pos = player.blockPosition();
            boolean nearCreate = false;
            outer:
            for (int dx = -8; dx <= 8; dx++) {
                for (int dy = -4; dy <= 4; dy++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        BlockPos target = pos.offset(dx, dy, dz);
                        BlockState state = player.level().getBlockState(target);
                        if (!state.isAir()) {
                            String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
                            if (isCreateMod(blockId)) {
                                nearCreate = true;
                                break outer;
                            }
                        }
                    }
                }
            }
            if (nearCreate) {
                addMechanicalXp(player, 1);
            }
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) {
            return;
        }
        String itemId = ForgeRegistries.ITEMS.getKey(crafted.getItem()).toString();
        int amount = crafted.getCount();
        
        if (isCreateMod(itemId)) {
            addMechanicalXp(player, 5 * amount);
        } else if (itemId.startsWith("tacz:") && itemId.contains("ammo")) {
            addMechanicalXp(player, 1 * amount);
        }
    }

    public static void addMechanicalXp(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }
        String cmd = "puffish_skills experience add " + player.getScoreboardName() + " mechanical " + amount;
        player.getServer().getCommands().performPrefixedCommand(
            player.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
            cmd
        );
    }
}
