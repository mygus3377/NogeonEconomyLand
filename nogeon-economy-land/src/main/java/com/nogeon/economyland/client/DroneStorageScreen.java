package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.DroneStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DroneStorageScreen extends AbstractContainerScreen<DroneStorageMenu> {
    public DroneStorageScreen(DroneStorageMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 238;
        this.inventoryLabelY = 145;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // 1. Sleek Cyberpunk Background
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0D);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141917);

        // 2. Cyan/Blue Neon Outline
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF00FFCC);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF00C8FF);
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF00FFCC);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF00C8FF);

        int droneLvl = menu.getDroneLevel();

        // 3. Draw Slots with clean spacing (17x17 border, 1px gap)
        for (Slot slot : menu.slots) {
            int sx = x + slot.x;
            int sy = y + slot.y;
            int slotIdx = slot.getSlotIndex();

            // Player slots are handled separately
            if (slot.container == menu.slots.get(47).container) {
                // Player Inventory slots
                graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF2A3D37);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF0A0F0D);
                continue;
            }

            if (slotIdx == 0) {
                // Gun Equip Slot
                graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF00FFCC);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF0D1614);
                if (!slot.hasItem()) {
                    graphics.drawCenteredString(font, Component.literal("총"), sx + 8, sy + 4, 0x7700FFCC);
                }
            } else if (slotIdx == 1) {
                // Ammo Equip Slot
                graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF00FFCC);
                graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF0D1614);
                if (!slot.hasItem()) {
                    graphics.drawCenteredString(font, Component.literal("탄"), sx + 8, sy + 4, 0x7700FFCC);
                }
            } else {
                // Storage Slots (2~46)
                int storageRow = (slotIdx - 2) / 9;
                boolean unlocked = storageRow < droneLvl;

                if (unlocked) {
                    graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF22574A);
                    graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF0A0F0D);
                } else {
                    // Locked Vault Slots (Dark red outline with 'x' locked marker)
                    graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF4A181C);
                    graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF140809);
                    if (!slot.hasItem()) {
                        graphics.drawCenteredString(font, Component.literal("✕"), sx + 8, sy + 4, 0x55FF3333);
                    }
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int droneLvl = menu.getDroneLevel();
        Component titleComponent = Component.literal("드론 보관함 §7(등급: §bLv." + droneLvl + "§7)");
        graphics.drawString(font, titleComponent, titleLabelX, titleLabelY, 0xFF00FFCC, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF769B8E, false);

        // Render Equip Slot Labels (y=39 to create comfortable spacing)
        graphics.drawString(font, Component.literal("장갑총 장착"), 36, 39, 0xFF769B8E, false);
        graphics.drawString(font, Component.literal("탄약고 장착"), 108, 39, 0xFF769B8E, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        
        // Show Locked warning tooltip on hovered locked slots
        Slot hovered = getHoveredSlot(mouseX, mouseY);
        if (hovered != null && hovered.getSlotIndex() >= 2 && hovered.getSlotIndex() < 47) {
            int row = (hovered.getSlotIndex() - 2) / 9;
            if (row >= menu.getDroneLevel()) {
                graphics.renderTooltip(font, Component.literal("§c이 슬롯은 잠겨 있습니다! (보관함 강화 필요)"), mouseX, mouseY);
            }
        }
    }

    private Slot getHoveredSlot(int mouseX, int mouseY) {
        for (Slot slot : menu.slots) {
            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }
}
