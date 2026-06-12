package com.nogeon.economyland.mixin;

import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CartAssemblerBlockEntity.class, remap = false)
public class CartAssemblerBlockEntityMixin {

    private int economyland$ticksSinceUpdate = 0;

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (this.economyland$ticksSinceUpdate < 60) {
            this.economyland$ticksSinceUpdate++;
        }
    }

    @Inject(method = "resetTicksSinceMinecartUpdate", at = @At("TAIL"))
    private void onReset(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        this.economyland$ticksSinceUpdate = 0;
    }

    /**
     * 수레 조립기의 작동 쿨다운을 기본 8틱(0.4초)에서 60틱(3.0초)으로 상향 조정합니다.
     * 이를 통해 무한 생성-해제 피드백 루프(무한 클럭)가 돌더라도 틱당 계속 조립과 해제가 반복되어 
     * 서버 렉을 발생시키거나 오류를 초래하는 현상을 안전하게 방어하고 멈춥니다.
     */
    @Inject(method = "isMinecartUpdateValid", at = @At("HEAD"), cancellable = true)
    private void redirectIsMinecartUpdateValid(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.economyland$ticksSinceUpdate >= 60);
    }
}
