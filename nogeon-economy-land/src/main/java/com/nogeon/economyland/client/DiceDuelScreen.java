package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.DiceDuelMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.TraderActionPacket;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;

public final class DiceDuelScreen extends AbstractContainerScreen<DiceDuelMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);

    private final RandomSource random = RandomSource.create();
    private EditBox raiseStakeBox;
    private int animationTicks;
    private int displayPlayerOne = 1;
    private int displayPlayerTwo = 1;
    private int displayDealerOne = 1;
    private int displayDealerTwo = 1;

    public DiceDuelScreen(DiceDuelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 350;
        imageHeight = 248;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        raiseStakeBox = new EditBox(font, leftPos + 122, topPos + 188, 78, 18,
            Component.translatable("gui.nogeon_economy_land.gamble_raise_hint"));
        raiseStakeBox.setFilter(value -> value.matches("\\d*"));
        raiseStakeBox.setMaxLength(12);
        raiseStakeBox.setValue(menu.hasResult() ? "0" : "1000");
        raiseStakeBox.setBordered(false);
        raiseStakeBox.setTextColor(0xFFFFD56A); // Gold input text!
        addRenderableWidget(raiseStakeBox);
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable(menu.hasResult() ? "gui.nogeon_economy_land.gamble_replay" : "gui.nogeon_economy_land.execute"), button -> replay())
            .bounds(leftPos + 208, topPos + 188, 84, 18)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.gamble_back"), button -> onClose())
            .bounds(leftPos + imageWidth / 2 - 36, topPos + 214, 72, 20)
            .danger(true)
            .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!menu.hasResult()) {
            displayPlayerOne = 1;
            displayPlayerTwo = 1;
            displayDealerOne = 1;
            displayDealerTwo = 1;
            return;
        }
        if (animationTicks < 24) {
            animationTicks++;
            displayPlayerOne = random.nextInt(6) + 1;
            displayPlayerTwo = random.nextInt(6) + 1;
            displayDealerOne = random.nextInt(6) + 1;
            displayDealerTwo = random.nextInt(6) + 1;
            return;
        }
        displayPlayerOne = menu.playerDieOne();
        displayPlayerTwo = menu.playerDieTwo();
        displayDealerOne = menu.dealerDieOne();
        displayDealerTwo = menu.dealerDieTwo();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        // 1. 프리미엄 헥스테크 칠흑 및 미드나이트 그린 테두리
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E); // 칠흑
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141918); // 그린 내벽
        
        // Win Streak Box (우측 상단)
        int hudX = x + imageWidth - 128;
        int hudY = y + 8;
        graphics.fill(hudX, hudY, hudX + 120, hudY + 30, 0xFF0E1311);
        drawCustomBorder(graphics, hudX, hudY, 120, 30, 0xFFFFD56A);
        
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF00FFCC); // 상단 Cyan 네온
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 하단 Blue 네온
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF00FFCC); // 좌측
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 우측

        // 플레이어/상인 주사위 챔버
        graphics.fill(x + 18, y + 40, x + 160, y + 176, 0xFF0E1311);
        drawCustomBorder(graphics, x + 18, y + 40, 142, 136, 0xFF1B2C27);

        graphics.fill(x + 190, y + 40, x + imageWidth - 18, y + 176, 0xFF0E1311);
        drawCustomBorder(graphics, x + 190, y + 40, imageWidth - 208, 136, 0xFF1B2C27);
        
        // 배팅 입력창 칠흑 배경 및 포커싱 네온 보더 직접 그리기!
        int inputX = x + 120;
        int inputY = y + 186;
        graphics.fill(inputX, inputY, inputX + 82, inputY + 22, 0xFF0E1311);
        int borderCol = raiseStakeBox.isFocused() ? 0xFF00FFCC : 0xFF1B2C27;
        drawCustomBorder(graphics, inputX, inputY, 82, 22, borderCol);

        drawDie(graphics, x + 42, y + 84 + wobble(0), 44, displayPlayerOne, 0xFFF5ECD8, 0xFF5C4B35);
        drawDie(graphics, x + 98, y + 84 + wobble(4), 44, displayPlayerTwo, 0xFFF5ECD8, 0xFF5C4B35);
        drawDie(graphics, x + 214, y + 84 + wobble(8), 44, displayDealerOne, 0xFFF0E5D2, 0xFF6A4034);
        drawDie(graphics, x + 270, y + 84 + wobble(12), 44, displayDealerTwo, 0xFFF0E5D2, 0xFF6A4034);
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
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFFEADCB8);
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.dice_duel_subtitle"), imageWidth / 2, 28, 0xFF98A49C);
        
        // Win Streak HUD Text
        int streak = menu.gambleStreak();
        com.nogeon.economyland.player.SocialClass socialClass = menu.socialClass();
        long baseCap = socialClass.maxBetCap();
        long maxCap = Math.min(1000000L, Math.round(baseCap * (1.0D + Math.min(10, streak) * 0.1D)));
        int bonusPercent = Math.min(10, streak) * 5;
        
        int txtX = imageWidth - 124;
        graphics.drawString(font, streak + " 연승 (+" + bonusPercent + "%)", txtX, 12, streak > 0 ? 0xFF00FFCC : 0xFF98A49C, false);
        graphics.drawString(font, "한도: " + CREDIT_FORMAT.format(maxCap) + " C", txtX, 22, 0xFFFFD56A, false);
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.gamble_player"), 89, 48, 0xFFEADCB8);
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.gamble_dealer"), 261, 48, 0xFFEADCB8);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gamble_stake").append(": ").append(CREDIT_FORMAT.format(nextStake())).append(" C"), 24, 158, 0xFFFFD56A, false);
        if (menu.hasResult()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gamble_total").append(": ").append(String.valueOf(menu.playerDieOne() + menu.playerDieTwo())), 24, 168, 0xFFE8E1C4, false);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gamble_dealer_total").append(": ").append(String.valueOf(menu.dealerDieOne() + menu.dealerDieTwo())), 194, 168, 0xFFE8E1C4, false);
            graphics.drawCenteredString(font, Component.translatable(animationTicks < 24 ? "gui.nogeon_economy_land.gamble_result_rolling" : menu.resultKey()), imageWidth / 2, 148, 0xFF8FD2A1);
            graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.gamble_payout").append(": ").append(CREDIT_FORMAT.format(menu.payout())).append(" C"), imageWidth / 2, 182, 0xFFE8E1C4);
        }
        graphics.drawString(font, Component.translatable(menu.hasResult() ? "gui.nogeon_economy_land.gamble_raise_hint" : "gui.nogeon_economy_land.gamble_stake"), 24, 192, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gamble_next_stake").append(": ").append(CREDIT_FORMAT.format(nextStake())).append(" C"), 24, 206, 0xFFFFD56A, false);
    }

    private void replay() {
        ModNetwork.CHANNEL.sendToServer(new TraderActionPacket("gambler", "dice_duel", nextStake()));
    }

    private long nextStake() {
        return menu.hasResult() ? menu.stake() + parsedExtraStake() : parsedExtraStake();
    }

    private long parsedExtraStake() {
        if (raiseStakeBox == null || raiseStakeBox.getValue().isBlank()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(raiseStakeBox.getValue()));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void drawDie(GuiGraphics graphics, int x, int y, int size, int value, int faceColor, int pipColor) {
        graphics.fill(x, y, x + size, y + size, 0xFF312A22);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, faceColor);
        int left = x + 10;
        int center = x + size / 2 - 2;
        int right = x + size - 14;
        int top = y + 10;
        int middle = y + size / 2 - 2;
        int bottom = y + size - 14;
        switch (value) {
            case 1 -> pip(graphics, center, middle, pipColor);
            case 2 -> {
                pip(graphics, left, top, pipColor);
                pip(graphics, right, bottom, pipColor);
            }
            case 3 -> {
                pip(graphics, left, top, pipColor);
                pip(graphics, center, middle, pipColor);
                pip(graphics, right, bottom, pipColor);
            }
            case 4 -> {
                pip(graphics, left, top, pipColor);
                pip(graphics, right, top, pipColor);
                pip(graphics, left, bottom, pipColor);
                pip(graphics, right, bottom, pipColor);
            }
            case 5 -> {
                pip(graphics, left, top, pipColor);
                pip(graphics, right, top, pipColor);
                pip(graphics, center, middle, pipColor);
                pip(graphics, left, bottom, pipColor);
                pip(graphics, right, bottom, pipColor);
            }
            default -> {
                pip(graphics, left, top, pipColor);
                pip(graphics, right, top, pipColor);
                pip(graphics, left, middle, pipColor);
                pip(graphics, right, middle, pipColor);
                pip(graphics, left, bottom, pipColor);
                pip(graphics, right, bottom, pipColor);
            }
        }
    }

    private void pip(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 5, y + 5, color);
    }

    private int wobble(int phase) {
        if (animationTicks >= 24) {
            return 0;
        }
        return Math.round((float) Math.sin((animationTicks + phase) * 0.55F) * 5.0F);
    }
}
