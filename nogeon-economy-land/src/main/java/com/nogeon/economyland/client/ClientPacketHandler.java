package com.nogeon.economyland.client;

import com.nogeon.economyland.land.LandType;
import com.nogeon.economyland.network.SyncLandSelectionPacket;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void handleOpenCookRecipeScreen(int maxSlots, List<String> selectedBuffs) {
        Minecraft.getInstance().setScreen(new CookRecipeScreen(maxSlots, selectedBuffs));
    }

    public static void handleEnhancedHitVfx(double x, double y, double z, double lookX, double lookZ, int level) {
        ClientForgeEvents.spawnEnhancedHitVfx(x, y, z, lookX, lookZ, level);
    }

    public static void handleSyncPlayerInventory(List<ItemStack> stacks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        int size = Math.min(stacks.size(), minecraft.player.getInventory().getContainerSize());
        for (int slot = 0; slot < size; slot++) {
            minecraft.player.getInventory().setItem(slot, stacks.get(slot).copy());
        }
        if (minecraft.screen instanceof EnhancementScrollScreen screen) {
            screen.refreshFromSyncedInventory();
        }
    }

    public static void handleSyncLandSelection(boolean active, String typeId, ResourceLocation dimensionId,
            List<SyncLandSelectionPacket.CuboidData> cuboids, BlockPos pendingFirst, boolean pendingAdditive) {
        if (!active) {
            ClientLandSelectionData.clear();
            return;
        }
        ClientLandSelectionData.set(LandType.byId(typeId), dimensionId, cuboids, pendingFirst, pendingAdditive);
    }

    public static void handleSyncExtendedInventoryNbt(net.minecraft.nbt.CompoundTag nbt) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu instanceof com.nogeon.economyland.menu.ShopMenu menu) {
            menu.setExtInventoryNbt(nbt);
        }
    }

    public static void handleOpenCompression(int remainingCooldownTicks) {
        Minecraft.getInstance().setScreen(new MechanicalCompressionScreen(remainingCooldownTicks));
    }

    public static void handleCompressionResult(int materialType, ItemStack rolledGem, double rolledPercent, int expGained, int creditsGained) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof MechanicalCompressionScreen screen) {
            screen.startRollAnimation(rolledGem, rolledPercent, expGained, creditsGained);
        }
    }

    public static void handleSyncShopLines(List<com.nogeon.economyland.menu.ShopLine> lines) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.player.containerMenu instanceof com.nogeon.economyland.menu.ShopMenu menu) {
            menu.setLines(lines);
            if (minecraft.screen instanceof ShopScreen screen) {
                screen.refreshShopLines();
            }
        }
    }
}
