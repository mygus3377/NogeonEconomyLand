package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.HomeSummary;
import com.nogeon.economyland.menu.PortalMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.PortalTeleportPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public final class PortalScreen extends AbstractContainerScreen<PortalMenu> {
    private static final int VISIBLE_ROWS = 5;
    private int scrollOffset;

    public PortalScreen(PortalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 260;
        this.imageHeight = 160;
        this.inventoryLabelY = 10000; // Hide player inventory labels
    }

    @Override
    protected void init() {
        super.init();
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxScroll());

        int startY = this.topPos + 32;
        int listSize = this.menu.homes().size();
        for (int i = 0; i < Math.min(VISIBLE_ROWS, listSize - this.scrollOffset); i++) {
            HomeSummary home = this.menu.homes().get(this.scrollOffset + i);
            int rowY = startY + i * 22;

            this.addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.go"),
                button -> {
                    ModNetwork.CHANNEL.sendToServer(new PortalTeleportPacket(home.name()));
                    if (this.minecraft.player != null) {
                        this.minecraft.player.closeContainer();
                    }
                })
                .bounds(this.leftPos + 196, rowY - 2, 48, 18)
                .build());
        }
    }

    private int maxScroll() {
        return Math.max(0, this.menu.homes().size() - VISIBLE_ROWS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= this.leftPos + 12 && mouseX < this.leftPos + 248 && mouseY >= this.topPos + 28 && mouseY < this.topPos + 144 && maxScroll() > 0) {
            int previous = this.scrollOffset;
            this.scrollOffset = Mth.clamp(this.scrollOffset + (delta < 0.0D ? 1 : -1), 0, maxScroll());
            if (this.scrollOffset != previous) {
                this.init();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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

        // 1. Midnight Dark Hextech BG
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFA0B0F0E); 
        graphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFA141918); 

        // Neon Glow border guides
        graphics.fill(x, y, x + this.imageWidth, y + 1, 0xFF00FFCC); // Top Cyan Neon
        graphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF00C8FF); // Bottom Blue Neon
        graphics.fill(x, y, x + 1, y + this.imageHeight, 0xFF00FFCC); // Left
        graphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF00C8FF); // Right

        // 2. Scroll Chamber Box
        graphics.fill(x + 12, y + 28, x + 248, y + 144, 0xFF0E1311);
        drawCustomBorder(graphics, x + 12, y + 28, 236, 116, 0xFF1B2C27);
    }

    private void drawCustomBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(this.font, this.title, this.imageWidth / 2, 8, 0xFF00FFCC);
        
        int startY = 32;
        int listSize = this.menu.homes().size();
        for (int i = 0; i < Math.min(VISIBLE_ROWS, listSize - this.scrollOffset); i++) {
            HomeSummary home = this.menu.homes().get(this.scrollOffset + i);
            int rowY = startY + i * 22;
            
            // Name
            graphics.drawString(this.font, this.font.plainSubstrByWidth(home.name(), 56), 18, rowY, 0xFFE8E1C4, false);
            // Memo
            graphics.drawString(this.font, this.font.plainSubstrByWidth(home.memo().isEmpty() ? "-" : home.memo(), 50), 78, rowY, 0xFF7E887D, false);
            // Pos
            String locationStr = shortWorld(home.world()) + " " + home.x() + "," + home.z();
            graphics.drawString(this.font, this.font.plainSubstrByWidth(locationStr, 56), 132, rowY, 0xFF98A49C, false);
        }

        if (maxScroll() > 0) {
            graphics.drawString(this.font, (this.scrollOffset + 1) + "/" + (maxScroll() + 1), 220, 147, 0xFF98A49C, false);
            
            int trackX = 244;
            int trackTop = 28;
            int trackHeight = 116;
            graphics.fill(trackX, trackTop, trackX + 3, trackTop + trackHeight, 0xFF10140F);
            
            int handleHeight = Math.max(12, trackHeight * VISIBLE_ROWS / listSize);
            int handleTop = trackTop + (trackHeight - handleHeight) * this.scrollOffset / maxScroll();
            graphics.fill(trackX, handleTop, trackX + 3, handleTop + handleHeight, 0xFF8A8268);
        }

        if (this.menu.homes().isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.nogeon_economy_land.no_saved_homes"), this.imageWidth / 2, 70, 0xFF98A49C);
        }

        // Teleport warning note
        graphics.drawCenteredString(this.font, Component.translatable("gui.nogeon_economy_land.portal_teleport_cost_info"), this.imageWidth / 2, 148, 0xFFE57373);
    }

    private String shortWorld(String worldKey) {
        int separator = worldKey.indexOf(':');
        return separator >= 0 && separator + 1 < worldKey.length() ? worldKey.substring(separator + 1) : worldKey;
    }
}
