package com.nogeon.economyland.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class MinerEyeBlockStateMixin {
    private BlockState nogeon$currentState() {
        return (BlockState) (Object) this;
    }

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true, require = 0)
    private void nogeon$hideRenderShapeForMinerEye(CallbackInfoReturnable<RenderShape> cir) {
        nogeon$hideRenderShapeForMinerEyeImpl(cir);
    }

    @Inject(method = "m_60799_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void nogeon$hideRenderShapeForMinerEyeSrg(CallbackInfoReturnable<RenderShape> cir) {
        nogeon$hideRenderShapeForMinerEyeImpl(cir);
    }

    private void nogeon$hideRenderShapeForMinerEyeImpl(CallbackInfoReturnable<RenderShape> cir) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            if (com.nogeon.economyland.client.MinerEyeClientBridge.isMinerEyeActiveAndShouldHide(nogeon$currentState())) {
                cir.setReturnValue(RenderShape.INVISIBLE);
            }
        }
    }

    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void nogeon$hideSolidRenderForMinerEye(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        nogeon$hideSolidRenderForMinerEyeImpl(cir);
    }

    @Inject(method = "m_60804_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void nogeon$hideSolidRenderForMinerEyeSrg(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        nogeon$hideSolidRenderForMinerEyeImpl(cir);
    }

    private void nogeon$hideSolidRenderForMinerEyeImpl(CallbackInfoReturnable<Boolean> cir) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            if (com.nogeon.economyland.client.MinerEyeClientBridge.isMinerEyeActiveAndShouldHide(nogeon$currentState())) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true, require = 0)
    private void nogeon$hideOccludeForMinerEye(CallbackInfoReturnable<Boolean> cir) {
        nogeon$hideOccludeForMinerEyeImpl(cir);
    }

    @Inject(method = "m_60815_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void nogeon$hideOccludeForMinerEyeSrg(CallbackInfoReturnable<Boolean> cir) {
        nogeon$hideOccludeForMinerEyeImpl(cir);
    }

    private void nogeon$hideOccludeForMinerEyeImpl(CallbackInfoReturnable<Boolean> cir) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            if (com.nogeon.economyland.client.MinerEyeClientBridge.isMinerEyeActiveAndShouldHide(nogeon$currentState())) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "propagatesSkylightDown", at = @At("HEAD"), cancellable = true, require = 0)
    private void nogeon$hidePropagatesSkylightForMinerEye(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        nogeon$hidePropagatesSkylightForMinerEyeImpl(cir);
    }

    @Inject(method = "m_60787_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void nogeon$hidePropagatesSkylightForMinerEyeSrg(CallbackInfoReturnable<Boolean> cir) {
        nogeon$hidePropagatesSkylightForMinerEyeImpl(cir);
    }

    private void nogeon$hidePropagatesSkylightForMinerEyeImpl(CallbackInfoReturnable<Boolean> cir) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            if (com.nogeon.economyland.client.MinerEyeClientBridge.isMinerEyeActiveAndShouldHide(nogeon$currentState())) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getLightBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void nogeon$hideLightBlockForMinerEye(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        nogeon$hideLightBlockForMinerEyeImpl(cir);
    }

    @Inject(method = "m_60791_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void nogeon$hideLightBlockForMinerEyeSrg(CallbackInfoReturnable<Integer> cir) {
        nogeon$hideLightBlockForMinerEyeImpl(cir);
    }

    private void nogeon$hideLightBlockForMinerEyeImpl(CallbackInfoReturnable<Integer> cir) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            if (com.nogeon.economyland.client.MinerEyeClientBridge.isMinerEyeActiveAndShouldHide(nogeon$currentState())) {
                cir.setReturnValue(0);
            }
        }
    }
}
