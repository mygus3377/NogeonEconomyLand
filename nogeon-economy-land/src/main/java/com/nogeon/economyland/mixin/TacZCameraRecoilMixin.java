package com.nogeon.economyland.mixin;

import com.nogeon.economyland.item.SmithEvents;
import com.tacz.guns.client.event.CameraSetupEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = CameraSetupEvent.class, remap = false)
public abstract class TacZCameraRecoilMixin {
    @ModifyArg(
        method = "initialCameraRecoil",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tacz/guns/resource/pojo/data/gun/GunRecoil;genPitchSplineFunction(F)Lorg/apache/commons/math3/analysis/polynomials/PolynomialSplineFunction;"
        )
    )
    private static float nogeon$reducePitchRecoil(float recoil) {
        return reduceRecoil(recoil);
    }

    @ModifyArg(
        method = "initialCameraRecoil",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tacz/guns/resource/pojo/data/gun/GunRecoil;genYawSplineFunction(F)Lorg/apache/commons/math3/analysis/polynomials/PolynomialSplineFunction;"
        )
    )
    private static float nogeon$reduceYawRecoil(float recoil) {
        return reduceRecoil(recoil);
    }

    private static float reduceRecoil(float recoil) {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack stack = player != null ? player.getMainHandItem() : ItemStack.EMPTY;
        double reduction = Math.min(0.8D, SmithEvents.reforgeValue(stack, "recoil_control"));
        if (reduction > 0) {
            return recoil * (float) (1.0D - reduction);
        }
        return recoil;
    }
}
