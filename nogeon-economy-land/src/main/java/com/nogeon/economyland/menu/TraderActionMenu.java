package com.nogeon.economyland.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class TraderActionMenu extends AbstractContainerMenu {
    private final String kindId;
    private final String traderDatabaseId;
    private final String socialClassId;
    private final List<TraderActionLine> lines;
    private final long lotteryJackpot1;
    private final long lotteryJackpot2;

    public TraderActionMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.TRADER_ACTION.get(), containerId);
        kindId = buffer.readUtf();
        traderDatabaseId = buffer.readUtf();
        socialClassId = buffer.readUtf();
        int count = buffer.readVarInt();
        lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add(new TraderActionLine(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readLong()));
        }
        lotteryJackpot1 = buffer.readLong();
        lotteryJackpot2 = buffer.readLong();
    }

    public TraderActionMenu(int containerId, String kindId, String traderDatabaseId, String socialClassId, List<TraderActionLine> lines) {
        this(containerId, kindId, traderDatabaseId, socialClassId, lines, 0L, 0L);
    }

    public TraderActionMenu(int containerId, String kindId, String traderDatabaseId, String socialClassId, List<TraderActionLine> lines, long lotteryJackpot1, long lotteryJackpot2) {
        super(ModMenus.TRADER_ACTION.get(), containerId);
        this.kindId = kindId;
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.socialClassId = socialClassId;
        this.lines = lines;
        this.lotteryJackpot1 = lotteryJackpot1;
        this.lotteryJackpot2 = lotteryJackpot2;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUtf(kindId);
        buffer.writeUtf(traderDatabaseId);
        buffer.writeUtf(socialClassId);
        buffer.writeVarInt(lines.size());
        for (TraderActionLine line : lines) {
            buffer.writeUtf(line.actionId());
            buffer.writeUtf(line.labelKey());
            buffer.writeUtf(line.descriptionKey());
            buffer.writeLong(line.price());
        }
        buffer.writeLong(lotteryJackpot1);
        buffer.writeLong(lotteryJackpot2);
    }

    public String kindId() {
        return kindId;
    }

    public String traderDatabaseId() {
        return traderDatabaseId;
    }

    public String socialClassId() {
        return socialClassId;
    }

    public List<TraderActionLine> lines() {
        return lines;
    }

    public long lotteryJackpot1() {
        return lotteryJackpot1;
    }

    public long lotteryJackpot2() {
        return lotteryJackpot2;
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
