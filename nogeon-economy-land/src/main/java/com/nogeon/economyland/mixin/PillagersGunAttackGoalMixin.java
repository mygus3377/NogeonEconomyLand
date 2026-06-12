package com.nogeon.economyland.mixin;

import com.scarasol.pillagers_gun.entity.goal.GunAttackGoal;
import com.scarasol.pillagers_gun.item.gun.BazookaItem;
import com.scarasol.pillagers_gun.item.gun.GunItem;
import com.scarasol.pillagers_gun.item.gun.PistolItem;
import com.scarasol.pillagers_gun.item.gun.ShotgunItem;
import com.scarasol.pillagers_gun.item.gun.SnipersRifleItem;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GunAttackGoal.class, remap = false)
public abstract class PillagersGunAttackGoalMixin {
    @Shadow
    @Final
    private Mob mob;

    @Redirect(
        method = "m_8037_",
        at = @At(
            value = "INVOKE",
            target = "Lcom/scarasol/pillagers_gun/item/gun/GunItem;getCooldownTime()I"
        )
    )
    private int nogeon$slowRaiderGunCooldown(GunItem gun) {
        int cooldown = gun.getCooldownTime();
        if (!(mob instanceof Raider)) {
            return cooldown;
        }

        if (gun instanceof PistolItem) {
            return cooldown + 8;
        }
        if (gun instanceof ShotgunItem) {
            return cooldown + 12;
        }
        if (gun instanceof SnipersRifleItem) {
            return cooldown + 20;
        }
        if (gun instanceof BazookaItem) {
            return cooldown + 20;
        }
        return cooldown;
    }
}
