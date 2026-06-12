package com.nogeon.economyland.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nogeon.economyland.client.ClientForgeEvents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {
    @Inject(
        method = "renderArmorPiece",
        at = @At("HEAD")
    )
    private void onRenderArmorPieceHead(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int light, A model, CallbackInfo ci) {
        ItemStack stack = entity.getItemBySlot(slot);
        ClientForgeEvents.setRenderingEnhanceStack(stack);
    }

    @Inject(
        method = "renderArmorPiece",
        at = @At("RETURN")
    )
    private void onRenderArmorPieceReturn(PoseStack poseStack, MultiBufferSource bufferSource, T entity, EquipmentSlot slot, int light, A model, CallbackInfo ci) {
        ClientForgeEvents.clearRenderingEnhanceStack();
    }
}
