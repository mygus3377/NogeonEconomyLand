package com.nogeon.economyland.land;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class LandRegion {
    private final int id;
    private final UUID owner;
    private final ResourceKey<Level> world;
    private final LandType type;
    private final BlockPos min;
    private final BlockPos max;
    private final long purchasePricePerBlock;
    private String memo;
    private final Map<UUID, LandPermission> permissions;
    private final Map<LandFlag, Boolean> flags;

    public LandRegion(
        int id,
        UUID owner,
        ResourceKey<Level> world,
        LandType type,
        BlockPos min,
        BlockPos max,
        long purchasePricePerBlock,
        String memo,
        Map<UUID, LandPermission> permissions,
        Map<LandFlag, Boolean> flags
    ) {
        this.id = id;
        this.owner = owner;
        this.world = world;
        this.type = type;
        this.min = min;
        this.max = max;
        this.purchasePricePerBlock = purchasePricePerBlock;
        this.memo = memo;
        this.permissions = permissions;
        this.flags = flags;
    }

    public int id() { return id; }
    public UUID owner() { return owner; }
    public ResourceKey<Level> world() { return world; }
    public LandType type() { return type; }
    public BlockPos min() { return min; }
    public BlockPos max() { return max; }
    public long purchasePricePerBlock() { return purchasePricePerBlock; }
    public String memo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public Map<UUID, LandPermission> permissions() { return permissions; }
    public Map<LandFlag, Boolean> flags() { return flags; }

    public boolean flag(LandFlag flag) {
        return flags.getOrDefault(flag, flag.defaultValue());
    }

    public void setFlag(LandFlag flag, boolean value) {
        flags.put(flag, value);
    }

    public boolean contains(ResourceKey<Level> worldKey, BlockPos pos) {
        return world.equals(worldKey)
            && pos.getX() >= min.getX() && pos.getX() <= max.getX()
            && pos.getY() >= min.getY() && pos.getY() <= max.getY()
            && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public boolean containsColumn(ResourceKey<Level> worldKey, BlockPos pos) {
        return world.equals(worldKey)
            && pos.getX() >= min.getX() && pos.getX() <= max.getX()
            && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    public long blocks() {
        return (long) (max.getX() - min.getX() + 1)
            * (max.getY() - min.getY() + 1)
            * (max.getZ() - min.getZ() + 1);
    }

    public LandPermission permission(UUID player) {
        if (owner.equals(player)) {
            return LandPermission.BUILD;
        }
        return permissions.getOrDefault(player, LandPermission.NONE);
    }

    public boolean canInteract(UUID player) {
        LandPermission permission = permission(player);
        return permission == LandPermission.INTERACT || permission == LandPermission.BUILD;
    }

    public boolean canBuild(UUID player) {
        return permission(player) == LandPermission.BUILD;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("id", id);
        nbt.putUUID("owner", owner);
        nbt.putString("world", world.location().toString());
        nbt.putString("type", type.id());
        nbt.putInt("minX", min.getX());
        nbt.putInt("minY", min.getY());
        nbt.putInt("minZ", min.getZ());
        nbt.putInt("maxX", max.getX());
        nbt.putInt("maxY", max.getY());
        nbt.putInt("maxZ", max.getZ());
        nbt.putLong("purchasePricePerBlock", purchasePricePerBlock);
        nbt.putString("memo", memo);
        CompoundTag permissionsNbt = new CompoundTag();
        for (Map.Entry<UUID, LandPermission> entry : permissions.entrySet()) {
            if (entry.getValue() != LandPermission.NONE) {
                permissionsNbt.putString(entry.getKey().toString(), entry.getValue().id());
            }
        }
        nbt.put("permissions", permissionsNbt);
        
        CompoundTag flagsNbt = new CompoundTag();
        for (Map.Entry<LandFlag, Boolean> entry : flags.entrySet()) {
            flagsNbt.putBoolean(entry.getKey().id(), entry.getValue());
        }
        nbt.put("flags", flagsNbt);
        
        return nbt;
    }

    public static LandRegion fromNbt(CompoundTag nbt) {
        ResourceKey<Level> world = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
            ResourceLocation.parse(nbt.getString("world")));
        Map<UUID, LandPermission> permissions = new HashMap<>();
        CompoundTag permissionsNbt = nbt.getCompound("permissions");
        for (String key : permissionsNbt.getAllKeys()) {
            permissions.put(UUID.fromString(key), LandPermission.byId(permissionsNbt.getString(key)));
        }

        Map<LandFlag, Boolean> flags = new EnumMap<>(LandFlag.class);
        if (nbt.contains("flags")) {
            CompoundTag flagsNbt = nbt.getCompound("flags");
            for (String key : flagsNbt.getAllKeys()) {
                try {
                    flags.put(LandFlag.byId(key), flagsNbt.getBoolean(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        return new LandRegion(
            nbt.getInt("id"),
            nbt.getUUID("owner"),
            world,
            LandType.byId(nbt.getString("type")),
            new BlockPos(nbt.getInt("minX"), nbt.getInt("minY"), nbt.getInt("minZ")),
            new BlockPos(nbt.getInt("maxX"), nbt.getInt("maxY"), nbt.getInt("maxZ")),
            nbt.contains("purchasePricePerBlock") ? nbt.getLong("purchasePricePerBlock") : LandType.byId(nbt.getString("type")).pricePerBlock(),
            nbt.getString("memo"),
            permissions,
            flags
        );
    }
}
