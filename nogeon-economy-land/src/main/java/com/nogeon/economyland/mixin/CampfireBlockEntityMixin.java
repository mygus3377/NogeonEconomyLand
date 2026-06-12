package com.nogeon.economyland.mixin;

import com.nogeon.economyland.shop.ShopItemProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {
    @Inject(
        method = "cookTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"),
        locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void nogeon$inheritShopPurchasedTag(
        Level level,
        BlockPos pos,
        BlockState state,
        CampfireBlockEntity campfire,
        CallbackInfo ci,
        boolean changed,
        int slot,
        ItemStack input,
        int previousProgress,
        Container container,
        ItemStack output
    ) {
        if (ShopItemProtection.isShopPurchased(input)) {
            ShopItemProtection.markPurchased(output);
        }
    }
}
