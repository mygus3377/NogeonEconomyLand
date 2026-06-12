package com.nogeon.economyland.mixin;

import com.nogeon.economyland.client.ClientCosmeticArmorData;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CustomHeadLayer.class)
public abstract class CustomHeadLayerMixin {
    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"
        ),
        require = 0
    )
    private ItemStack nogeonEconomyLand$useCosmeticHead(LivingEntity entity, EquipmentSlot slot) {
        if (entity instanceof Player player && ClientCosmeticArmorData.has(player.getUUID())) {
            return ClientCosmeticArmorData.renderStackFor(player.getUUID(), slot, entity.getItemBySlot(slot));
        }
        return entity.getItemBySlot(slot);
    }

    @Redirect(
        method = "m_6494_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"
        ),
        remap = false,
        require = 0
    )
    private ItemStack nogeonEconomyLand$useCosmeticHeadSrg(LivingEntity entity, EquipmentSlot slot) {
        if (entity instanceof Player player && ClientCosmeticArmorData.has(player.getUUID())) {
            return ClientCosmeticArmorData.renderStackFor(player.getUUID(), slot, entity.getItemBySlot(slot));
        }
        return entity.getItemBySlot(slot);
    }
}
