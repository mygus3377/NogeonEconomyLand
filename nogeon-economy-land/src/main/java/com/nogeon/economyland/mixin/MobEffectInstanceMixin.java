package com.nogeon.economyland.mixin;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public class MobEffectInstanceMixin {
    @Shadow private MobEffect effect;
    @Shadow private int amplifier;

    @Inject(method = "<init>(Lnet/minecraft/world/effect/MobEffect;IIZZZLnet/minecraft/world/effect/MobEffectInstance;Ljava/util/Optional;)V", at = @At("TAIL"))
    private void onInit(MobEffect effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon, MobEffectInstance hiddenEffect, java.util.Optional<?> factorData, CallbackInfo ci) {
        limitVigor();
    }

    @Inject(method = "getAmplifier()I", at = @At("HEAD"), cancellable = true)
    private void onGetAmplifier(CallbackInfoReturnable<Integer> cir) {
        if (this.effect != null) {
            String name = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(this.effect).toString();
            if ("irons_spellbooks:vigor".equals(name)) {
                if (this.amplifier > 9) {
                    cir.setReturnValue(9);
                }
            }
        }
    }

    private void limitVigor() {
        if (this.effect != null) {
            String name = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(this.effect).toString();
            if ("irons_spellbooks:vigor".equals(name)) {
                if (this.amplifier > 9) {
                    this.amplifier = 9;
                }
            }
        }
    }
}
