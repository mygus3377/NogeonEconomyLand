package com.nogeon.economyland.mixin;

import com.nogeon.economyland.client.ClientCosmeticArmorData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.client.renderer.patched.layer.WearableItemLayer;

@Mixin(value = WearableItemLayer.class, remap = false)
public abstract class WearableItemLayerMixin {
    @Redirect(
        method = "renderLayer(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I[Lyesman/epicfight/api/utils/math/OpenMatrix4f;FFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;m_6844_(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"
        ),
        require = 0
    )
    private ItemStack nogeonEconomyLand$useCosmeticArmor(LivingEntity entity, EquipmentSlot slot) {
        if (entity instanceof Player player && ClientCosmeticArmorData.has(player.getUUID())) {
            return ClientCosmeticArmorData.renderStackFor(player.getUUID(), slot, entity.getItemBySlot(slot));
        }
        return entity.getItemBySlot(slot);
    }

    @Redirect(
        method = "renderLayer(Lyesman/epicfight/world/capabilities/entitypatch/LivingEntityPatch;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I[Lyesman/epicfight/api/utils/math/OpenMatrix4f;FFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"
        ),
        require = 0
    )
    private ItemStack nogeonEconomyLand$useCosmeticArmorDev(LivingEntity entity, EquipmentSlot slot) {
        if (entity instanceof Player player && ClientCosmeticArmorData.has(player.getUUID())) {
            return ClientCosmeticArmorData.renderStackFor(player.getUUID(), slot, entity.getItemBySlot(slot));
        }
        return entity.getItemBySlot(slot);
    }
}
