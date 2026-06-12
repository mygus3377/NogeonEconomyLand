package com.nogeon.economyland.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class MinerEyeClientBridge {
    public static boolean shouldRenderBlock(BlockState state, BlockPos pos) {
        return !isMinerEyeActiveAndShouldHide(state);
    }

    public static boolean isMinerEyeActiveAndShouldHide(BlockState state) {
        if (!ClientMinerData.minerEyeActive() || ClientMinerData.minerEyeRadius() <= 0) {
            return false;
        }

        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return !(path.contains("ore") || path.equals("ancient_debris"));
    }
}
