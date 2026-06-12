package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class DiceDuelMenu extends AbstractContainerMenu {
    private final long stake;
    private final int playerDieOne;
    private final int playerDieTwo;
    private final int dealerDieOne;
    private final int dealerDieTwo;
    private final long payout;
    private final String resultKey;
    private final int gambleStreak;
    private final com.nogeon.economyland.player.SocialClass socialClass;

    public DiceDuelMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.DICE_DUEL.get(), containerId);
        stake = buffer.readLong();
        playerDieOne = buffer.readVarInt();
        playerDieTwo = buffer.readVarInt();
        dealerDieOne = buffer.readVarInt();
        dealerDieTwo = buffer.readVarInt();
        payout = buffer.readLong();
        resultKey = buffer.readUtf();
        gambleStreak = buffer.readVarInt();
        socialClass = com.nogeon.economyland.player.SocialClass.valueOf(buffer.readUtf());
    }

    public DiceDuelMenu(int containerId, long stake, int playerDieOne, int playerDieTwo, int dealerDieOne, int dealerDieTwo, long payout, String resultKey, int gambleStreak, com.nogeon.economyland.player.SocialClass socialClass) {
        super(ModMenus.DICE_DUEL.get(), containerId);
        this.stake = stake;
        this.playerDieOne = playerDieOne;
        this.playerDieTwo = playerDieTwo;
        this.dealerDieOne = dealerDieOne;
        this.dealerDieTwo = dealerDieTwo;
        this.payout = payout;
        this.resultKey = resultKey;
        this.gambleStreak = gambleStreak;
        this.socialClass = socialClass;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(stake);
        buffer.writeVarInt(playerDieOne);
        buffer.writeVarInt(playerDieTwo);
        buffer.writeVarInt(dealerDieOne);
        buffer.writeVarInt(dealerDieTwo);
        buffer.writeLong(payout);
        buffer.writeUtf(resultKey);
        buffer.writeVarInt(gambleStreak);
        buffer.writeUtf(socialClass.name());
    }

    public long stake() {
        return stake;
    }

    public int playerDieOne() {
        return playerDieOne;
    }

    public int playerDieTwo() {
        return playerDieTwo;
    }

    public int dealerDieOne() {
        return dealerDieOne;
    }

    public int dealerDieTwo() {
        return dealerDieTwo;
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
