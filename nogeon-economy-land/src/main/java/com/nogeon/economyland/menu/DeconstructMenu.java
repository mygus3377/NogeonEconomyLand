package com.nogeon.economyland.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class DeconstructMenu extends AbstractContainerMenu {
    private final int selectedSlot;
    private final int currentTab;
    private final Component status;
    private final String autoFuelItem;
    private final boolean droneBroken;
    private final ItemStack gunStack;
    private final ItemStack ammoStack;
    private final int upgInvLvl;
    private final int upgTransLvl;
    private final int upgBoostLvl;
    private final int upgSensorLvl;
    private final int upgGrabberLvl;
    private final String droneName;
    private final int statAttack;
    private final int statHealth;
    private final int statRange;
    private final boolean magnetDisabled;
    private final boolean smithyMode;

    public DeconstructMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.DECONSTRUCT.get(), containerId);
        selectedSlot = buffer.readVarInt();
        currentTab = buffer.readVarInt();
        status = buffer.readComponent();
        autoFuelItem = buffer.readUtf();
        droneBroken = buffer.readBoolean();
        gunStack = buffer.readItem();
        ammoStack = buffer.readItem();
        upgInvLvl = buffer.readVarInt();
        upgTransLvl = buffer.readVarInt();
        upgBoostLvl = buffer.readVarInt();
        upgSensorLvl = buffer.readVarInt();
        upgGrabberLvl = buffer.readVarInt();
        droneName = buffer.readUtf();
        statAttack = buffer.readVarInt();
        statHealth = buffer.readVarInt();
        statRange = buffer.readVarInt();
        magnetDisabled = buffer.readBoolean();
        smithyMode = buffer.readBoolean();
    }

    public DeconstructMenu(int containerId, int selectedSlot, Component status) {
        this(containerId, selectedSlot, 0, status, "", false, ItemStack.EMPTY, ItemStack.EMPTY, 0, 0, 0, 0, 0, "오토 스크랩 드론", 1, 1, 1, false, false);
    }

    public DeconstructMenu(int containerId, int selectedSlot, int currentTab, Component status, String autoFuelItem, boolean droneBroken, ItemStack gunStack, ItemStack ammoStack, int upgInvLvl, int upgTransLvl, int upgBoostLvl, int upgSensorLvl, int upgGrabberLvl, String droneName, int statAttack, int statHealth, int statRange, boolean magnetDisabled, boolean smithyMode) {
        super(ModMenus.DECONSTRUCT.get(), containerId);
        this.selectedSlot = selectedSlot;
        this.currentTab = currentTab;
        this.status = status;
        this.autoFuelItem = autoFuelItem;
        this.droneBroken = droneBroken;
        this.gunStack = gunStack;
        this.ammoStack = ammoStack;
        this.upgInvLvl = upgInvLvl;
        this.upgTransLvl = upgTransLvl;
        this.upgBoostLvl = upgBoostLvl;
        this.upgSensorLvl = upgSensorLvl;
        this.upgGrabberLvl = upgGrabberLvl;
        this.droneName = droneName;
        this.statAttack = statAttack;
        this.statHealth = statHealth;
        this.statRange = statRange;
        this.magnetDisabled = magnetDisabled;
        this.smithyMode = smithyMode;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(selectedSlot);
        buffer.writeVarInt(currentTab);
        buffer.writeComponent(status);
        buffer.writeUtf(autoFuelItem);
        buffer.writeBoolean(droneBroken);
        buffer.writeItem(gunStack);
        buffer.writeItem(ammoStack);
        buffer.writeVarInt(upgInvLvl);
        buffer.writeVarInt(upgTransLvl);
        buffer.writeVarInt(upgBoostLvl);
        buffer.writeVarInt(upgSensorLvl);
        buffer.writeVarInt(upgGrabberLvl);
        buffer.writeUtf(droneName);
        buffer.writeVarInt(statAttack);
        buffer.writeVarInt(statHealth);
        buffer.writeVarInt(statRange);
        buffer.writeBoolean(magnetDisabled);
        buffer.writeBoolean(smithyMode);
    }

    public int selectedSlot() {
        return selectedSlot;
    }

    public int currentTab() {
        return currentTab;
    }

    public boolean isSmithyMode() {
        return smithyMode;
    }

    public Component status() {
        return status;
    }

    public String autoFuelItem() {
        return autoFuelItem;
    }

    public boolean isDroneBroken() {
        return droneBroken;
    }

    public ItemStack gunStack() {
        return gunStack;
    }

    public ItemStack ammoStack() {
        return ammoStack;
    }

    public boolean hasInventoryUpgrade() {
        return upgInvLvl > 0;
    }

    public int inventoryUpgradeLevel() {
        return upgInvLvl;
    }

    public boolean hasTransmitterUpgrade() {
        return upgTransLvl > 0;
    }

    public int transmitterUpgradeLevel() {
        return upgTransLvl;
    }

    public boolean hasBoosterUpgrade() {
        return upgBoostLvl > 0;
    }

    public int boosterUpgradeLevel() {
        return upgBoostLvl;
    }

    public boolean hasSensorUpgrade() {
        return upgSensorLvl > 0;
    }

    public int sensorUpgradeLevel() {
        return upgSensorLvl;
    }

    public boolean hasGrabberUpgrade() {
        return upgGrabberLvl > 0;
    }

    public int grabberUpgradeLevel() {
        return upgGrabberLvl;
    }

    public String droneName() {
        return droneName;
    }

    public int statAttack() {
        return statAttack;
    }

    public int statHealth() {
        return statHealth;
    }

    public int statRange() {
        return statRange;
    }

    public boolean isMagnetDisabled() {
        return magnetDisabled;
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
