package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class AuctionMenu extends AbstractContainerMenu {
    private final List<AuctionLine> lines;

    public AuctionMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.AUCTION.get(), containerId);
        int count = buffer.readVarInt();
        lines = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            lines.add(new AuctionLine(
                buffer.readVarInt(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readVarInt(),
                buffer.readLong(),
                buffer.readBoolean(),
                buffer.readItem()
            ));
        }
    }

    public AuctionMenu(int containerId, List<AuctionLine> lines) {
        super(ModMenus.AUCTION.get(), containerId);
        this.lines = lines;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(lines.size());
        for (AuctionLine line : lines) {
            buffer.writeVarInt(line.auctionId());
            buffer.writeUtf(line.sellerName());
            buffer.writeUtf(line.itemId());
            buffer.writeUtf(line.itemKey());
            buffer.writeVarInt(line.count());
            buffer.writeLong(line.price());
            buffer.writeBoolean(line.mine());
            buffer.writeItem(line.stack());
        }
    }

    public List<AuctionLine> lines() {
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