package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.CosmeticArmorMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.ToggleCosmeticArmorPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CosmeticArmorScreen extends AbstractContainerScreen<CosmeticArmorMenu> {
    private Button toggleButton;

    public CosmeticArmorScreen(CosmeticArmorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void init() {
        super.init();
        toggleButton = Button.builder(toggleText(), button -> {
            boolean next = !menu.visible();
            menu.setVisible(next);
            button.setMessage(toggleText());
            ModNetwork.CHANNEL.sendToServer(new ToggleCosmeticArmorPacket(next));
        }).bounds(this.leftPos + 118, this.topPos + 34, 50, 20).build();
        this.addRenderableWidget(toggleButton);
    }

    private Component toggleText() {
        return Component.literal(menu.visible() ? "\ud45c\uc2dc ON" : "\ud45c\uc2dc OFF");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFFFFFFFF);
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFFFFFFFF);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF555555);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);

        graphics.fill(x + 7, y + 17, x + 169, y + 62, 0xFFB3B3B3);
        drawPanelBorder(graphics, x + 7, y + 17, 162, 45);
        for (int slotX : CosmeticArmorMenu.COSMETIC_SLOT_X) {
            drawSlot(graphics, x + slotX, y + CosmeticArmorMenu.COSMETIC_SLOT_Y);
        }

        graphics.fill(x + 7, y + 80, x + 169, y + 136, 0xFFB3B3B3);
        graphics.fill(x + 7, y + 140, x + 169, y + 158, 0xFFB3B3B3);
        drawPanelBorder(graphics, x + 7, y + 80, 162, 56);
        drawPanelBorder(graphics, x + 7, y + 140, 162, 18);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(graphics, x + 8 + col * 18, y + 84 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(graphics, x + 8 + col * 18, y + 142);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        String[] labels = { "\uba38\ub9ac", "\ubab8", "\ub2e4\ub9ac", "\ubc1c" };
        for (int i = 0; i < labels.length; i++) {
            graphics.drawCenteredString(this.font, labels[i], CosmeticArmorMenu.COSMETIC_SLOT_X[i] + 8, 22, 0xFF555555);
        }
    }

    private static void drawPanelBorder(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, 0xFF555555);
        graphics.fill(x, y, x + 1, y + height, 0xFF555555);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF555555);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }
}
