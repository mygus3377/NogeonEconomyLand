package com.nogeon.economyland.land;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class LandSelectionValidator {
    private LandSelectionValidator() {
    }

    public static String validate(ServerLevel level, LandSelection selection) {
        if (selection == null || selection.cuboids().isEmpty()) {
            return "message.nogeon_economy_land.land.invalid_selection";
        }
        return areCuboidsConnected(selection.cuboids())
            ? null
            : "message.nogeon_economy_land.land.disconnected";
    }

    private static boolean areCuboidsConnected(List<LandSelection.Cuboid> cuboids) {
        if (cuboids.size() <= 1) {
            return true;
        }

        Set<Integer> visited = new HashSet<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(0);
        visited.add(0);

        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            LandSelection.Cuboid c1 = cuboids.get(current);
            for (int i = 0; i < cuboids.size(); i++) {
                if (visited.contains(i)) {
                    continue;
                }
                LandSelection.Cuboid c2 = cuboids.get(i);
                if (touchesOrOverlaps(c1, c2)) {
                    visited.add(i);
                    queue.addLast(i);
                }
            }
        }

        return visited.size() == cuboids.size();
    }

    private static boolean touchesOrOverlaps(LandSelection.Cuboid c1, LandSelection.Cuboid c2) {
        BlockPos min1 = c1.min();
        BlockPos max1 = c1.max();
        BlockPos min2 = c2.min();
        BlockPos max2 = c2.max();

        return min1.getX() <= max2.getX() + 1 && max1.getX() >= min2.getX() - 1
            && min1.getY() <= max2.getY() + 1 && max1.getY() >= min2.getY() - 1
            && min1.getZ() <= max2.getZ() + 1 && max1.getZ() >= min2.getZ() - 1;
    }
}