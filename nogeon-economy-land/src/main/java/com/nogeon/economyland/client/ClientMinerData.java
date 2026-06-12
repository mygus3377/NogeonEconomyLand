package com.nogeon.economyland.client;

public final class ClientMinerData {
    private static volatile boolean minerBodyActive = true;
    private static volatile boolean minerEyeActive = false;
    private static volatile int minerEyeRadius = 0;
    
    public static volatile double playerX = 0.0D;
    public static volatile double playerY = 0.0D;
    public static volatile double playerZ = 0.0D;

    private ClientMinerData() {
    }

    public static boolean minerBodyActive() {
        return minerBodyActive;
    }

    public static void setMinerBodyActive(boolean active) {
        minerBodyActive = active;
    }

    public static boolean minerEyeActive() {
        return minerEyeActive;
    }

    public static void setMinerEyeActive(boolean active) {
        if (active && !minerEyeActive) {
            ClientVideoOverlay.start();
        } else if (!active && minerEyeActive) {
            ClientVideoOverlay.stop();
        }
        boolean changed = minerEyeActive != active;
        minerEyeActive = active;
        if (changed && net.minecraft.client.Minecraft.getInstance().levelRenderer != null) {
            // Update thread-safe volatile coordinates immediately on main thread before scheduling chunk rebuilds
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                playerX = mc.player.getX();
                playerY = mc.player.getY();
                playerZ = mc.player.getZ();
            }
            net.minecraft.client.Minecraft.getInstance().levelRenderer.allChanged();
        }
    }

    public static int minerEyeRadius() {
        return minerEyeRadius;
    }

    public static void setMinerEyeRadius(int radius) {
        boolean changed = minerEyeRadius != radius;
        minerEyeRadius = radius;
        if (changed && minerEyeActive && net.minecraft.client.Minecraft.getInstance().levelRenderer != null) {
            net.minecraft.client.Minecraft.getInstance().levelRenderer.allChanged();
        }
    }
}
