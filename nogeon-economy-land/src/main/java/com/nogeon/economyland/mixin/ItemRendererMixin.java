package com.nogeon.economyland.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.nogeon.economyland.client.ClientForgeEvents;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void onRenderHead(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, BakedModel model, CallbackInfo ci) {
        ClientForgeEvents.setRenderingEnhanceStack(stack);
    }

    @Inject(
        method = "render",
        at = @At("RETURN")
    )
    private void onRenderReturn(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay, BakedModel model, CallbackInfo ci) {
        ClientForgeEvents.clearRenderingEnhanceStack();
    }

    @Inject(
        method = "getFoilBuffer",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onGetFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean isItemGlint, boolean glint, CallbackInfoReturnable<VertexConsumer> cir) {
        if (ClientForgeEvents.isRenderingEnhanced()) {
            int level = ClientForgeEvents.getRenderingEnhanceLevel();
            RenderType targetGlint;
            if (net.minecraft.client.Minecraft.useShaderTransparency() && renderType == net.minecraft.client.renderer.Sheets.translucentItemSheet()) {
                targetGlint = ClientForgeEvents.getEnhanceGlintRenderType(RenderType.glintTranslucent(), level);
            } else {
                targetGlint = ClientForgeEvents.getEnhanceGlintRenderType(isItemGlint ? RenderType.glint() : RenderType.entityGlint(), level);
            }
            cir.setReturnValue(VertexMultiConsumer.create(bufferSource.getBuffer(targetGlint), bufferSource.getBuffer(renderType)));
        }
    }

    @Inject(
        method = "getFoilBufferDirect",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onGetFoilBufferDirect(MultiBufferSource bufferSource, RenderType renderType, boolean isItemGlint, boolean glint, CallbackInfoReturnable<VertexConsumer> cir) {
        if (ClientForgeEvents.isRenderingEnhanced()) {
            int level = ClientForgeEvents.getRenderingEnhanceLevel();
            RenderType targetGlint = ClientForgeEvents.getEnhanceGlintRenderType(isItemGlint ? RenderType.glintDirect() : RenderType.entityGlintDirect(), level);
            cir.setReturnValue(VertexMultiConsumer.create(bufferSource.getBuffer(targetGlint), bufferSource.getBuffer(renderType)));
        }
    }

    @Inject(
        method = "getArmorFoilBuffer",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onGetArmorFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean glint, boolean glintCompatible, CallbackInfoReturnable<VertexConsumer> cir) {
        if (ClientForgeEvents.isRenderingEnhanced()) {
            int level = ClientForgeEvents.getRenderingEnhanceLevel();
            RenderType targetGlint = ClientForgeEvents.getEnhanceGlintRenderType(glintCompatible ? RenderType.armorGlint() : RenderType.armorEntityGlint(), level);
            cir.setReturnValue(VertexMultiConsumer.create(bufferSource.getBuffer(targetGlint), bufferSource.getBuffer(renderType)));
        }
    }

    @Inject(
        method = "renderStatic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V",
        at = @At("HEAD")
    )
    private void onRenderStaticHead1(ItemStack stack, ItemDisplayContext displayContext, int combinedLight, int combinedOverlay, PoseStack poseStack, MultiBufferSource bufferSource, Level level, int seed, CallbackInfo ci) {
        ClientForgeEvents.setRenderingEnhanceStack(stack);
    }

    @Inject(
        method = "renderStatic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V",
        at = @At("RETURN")
    )
    private void onRenderStaticReturn1(ItemStack stack, ItemDisplayContext displayContext, int combinedLight, int combinedOverlay, PoseStack poseStack, MultiBufferSource bufferSource, Level level, int seed, CallbackInfo ci) {
        ClientForgeEvents.clearRenderingEnhanceStack();
    }

    @Inject(
        method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
        at = @At("HEAD")
    )
    private void onRenderStaticHead2(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, Level level, int combinedLight, int combinedOverlay, int seed, CallbackInfo ci) {
        ClientForgeEvents.setRenderingEnhanceStack(stack);
    }

    @Inject(
        method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
        at = @At("RETURN")
    )
    private void onRenderStaticReturn2(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, Level level, int combinedLight, int combinedOverlay, int seed, CallbackInfo ci) {
        ClientForgeEvents.clearRenderingEnhanceStack();
    }
}
