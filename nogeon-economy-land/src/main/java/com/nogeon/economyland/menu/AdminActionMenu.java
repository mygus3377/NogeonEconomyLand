package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class AdminActionMenu extends AbstractContainerMenu {
    private final String kindId;
    private final List<TraderActionLine> lines;

    public AdminActionMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.ADMIN_ACTION.get(), containerId);
        kindId = buffer.readUtf();
        int count = buffer.readVarInt();
        lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add(new TraderActionLine(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readLong()));
        }
    }

    public AdminActionMenu(int containerId, String kindId, List<TraderActionLine> lines) {
        super(ModMenus.ADMIN_ACTION.get(), containerId);
        this.kindId = kindId;
        this.lines = lines;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(kindId);
        buffer.writeVarInt(lines.size());
        for (TraderActionLine line : lines) {
            buffer.writeUtf(line.actionId());
            buffer.writeUtf(line.labelKey());
            buffer.writeUtf(line.descriptionKey());
            buffer.writeLong(line.price());
        }
    }

    public String kindId() {
        return kindId;
    }

    public List<TraderActionLine> lines() {
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