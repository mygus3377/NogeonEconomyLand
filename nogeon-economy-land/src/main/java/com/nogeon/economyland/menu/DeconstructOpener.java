package com.nogeon.economyland.menu;

import com.nogeon.economyland.item.SmithingService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public final class DeconstructOpener {
    private DeconstructOpener() {
    }

    public static void open(ServerPlayer player, int selectedSlot, Component status) {
        open(player, selectedSlot, status, false);
    }

    public static void open(ServerPlayer player, int selectedSlot, Component status, boolean smithyMode) {
        int defaultTab = player.getPersistentData().getBoolean("nogeon_engineer_drone_broken") ? 2 : 0;
        open(player, selectedSlot, status, defaultTab, smithyMode);
    }

    public static void open(ServerPlayer player, int selectedSlot, Component status, int currentTab) {
        open(player, selectedSlot, status, currentTab, false);
    }

    public static void open(ServerPlayer player, int selectedSlot, Component status, int currentTab, boolean smithyMode) {
        int resolvedSlot = SmithingService.normalizeSelectedSlot(player, selectedSlot);
        Component resolvedStatus = status == null
            ? SmithingService.defaultStatus(SmithingService.stackForSlot(player, resolvedSlot))
            : status;
        String autoFuel = player.getPersistentData().getString("nogeon_engineer_drone_autofuel_item");
        boolean broken = player.getPersistentData().getBoolean("nogeon_engineer_drone_broken");
        ItemStack gunStack = player.getPersistentData().contains("nogeon_engineer_drone_gun")
            ? ItemStack.of(player.getPersistentData().getCompound("nogeon_engineer_drone_gun"))
            : ItemStack.EMPTY;
        ItemStack ammoStack = player.getPersistentData().contains("nogeon_engineer_drone_ammo")
            ? ItemStack.of(player.getPersistentData().getCompound("nogeon_engineer_drone_ammo"))
            : ItemStack.EMPTY;
        int rawInvLvl = player.getPersistentData().getInt("nogeon_engineer_drone_upgrade_inventory_level");
        if (rawInvLvl <= 0 && player.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_inventory")) {
            rawInvLvl = 1;
            player.getPersistentData().putInt("nogeon_engineer_drone_upgrade_inventory_level", 1);
        }
        int upgInvLvl = rawInvLvl;
        int upgTransLvl = player.getPersistentData().getInt("nogeon_engineer_drone_upgrade_transmitter_level");
        if (upgTransLvl <= 0 && player.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_transmitter")) {
            upgTransLvl = 1;
            player.getPersistentData().putInt("nogeon_engineer_drone_upgrade_transmitter_level", 1);
        }
        int upgBoostLvl = player.getPersistentData().getInt("nogeon_engineer_drone_upgrade_booster_level");
        if (upgBoostLvl <= 0 && player.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_booster")) {
            upgBoostLvl = 1;
            player.getPersistentData().putInt("nogeon_engineer_drone_upgrade_booster_level", 1);
        }
        int upgSensorLvl = player.getPersistentData().getInt("nogeon_engineer_drone_upgrade_sensor_level");
        if (upgSensorLvl <= 0 && player.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_sensor")) {
            upgSensorLvl = 1;
            player.getPersistentData().putInt("nogeon_engineer_drone_upgrade_sensor_level", 1);
        }
        int upgGrabberLvl = player.getPersistentData().getInt("nogeon_engineer_drone_upgrade_grabber_level");
        if (upgGrabberLvl <= 0 && player.getPersistentData().getBoolean("nogeon_engineer_drone_upgrade_grabber")) {
            upgGrabberLvl = 1;
            player.getPersistentData().putInt("nogeon_engineer_drone_upgrade_grabber_level", 1);
        }

        String rawName = player.getPersistentData().getString("nogeon_engineer_drone_name");
        final String droneName = rawName.isEmpty() ? "오토 스크랩 드론" : rawName;
        int rawAttack = player.getPersistentData().getInt("nogeon_engineer_drone_stat_attack");
        final int statAttack = rawAttack <= 0 ? 1 : rawAttack;
        int rawHealth = player.getPersistentData().getInt("nogeon_engineer_drone_stat_health");
        final int statHealth = rawHealth <= 0 ? 1 : rawHealth;
        int rawRange = player.getPersistentData().getInt("nogeon_engineer_drone_stat_range");
        final int statRange = rawRange <= 0 ? 1 : rawRange;
        final boolean magnetDisabled = player.getPersistentData().getBoolean("nogeon_engineer_drone_magnet_disabled");

        final int finalUpgInvLvl = upgInvLvl;
        final int finalUpgTransLvl = upgTransLvl;
        final int finalUpgBoostLvl = upgBoostLvl;
        final int finalUpgSensorLvl = upgSensorLvl;
        final int finalUpgGrabberLvl = upgGrabberLvl;
        final int finalCurrentTab = Math.max(0, Math.min(3, currentTab));

        DeconstructMenu snapshot = new DeconstructMenu(0, resolvedSlot, finalCurrentTab, resolvedStatus, autoFuel, broken, gunStack, ammoStack, finalUpgInvLvl, finalUpgTransLvl, finalUpgBoostLvl, finalUpgSensorLvl, finalUpgGrabberLvl, droneName, statAttack, statHealth, statRange, magnetDisabled, smithyMode);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new DeconstructMenu(containerId, resolvedSlot, finalCurrentTab, resolvedStatus, autoFuel, broken, gunStack, ammoStack, finalUpgInvLvl, finalUpgTransLvl, finalUpgBoostLvl, finalUpgSensorLvl, finalUpgGrabberLvl, droneName, statAttack, statHealth, statRange, magnetDisabled, smithyMode),
            Component.translatable("screen.nogeon_economy_land.drone_control")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
