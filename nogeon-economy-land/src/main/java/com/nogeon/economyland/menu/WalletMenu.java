package com.nogeon.economyland.menu;

import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class WalletMenu extends AbstractContainerMenu {
    private final long credits;
    private final String jobId;
    private final int jobLevel;
    private final int jobExp;
    private final int jobExpToNextLevel;
    private final int skillPoints;
    private final int homeCount;
    private final int inventoryKeepCharges;
    private final int enhancementGuardCharges;
    private final int lowDowngradeScrolls;
    private final int midDowngradeScrolls;
    private final int highDowngradeScrolls;
    private final int highestDowngradeScrolls;
    private final int resetProtectionScrolls;
    private final boolean admin;
    private final int totalEnhanceAttempts;
    private final long totalEnhanceSpent;
    private final int highestEnhanceLevel;
    private final int totalEnhanceFails;

    public WalletMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.WALLET.get(), containerId);
        credits = buffer.readLong();
        jobId = buffer.readUtf();
        jobLevel = buffer.readVarInt();
        jobExp = buffer.readVarInt();
        jobExpToNextLevel = buffer.readVarInt();
        skillPoints = buffer.readVarInt();
        homeCount = buffer.readVarInt();
        inventoryKeepCharges = buffer.readVarInt();
        enhancementGuardCharges = buffer.readVarInt();
        lowDowngradeScrolls = buffer.readVarInt();
        midDowngradeScrolls = buffer.readVarInt();
        highDowngradeScrolls = buffer.readVarInt();
        highestDowngradeScrolls = buffer.readVarInt();
        resetProtectionScrolls = buffer.readVarInt();
        admin = buffer.readBoolean();
        totalEnhanceAttempts = buffer.readVarInt();
        totalEnhanceSpent = buffer.readVarLong();
        highestEnhanceLevel = buffer.readVarInt();
        totalEnhanceFails = buffer.readVarInt();
    }

    public WalletMenu(int containerId, PlayerProfile profile, boolean admin) {
        super(ModMenus.WALLET.get(), containerId);
        JobType job = profile.selectedJob();
        credits = profile.credits();
        jobId = job.id();
        jobLevel = profile.job(job).level();
        jobExp = profile.job(job).exp();
        jobExpToNextLevel = profile.job(job).expToNextLevel();
        skillPoints = profile.job(job).skillPoints();
        homeCount = profile.homes().size();
        inventoryKeepCharges = profile.inventoryKeepCharges();
        lowDowngradeScrolls = profile.enhancementDowngradeCharges(10);
        midDowngradeScrolls = profile.enhancementDowngradeCharges(15);
        highDowngradeScrolls = profile.enhancementDowngradeCharges(17);
        highestDowngradeScrolls = profile.enhancementDowngradeCharges(20);
        resetProtectionScrolls = profile.enhancementResetProtectionCharges();
        enhancementGuardCharges = 0;
        this.admin = admin;
        totalEnhanceAttempts = profile.totalEnhanceAttempts();
        totalEnhanceSpent = profile.totalEnhanceSpent();
        highestEnhanceLevel = profile.highestEnhanceLevel();
        totalEnhanceFails = profile.totalEnhanceFails();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(credits);
        buffer.writeUtf(jobId);
        buffer.writeVarInt(jobLevel);
        buffer.writeVarInt(jobExp);
        buffer.writeVarInt(jobExpToNextLevel);
        buffer.writeVarInt(skillPoints);
        buffer.writeVarInt(homeCount);
        buffer.writeVarInt(inventoryKeepCharges);
        buffer.writeVarInt(enhancementGuardCharges);
        buffer.writeVarInt(lowDowngradeScrolls);
        buffer.writeVarInt(midDowngradeScrolls);
        buffer.writeVarInt(highDowngradeScrolls);
        buffer.writeVarInt(highestDowngradeScrolls);
        buffer.writeVarInt(resetProtectionScrolls);
        buffer.writeBoolean(admin);
        buffer.writeVarInt(totalEnhanceAttempts);
        buffer.writeVarLong(totalEnhanceSpent);
        buffer.writeVarInt(highestEnhanceLevel);
        buffer.writeVarInt(totalEnhanceFails);
    }

    public int totalEnhanceAttempts() {
        return totalEnhanceAttempts;
    }

    public long totalEnhanceSpent() {
        return totalEnhanceSpent;
    }

    public int highestEnhanceLevel() {
        return highestEnhanceLevel;
    }

    public int totalEnhanceFails() {
        return totalEnhanceFails;
    }

    public long credits() {
        return credits;
    }

    public String jobId() {
        return jobId;
    }

    public int jobLevel() {
        return jobLevel;
    }

    public int jobExp() {
        return jobExp;
    }

    public int jobExpToNextLevel() {
        return jobExpToNextLevel;
    }

    public int skillPoints() {
        return skillPoints;
    }

    public int homeCount() {
        return homeCount;
    }

    public int inventoryKeepCharges() {
        return inventoryKeepCharges;
    }

    public int enhancementGuardCharges() {
        return enhancementGuardCharges;
    }

    public int lowDowngradeScrolls() {
        return lowDowngradeScrolls;
    }

    public int midDowngradeScrolls() {
        return midDowngradeScrolls;
    }

    public int highDowngradeScrolls() {
        return highDowngradeScrolls;
    }

    public int highestDowngradeScrolls() {
        return highestDowngradeScrolls;
    }

    public int resetProtectionScrolls() {
        return resetProtectionScrolls;
    }

    public boolean admin() {
        return admin;
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
