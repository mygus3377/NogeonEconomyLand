package com.nogeon.economyland.mixin;

import com.nogeon.economyland.client.TacZReloadAnimationSpeed;
import com.tacz.guns.api.client.animation.AnimationController;
import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.api.client.animation.ObjectAnimationRunner;
import java.util.ArrayList;
import java.util.Locale;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AnimationController.class, remap = false)
public abstract class TacZReloadAnimationSpeedMixin {
    @Shadow @Final protected ArrayList<ObjectAnimationRunner> currentRunners;

    @Inject(method = "run", at = @At("TAIL"))
    private void nogeon$markReloadAnimationRunner(int track, String name, ObjectAnimation.PlayType playType, float transitionTimeS, CallbackInfo ci) {
        if (name == null || !name.toLowerCase(Locale.ROOT).contains("reload")) {
            return;
        }
        if (track < 0 || track >= this.currentRunners.size()) {
            return;
        }

        ObjectAnimationRunner runner = this.currentRunners.get(track);
        ObjectAnimationRunner transitionTo = runner == null ? null : runner.getTransitionTo();
        TacZReloadAnimationSpeed.markReloadRunner(transitionTo != null ? transitionTo : runner, name);
    }
}
