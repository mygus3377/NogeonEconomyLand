package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.SlotMachineMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.SlotMachineActionPacket;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;

public final class SlotMachineScreen extends AbstractContainerScreen<SlotMachineMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int REEL_TOP = 72;
    private final RandomSource random = RandomSource.create();
    private EditBox stakeBox;
    private int animationTicks;
    private int leftDisplay;
    private int middleDisplay;
    private int rightDisplay;

    public SlotMachineScreen(SlotMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 356;
        imageHeight = 232;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        stakeBox = new EditBox(font, leftPos + 116, topPos + 178, 86, 18,
            Component.translatable("gui.nogeon_economy_land.gamble_stake"));
        stakeBox.setFilter(value -> value.matches("\\d*"));
        stakeBox.setMaxLength(12);
        stakeBox.setValue(menu.hasResult() ? String.valueOf(menu.stake()) : "1000");
        stakeBox.setBordered(false);
        stakeBox.setTextColor(0xFFFFD56A);
        addRenderableWidget(stakeBox);
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable(menu.hasResult() ? "gui.nogeon_economy_land.gamble_replay" : "gui.nogeon_economy_land.execute"),
            button -> ModNetwork.CHANNEL.sendToServer(new SlotMachineActionPacket(parsedStake())))
            .bounds(leftPos + 210, topPos + 178, 78, 18)
            .build());
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.gamble_back"), button -> onClose())
            .bounds(leftPos + imageWidth / 2 - 36, topPos + 204, 72, 20)
            .danger(true)
            .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!menu.hasResult()) {
            leftDisplay = 0;
            middleDisplay = 1;
            rightDisplay = 2;
            return;
        }
        animationTicks++;
        leftDisplay = displaySymbol(menu.leftSymbol(), 18);
        middleDisplay = displaySymbol(menu.middleSymbol(), 28);
        rightDisplay = displaySymbol(menu.rightSymbol(), 38);
    }

    private int displaySymbol(int result, int stopTick) {
        if (animationTicks < stopTick) {
            return random.nextInt(6);
        }
        return result;
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

        // 메인 릴 외곽 챔버
        graphics.fill(x + 22, y + 44, x + imageWidth - 22, y + 154, 0xFF0E1311);
        drawCustomBorder(graphics, x + 22, y + 44, imageWidth - 44, 110, 0xFF1B2C27);

        // 중앙 어두운 릴 궤적 fill
        graphics.fill(x + 46, y + 62, x + imageWidth - 46, y + 138, 0xFF070A08);
        
        // 릴 황금 격발 가로선!
        graphics.fill(x + 48, y + 98, x + imageWidth - 48, y + 100, 0xFFFFD56A);
        
        // 배팅 입력창 칠흑 배경 및 포커싱 네온 보더 직접 그리기!
        int inputX = x + 116;
        int inputY = y + 178;
        graphics.fill(inputX - 2, inputY - 2, inputX + 86 + 2, inputY + 18 + 2, 0xFF0E1311);
        int borderCol = stakeBox.isFocused() ? 0xFF00FFCC : 0xFF1B2C27;
        drawCustomBorder(graphics, inputX - 2, inputY - 2, 90, 22, borderCol);

        drawReel(graphics, x + 70, y + REEL_TOP, leftDisplay);
        drawReel(graphics, x + 144, y + REEL_TOP, middleDisplay);
        drawReel(graphics, x + 218, y + REEL_TOP, rightDisplay);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFFF2E3BC);
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.slot_machine_subtitle"), imageWidth / 2, 28, 0xFF9FA79A);
        
        // Win Streak HUD Text
        int streak = menu.gambleStreak();
        com.nogeon.economyland.player.SocialClass socialClass = menu.socialClass();
        long baseCap = socialClass.maxBetCap();
        long maxCap = Math.min(1000000L, Math.round(baseCap * (1.0D + Math.min(10, streak) * 0.1D)));
        int bonusPercent = Math.min(10, streak) * 5;
        
        int txtX = imageWidth - 124;
        graphics.drawString(font, streak + " 연승 (+" + bonusPercent + "%)", txtX, 12, streak > 0 ? 0xFF00FFCC : 0xFF98A49C, false);
        graphics.drawString(font, "한도: " + CREDIT_FORMAT.format(maxCap) + " C", txtX, 22, 0xFFFFD56A, false);
        if (menu.hasResult()) {
            boolean rolling = animationTicks < 38;
            graphics.drawCenteredString(font, Component.translatable(rolling ? "gui.nogeon_economy_land.slot_machine_result_rolling" : menu.resultKey()),
                imageWidth / 2, 154, resultColor());
            graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.gamble_payout").append(": ")
                .append(CREDIT_FORMAT.format(menu.payout())).append(" C"), imageWidth / 2, 166, 0xFFE8E1C4);
        } else {
            graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.slot_machine_ready"), imageWidth / 2, 158, 0xFF98A49C);
        }
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gamble_stake"), 52, 181, 0xFFE8E1C4, false);
    }

    private int resultColor() {
        if (menu.resultKey().endsWith("jackpot")) {
            return 0xFFFFD56A;
        }
        if (menu.payout() > 0L) {
            return 0xFF8FD2A1;
        }
        return 0xFFD47B7B;
    }

    private long parsedStake() {
        if (stakeBox == null || stakeBox.getValue().isBlank()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(stakeBox.getValue()));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void drawReel(GuiGraphics graphics, int x, int y, int symbol) {
        // 릴 테두리는 고급스러운 황금 네온
        graphics.fill(x, y, x + 52, y + 52, 0xFF1B2C27);
        graphics.fill(x + 1, y + 1, x + 51, y + 51, 0xFF0A0F0D);
        drawCustomBorder(graphics, x, y, 52, 52, 0xFFFFD56A);
        
        graphics.pose().pushPose();
        graphics.pose().scale(2.35F, 2.35F, 1.0F);
        graphics.drawCenteredString(font, symbolLabel(symbol), Math.round((x + 26) / 2.35F), Math.round((y + 18) / 2.35F), symbolColor(symbol));
        graphics.pose().popPose();
    }

    private String symbolLabel(int symbol) {
        return switch (symbol) {
            case 1 -> "G";
            case 2 -> "E";
            case 3 -> "D";
            case 4 -> "$";
            case 5 -> "7";
            default -> "X";
        };
    }

    private int symbolColor(int symbol) {
        return switch (symbol) {
            case 1 -> 0xFFD8A441;
            case 2 -> 0xFF36D37E;
            case 3 -> 0xFF6FC6FF;
            case 4 -> 0xFFFFD56A;
            case 5 -> 0xFFE85B5B;
            default -> 0xFF5D5A52;
        };
    }

    private void drawCustomBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
