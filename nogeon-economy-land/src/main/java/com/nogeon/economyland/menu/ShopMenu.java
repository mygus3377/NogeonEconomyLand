package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class ShopMenu extends AbstractContainerMenu {
    private final String traderDatabaseId;
    private List<ShopLine> lines;
    private net.minecraft.nbt.CompoundTag extInventoryNbt;

    public ShopMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.SHOP.get(), containerId);
        traderDatabaseId = buffer.readUtf();
        int count = buffer.readVarInt();
        lines = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            lines.add(new ShopLine(buffer.readUtf(), buffer.readUtf(), buffer.readItem(), buffer.readLong(), buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt()));
        }
        extInventoryNbt = buffer.readNbt();
    }

    public ShopMenu(int containerId, String traderDatabaseId, List<ShopLine> lines, net.minecraft.nbt.CompoundTag extInventoryNbt) {
        super(ModMenus.SHOP.get(), containerId);
        this.traderDatabaseId = traderDatabaseId;
        this.lines = new ArrayList<>(lines);
        this.extInventoryNbt = extInventoryNbt;
    }

    public void setLines(List<ShopLine> lines) {
        this.lines = new ArrayList<>(lines);
    }

    public void setExtInventoryNbt(net.minecraft.nbt.CompoundTag extInventoryNbt) {
        this.extInventoryNbt = extInventoryNbt;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(traderDatabaseId);
        buffer.writeVarInt(lines.size());
        for (ShopLine line : lines) {
            buffer.writeUtf(line.kindId());
            buffer.writeUtf(line.id());
            buffer.writeItem(line.stack());
            buffer.writeLong(line.price());
            buffer.writeVarInt(line.remaining());
            buffer.writeBoolean(line.delivery());
            buffer.writeVarInt(line.currentSaturation());
            buffer.writeVarInt(line.maxSaturation());
        }
        buffer.writeNbt(extInventoryNbt);
    }

    public String traderDatabaseId() {
        return traderDatabaseId;
    }

    public List<ShopLine> lines() {
        return lines;
    }

    public net.minecraft.nbt.CompoundTag extInventoryNbt() {
        return extInventoryNbt;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
