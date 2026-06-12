package com.nogeon.economyland.mixin;

import com.nogeon.economyland.client.ClientCosmeticArmorData;
import com.nogeon.economyland.client.ClientForgeEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityCosmeticMixin {
    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
    private void onGetItemBySlot(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (self.level() != null && self.level().isClientSide() && self instanceof Player player) {
                if (ClientCosmeticArmorData.has(player.getUUID())) {
                    ItemStack cosmeticStack = ClientCosmeticArmorData.renderStackFor(player.getUUID(), slot, ItemStack.EMPTY);
                    if (cosmeticStack != null && !cosmeticStack.isEmpty()) {
                        cir.setReturnValue(cosmeticStack);
                    }
                }
            }
        }
    }
}
