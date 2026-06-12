package com.nogeon.economyland.mixin;

import com.nogeon.economyland.client.TacZReloadAnimationSpeed;
import com.tacz.guns.api.client.animation.ObjectAnimationRunner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ObjectAnimationRunner.class, remap = false)
public abstract class TacZAnimationRunnerSpeedMixin {
    @ModifyVariable(method = "updateProgress", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private long nogeon$boostReloadAnimationDelta(long deltaNs) {
        return TacZReloadAnimationSpeed.scaleDelta((ObjectAnimationRunner) (Object) this, deltaNs);
    }
}
