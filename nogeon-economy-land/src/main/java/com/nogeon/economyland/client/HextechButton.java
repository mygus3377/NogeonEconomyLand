package com.nogeon.economyland.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class HextechButton extends Button {
    private boolean danger;

    public void danger(boolean danger) {
        this.danger = danger;
    }

    public HextechButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration, boolean danger) {
        super(x, y, width, height, message, onPress, createNarration);
        this.danger = danger;
    }

    public static Builder hextechBuilder(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;

        boolean hovered = this.isHoveredOrFocused();
        
        // 1. 배경 및 테두리, 텍스트 색상 설정
        int bgColor = 0xFF0E1311; // 기본 Hhextech 에메랄드-칠흑
        int borderColor = 0xFF1B2C27; // 기본 프레임 그린
        int textColor = 0xFF769B8E; // 기본 텍스트 민트그레이
        
        if (danger) {
            bgColor = 0xFF140D0E; // 칠흑 레드
            borderColor = 0xFF2C1B1D; // 프레임 레드
            textColor = 0xFF9B767A; // 민트레드그레이
        }
        
        if (!this.active) {
            if (danger) {
                bgColor = 0xFF080505;
                borderColor = 0xFF0F0A0B;
                textColor = 0xFF4D3A3C;
            } else {
                bgColor = 0xFF050807;
                borderColor = 0xFF0A0F0D;
                textColor = 0xFF3A4D45;
            }
        } else if (hovered) {
            if (danger) {
                bgColor = 0xFF201314;
                borderColor = 0xFFFF5555; // 경고 레드 네온
                textColor = 0xFFFFFFFF; // 순백색
            } else {
                bgColor = 0xFF14201D;
                borderColor = 0xFF00FFCC; // Cyan 네온
                textColor = 0xFFFFFFFF; // 순백색
            }
        }

        // 2. 배경 그리기
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

        // 3. 테두리 그리기
        int x = this.getX();
        int y = this.getY();
        int w = this.width;
        int h = this.height;
        
        graphics.fill(x, y, x + w, y + 1, borderColor); // 상
        graphics.fill(x, y + h - 1, x + w, y + h, borderColor); // 하
        graphics.fill(x, y, x + 1, y + h, borderColor); // 좌
        graphics.fill(x + w - 1, y, x + w, y + h, borderColor); // 우

        // 4. 텍스트 그리기 (가운데 정렬)
        int textX = this.getX() + this.width / 2;
        int textY = this.getY() + (this.height - 8) / 2;
        graphics.drawCenteredString(font, this.getMessage(), textX, textY, textColor);
    }

    public static class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private CreateNarration createNarration = Button.DEFAULT_NARRATION;
        private net.minecraft.client.gui.components.Tooltip tooltip;
        private boolean danger = false;

        public Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder tooltip(net.minecraft.client.gui.components.Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder danger(boolean danger) {
            this.danger = danger;
            return this;
        }

        public HextechButton build() {
            HextechButton button = new HextechButton(this.x, this.y, this.width, this.height, this.message, this.onPress, this.createNarration, this.danger);
            if (this.tooltip != null) {
                button.setTooltip(this.tooltip);
            }
            return button;
        }
    }
}
