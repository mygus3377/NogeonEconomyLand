package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.TradeRequestMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.TradeRespondPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TradeRequestScreen extends AbstractContainerScreen<TradeRequestMenu> {
    private boolean responded;

    public TradeRequestScreen(TradeRequestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 260;
        imageHeight = 138;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        // 수락 단추: 일반 시안 네온
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_accept"),
            button -> respond(true))
            .bounds(leftPos + 36, topPos + 92, 74, 20)
            .build());
            
        // 거절 단추: 레드 네온 경고 단추
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_deny"),
            button -> respond(false))
            .bounds(leftPos + 150, topPos + 92, 74, 20)
            .danger(true)
            .build());
    }

    @Override
    public void onClose() {
        if (!responded) {
            respond(false);
        }
        super.onClose();
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

        // 2. 중앙 내용 챔버 판넬
        graphics.fill(x + 16, y + 36, x + imageWidth - 16, y + 84, 0xFF0E1311);
        drawCustomBorder(graphics, x + 16, y + 36, imageWidth - 32, 48, 0xFF1B2C27);
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
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.trade_request_from", menu.requesterName()), imageWidth / 2, 42, 0xFFE8E1C4);
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.trade_request_question"), imageWidth / 2, 58, 0xFF98A49C);
    }

    private void respond(boolean accept) {
        responded = true;
        ModNetwork.CHANNEL.sendToServer(new TradeRespondPacket(menu.requesterId(), accept));
    }
}
