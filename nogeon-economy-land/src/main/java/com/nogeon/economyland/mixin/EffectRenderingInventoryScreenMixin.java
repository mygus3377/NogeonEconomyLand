package com.nogeon.economyland.mixin;

import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EffectRenderingInventoryScreen.class)
public class EffectRenderingInventoryScreenMixin {
    @Inject(method = "canSeeEffects", at = @At("HEAD"), cancellable = true, remap = true)
    private void nogeon$preventEffectShift(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
