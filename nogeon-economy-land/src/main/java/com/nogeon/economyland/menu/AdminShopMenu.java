package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class AdminShopMenu extends AbstractContainerMenu {
    private final String kindId;
    private final String traderDatabaseId;
    private final List<ShopLine> lines;

    public AdminShopMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.ADMIN_SHOP.get(), containerId);
        kindId = buffer.readUtf();
        traderDatabaseId = buffer.readUtf();
        int count = buffer.readVarInt();
        lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add(new ShopLine(buffer.readUtf(), buffer.readUtf(), buffer.readItem(), buffer.readLong(), buffer.readVarInt(), buffer.readBoolean()));
        }
    }

    public AdminShopMenu(int containerId, String kindId, String traderDatabaseId, List<ShopLine> lines) {
        super(ModMenus.ADMIN_SHOP.get(), containerId);
        this.kindId = kindId;
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.lines = lines;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(kindId);
        buffer.writeUtf(traderDatabaseId);
        buffer.writeVarInt(lines.size());
        for (ShopLine line : lines) {
            buffer.writeUtf(line.kindId());
            buffer.writeUtf(line.id());
            buffer.writeItem(line.stack());
            buffer.writeLong(line.price());
            buffer.writeVarInt(line.remaining());
            buffer.writeBoolean(line.delivery());
        }
    }

    public String kindId() {
        return kindId;
    }

    public String traderDatabaseId() {
        return traderDatabaseId;
    }

    public List<ShopLine> lines() {
        return lines;
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
