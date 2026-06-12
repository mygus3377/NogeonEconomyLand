package com.nogeon.economyland.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;

public class DroneWitherSkull extends AbstractHurtingProjectile {
    public DroneWitherSkull(EntityType<? extends DroneWitherSkull> type, Level level) {
        super(type, level);
    }

    public DroneWitherSkull(Level level, LivingEntity shooter, double xPower, double yPower, double zPower) {
        super(ModEntities.DRONE_WITHER_SKULL.get(), shooter, xPower, yPower, zPower, level);
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        if (!this.level().isClientSide) {
            Entity target = pResult.getEntity();
            Entity shooter = this.getOwner();
            boolean hurtSuccess = target.hurt(this.damageSources().indirectMagic(this, shooter), 8.0F);
            if (hurtSuccess) {
                if (target instanceof LivingEntity living) {
                    int seconds = 5;
                    if (this.level().getDifficulty() == Difficulty.NORMAL) {
                        seconds = 10;
                    } else if (this.level().getDifficulty() == Difficulty.HARD) {
                        seconds = 40;
                    }
                    living.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * seconds, 1), shooter);
                }
            }
        }
    }

    @Override
    protected void onHit(HitResult pResult) {
        super.onHit(pResult);
        if (!this.level().isClientSide) {
            boolean mobGriefing = this.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.0F, false, mobGriefing ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
            this.discard();
        }
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }
}
