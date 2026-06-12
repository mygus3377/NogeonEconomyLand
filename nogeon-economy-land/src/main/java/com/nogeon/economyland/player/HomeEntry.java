package com.nogeon.economyland.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class HomeEntry {
    private final String name;
    private final ResourceKey<Level> worldKey;
    private final BlockPos pos;
    private String memo;

    public HomeEntry(String name, ResourceKey<Level> worldKey, BlockPos pos, String memo) {
        this.name = name;
        this.worldKey = worldKey;
        this.pos = pos;
        this.memo = memo;
    }

    public String name() {
        return name;
    }

    public ResourceKey<Level> worldKey() {
        return worldKey;
    }

    public BlockPos pos() {
        return pos;
    }

    public String memo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public static HomeEntry fromPlayer(String name, ServerPlayer player) {
        return new HomeEntry(name, player.level().dimension(), player.blockPosition(), "");
    }

    public static HomeEntry fromNbt(CompoundTag nbt) {
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(nbt.getString("world")));
        BlockPos pos = new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
        return new HomeEntry(nbt.getString("name"), worldKey, pos, nbt.getString("memo"));
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", name);
        nbt.putString("world", worldKey.location().toString());
        nbt.putInt("x", pos.getX());
        nbt.putInt("y", pos.getY());
        nbt.putInt("z", pos.getZ());
        nbt.putString("memo", memo);
        return nbt;
    }
}
