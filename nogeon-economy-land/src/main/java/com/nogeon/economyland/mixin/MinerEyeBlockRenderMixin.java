package com.nogeon.economyland.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nogeon.economyland.client.ClientForgeEvents;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public abstract class MinerEyeBlockRenderMixin {
    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true, require = 0)
    private void nogeon$hideNonOreForMinerEye(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random, ModelData modelData, RenderType renderType, CallbackInfo ci) {
        if (!ClientForgeEvents.shouldRenderMinerEyeBlock(state, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "m_234355_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void nogeon$hideNonOreForMinerEyeSrg(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer consumer, boolean checkSides, RandomSource random, CallbackInfo ci) {
        if (!ClientForgeEvents.shouldRenderMinerEyeBlock(state, pos)) {
            ci.cancel();
        }
    }
}
