package com.nogeon.economyland.mixin;

import com.nogeon.economyland.shop.ShopItemProtection;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {
    @Inject(method = "burn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private void nogeon$inheritShopPurchasedTag(
        RegistryAccess access,
        @Nullable Recipe<?> recipe,
        NonNullList<ItemStack> items,
        int maxStackSize,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (items.size() > 2 && ShopItemProtection.isShopPurchased(items.get(0))) {
            ShopItemProtection.markPurchased(items.get(2));
        }
    }
}
