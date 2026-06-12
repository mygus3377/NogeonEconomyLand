package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.TradeChatLine;
import com.nogeon.economyland.menu.TradeMenu;
import com.nogeon.economyland.menu.TradeLandLine;
import com.nogeon.economyland.menu.TradeOfferLine;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.TradeCancelPacket;
import com.nogeon.economyland.network.TradeChatPacket;
import com.nogeon.economyland.network.TradeClearOfferPacket;
import com.nogeon.economyland.network.TradeConfirmPacket;
import com.nogeon.economyland.network.OpenTradeItemPacket;
import com.nogeon.economyland.network.TradeOfferLandPacket;
import com.nogeon.economyland.network.TradeSetCreditsPacket;
import com.nogeon.economyland.network.TradeToggleReadyPacket;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import net.minecraft.world.entity.player.Inventory;

public final class TradeScreen extends AbstractContainerScreen<TradeMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private EditBox creditBox;
    private EditBox landIdBox;
    private EditBox chatBox;
    private Button exitYesButton;
    private Button exitNoButton;
    private boolean exitDialog;

    public TradeScreen(TradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 468;
        imageHeight = 352;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        creditBox = new EditBox(font, leftPos + 110, topPos + 208, 54, 18, Component.translatable("gui.nogeon_economy_land.trade_credit"));
        creditBox.setValue(String.valueOf(menu.offeredCredits()));
        creditBox.setFilter(this::isDigits);
        addRenderableWidget(creditBox);
        
        landIdBox = new EditBox(font, leftPos + 110, topPos + 234, 54, 18, Component.translatable("gui.nogeon_economy_land.trade_land_id"));
        landIdBox.setFilter(this::isDigits);
        addRenderableWidget(landIdBox);
        
        chatBox = new EditBox(font, leftPos + 260, topPos + 312, 138, 18, Component.translatable("gui.nogeon_economy_land.trade_chat_input"));
        chatBox.setMaxLength(96);
        addRenderableWidget(chatBox);

        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_offer_inventory"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenTradeItemPacket()))
            .bounds(leftPos + 22, topPos + 206, 82, 20)
            .build());
            
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_apply_credit"),
            button -> ModNetwork.CHANNEL.sendToServer(new TradeSetCreditsPacket(menu.partnerId(), readLong(creditBox.getValue()))))
            .bounds(leftPos + 168, topPos + 206, 60, 20)
            .build());
            
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_offer_land"),
            button -> ModNetwork.CHANNEL.sendToServer(new TradeOfferLandPacket(menu.partnerId(), (int) readLong(landIdBox.getValue()))))
            .bounds(leftPos + 168, topPos + 232, 60, 20)
            .build());
            
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_clear"),
            button -> ModNetwork.CHANNEL.sendToServer(new TradeClearOfferPacket(menu.partnerId())))
            .bounds(leftPos + 22, topPos + 260, 60, 20)
            .build());
            
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable(menu.ready() ? "gui.nogeon_economy_land.trade_unready" : "gui.nogeon_economy_land.trade_ready"),
            button -> ModNetwork.CHANNEL.sendToServer(new TradeToggleReadyPacket(menu.partnerId())))
            .bounds(leftPos + 86, topPos + 260, 66, 20)
            .build());
            
        Button confirmButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_confirm"),
            button -> ModNetwork.CHANNEL.sendToServer(new TradeConfirmPacket(menu.partnerId())))
            .bounds(leftPos + 156, topPos + 260, 72, 20)
            .build());
        confirmButton.active = menu.ready() && menu.partnerReady() && !menu.confirmed();
        
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_cancel"),
            button -> cancelTrade())
            .bounds(leftPos + 378, topPos + 12, 72, 18)
            .build());
            
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_chat_send"),
            button -> sendTradeChat())
            .bounds(leftPos + 402, topPos + 312, 44, 20)
            .build());

        exitYesButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_exit_yes"),
            button -> cancelTrade())
            .bounds(leftPos + 158, topPos + 188, 70, 22)
            .danger(true)
            .build());
        exitNoButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_exit_no"),
            button -> hideExitDialog())
            .bounds(leftPos + 240, topPos + 188, 70, 22)
            .build());
        hideExitDialog();
    }

    private boolean isDigits(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private long readLong(String value) {
        if (value.isEmpty()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        // 1. 프리미엄 헥스테크 미드나이트-다크 & 시안 네온 그라데이션 외곽선
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E); // 칠흑
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141918); // 미드나이트 그린 내벽
        
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF00FFCC); // 상단 Cyan 네온
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 하단 Blue 네온
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF00FFCC); // 좌측
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 우측

        // 2. 격자 내부 챔버 분할 및 네온 라인 테두리 장식
        // 좌상단 내 거래 물품 제안 영역
        graphics.fill(x + 16, y + 38, x + 220, y + 122, 0xFF0E1311);
        drawCustomBorder(graphics, x + 16, y + 38, 204, 84, 0xFF1B2C27);

        // 우상단 상대방 거래 물품 제안 영역
        graphics.fill(x + 248, y + 38, x + imageWidth - 16, y + 122, 0xFF0E1311);
        drawCustomBorder(graphics, x + 248, y + 38, 204, 84, 0xFF1B2C27);

        // 좌중단 내 거래 토지 제안 영역
        graphics.fill(x + 16, y + 128, x + 220, y + 184, 0xFF111715);
        drawCustomBorder(graphics, x + 16, y + 128, 204, 56, 0xFF22312A);

        // 우중단 상대방 거래 토지 제안 영역
        graphics.fill(x + 248, y + 128, x + imageWidth - 16, y + 184, 0xFF111715);
        drawCustomBorder(graphics, x + 248, y + 128, 204, 56, 0xFF22312A);

        // 좌하단 내 거래 제안 컨트롤 패널
        graphics.fill(x + 16, y + 190, x + 236, y + 338, 0xFF0E1311);
        drawCustomBorder(graphics, x + 16, y + 190, 220, 148, 0xFF1B2C27);
        graphics.fill(x + 16, y + 190, x + 18, y + 338, 0xFF00FFCC); // 가이드 네온 액센트

        // 우하단 상대방 및 채팅 상태 판넬
        graphics.fill(x + 248, y + 190, x + imageWidth - 16, y + 338, 0xFF111715);
        drawCustomBorder(graphics, x + 248, y + 190, 204, 148, 0xFF22312A);

        // 채팅창 히스토리 박스 테두리
        graphics.fill(x + 256, y + 248, x + imageWidth - 24, y + 304, 0xFF0E1311);
        drawCustomBorder(graphics, x + 256, y + 248, 188, 56, 0xFF1B2C27);
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
        if (exitDialog) {
            renderExitDialog(graphics, mouseX, mouseY, partialTick);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFF00FFCC);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_partner", menu.partnerName()), 22, 28, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_my_offer"), 24, 44, 0xFF00FFCC, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_partner_offer"), 256, 44, 0xFF00C8FF, false);
        drawOffers(graphics, menu.ownOffers(), 24, 62);
        drawOffers(graphics, menu.partnerOffers(), 256, 62);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_my_land_offer"), 24, 134, 0xFF00FFCC, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_partner_land_offer"), 256, 134, 0xFF00C8FF, false);
        drawLandOffers(graphics, menu.ownLandOffers(), 24, 150);
        drawLandOffers(graphics, menu.partnerLandOffers(), 256, 150);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_land_hint"), 24, 172, 0xFF769B8E, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_credit"), 22, 198, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_land_id"), 22, 224, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_have_credit").append(": ").append(CREDIT_FORMAT.format(menu.availableCredits())), 256, 198, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_my_credit").append(": ").append(CREDIT_FORMAT.format(menu.offeredCredits())), 256, 212, 0xFFFFD56A, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_partner_credit").append(": ").append(CREDIT_FORMAT.format(menu.partnerCredits())), 256, 226, 0xFFFFD56A, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_chat_title"), 256, 238, 0xFF00FFCC, false);
        
        // 준비/확정 지시 인디케이터 컬러 대조 보강
        graphics.drawString(font, Component.translatable(menu.ready() ? "gui.nogeon_economy_land.trade_ready_on" : "gui.nogeon_economy_land.trade_ready_off"), 340, 44, menu.ready() ? 0xFF55FF55 : 0xFFFF5555, false);
        graphics.drawString(font, Component.translatable(menu.partnerReady() ? "gui.nogeon_economy_land.trade_partner_ready_on" : "gui.nogeon_economy_land.trade_partner_ready_off"), 370, 44, menu.partnerReady() ? 0xFF55FF55 : 0xFFFF5555, false);
        
        int statusY = 28;
        graphics.drawString(font, Component.translatable(menu.confirmed() ? "gui.nogeon_economy_land.trade_confirmed_on" : "gui.nogeon_economy_land.trade_confirmed_off"), 256, statusY, menu.confirmed() ? 0xFF55FF55 : 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable(menu.partnerConfirmed() ? "gui.nogeon_economy_land.trade_partner_confirmed_on" : "gui.nogeon_economy_land.trade_partner_confirmed_off"), 360, statusY, menu.partnerConfirmed() ? 0xFF55FF55 : 0xFF98A49C, false);
        
        drawChatLines(graphics, menu.chatLines(), 260, 252, 180, 4);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (exitDialog) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                hideExitDialog();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                cancelTrade();
                return true;
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            showExitDialog();
            return true;
        }
        if (chatBox != null && chatBox.isFocused() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            sendTradeChat();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (exitDialog) {
            if (exitYesButton != null && exitYesButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (exitNoButton != null && exitNoButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        showExitDialog();
    }

    private void drawOffers(GuiGraphics graphics, List<TradeOfferLine> offers, int x, int startY) {
        int row = 0;
        for (TradeOfferLine line : offers) {
            if (row >= 3) {
                break;
            }
            int y = startY + row * 18;
            graphics.drawString(font, Component.translatable(line.itemKey()), x, y, 0xFFE8E1C4, false);
            graphics.drawString(font, "x" + line.count(), x + 128, y, 0xFFFFD56A, false);
            row++;
        }
        if (offers.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_empty_offer"), x, startY, 0xFF4A6057, false);
        }
    }

    private void drawLandOffers(GuiGraphics graphics, List<TradeLandLine> lands, int x, int startY) {
        int row = 0;
        for (TradeLandLine line : lands) {
            if (row >= 2) {
                break;
            }
            int y = startY + row * 16;
            graphics.drawString(font, "#" + line.landId() + " ", x, y, 0xFFFFD56A, false);
            graphics.drawString(font, Component.translatable(line.typeKey()), x + 30, y, 0xFFE8E1C4, false);
            graphics.drawString(font, line.blocks() + "B", x + 120, y, 0xFF98A49C, false);
            row++;
        }
        if (lands.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_empty_land_offer"), x, startY, 0xFF4A6057, false);
        }
    }

    private void drawChatLines(GuiGraphics graphics, List<TradeChatLine> lines, int x, int startY, int width, int maxRows) {
        int visible = Math.min(maxRows, lines.size());
        int startIndex = Math.max(0, lines.size() - visible);
        for (int row = 0; row < visible; row++) {
            TradeChatLine line = lines.get(startIndex + row);
            String prefix = line.own() ? "나" : line.senderName();
            String text = font.plainSubstrByWidth(prefix + ": " + line.message(), width);
            graphics.drawString(font, text, x, startY + row * 12, line.own() ? 0xFF00FFCC : 0xFFE8E1C4, false);
        }
        if (lines.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_chat_empty"), x, startY + 12, 0xFF4A6057, false);
        }
    }

    private void sendTradeChat() {
        if (chatBox == null) {
            return;
        }
        String message = chatBox.getValue().trim();
        if (message.isEmpty()) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new TradeChatPacket(menu.partnerId(), message));
        chatBox.setValue("");
    }

    private void showExitDialog() {
        exitDialog = true;
        if (exitYesButton != null) {
            exitYesButton.visible = true;
            exitYesButton.active = true;
        }
        if (exitNoButton != null) {
            exitNoButton.visible = true;
            exitNoButton.active = true;
        }
    }

    private void hideExitDialog() {
        exitDialog = false;
        if (exitYesButton != null) {
            exitYesButton.visible = false;
            exitYesButton.active = false;
        }
        if (exitNoButton != null) {
            exitNoButton.visible = false;
            exitNoButton.active = false;
        }
    }

    private void cancelTrade() {
        ModNetwork.CHANNEL.sendToServer(new TradeCancelPacket(menu.partnerId()));
    }

    private void renderExitDialog(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xAA000000);
        int boxX = leftPos + 112;
        int boxY = topPos + 128;
        int boxW = 244;
        int boxH = 96;
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF101614);
        drawCustomBorder(graphics, boxX, boxY, boxW, boxH, 0xFF00FFCC);
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.trade_exit_title"), leftPos + imageWidth / 2, boxY + 18, 0xFFE8E1C4);
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.trade_exit_question"), leftPos + imageWidth / 2, boxY + 40, 0xFF98A49C);
        if (exitYesButton != null) {
            exitYesButton.render(graphics, mouseX, mouseY, partialTick);
        }
        if (exitNoButton != null) {
            exitNoButton.render(graphics, mouseX, mouseY, partialTick);
        }
    }
}
