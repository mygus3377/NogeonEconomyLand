package com.nogeon.economyland.client;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.item.SmithEvents;
import com.tacz.guns.api.client.animation.ObjectAnimationRunner;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

public final class TacZReloadAnimationSpeed {
    private static final Map<ObjectAnimationRunner, Double> RELOAD_SPEEDS = Collections.synchronizedMap(new WeakHashMap<>());
    private static long lastDebugLog;

    private TacZReloadAnimationSpeed() {
    }

    public static void markReloadRunner(ObjectAnimationRunner runner, String animationName) {
        double multiplier = currentReloadMultiplier();
        if (runner == null || multiplier <= 1.0D) {
            return;
        }

        RELOAD_SPEEDS.put(runner, multiplier);
        long now = System.currentTimeMillis();
        if (now - lastDebugLog >= 1000L) {
            lastDebugLog = now;
            NoGeonEconomyLand.LOGGER.info("TaCZ reload animation debug: name={}, speedMultiplier={}", animationName, multiplier);
        }
    }

    public static long scaleDelta(ObjectAnimationRunner runner, long deltaNs) {
        Double multiplier = RELOAD_SPEEDS.get(runner);
        if (multiplier == null || multiplier <= 1.0D || deltaNs <= 0L) {
            return deltaNs;
        }
        return Math.max(deltaNs, Math.round((double) deltaNs * multiplier));
    }

    private static double currentReloadMultiplier() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 1.0D;
        }

        ItemStack stack = player.getMainHandItem();
        double boost = SmithEvents.reforgeValue(stack, "reload_mastery");
        if (boost <= 0) {
            return 1.0D;
        }
        return 1.0D / Math.max(0.2D, 1.0D - Math.min(0.8D, boost));
    }
}
