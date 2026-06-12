package com.nogeon.economyland.menu;

import java.util.Map;

public record LandSummary(int id, String typeKey, String world, long blocks, int x, int y, int z, String memo, Map<String, Boolean> flags, Map<String, String> permissions) {
}
