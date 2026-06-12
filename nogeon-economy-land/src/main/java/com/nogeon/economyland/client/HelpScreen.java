package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.HelpMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class HelpScreen extends AbstractContainerScreen<HelpMenu> {
    private int page;

    public HelpScreen(HelpMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 292;
        imageHeight = 206;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page = Math.max(0, page - 1);
            rebuildWidgets();
        }).bounds(leftPos + 16, topPos + 174, 36, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page = Math.min(3, page + 1);
            rebuildWidgets();
        }).bounds(leftPos + imageWidth - 52, topPos + 174, 36, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.nogeon_economy_land.close"), button -> onClose())
            .bounds(leftPos + 108, topPos + 174, 76, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF0181814);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xF0272A22);
        graphics.fill(leftPos + 16, topPos + 36, leftPos + imageWidth - 16, topPos + 164, 0xFF121814);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 14, 0xFFE8E1C4);
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.help_page", page + 1, 4), imageWidth / 2, 178, 0xFFBFC7A7);
        int y = 48;
        for (Component line : lines()) {
            graphics.drawString(font, line, 28, y, 0xFFE8E1C4, false);
            y += 15;
        }
    }

    private List<Component> lines() {
        return switch (page) {
            case 1 -> List.of(
                Component.translatable("help.nogeon_economy_land.jobs.1"),
                Component.translatable("help.nogeon_economy_land.jobs.2"),
                Component.translatable("help.nogeon_economy_land.jobs.3"),
                Component.translatable("help.nogeon_economy_land.jobs.4"),
                Component.translatable("help.nogeon_economy_land.jobs.5")
            );
            case 2 -> List.of(
                Component.translatable("help.nogeon_economy_land.traders.1"),
                Component.translatable("help.nogeon_economy_land.traders.2"),
                Component.translatable("help.nogeon_economy_land.traders.3"),
                Component.translatable("help.nogeon_economy_land.traders.4"),
                Component.translatable("help.nogeon_economy_land.traders.5")
            );
            case 3 -> List.of(
                Component.translatable("help.nogeon_economy_land.systems.1"),
                Component.translatable("help.nogeon_economy_land.systems.2"),
                Component.translatable("help.nogeon_economy_land.systems.3"),
                Component.translatable("help.nogeon_economy_land.systems.4"),
                Component.translatable("help.nogeon_economy_land.systems.5")
            );
            default -> List.of(
                Component.translatable("help.nogeon_economy_land.start.1"),
                Component.translatable("help.nogeon_economy_land.start.2"),
                Component.translatable("help.nogeon_economy_land.start.3"),
                Component.translatable("help.nogeon_economy_land.start.4"),
                Component.translatable("help.nogeon_economy_land.start.5")
            );
        };
    }
}
