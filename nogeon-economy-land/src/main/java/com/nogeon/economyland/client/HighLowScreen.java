package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.HighLowMenu;
import com.nogeon.economyland.network.HighLowActionPacket;
import com.nogeon.economyland.network.ModNetwork;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class HighLowScreen extends AbstractContainerScreen<HighLowMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int TOP_PANEL_BOTTOM = 108;
    private static final int CARD_PANEL_TOP = 116;
    private static final int CARD_TOP = 142;
    private static final int FOOTER_TOP = 218;
    private EditBox raiseStakeBox;

    public HighLowScreen(HighLowMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 392;
        imageHeight = 278;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        if (!menu.hasSession()) {
            raiseStakeBox = new EditBox(font, leftPos + 150, topPos + 96, 92, 18,
                Component.translatable("gui.nogeon_economy_land.gamble_stake"));
            raiseStakeBox.setFilter(value -> value.matches("\\d*"));
            raiseStakeBox.setMaxLength(12);
            raiseStakeBox.setValue("1000");
            raiseStakeBox.setBordered(false);
            raiseStakeBox.setTextColor(0xFFFFD56A);
            addRenderableWidget(raiseStakeBox);
            addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.execute"),
                button -> ModNetwork.CHANNEL.sendToServer(new HighLowActionPacket("start", parsedRaiseStake())))
                .bounds(leftPos + 248, topPos + 96, 64, 18)
                .build());
            addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.high_low_leave"),
                button -> ModNetwork.CHANNEL.sendToServer(new HighLowActionPacket("leave")))
                .bounds(leftPos + 160, topPos + 228, 72, 20)
                .danger(true)
                .build());
            return;
        }

        // Active Session Controls
        Button hitButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.blackjack_hit"),
            button -> ModNetwork.CHANNEL.sendToServer(new HighLowActionPacket("higher")))
            .bounds(leftPos + 26, topPos + 228, 78, 20)
            .build());
        hitButton.active = menu.canGuess(); // canHit

        Button standButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.blackjack_stand"),
            button -> ModNetwork.CHANNEL.sendToServer(new HighLowActionPacket("lower")))
            .bounds(leftPos + 110, topPos + 228, 78, 20)
            .build());
        standButton.active = menu.canCashOut(); // canStand

        Button doubleButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.blackjack_double"),
            button -> ModNetwork.CHANNEL.sendToServer(new HighLowActionPacket("raise")))
            .bounds(leftPos + 194, topPos + 228, 86, 20)
            .build());
        doubleButton.active = menu.canAdvance(); // canDoubleDown

        // Forfeit or Leave Button depending on whether the game is resolved
        boolean resolved = !menu.canGuess() && !menu.canCashOut();
        Component exitLabel = resolved
            ? Component.translatable("gui.nogeon_economy_land.high_low_leave")
            : Component.translatable("gui.nogeon_economy_land.blackjack_forfeit");

        Button leaveButton = addRenderableWidget(HextechButton.hextechBuilder(exitLabel,
            button -> ModNetwork.CHANNEL.sendToServer(new HighLowActionPacket("leave")))
            .bounds(leftPos + 286, topPos + 228, 80, 20)
            .danger(!resolved)
            .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        // 1. Premium Hextech Midnight felt styling
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E); // Base dark void
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141918); // Felt border green tint
        
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF00FFCC); // Neon Cyan top
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF00C8FF); // Neon Blue bottom
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF00FFCC); 
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF00C8FF); 

        // Concept panels
        graphics.fill(x + 18, y + 38, x + imageWidth - 18, y + TOP_PANEL_BOTTOM, 0xFF0E1311);
        drawCustomBorder(graphics, x + 18, y + 38, imageWidth - 36, TOP_PANEL_BOTTOM - 38, 0xFF1B2C27);

        // Win Streak Box (우측 패널 내부)
        int hudX = x + imageWidth - 142;
        int hudY = y + 44;
        graphics.fill(hudX, hudY, hudX + 120, hudY + 46, 0xFF070A08);
        drawCustomBorder(graphics, hudX, hudY, 120, 46, 0xFFFFD56A);

        // Player panel
        graphics.fill(x + 18, y + CARD_PANEL_TOP, x + 184, y + 212, 0xFF0E1311);
        drawCustomBorder(graphics, x + 18, y + CARD_PANEL_TOP, 166, 212 - CARD_PANEL_TOP, 0xFF1B2C27);

        // Dealer panel
        graphics.fill(x + 208, y + CARD_PANEL_TOP, x + imageWidth - 18, y + 212, 0xFF0E1311);
        drawCustomBorder(graphics, x + 208, y + CARD_PANEL_TOP, imageWidth - 226, 212 - CARD_PANEL_TOP, 0xFF1B2C27);

        // Footer panel
        graphics.fill(x + 18, y + FOOTER_TOP, x + imageWidth - 18, y + imageHeight - 6, 0xFF0D1210);
        drawCustomBorder(graphics, x + 18, y + FOOTER_TOP, imageWidth - 36, imageHeight - 6 - FOOTER_TOP, 0xFF1B2C27);
        
        // Bet box felt styling
        if (raiseStakeBox != null) {
            int inputX = raiseStakeBox.getX();
            int inputY = raiseStakeBox.getY();
            int inputW = raiseStakeBox.getWidth();
            int inputH = raiseStakeBox.getHeight();
            graphics.fill(inputX - 2, inputY - 2, inputX + inputW + 2, inputY + inputH + 2, 0xFF0E1311);
            int borderCol = raiseStakeBox.isFocused() ? 0xFF00FFCC : 0xFF1B2C27;
            drawCustomBorder(graphics, inputX - 2, inputY - 2, inputW + 4, inputH + 4, borderCol);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.blackjack_title"), imageWidth / 2, 12, 0xFFF2E3BC);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.blackjack_subtitle"), 24, 28, 0xFF9FA79A, false);

        // Win Streak HUD Text (우측 상단 3줄 카드)
        int streak = menu.gambleStreak();
        com.nogeon.economyland.player.SocialClass socialClass = menu.socialClass();
        long baseCap = socialClass.maxBetCap();
        long maxCap = Math.min(1000000L, Math.round(baseCap * (1.0D + Math.min(10, streak) * 0.1D)));
        int bonusPercent = Math.min(10, streak) * 5;
        
        int txtX = imageWidth - 138;
        graphics.drawString(font, streak + " 연승", txtX, 48, streak > 0 ? 0xFF00FFCC : 0xFF98A49C, false);
        graphics.drawString(font, "보너스: +" + bonusPercent + "%", txtX, 58, 0xFF00FFCC, false);
        graphics.drawString(font, "한도: " + CREDIT_FORMAT.format(maxCap) + " C", txtX, 68, 0xFFFFD56A, false);
        if (!menu.hasSession()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gamble_stake"), 80, 98, 0xFFE8E1C4, false);
            graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.blackjack_help"), imageWidth / 2, 130, 0xFF98A49C);
            return;
        }

        // Active game labels
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.blackjack_rules"), 24, 46, 0xFF9FA79A, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.high_low_stake").append(": ").append(CREDIT_FORMAT.format(menu.stake())).append(" C"), 24, 72, 0xFFE5C067, false);
        graphics.drawString(font, Component.translatable(menu.statusKey()), 24, 86, statusColor(), false);

        // Draw Player Cards & Sum
        List<Integer> pCards = com.nogeon.economyland.state.HighLowSession.parseCards(menu.playerCardsStr());
        int pSum = com.nogeon.economyland.state.HighLowSession.calculateVal(pCards);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.blackjack_player_score", pSum), 26, 126, 0xFFF6E7C3, false);
        for (int i = 0; i < pCards.size(); i++) {
            drawMiniCard(graphics, 26 + i * 20, CARD_TOP, cardLabel(pCards.get(i)), 0xFFF6E7C3);
        }

        // Draw Dealer Cards & Sum
        List<Integer> dCards = com.nogeon.economyland.state.HighLowSession.parseCards(menu.dealerCardsStr());
        String dScoreLabel = dealerScoreStr(dCards);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.blackjack_dealer_score", dScoreLabel), 216, 126, 0xFFE5C067, false);
        for (int i = 0; i < dCards.size(); i++) {
            if (menu.canGuess() && i == 1) { // Hide dealer's second card during player's turn
                drawMiniCard(graphics, 216 + i * 20, CARD_TOP, "?", 0xFF98A49C);
            } else if (menu.canGuess() && i > 1) {
                // Should not happen, but hide any extra cards if player is still playing
            } else {
                drawMiniCard(graphics, 216 + i * 20, CARD_TOP, cardLabel(dCards.get(i)), 0xFFE5C067);
            }
        }

        // Show payout or details at the bottom of cards
        if (menu.payout() > 0L) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.high_low_payout").append(": ").append(CREDIT_FORMAT.format(menu.payout())), 26, 202, 0xFF00FFCC, false);
        } else if (!menu.canGuess() && !menu.canCashOut()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.blackjack_lost_bet"), 26, 202, 0xFFD47B7B, false);
        }
    }

    private String dealerScoreStr(List<Integer> dCards) {
        if (dCards.isEmpty()) return "0";
        if (menu.canGuess()) { // canHit (player's turn)
            int firstCard = dCards.get(0);
            int val = firstCard == 1 ? 11 : (firstCard >= 10 ? 10 : firstCard);
            return val + " + ?";
        } else {
            return String.valueOf(com.nogeon.economyland.state.HighLowSession.calculateVal(dCards));
        }
    }

    private void drawMiniCard(GuiGraphics graphics, int x, int y, String label, int color) {
        int width = 36;
        int height = 54;
        graphics.fill(x, y, x + width, y + height, 0xFF1B2C27);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF0A0F0D);
        drawCustomBorder(graphics, x, y, width, height, 0xFFFFD56A);
        
        graphics.pose().pushPose();
        graphics.pose().scale(1.4F, 1.4F, 1.0F);
        graphics.drawCenteredString(font, label, Math.round((x + width / 2) / 1.4F), Math.round((y + 18) / 1.4F), color);
        graphics.pose().popPose();
    }

    private long parsedRaiseStake() {
        if (raiseStakeBox == null || raiseStakeBox.getValue().isBlank()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(raiseStakeBox.getValue()));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private int statusColor() {
        String key = menu.statusKey();
        if (key.endsWith("win") || key.endsWith("jackpot")) {
            return 0xFF8ED79E;
        }
        if (key.endsWith("lose")) {
            return 0xFFD47B7B;
        }
        return 0xFF9FA79A;
    }

    private String cardLabel(int value) {
        return switch (value) {
            case 1 -> "A";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            default -> String.valueOf(value);
        };
    }

    private void drawCustomBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
