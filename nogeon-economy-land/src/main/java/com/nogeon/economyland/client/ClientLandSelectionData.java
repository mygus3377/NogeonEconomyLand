package com.nogeon.economyland.client;

import com.nogeon.economyland.land.LandType;
import com.nogeon.economyland.network.SyncLandSelectionPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;

public final class ClientLandSelectionData {
    private static LandType type;
    private static ResourceLocation dimensionId;
    private static List<Cuboid> cuboids = new ArrayList<>();
    private static BlockPos pendingFirst;
    private static boolean pendingAdditive;

    private ClientLandSelectionData() {
    }

    public static void set(LandType landType, ResourceLocation worldId, List<SyncLandSelectionPacket.CuboidData> cuboidData, BlockPos firstPos, boolean firstAdditive) {
        type = landType;
        dimensionId = worldId;
        cuboids.clear();
        for (SyncLandSelectionPacket.CuboidData data : cuboidData) {
            cuboids.add(new Cuboid(
                new BlockPos(Math.min(data.first().getX(), data.second().getX()), Math.min(data.first().getY(), data.second().getY()), Math.min(data.first().getZ(), data.second().getZ())),
                new BlockPos(Math.max(data.first().getX(), data.second().getX()), Math.max(data.first().getY(), data.second().getY()), Math.max(data.first().getZ(), data.second().getZ())),
                data.additive()
            ));
        }
        pendingFirst = firstPos == null ? null : firstPos.immutable();
        pendingAdditive = firstAdditive;
    }

    public static void clear() {
        type = null;
        dimensionId = null;
        cuboids.clear();
        pendingFirst = null;
        pendingAdditive = true;
    }

    public static Preview preview(Minecraft minecraft) {
        if (minecraft.level == null || type == null || dimensionId == null) {
            return null;
        }
        if (!minecraft.level.dimension().location().equals(dimensionId)) {
            return null;
        }

        List<Cuboid> sourceCuboids = new ArrayList<>(cuboids);
        Cuboid stagedCuboid = null;
        if (pendingFirst != null) {
            BlockPos second = hoveredBlock(minecraft);
            if (second == null) {
                second = pendingFirst;
            }
            stagedCuboid = new Cuboid(
                new BlockPos(Math.min(pendingFirst.getX(), second.getX()), Math.min(pendingFirst.getY(), second.getY()), Math.min(pendingFirst.getZ(), second.getZ())),
                new BlockPos(Math.max(pendingFirst.getX(), second.getX()), Math.max(pendingFirst.getY(), second.getY()), Math.max(pendingFirst.getZ(), second.getZ())),
                pendingAdditive
            );
            sourceCuboids.add(stagedCuboid);
        }

        List<Cuboid> resolvedCuboids = resolveEffectiveCuboids(sourceCuboids);
        List<Cuboid> displayCuboids = new ArrayList<>(resolvedCuboids);
        if (stagedCuboid != null && !stagedCuboid.additive()) {
            displayCuboids.add(stagedCuboid);
        }

        long blocks = 0;
        for (Cuboid cuboid : resolvedCuboids) {
            blocks += cuboid.blocks();
        }

        return new Preview(type, displayCuboids, Math.max(0, blocks), Math.max(0, blocks) * type.pricePerBlock(), pendingFirst == null);
    }

    private static List<Cuboid> resolveEffectiveCuboids(List<Cuboid> sourceCuboids) {
        if (sourceCuboids.isEmpty()) {
            return List.of();
        }

        java.util.TreeSet<Integer> xCoords = new java.util.TreeSet<>();
        java.util.TreeSet<Integer> yCoords = new java.util.TreeSet<>();
        java.util.TreeSet<Integer> zCoords = new java.util.TreeSet<>();
        for (Cuboid cuboid : sourceCuboids) {
            xCoords.add(cuboid.min().getX());
            xCoords.add(cuboid.max().getX() + 1);
            yCoords.add(cuboid.min().getY());
            yCoords.add(cuboid.max().getY() + 1);
            zCoords.add(cuboid.min().getZ());
            zCoords.add(cuboid.max().getZ() + 1);
        }

        Integer[] xs = xCoords.toArray(new Integer[0]);
        Integer[] ys = yCoords.toArray(new Integer[0]);
        Integer[] zs = zCoords.toArray(new Integer[0]);
        List<Cuboid> resolved = new ArrayList<>();
        for (int xIndex = 0; xIndex < xs.length - 1; xIndex++) {
            for (int yIndex = 0; yIndex < ys.length - 1; yIndex++) {
                for (int zIndex = 0; zIndex < zs.length - 1; zIndex++) {
                    int x1 = xs[xIndex];
                    int x2 = xs[xIndex + 1];
                    int y1 = ys[yIndex];
                    int y2 = ys[yIndex + 1];
                    int z1 = zs[zIndex];
                    int z2 = zs[zIndex + 1];
                    Boolean additive = finalCoverage(sourceCuboids, x1, x2, y1, y2, z1, z2);
                    if (!Boolean.TRUE.equals(additive)) {
                        continue;
                    }
                    resolved.add(new Cuboid(new BlockPos(x1, y1, z1), new BlockPos(x2 - 1, y2 - 1, z2 - 1), true));
                }
            }
        }
        return resolved;
    }

    private static Boolean finalCoverage(List<Cuboid> sourceCuboids, int x1, int x2, int y1, int y2, int z1, int z2) {
        for (int index = sourceCuboids.size() - 1; index >= 0; index--) {
            Cuboid cuboid = sourceCuboids.get(index);
            if (x1 < cuboid.min().getX() || x2 > cuboid.max().getX() + 1) {
                continue;
            }
            if (y1 < cuboid.min().getY() || y2 > cuboid.max().getY() + 1) {
                continue;
            }
            if (z1 < cuboid.min().getZ() || z2 > cuboid.max().getZ() + 1) {
                continue;
            }
            return cuboid.additive();
        }
        return null;
    }

    private static BlockPos hoveredBlock(Minecraft minecraft) {
        return minecraft.hitResult instanceof BlockHitResult hitResult ? hitResult.getBlockPos() : null;
    }

    public record Cuboid(BlockPos min, BlockPos max, boolean additive) {
        public long blocks() {
            return (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        }
    }

    public record Preview(LandType type, List<Cuboid> cuboids, long blocks, long price, boolean locked) {
    }
}
