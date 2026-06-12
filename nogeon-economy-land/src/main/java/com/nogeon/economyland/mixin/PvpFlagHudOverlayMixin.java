package com.nogeon.economyland.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "io.github.realkarmakun.pvpflag.hud.PvpFlagHudOverlay", remap = false)
public class PvpFlagHudOverlayMixin {

    @ModifyConstant(method = "lambda$onHudRender$0", constant = @Constant(intValue = 95), remap = false)
    private static int nogeon$changeSkullXPosition(int original) {
        // 원래 핫바 우측 위치 상수인 95를 핫바 좌측 끝 위치인 -111로 변경하여 온도 UI 겹침 회피
        return -111;
    }
}
