package com.nogeon.economyland.mixin;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.item.SmithEvents;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.item.ModernKineticGunItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModernKineticGunItem.class, remap = false)
public abstract class TacZReloadStateMixin {
    private static long nogeon$lastReloadStateDebugLog;

    @Inject(method = "tickReload", at = @At("RETURN"))
    private void nogeon$boostReloadStateCountdown(ShooterDataHolder data, ItemStack stack, LivingEntity shooter, CallbackInfoReturnable<ReloadState> cir) {
        ReloadState state = cir.getReturnValue();
        if (state == null || stack.isEmpty()) {
            return;
        }

        long countDown = state.getCountDown();
        if (countDown <= 0) {
            return;
        }

        double boost = SmithEvents.reforgeValue(stack, "reload_mastery");
        if (boost <= 0) {
            return;
        }

        double multiplier = Math.max(0.2D, 1.0D - Math.min(0.8D, boost));
        long adjusted = Math.max(0L, Math.round((double) countDown * multiplier));
        state.setCountDown(adjusted);

        long now = System.currentTimeMillis();
        if (now - nogeon$lastReloadStateDebugLog >= 1000L) {
            nogeon$lastReloadStateDebugLog = now;
            NoGeonEconomyLand.LOGGER.info(
                "TaCZ reload state debug: item={}, boost={}, state={}, countDown={} -> {}",
                stack.getHoverName().getString(),
                boost,
                state.getStateType(),
                countDown,
                adjusted
            );
        }
    }
}
