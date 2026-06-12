package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class HighLowMenu extends AbstractContainerMenu {
    private final long stake;
    private final String playerCardsStr;
    private final String dealerCardsStr;
    private final int successfulStages;
    private final int maxStages;
    private final long payout;
    private final long nextPayout;
    private final boolean canGuess;     // canHit
    private final boolean canAdvance;   // canDoubleDown
    private final boolean canCashOut;   // canStand
    private final String statusKey;
    private final int gambleStreak;
    private final com.nogeon.economyland.player.SocialClass socialClass;

    public HighLowMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.HIGH_LOW.get(), containerId);
        this.stake = buffer.readLong();
        this.playerCardsStr = buffer.readUtf();
        this.dealerCardsStr = buffer.readUtf();
        this.successfulStages = buffer.readVarInt();
        this.maxStages = buffer.readVarInt();
        this.payout = buffer.readLong();
        this.nextPayout = buffer.readLong();
        this.canGuess = buffer.readBoolean();
        this.canAdvance = buffer.readBoolean();
        this.canCashOut = buffer.readBoolean();
        this.statusKey = buffer.readUtf();
        this.gambleStreak = buffer.readVarInt();
        this.socialClass = com.nogeon.economyland.player.SocialClass.valueOf(buffer.readUtf());
    }

    public HighLowMenu(int containerId, long stake, String playerCardsStr, String dealerCardsStr, int successfulStages, int maxStages,
        long payout, long nextPayout, boolean canGuess, boolean canAdvance, boolean canCashOut, String statusKey, int gambleStreak, com.nogeon.economyland.player.SocialClass socialClass) {
        super(ModMenus.HIGH_LOW.get(), containerId);
        this.stake = stake;
        this.playerCardsStr = playerCardsStr;
        this.dealerCardsStr = dealerCardsStr;
        this.successfulStages = successfulStages;
        this.maxStages = maxStages;
        this.payout = payout;
        this.nextPayout = nextPayout;
        this.canGuess = canGuess;
        this.canAdvance = canAdvance;
        this.canCashOut = canCashOut;
        this.statusKey = statusKey;
        this.gambleStreak = gambleStreak;
        this.socialClass = socialClass;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(stake);
        buffer.writeUtf(playerCardsStr);
        buffer.writeUtf(dealerCardsStr);
        buffer.writeVarInt(successfulStages);
        buffer.writeVarInt(maxStages);
        buffer.writeLong(payout);
        buffer.writeLong(nextPayout);
        buffer.writeBoolean(canGuess);
        buffer.writeBoolean(canAdvance);
        buffer.writeBoolean(canCashOut);
        buffer.writeUtf(statusKey);
        buffer.writeVarInt(gambleStreak);
        buffer.writeUtf(socialClass.name());
    }

    public long stake() {
        return stake;
    }

    public String playerCardsStr() {
        return playerCardsStr;
    }

    public String dealerCardsStr() {
        return dealerCardsStr;
    }

    public int successfulStages() {
        return successfulStages;
    }

    public int maxStages() {
        return maxStages;
    }

    public long payout() {
        return payout;
    }

    public long nextPayout() {
        return nextPayout;
    }

    public boolean canGuess() {
        return canGuess;
    }

    public boolean canAdvance() {
        return canAdvance;
    }

    public boolean canCashOut() {
        return canCashOut;
    }

    public String statusKey() {
        return statusKey;
    }

    public int gambleStreak() {
        return gambleStreak;
    }

    public com.nogeon.economyland.player.SocialClass socialClass() {
        return socialClass;
    }

    public boolean hasSession() {
        return stake > 0L;
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
