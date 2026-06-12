package com.nogeon.economyland.client;

public final class ClientHunterData {
    private static boolean hunterSenseActive = false;
    private static int hunterSenseRadius = 0;
    private static String hunterPreyMarkedUUID = "";

    private ClientHunterData() {
    }

    public static boolean hunterSenseActive() {
        return hunterSenseActive;
    }

    public static void setHunterSenseActive(boolean active) {
        hunterSenseActive = active;
    }

    public static int hunterSenseRadius() {
        return hunterSenseRadius;
    }

    public static void setHunterSenseRadius(int radius) {
        hunterSenseRadius = radius;
    }

    public static String hunterPreyMarkedUUID() {
        return hunterPreyMarkedUUID;
    }

    public static void setHunterPreyMarkedUUID(String uuid) {
        hunterPreyMarkedUUID = uuid == null ? "" : uuid;
    }
}
