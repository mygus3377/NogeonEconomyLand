package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class SlotMachineMenu extends AbstractContainerMenu {
    private final long stake;
    private final int leftDisplay; // leftSymbol
    private final int middleDisplay; // middleSymbol
    private final int rightDisplay; // rightSymbol
    private final long payout;
    private final String resultKey;
    private final int gambleStreak;
    private final com.nogeon.economyland.player.SocialClass socialClass;

    public SlotMachineMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.SLOT_MACHINE.get(), containerId);
        stake = buffer.readLong();
        leftDisplay = buffer.readVarInt();
        middleDisplay = buffer.readVarInt();
        rightDisplay = buffer.readVarInt();
        payout = buffer.readLong();
        resultKey = buffer.readUtf();
        gambleStreak = buffer.readVarInt();
        socialClass = com.nogeon.economyland.player.SocialClass.valueOf(buffer.readUtf());
    }

    public SlotMachineMenu(int containerId, long stake, int leftDisplay, int middleDisplay, int rightDisplay, long payout, String resultKey, int gambleStreak, com.nogeon.economyland.player.SocialClass socialClass) {
        super(ModMenus.SLOT_MACHINE.get(), containerId);
        this.stake = stake;
        this.leftDisplay = leftDisplay;
        this.middleDisplay = middleDisplay;
        this.rightDisplay = rightDisplay;
        this.payout = payout;
        this.resultKey = resultKey;
        this.gambleStreak = gambleStreak;
        this.socialClass = socialClass;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(stake);
        buffer.writeVarInt(leftDisplay);
        buffer.writeVarInt(middleDisplay);
        buffer.writeVarInt(rightDisplay);
        buffer.writeLong(payout);
        buffer.writeUtf(resultKey);
        buffer.writeVarInt(gambleStreak);
        buffer.writeUtf(socialClass.name());
    }

    public long stake() {
        return stake;
    }

    public int leftSymbol() {
        return leftDisplay;
    }

    public int middleSymbol() {
        return middleDisplay;
    }

    public int rightSymbol() {
        return rightDisplay;
    }

    public long payout() {
        return payout;
    }

    public String resultKey() {
        return resultKey;
    }

    public int gambleStreak() {
        return gambleStreak;
    }

    public com.nogeon.economyland.player.SocialClass socialClass() {
        return socialClass;
    }

    public boolean hasResult() {
        return stake > 0L && !resultKey.isBlank();
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
