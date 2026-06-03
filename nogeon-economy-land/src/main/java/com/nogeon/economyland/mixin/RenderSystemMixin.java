package com.nogeon.economyland.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nogeon.economyland.client.ClientForgeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSystem.class, remap = false)
public class RenderSystemMixin {
    private static boolean isModifyingColor = false;

    @Inject(method = "setShaderColor", at = @At("HEAD"), cancellable = true)
    private static void onSetShaderColor(float r, float g, float b, float a, CallbackInfo ci) {
        if (!isModifyingColor && ClientForgeEvents.isRenderingEnhanced() && 
            Math.abs(r - 0.5F) < 0.01F && Math.abs(g - 0.25F) < 0.01F && Math.abs(b - 0.8F) < 0.01F) {
            
            float[] color = ClientForgeEvents.getEnhanceGlintColor();
            if (color != null) {
                isModifyingColor = true;
                try {
                    RenderSystem.setShaderColor(color[0], color[1], color[2], a);
                } finally {
                    isModifyingColor = false;
                }
                ci.cancel();
            }
        }
    }
}
