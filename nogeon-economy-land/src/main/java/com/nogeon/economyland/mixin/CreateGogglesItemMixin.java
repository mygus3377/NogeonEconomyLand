package com.nogeon.economyland.mixin;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.nogeon.economyland.entity.ScrapDroneEntity;
import net.minecraft.world.phys.AABB;
import java.util.List;

@Mixin(value = GogglesItem.class, remap = false)
public class CreateGogglesItemMixin {
    @Inject(method = "isWearingGoggles", at = @At("HEAD"), cancellable = true)
    private static void onIsWearingGoggles(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player != null) {
            int sensorLvl = player.getPersistentData().getInt("nogeon_engineer_drone_upgrade_sensor_level");
            if (sensorLvl <= 0 && player.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_sensor")) {
                sensorLvl = 1;
                player.getPersistentData().putInt("nogeon_engineer_drone_upgrade_sensor_level", 1);
            }
            boolean hasSensor = sensorLvl > 0;
            boolean droneBroken = player.getPersistentData().getBoolean("nogeon_engineer_drone_broken");
            if (hasSensor && !droneBroken) {
                // Check if drone is active, nearby, and has charge
                AABB searchBox = player.getBoundingBox().inflate(32.0D);
                List<ScrapDroneEntity> drones = player.level().getEntitiesOfClass(
                    ScrapDroneEntity.class, searchBox,
                    d -> d.getOwnerUuid().map(uuid -> uuid.equals(player.getUUID())).orElse(false)
                );
                if (!drones.isEmpty() && drones.get(0).getCharge() > 0) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
