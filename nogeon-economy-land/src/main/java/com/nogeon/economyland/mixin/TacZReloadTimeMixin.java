package com.nogeon.economyland.mixin;

import com.nogeon.economyland.item.SmithEvents;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public abstract class TacZReloadTimeMixin {
    @Inject(method = "getReloadTime", at = @At("RETURN"), cancellable = true)
    private void nogeon$boostReloadTime(CallbackInfoReturnable<Long> cir) {
        long elapsed = cir.getReturnValue();
        if (elapsed <= 0) {
            return;
        }

        ItemStack stack = ((ModernKineticGunScriptAPI) (Object) this).getItemStack();
        double boost = SmithEvents.reforgeValue(stack, "reload_mastery");
        if (boost > 0) {
            cir.setReturnValue(Math.round(elapsed / Math.max(0.2D, 1.0D - Math.min(0.8D, boost))));
        }
    }
}
