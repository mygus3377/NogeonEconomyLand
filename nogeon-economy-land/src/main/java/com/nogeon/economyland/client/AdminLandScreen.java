package com.nogeon.economyland.client;

import com.nogeon.economyland.land.LandFlag;
import com.nogeon.economyland.menu.AdminLandMenu;
import com.nogeon.economyland.menu.LandSummary;
import com.nogeon.economyland.network.AdminLandFlagPacket;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.OpenLandHomePacket;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AdminLandScreen extends AbstractContainerScreen<AdminLandMenu> {
    private final Map<LandFlag, Button> flagButtons = new EnumMap<>(LandFlag.class);
    private int landIndex;

    public AdminLandScreen(AdminLandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 312;
        imageHeight = 208;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        landIndex = Math.max(0, Math.min(landIndex, Math.max(0, menu.lands().size() - 1)));
        addRenderableWidget(Button.builder(Component.literal("<"), button -> shiftLand(-1))
            .bounds(leftPos + 214, topPos + 44, 20, 18)
            .build()).active = menu.lands().size() > 1;
        addRenderableWidget(Button.builder(Component.literal(">"), button -> shiftLand(1))
            .bounds(leftPos + 268, topPos + 44, 20, 18)
            .build()).active = menu.lands().size() > 1;
        addRenderableWidget(Button.builder(Component.translatable("screen.nogeon_economy_land.land_home"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenLandHomePacket()))
            .bounds(leftPos + 22, topPos + 168, 96, 20)
            .build());

        flagButtons.clear();
        int row = 0;
        for (LandFlag flag : LandFlag.values()) {
            Button button = addRenderableWidget(Button.builder(Component.empty(), ignored -> toggleFlag(flag))
                .bounds(leftPos + 216, topPos + 92 + row * 22, 72, 18)
                .build());
            flagButtons.put(flag, button);
            row++;
        }
        updateFlagButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xF0181814);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xF026281F);
        graphics.fill(x + 16, y + 36, x + 194, y + 154, 0xFF171B18);
        graphics.fill(x + 204, y + 36, x + imageWidth - 16, y + 154, 0xFF252D25);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFFE8E1C4);
        LandSummary land = currentLand();
        if (land == null) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin_land_empty"), 24, 54, 0xFF98A49C, false);
            return;
        }
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin_land"), 24, 48, 0xFFFFD56A, false);
        graphics.drawString(font, "#" + land.id() + "  " + land.blocks() + "B", 24, 68, 0xFFE8E1C4, false);
        graphics.drawString(font, land.world(), 24, 84, 0xFF98A49C, false);
        graphics.drawString(font, land.x() + ", " + land.y() + ", " + land.z(), 24, 100, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_page", landIndex + 1, menu.lands().size()), 238, 49, 0xFF98A49C, false);

        int row = 0;
        for (LandFlag flag : LandFlag.values()) {
            drawClippedText(graphics, Component.translatable(flag.translationKey()), 214, 80 + row * 22, 76, 0xFFAEB7A0);
            row++;
        }
    }

    private void drawClippedText(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }

    private void toggleFlag(LandFlag flag) {
        LandSummary land = currentLand();
        if (land != null) {
            boolean current = land.flags().getOrDefault(flag.id(), flag.defaultValue());
            ModNetwork.CHANNEL.sendToServer(new AdminLandFlagPacket(land.id(), flag.id(), !current));
        }
    }

    private void shiftLand(int delta) {
        if (!menu.lands().isEmpty()) {
            landIndex = (landIndex + delta + menu.lands().size()) % menu.lands().size();
            updateFlagButtons();
        }
    }

    private void updateFlagButtons() {
        LandSummary land = currentLand();
        for (Map.Entry<LandFlag, Button> entry : flagButtons.entrySet()) {
            LandFlag flag = entry.getKey();
            Button button = entry.getValue();
            boolean enabled = land != null && land.flags().getOrDefault(flag.id(), flag.defaultValue());
            button.setMessage(Component.literal(enabled ? "ON" : "OFF"));
            button.active = land != null;
        }
    }

    private LandSummary currentLand() {
        if (menu.lands().isEmpty()) {
            return null;
        }
        landIndex = Math.max(0, Math.min(landIndex, menu.lands().size() - 1));
        return menu.lands().get(landIndex);
    }
}
