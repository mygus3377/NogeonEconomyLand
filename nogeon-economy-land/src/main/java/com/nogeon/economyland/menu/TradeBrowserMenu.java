package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class TradeBrowserMenu extends AbstractContainerMenu {
    private final List<TradeTargetLine> lines;

    public TradeBrowserMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.TRADE_BROWSER.get(), containerId);
        int count = buffer.readVarInt();
        lines = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            lines.add(new TradeTargetLine(buffer.readUtf(), buffer.readUtf(), buffer.readVarInt(), buffer.readBoolean()));
        }
    }

    public TradeBrowserMenu(int containerId, List<TradeTargetLine> lines) {
        super(ModMenus.TRADE_BROWSER.get(), containerId);
        this.lines = lines;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(lines.size());
        for (TradeTargetLine line : lines) {
            buffer.writeUtf(line.playerId());
            buffer.writeUtf(line.name());
            buffer.writeVarInt(line.distance());
            buffer.writeBoolean(line.busy());
        }
    }

    public List<TradeTargetLine> lines() {
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