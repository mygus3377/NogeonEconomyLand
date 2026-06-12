package com.nogeon.economyland.player;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public final class TemperatureAccessoryEvents {
    public static final ResourceLocation FROSTWARD_RING = new ResourceLocation("irons_spellbooks", "frostward_ring");
    public static final ResourceLocation CRYOMANCER_RING = new ResourceLocation("irons_spellbooks", "cryomancer_ring");
    
    public static final ResourceLocation FIREWARD_RING = new ResourceLocation("irons_spellbooks", "fireward_ring");
    public static final ResourceLocation PYROMANCER_RING = new ResourceLocation("irons_spellbooks", "pyromancer_ring");
    public static final ResourceLocation HEAT_SHELL_RING = new ResourceLocation("irons_spellbooks", "heat_shell_ring");
    
    public static final ResourceLocation ICE_RESISTANCE = new ResourceLocation("cold_sweat", "ice_resistance");
    private static final ResourceLocation FIRE_RESISTANCE = new ResourceLocation("minecraft", "fire_resistance");
    private static final ResourceKey<DamageType> COLD_SWEAT_COLD = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("cold_sweat", "cold"));

    private TemperatureAccessoryEvents() {
    }

    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (hasCurio(entity, FROSTWARD_RING) || hasCurio(entity, CRYOMANCER_RING)) {
            applyEffect(entity, ICE_RESISTANCE, 60);
            entity.setTicksFrozen(0);
        } else if (hasColdProtection(entity)) {
            entity.setTicksFrozen(0);
        }

        if (hasCurio(entity, FIREWARD_RING) || hasCurio(entity, PYROMANCER_RING) || hasCurio(entity, HEAT_SHELL_RING)) {
            applyEffect(entity, FIRE_RESISTANCE, 60);
            entity.clearFire();
        } else if (hasHeatProtection(entity)) {
            entity.clearFire();
        }
    }

    public static void onEffectApplicable(net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        MobEffect effect = event.getEffectInstance().getEffect();

        // 🥶 냉기 면역 장신구 착용 시 추위 디버프 적용 원천 차단
        if (hasCurio(entity, FROSTWARD_RING) || hasCurio(entity, CRYOMANCER_RING)) {
            if (effect == MobEffects.MOVEMENT_SLOWDOWN
                || effect == MobEffects.WEAKNESS
                || effect == MobEffects.DIG_SLOWDOWN) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            }
        }

        // 🔥 열기 면역 장신구 착용 시 더위 디버프 적용 원천 차단
        if (hasCurio(entity, FIREWARD_RING) || hasCurio(entity, PYROMANCER_RING) || hasCurio(entity, HEAT_SHELL_RING)) {
            if (effect == MobEffects.MOVEMENT_SLOWDOWN
                || effect == MobEffects.WEAKNESS
                || effect == MobEffects.CONFUSION) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            }
        }
    }

    public static void onLivingAttack(LivingAttackEvent event) {
        if (blocksDamage(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    public static void onLivingHurt(LivingHurtEvent event) {
        if (blocksDamage(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    public static void onPlayEntitySound(PlayLevelSoundEvent.AtEntity event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (entity instanceof Player) {
            if (isAnnoyingIceShatterNoise(event.getSound().value())) {
                event.setCanceled(true);
                return;
            }
        }
        if (hasColdProtection(entity) && isColdProtectionNoise(event.getSound().value())) {
            event.setCanceled(true);
        }
    }

    public static void onPlayPositionSound(PlayLevelSoundEvent.AtPosition event) {
        if (!isColdProtectionNoise(event.getSound().value())) {
            return;
        }
        Vec3 pos = event.getPosition();
        for (Player player : event.getLevel().players()) {
            if (player.distanceToSqr(pos) <= 144.0D && hasColdProtection(player)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    private static boolean blocksDamage(LivingEntity entity, DamageSource source) {
        if (isColdDamage(source)) {
            return hasColdProtection(entity);
        }
        if (isHeatDamage(source)) {
            return hasHeatProtection(entity);
        }
        return false;
    }

    private static boolean hasColdProtection(LivingEntity entity) {
        return hasCurio(entity, FROSTWARD_RING) || hasCurio(entity, CRYOMANCER_RING) || hasEffect(entity, ICE_RESISTANCE) || hasStarterClimateArmor(entity);
    }

    private static boolean hasHeatProtection(LivingEntity entity) {
        return hasCurio(entity, FIREWARD_RING) || hasCurio(entity, PYROMANCER_RING) || hasCurio(entity, HEAT_SHELL_RING) || entity.fireImmune() || hasEffect(entity, FIRE_RESISTANCE) || hasStarterClimateArmor(entity);
    }

    private static boolean hasStarterClimateArmor(LivingEntity entity) {
        for (net.minecraft.world.item.ItemStack stack : entity.getArmorSlots()) {
            if (PlayerSyncEvents.isStarterClimateArmor(stack)) {
                return true;
            }
        }
        return false;
    }

    private static String getDamageTypeId(DamageSource source) {
        return source.typeHolder().unwrapKey()
            .map(key -> key.location().toString())
            .orElseGet(source::getMsgId);
    }

    private static boolean isColdDamage(DamageSource source) {
        String id = getDamageTypeId(source);
        return source.is(DamageTypes.FREEZE)
            || source.is(COLD_SWEAT_COLD)
            || "cold_sweat:cold".equals(id)
            || "cold_sweat:cold_scaling".equals(id)
            || "ice_magic".equals(id)
            || "attributeslib:cold_damage".equals(id)
            || "nullflare_ice".equals(id);
    }

    private static boolean isColdProtectionNoise(SoundEvent sound) {
        if (sound == null) {
            return false;
        }
        if (sound == SoundEvents.PLAYER_HURT_FREEZE) {
            return true;
        }
        ResourceLocation key = BuiltInRegistries.SOUND_EVENT.getKey(sound);
        if (key == null) {
            return false;
        }
        String id = key.toString();
        return id.equals("minecraft:entity.player.hurt_freeze")
            || id.contains("glass")
            || id.contains("ice")
            || id.contains("snow")
            || id.contains("freeze")
            || id.contains("potion")
            || id.contains("bottle");
    }

    private static boolean isAnnoyingIceShatterNoise(SoundEvent sound) {
        if (sound == null) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.SOUND_EVENT.getKey(sound);
        if (key == null) {
            return false;
        }
        String id = key.toString();
        return id.contains("glass")
            || id.contains("ice")
            || id.contains("snow")
            || id.contains("freeze")
            || id.contains("potion")
            || id.contains("bottle");
    }

    private static boolean isHeatDamage(DamageSource source) {
        String id = getDamageTypeId(source);
        return source.is(DamageTypeTags.IS_FIRE)
            || source.is(DamageTypes.HOT_FLOOR)
            || "cold_sweat:hot".equals(id)
            || "fire_magic".equals(id)
            || "fire_field".equals(id)
            || "attributeslib:fire_damage".equals(id)
            || "nullflare_fire".equals(id)
            || "tectonic_crest".equals(id);
    }

    private static boolean hasEffect(LivingEntity entity, ResourceLocation effectId) {
        Optional<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getOptional(effectId);
        return effect.isPresent() && entity.hasEffect(effect.get());
    }

    private static void applyEffect(LivingEntity entity, ResourceLocation effectId, int durationTicks) {
        Optional<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getOptional(effectId);
        if (effect.isPresent()) {
            MobEffect mobEffect = effect.get();
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(mobEffect, durationTicks, 0, true, false, false));
        }
    }

    private static boolean hasCurio(LivingEntity entity, ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            return false;
        }
        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object helper = curiosApi.getMethod("getCuriosHelper").invoke(null);
            for (Method method : helper.getClass().getMethods()) {
                if (!"findFirstCurio".equals(method.getName()) || method.getParameterCount() != 2) {
                    continue;
                }
                Object result = method.invoke(helper, entity, item);
                return result instanceof Optional<?> optional && optional.isPresent();
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
        return false;
    }
}
