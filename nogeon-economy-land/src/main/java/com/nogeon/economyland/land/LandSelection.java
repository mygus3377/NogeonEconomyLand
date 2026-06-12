package com.nogeon.economyland.land;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class LandSelection {
    private final LandType type;
    private final ResourceKey<Level> world;
    private final List<Cuboid> cuboids = new ArrayList<>();

    public LandSelection(LandType type, ResourceKey<Level> world) {
        this.type = type;
        this.world = world;
    }

    public LandType type() {
        return type;
    }

    public ResourceKey<Level> world() {
        return world;
    }

    public List<Cuboid> cuboids() {
        return cuboids;
    }

    public void addCuboid(BlockPos first, BlockPos second, boolean additive) {
        cuboids.add(new Cuboid(first, second, additive));
    }

    public long blocks() {
        if (cuboids.isEmpty()) return 0L;
        
        java.util.TreeSet<Integer> xCoords = new java.util.TreeSet<>();
        java.util.TreeSet<Integer> yCoords = new java.util.TreeSet<>();
        java.util.TreeSet<Integer> zCoords = new java.util.TreeSet<>();
        
        for (Cuboid c : cuboids) {
            BlockPos min = c.min();
            BlockPos max = c.max();
            xCoords.add(min.getX());
            xCoords.add(max.getX() + 1);
            yCoords.add(min.getY());
            yCoords.add(max.getY() + 1);
            zCoords.add(min.getZ());
            zCoords.add(max.getZ() + 1);
        }
        
        Integer[] xs = xCoords.toArray(new Integer[0]);
        Integer[] ys = yCoords.toArray(new Integer[0]);
        Integer[] zs = zCoords.toArray(new Integer[0]);
        
        long totalVolume = 0;
        for (int i = 0; i < xs.length - 1; i++) {
            for (int j = 0; j < ys.length - 1; j++) {
                for (int k = 0; k < zs.length - 1; k++) {
                    int x1 = xs[i], x2 = xs[i+1];
                    int y1 = ys[j], y2 = ys[j+1];
                    int z1 = zs[k], z2 = zs[k+1];
                    
                    boolean coveredByAdditive = false;
                    boolean coveredBySubtractive = false;

                    for (int idx = cuboids.size() - 1; idx >= 0; idx--) {
                        Cuboid c = cuboids.get(idx);
                        BlockPos min = c.min();
                        BlockPos max = c.max();
                        if (x1 >= min.getX() && x2 <= max.getX() + 1 &&
                            y1 >= min.getY() && y2 <= max.getY() + 1 &&
                            z1 >= min.getZ() && z2 <= max.getZ() + 1) {
                            if (c.additive()) {
                                coveredByAdditive = true;
                            } else {
                                coveredBySubtractive = true;
                            }
                            break; 
                        }
                    }
                    
                    if (coveredByAdditive && !coveredBySubtractive) {
                        totalVolume += (long)(x2 - x1) * (y2 - y1) * (z2 - z1);
                    }
                }
            }
        }
        return totalVolume;
    }

    public void removeLast() {
        if (!cuboids.isEmpty()) {
            cuboids.remove(cuboids.size() - 1);
        }
    }

    public long price() {
        return blocks() * type.pricePerBlock();
    }

    public record Cuboid(BlockPos first, BlockPos second, boolean additive) {
        public BlockPos min() {
            return new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ()));
        }

        public BlockPos max() {
            return new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ()));
        }

        public long blocks() {
            BlockPos min = min();
            BlockPos max = max();
            return (long) (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        }
    }
}
