package com.nogeon.economyland.player;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class PlayerProfile {
    public static final int COSMETIC_ARMOR_SLOTS = 4;
    private long credits;
    private JobType selectedJob = JobType.FARMER;
    private SocialClass socialClass = SocialClass.COMMONER;
    private boolean starterLedgerGranted;
    private int starterPackageVersion;
    private boolean peacefulFlag;
    private int inventoryKeepCharges;
    private int enhancementGuardCharges;
    private int lowEnhancementDowngradeCharges;
    private int midEnhancementDowngradeCharges;
    private int highEnhancementDowngradeCharges;
    private int highestEnhancementDowngradeCharges;
    private int enhancementResetProtectionCharges;
    private int totalEnhanceAttempts;
    private long totalEnhanceSpent;
    private int highestEnhanceLevel;
    private int totalEnhanceFails;
    private int unluckyTokens;
    private int inventoryExtLevel = 1;
    private CompoundTag extInventoryData = new CompoundTag();
    private final ItemStack[] cosmeticArmor = new ItemStack[COSMETIC_ARMOR_SLOTS];
    private boolean cosmeticArmorVisible = true;
    private boolean minerBodyActive = true;
    private boolean minerEyeActive;
    private boolean hunterSenseActive;
    private int hunterSenseTicks;
    private String hunterPreyMarkedUUID = "";
    private final EnumMap<JobType, JobProgress> jobs = new EnumMap<>(JobType.class);
    private final LinkedHashMap<String, HomeEntry> homes = new LinkedHashMap<>();
    private final java.util.List<String> cookRecipeBuffs = new java.util.ArrayList<>();
    private int gambleStreak = 0;

    public PlayerProfile() {
        for (JobType type : JobType.values()) {
            jobs.put(type, new JobProgress(type));
        }
        for (int i = 0; i < cosmeticArmor.length; i++) {
            cosmeticArmor[i] = ItemStack.EMPTY;
        }
    }

    public long credits() {
        return credits;
    }

    public void addCredits(long amount) {
        credits = Math.addExact(credits, Math.max(0, amount));
    }

    public void setCredits(long amount) {
        credits = Math.max(0, amount);
    }

    public boolean spendCredits(long amount) {
        if (amount < 0 || credits < amount) {
            return false;
        }
        credits -= amount;
        return true;
    }

    public JobType selectedJob() {
        return selectedJob;
    }

    public void setSelectedJob(JobType selectedJob) {
        this.selectedJob = selectedJob;
    }

    public SocialClass socialClass() {
        return socialClass;
    }

    public void setSocialClass(SocialClass socialClass) {
        this.socialClass = socialClass;
    }

    public Map<String, HomeEntry> homes() {
        return homes;
    }

    public boolean canAddHome() {
        return socialClass.unlimitedHomes() || homes.size() < socialClass.homeLimit();
    }

    public JobProgress job(JobType type) {
        return jobs.get(type);
    }

    public void resetAllJobProgress() {
        for (JobProgress progress : jobs.values()) {
            progress.reset();
        }
        minerBodyActive = false;
        minerEyeActive = false;
        hunterSenseActive = false;
        hunterSenseTicks = 0;
        hunterPreyMarkedUUID = "";
    }

    public boolean starterLedgerGranted() {
        return starterLedgerGranted;
    }

    public void setStarterLedgerGranted(boolean starterLedgerGranted) {
        this.starterLedgerGranted = starterLedgerGranted;
    }

    public int starterPackageVersion() {
        return starterPackageVersion;
    }

    public void setStarterPackageVersion(int starterPackageVersion) {
        this.starterPackageVersion = Math.max(0, starterPackageVersion);
    }

    public boolean peacefulFlag() {
        return peacefulFlag;
    }

    public void setPeacefulFlag(boolean peacefulFlag) {
        this.peacefulFlag = peacefulFlag;
    }

    public int inventoryKeepCharges() {
        return inventoryKeepCharges;
    }

    public void addInventoryKeepCharges(int amount) {
        inventoryKeepCharges = Math.addExact(inventoryKeepCharges, Math.max(0, amount));
    }

    public boolean consumeInventoryKeepCharge() {
        if (inventoryKeepCharges <= 0) {
            return false;
        }
        inventoryKeepCharges--;
        return true;
    }

    public int enhancementGuardCharges() {
        return enhancementGuardCharges;
    }

    public void addEnhancementGuardCharges(int amount) {
        enhancementGuardCharges = Math.addExact(enhancementGuardCharges, Math.max(0, amount));
    }

    public boolean consumeEnhancementGuardCharge() {
        if (enhancementGuardCharges <= 0) {
            return false;
        }
        enhancementGuardCharges--;
        return true;
    }

    public void addEnhancementDowngradeCharge(int targetLevel, int amount) {
        amount = Math.max(0, amount);
        if (targetLevel <= 10) {
            lowEnhancementDowngradeCharges = Math.addExact(lowEnhancementDowngradeCharges, amount);
        } else if (targetLevel <= 15) {
            midEnhancementDowngradeCharges = Math.addExact(midEnhancementDowngradeCharges, amount);
        } else if (targetLevel <= 17) {
            highEnhancementDowngradeCharges = Math.addExact(highEnhancementDowngradeCharges, amount);
        } else {
            highestEnhancementDowngradeCharges = Math.addExact(highestEnhancementDowngradeCharges, amount);
        }
    }

    public int enhancementDowngradeCharges(int targetLevel) {
        if (targetLevel <= 10) {
            return lowEnhancementDowngradeCharges;
        }
        if (targetLevel <= 15) {
            return midEnhancementDowngradeCharges;
        }
        if (targetLevel <= 17) {
            return highEnhancementDowngradeCharges;
        }
        return highestEnhancementDowngradeCharges;
    }

    public boolean consumeEnhancementDowngradeCharge(int targetLevel) {
        if (targetLevel <= 10) {
            if (lowEnhancementDowngradeCharges <= 0) return false;
            lowEnhancementDowngradeCharges--;
            return true;
        }
        if (targetLevel <= 15) {
            if (midEnhancementDowngradeCharges <= 0) return false;
            midEnhancementDowngradeCharges--;
            return true;
        }
        if (targetLevel <= 17) {
            if (highEnhancementDowngradeCharges <= 0) return false;
            highEnhancementDowngradeCharges--;
            return true;
        }
        if (highestEnhancementDowngradeCharges <= 0) return false;
        highestEnhancementDowngradeCharges--;
        return true;
    }

    public int enhancementResetProtectionCharges() {
        return enhancementResetProtectionCharges;
    }

    public void addEnhancementResetProtectionCharges(int amount) {
        enhancementResetProtectionCharges = Math.addExact(enhancementResetProtectionCharges, Math.max(0, amount));
    }

    public boolean consumeEnhancementResetProtectionCharge() {
        if (enhancementResetProtectionCharges <= 0) {
            return false;
        }
        enhancementResetProtectionCharges--;
        return true;
    }

    public int unluckyTokens() {
        return unluckyTokens;
    }

    public void addUnluckyTokens(int amount) {
        unluckyTokens = Math.addExact(unluckyTokens, Math.max(0, amount));
    }

    public boolean spendUnluckyTokens(int amount) {
        if (amount < 0 || unluckyTokens < amount) {
            return false;
        }
        unluckyTokens -= amount;
        return true;
    }

    public int inventoryExtLevel() {
        return inventoryExtLevel;
    }

    public void setInventoryExtLevel(int level) {
        this.inventoryExtLevel = level;
    }

    public CompoundTag extInventoryData() {
        return extInventoryData;
    }

    public void setExtInventoryData(CompoundTag data) {
        this.extInventoryData = data;
    }

    public long getInventoryUpgradeCost() {
        int nextLevel = inventoryExtLevel + 1;
        return switch (nextLevel) {
            case 2 -> 20000L;
            case 3 -> 40000L;
            case 4 -> 80000L;
            case 5 -> 150000L;
            case 6 -> 250000L;
            case 7 -> 450000L;
            case 8 -> 700000L;
            case 9 -> 1000000L;
            default -> 1000000L + (nextLevel - 9) * 500000L;
        };
    }

    public ItemStack cosmeticArmor(int slot) {
        if (slot < 0 || slot >= cosmeticArmor.length) {
            return ItemStack.EMPTY;
        }
        return cosmeticArmor[slot];
    }

    public void setCosmeticArmor(int slot, ItemStack stack) {
        if (slot < 0 || slot >= cosmeticArmor.length) {
            return;
        }
        cosmeticArmor[slot] = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public boolean cosmeticArmorVisible() {
        return cosmeticArmorVisible;
    }

    public void setCosmeticArmorVisible(boolean visible) {
        this.cosmeticArmorVisible = visible;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("credits", credits);
        nbt.putString("selectedJob", selectedJob.id());
        nbt.putString("socialClass", socialClass.name());
        nbt.putBoolean("starterLedgerGranted", starterLedgerGranted);
        nbt.putInt("starterPackageVersion", starterPackageVersion);
        nbt.putBoolean("peacefulFlag", peacefulFlag);
        nbt.putInt("inventoryExtLevel", inventoryExtLevel);
        nbt.put("extInventoryData", extInventoryData);
        nbt.putInt("inventoryKeepCharges", inventoryKeepCharges);
        nbt.putInt("enhancementGuardCharges", enhancementGuardCharges);
        nbt.putInt("lowEnhancementDowngradeCharges", lowEnhancementDowngradeCharges);
        nbt.putInt("midEnhancementDowngradeCharges", midEnhancementDowngradeCharges);
        nbt.putInt("highEnhancementDowngradeCharges", highEnhancementDowngradeCharges);
        nbt.putInt("highestEnhancementDowngradeCharges", highestEnhancementDowngradeCharges);
        nbt.putInt("enhancementResetProtectionCharges", enhancementResetProtectionCharges);
        nbt.putInt("totalEnhanceAttempts", totalEnhanceAttempts);
        nbt.putLong("totalEnhanceSpent", totalEnhanceSpent);
        nbt.putInt("highestEnhanceLevel", highestEnhanceLevel);
        nbt.putInt("totalEnhanceFails", totalEnhanceFails);
        nbt.putInt("unluckyTokens", unluckyTokens);
        nbt.putBoolean("cosmeticArmorVisible", cosmeticArmorVisible);
        nbt.putBoolean("minerBodyActive", minerBodyActive);
        nbt.putBoolean("minerEyeActive", minerEyeActive);
        nbt.putBoolean("hunterSenseActive", hunterSenseActive);
        nbt.putInt("hunterSenseTicks", hunterSenseTicks);
        nbt.putString("hunterPreyMarkedUUID", hunterPreyMarkedUUID);

        ListTag cosmeticArmorNbt = new ListTag();
        for (int i = 0; i < cosmeticArmor.length; i++) {
            ItemStack stack = cosmeticArmor[i];
            if (!stack.isEmpty()) {
                CompoundTag itemNbt = new CompoundTag();
                itemNbt.putInt("Slot", i);
                stack.save(itemNbt);
                cosmeticArmorNbt.add(itemNbt);
            }
        }
        nbt.put("cosmeticArmor", cosmeticArmorNbt);

        CompoundTag jobsNbt = new CompoundTag();
        for (Map.Entry<JobType, JobProgress> entry : jobs.entrySet()) {
            jobsNbt.put(entry.getKey().id(), entry.getValue().toNbt());
        }
        nbt.put("jobs", jobsNbt);

        CompoundTag homesNbt = new CompoundTag();
        for (Map.Entry<String, HomeEntry> entry : homes.entrySet()) {
            homesNbt.put(entry.getKey(), entry.getValue().toNbt());
        }
        nbt.put("homes", homesNbt);

        ListTag cookRecipeBuffsNbt = new ListTag();
        for (String buff : cookRecipeBuffs) {
            cookRecipeBuffsNbt.add(net.minecraft.nbt.StringTag.valueOf(buff));
        }
        nbt.put("cookRecipeBuffs", cookRecipeBuffsNbt);
        nbt.putInt("gambleStreak", gambleStreak);

        return nbt;
    }

    public static PlayerProfile fromNbt(CompoundTag nbt) {
        PlayerProfile profile = new PlayerProfile();
        profile.credits = Math.max(0, nbt.getLong("credits"));
        if (nbt.contains("selectedJob")) {
            profile.selectedJob = JobType.byId(nbt.getString("selectedJob"));
        }
        if (nbt.contains("socialClass")) {
            profile.socialClass = SocialClass.valueOf(nbt.getString("socialClass"));
        }
        profile.starterLedgerGranted = nbt.getBoolean("starterLedgerGranted");
        profile.starterPackageVersion = Math.max(0, nbt.getInt("starterPackageVersion"));
        profile.peacefulFlag = nbt.getBoolean("peacefulFlag");
        profile.inventoryKeepCharges = Math.max(0, nbt.getInt("inventoryKeepCharges"));
        profile.enhancementGuardCharges = Math.max(0, nbt.getInt("enhancementGuardCharges"));
        profile.lowEnhancementDowngradeCharges = Math.max(0, nbt.getInt("lowEnhancementDowngradeCharges"));
        profile.midEnhancementDowngradeCharges = Math.max(0, nbt.getInt("midEnhancementDowngradeCharges"));
        profile.highEnhancementDowngradeCharges = Math.max(0, nbt.getInt("highEnhancementDowngradeCharges"));
        profile.highestEnhancementDowngradeCharges = Math.max(0, nbt.getInt("highestEnhancementDowngradeCharges"));
        profile.enhancementResetProtectionCharges = Math.max(0, nbt.getInt("enhancementResetProtectionCharges"));
        profile.totalEnhanceAttempts = Math.max(0, nbt.getInt("totalEnhanceAttempts"));
        profile.totalEnhanceSpent = Math.max(0L, nbt.getLong("totalEnhanceSpent"));
        profile.highestEnhanceLevel = Math.max(0, nbt.getInt("highestEnhanceLevel"));
        profile.totalEnhanceFails = Math.max(0, nbt.getInt("totalEnhanceFails"));
        profile.unluckyTokens = Math.max(0, nbt.getInt("unluckyTokens"));
        profile.inventoryExtLevel = nbt.contains("inventoryExtLevel") ? Math.max(1, nbt.getInt("inventoryExtLevel")) : 1;
        profile.extInventoryData = nbt.contains("extInventoryData") ? nbt.getCompound("extInventoryData") : new CompoundTag();
        profile.cosmeticArmorVisible = !nbt.contains("cosmeticArmorVisible") || nbt.getBoolean("cosmeticArmorVisible");
        profile.minerBodyActive = !nbt.contains("minerBodyActive") || nbt.getBoolean("minerBodyActive");
        profile.minerEyeActive = nbt.getBoolean("minerEyeActive");
        profile.hunterSenseActive = nbt.getBoolean("hunterSenseActive");
        profile.hunterSenseTicks = nbt.getInt("hunterSenseTicks");
        profile.hunterPreyMarkedUUID = nbt.contains("hunterPreyMarkedUUID") ? nbt.getString("hunterPreyMarkedUUID") : "";
        if (nbt.contains("cosmeticArmor", Tag.TAG_LIST)) {
            ListTag cosmeticArmorNbt = nbt.getList("cosmeticArmor", Tag.TAG_COMPOUND);
            for (int i = 0; i < cosmeticArmorNbt.size(); i++) {
                CompoundTag itemNbt = cosmeticArmorNbt.getCompound(i);
                int slot = itemNbt.getInt("Slot");
                if (slot >= 0 && slot < profile.cosmeticArmor.length) {
                    profile.cosmeticArmor[slot] = ItemStack.of(itemNbt);
                }
            }
        }
        CompoundTag jobsNbt = nbt.getCompound("jobs");
        for (JobType type : JobType.values()) {
            if (jobsNbt.contains(type.id())) {
                profile.jobs.put(type, JobProgress.fromNbt(type, jobsNbt.getCompound(type.id())));
            }
        }
        CompoundTag homesNbt = nbt.getCompound("homes");
        for (String key : homesNbt.getAllKeys()) {
            profile.homes.put(key, HomeEntry.fromNbt(homesNbt.getCompound(key)));
        }

        if (nbt.contains("cookRecipeBuffs", Tag.TAG_LIST)) {
            ListTag cookRecipeBuffsNbt = nbt.getList("cookRecipeBuffs", Tag.TAG_STRING);
            profile.cookRecipeBuffs.clear();
            for (int i = 0; i < cookRecipeBuffsNbt.size(); i++) {
                profile.cookRecipeBuffs.add(cookRecipeBuffsNbt.getString(i));
            }
        }
        profile.gambleStreak = Math.max(0, nbt.getInt("gambleStreak"));

        return profile;
    }

    public boolean minerBodyActive() {
        return minerBodyActive;
    }

    public void setMinerBodyActive(boolean active) {
        this.minerBodyActive = active;
    }

    public boolean minerEyeActive() {
        return minerEyeActive;
    }

    public void setMinerEyeActive(boolean active) {
        this.minerEyeActive = active;
    }

    public boolean hunterSenseActive() {
        return hunterSenseActive;
    }

    public void setHunterSenseActive(boolean active) {
        this.hunterSenseActive = active;
    }

    public int hunterSenseTicks() {
        return hunterSenseTicks;
    }

    public void setHunterSenseTicks(int ticks) {
        this.hunterSenseTicks = ticks;
    }

    public String hunterPreyMarkedUUID() {
        return hunterPreyMarkedUUID;
    }

    public void setHunterPreyMarkedUUID(String uuid) {
        this.hunterPreyMarkedUUID = uuid == null ? "" : uuid;
    }

    public java.util.List<String> cookRecipeBuffs() {
        return cookRecipeBuffs;
    }

    public void setCookRecipeBuffs(java.util.List<String> buffs) {
        this.cookRecipeBuffs.clear();
        if (buffs != null) {
            this.cookRecipeBuffs.addAll(buffs);
        }
    }

    public int gambleStreak() {
        return gambleStreak;
    }

    public void setGambleStreak(int gambleStreak) {
        this.gambleStreak = Math.max(0, gambleStreak);
    }

    public void incrementGambleStreak() {
        this.gambleStreak++;
    }

    public void resetGambleStreak() {
        this.gambleStreak = 0;
    }

    public int totalEnhanceAttempts() {
        return totalEnhanceAttempts;
    }

    public void incrementEnhanceAttempts() {
        this.totalEnhanceAttempts++;
    }

    public long totalEnhanceSpent() {
        return totalEnhanceSpent;
    }

    public void addEnhanceSpent(long amount) {
        this.totalEnhanceSpent += amount;
    }

    public int highestEnhanceLevel() {
        return highestEnhanceLevel;
    }

    public void trackHighestEnhanceLevel(int level) {
        if (level > this.highestEnhanceLevel) {
            this.highestEnhanceLevel = level;
        }
    }

    public int totalEnhanceFails() {
        return totalEnhanceFails;
    }

    public void incrementEnhanceFails() {
        this.totalEnhanceFails++;
    }
}
