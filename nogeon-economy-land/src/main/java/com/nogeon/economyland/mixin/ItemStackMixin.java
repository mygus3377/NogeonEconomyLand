package com.nogeon.economyland.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void onHasFoil(CallbackInfoReturnable<Boolean> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.hasTag() && self.getTag().contains("NoGeonEnhanceLevel")) {
            int level = self.getTag().getInt("NoGeonEnhanceLevel");
            if (level > 0) {
                cir.setReturnValue(true);
            }
        }
    }
}
