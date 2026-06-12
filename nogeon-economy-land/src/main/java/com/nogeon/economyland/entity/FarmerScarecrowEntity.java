package com.nogeon.economyland.entity;

import com.nogeon.economyland.item.ModItems;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class FarmerScarecrowEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> SKILL_LEVEL =
        SynchedEntityData.defineId(FarmerScarecrowEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
        SynchedEntityData.defineId(FarmerScarecrowEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public FarmerScarecrowEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setNoAi(true);
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.0D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SKILL_LEVEL, 1);
        entityData.define(OWNER, Optional.empty());
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public int skillLevel() {
        return Math.max(1, entityData.get(SKILL_LEVEL));
    }

    public void setup(UUID owner, int skillLevel) {
        entityData.set(OWNER, Optional.of(owner));
        entityData.set(SKILL_LEVEL, Math.max(1, skillLevel));
        setCustomName(Component.translatable("entity.nogeon_economy_land.farmer_scarecrow", skillLevel()));
        setCustomNameVisible(true);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) {
            return true;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        Optional<UUID> owner = entityData.get(OWNER);
        if (owner.isPresent() && !owner.get().equals(player.getUUID()) && !player.hasPermissions(2)) {
            return false;
        }
        spawnAtLocation(new ItemStack(ModItems.FARMER_SCARECROW.get()));
        discard();
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("SkillLevel", skillLevel());
        entityData.get(OWNER).ifPresent(owner -> nbt.putUUID("Owner", owner));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        int skillLevel = nbt.contains("SkillLevel") ? nbt.getInt("SkillLevel") : 1;
        entityData.set(SKILL_LEVEL, Math.max(1, skillLevel));
        if (nbt.hasUUID("Owner")) {
            entityData.set(OWNER, Optional.of(nbt.getUUID("Owner")));
        }
        setCustomName(Component.translatable("entity.nogeon_economy_land.farmer_scarecrow", skillLevel()));
        setCustomNameVisible(true);
    }
}
