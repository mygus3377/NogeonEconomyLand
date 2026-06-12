package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.TradeBrowserMenu;
import com.nogeon.economyland.menu.TradeTargetLine;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.OpenWalletPacket;
import com.nogeon.economyland.network.TradeRequestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TradeBrowserScreen extends AbstractContainerScreen<TradeBrowserMenu> {
    public TradeBrowserScreen(TradeBrowserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320;
        imageHeight = 220;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.wallet_tab"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenWalletPacket()))
            .bounds(leftPos + 20, topPos + 188, 58, 20)
            .build());
            
        int row = 0;
        for (TradeTargetLine line : menu.lines()) {
            if (row >= 7) {
                break;
            }
            int y = topPos + 50 + row * 20;
            Button requestButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_request"),
                button -> ModNetwork.CHANNEL.sendToServer(new TradeRequestPacket(line.playerId())))
                .bounds(leftPos + 246, y - 2, 54, 18)
                .build());
            requestButton.active = !line.busy();
            row++;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        // 1. 프리미엄 헥스테크 미드나이트-다크 & 시안 네온 그라데이션 라인 테두리
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E); // 칠흑
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141918); // 미드나이트 그린 내벽
        
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF00FFCC); // 상단 Cyan 네온
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 하단 Blue 네온
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF00FFCC); // 좌측
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 우측

        // 2. 주변 플레이어 목록 챔버 판넬
        graphics.fill(x + 16, y + 38, x + imageWidth - 16, y + 180, 0xFF0E1311);
        drawCustomBorder(graphics, x + 16, y + 38, imageWidth - 32, 142, 0xFF1B2C27);
        graphics.fill(x + 16, y + 38, x + 18, y + 180, 0xFF00FFCC); // 리스트 가이드 네온 액센트
    }

    private void drawCustomBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFF00FFCC);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_nearby_hint"), 22, 28, 0xFF769B8E, false);
        int row = 0;
        for (TradeTargetLine line : menu.lines()) {
            if (row >= 7) {
                break;
            }
            int y = 52 + row * 20;
            graphics.drawString(font, Component.literal(line.name()), 24, y, 0xFFE8E1C4, false);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_distance").append(": ").append(String.valueOf(line.distance())), 128, y, 0xFF98A49C, false);
            if (line.busy()) {
                graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_busy"), 210, y, 0xFFFF5555, false);
            }
            row++;
        }
        if (menu.lines().isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_none_nearby"), 24, 54, 0xFF4A6057, false);
        }
    }
}