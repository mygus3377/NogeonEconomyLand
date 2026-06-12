package com.nogeon.economyland.client;

import net.minecraft.core.BlockPos;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientFisherData {
    private static int flowGauge;
    private static BlockPos hotspotPos = null;
    private static double hotspotRadius = 0.0D;
    private static final Map<BlockPos, Double> fisheryZones = new ConcurrentHashMap<>();

    private ClientFisherData() {
    }

    public static int flowGauge() {
        return flowGauge;
    }

    public static void setFlowGauge(int value) {
        flowGauge = value;
    }

    public static BlockPos hotspotPos() {
        return hotspotPos;
    }

    public static void setHotspotPos(BlockPos pos) {
        hotspotPos = pos;
    }

    public static double hotspotRadius() {
        return hotspotRadius;
    }

    public static void setHotspotRadius(double radius) {
        hotspotRadius = radius;
    }

    public static Map<BlockPos, Double> fisheryZones() {
        return fisheryZones;
    }

    public static void updateFisheryZones(Map<BlockPos, Double> zones) {
        fisheryZones.clear();
        fisheryZones.putAll(zones);
    }
}

