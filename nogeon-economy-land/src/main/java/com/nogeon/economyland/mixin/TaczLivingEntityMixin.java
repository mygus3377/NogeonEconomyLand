package com.nogeon.economyland.mixin;

import com.nogeon.economyland.entity.ScrapDroneEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.tacz.guns.mixin.common.LivingEntityMixin")
public class TaczLivingEntityMixin {
    @Inject(
        method = "needCheckAmmo",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void onNeedCheckAmmo(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ScrapDroneEntity) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "consumesAmmoOrNot",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void onConsumesAmmoOrNot(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ScrapDroneEntity) {
            cir.setReturnValue(false);
        }
    }
}
