package com.nogeon.economyland.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nogeon.economyland.menu.ExtendedInventoryMenu;
import com.nogeon.economyland.network.ExtendedInventoryPagePacket;
import com.nogeon.economyland.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class ExtendedInventoryScreen extends AbstractContainerScreen<ExtendedInventoryMenu> {
    private static final ResourceLocation CONTAINER_BACKGROUND = new ResourceLocation("textures/gui/container/generic_54.png");

    private Button leftButton;
    private Button rightButton;

    public ExtendedInventoryScreen(ExtendedInventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 73; // 플레이어 인벤토리 레이블의 Y좌표 위치 지정
    }

    @Override
    protected void init() {
        super.init();
        
        // 인벤토리 레벨이 1보다 크면(한 번이라도 강화하여 2레벨 이상이 되면) 바로 페이지 넘기기 창 추가
        if (menu.inventoryExtLevel() > 1) {
            int maxPage = Math.min(10, ((menu.inventoryExtLevel() - 1) / 3) + 2);
            
            this.leftButton = Button.builder(Component.literal("◀"), button -> changePage(-1))
                .bounds(this.leftPos + 38, this.topPos + 168, 20, 20)
                .build();
                
            this.rightButton = Button.builder(Component.literal("▶"), button -> changePage(1))
                .bounds(this.leftPos + 118, this.topPos + 168, 20, 20)
                .build();
                
            this.addRenderableWidget(this.leftButton);
            this.addRenderableWidget(this.rightButton);
            
            updateButtonStates(maxPage);
        }
    }

    private void changePage(int delta) {
        int newPage = menu.currentPage() + delta;
        int maxPage = Math.min(10, ((menu.inventoryExtLevel() - 1) / 3) + 2);
        if (newPage >= 0 && newPage < maxPage) {
            menu.setCurrentPage(newPage);
            ModNetwork.CHANNEL.sendToServer(new ExtendedInventoryPagePacket(newPage));
            updateButtonStates(maxPage);
        }
    }

    private void updateButtonStates(int maxPage) {
        if (this.leftButton != null) {
            this.leftButton.active = menu.currentPage() > 0;
        }
        if (this.rightButton != null) {
            this.rightButton.active = menu.currentPage() < maxPage - 1;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        // 잠긴 슬롯 위에 반투명 검은색 칠하고 코드로 18x18 투명 배경 자물쇠 렌더링
        for (int i = 0; i < 27; i++) {
            Slot slot = menu.slots.get(i);
            if (menu.isSlotLocked(i)) {
                int slotX = this.leftPos + slot.x;
                int slotY = this.topPos + slot.y;
                
                // 1. 슬롯 전체(18x18 테두리 포함)를 고급스러운 반투명 짙은 그레이로 덮기
                graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xBF0F0F14);
                
                // 2. 코드로 완벽한 투명 배경의 프리미엄 실버 철 자물쇠 그리기 (슬롯 내부 중앙 정렬)
                drawLockIcon(graphics, slotX, slotY);
            }
        }
        
        RenderSystem.disableBlend();
        
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    /**
     * 16x16 슬롯 영역 안에 투명 배경을 보장하는 고성능 픽셀아트 자물쇠를 그립니다.
     */
    private void drawLockIcon(GuiGraphics graphics, int x, int y) {
        // 1. 자물쇠 고리 (Shackle) - 은빛 철 느낌의 픽셀아트
        graphics.fill(x + 6, y + 2, x + 10, y + 3, 0xFFB0B5B9); // 고리 상단 수평선
        graphics.fill(x + 5, y + 3, x + 6, y + 6, 0xFFB0B5B9);  // 고리 좌측 기둥
        graphics.fill(x + 10, y + 3, x + 11, y + 6, 0xFF6A6E72); // 고리 우측 기둥 (그림자)

        // 2. 자물쇠 몸체 (Lock Body) - 세련된 실버 철광석 톤
        graphics.fill(x + 3, y + 6, x + 13, y + 13, 0xFF808589);  // 메인 철회색 바디
        
        // 3. 자물쇠 입체감 하이라이트 (좌상단 테두리에 밝은 톤 추가)
        graphics.fill(x + 4, y + 7, x + 12, y + 8, 0xFFDDE1E5);   // 상단 밝은 하이라이트
        graphics.fill(x + 4, y + 7, x + 5, y + 12, 0xFFDDE1E5);   // 좌측 밝은 하이라이트

        // 4. 자물쇠 그림자 (우하단 테두리에 어두운 톤 추가)
        graphics.fill(x + 3, y + 12, x + 13, y + 13, 0xFF4A4E52); // 하단 그림자 테두리
        graphics.fill(x + 12, y + 7, x + 13, y + 12, 0xFF4A4E52); // 우측 그림자 테두리

        // 5. 열쇠구멍 (Keyhole) - 깊고 어두운 다크 그레이
        graphics.fill(x + 7, y + 9, x + 9, y + 10, 0xFF151515);   // 구멍 머리
        graphics.fill(x + 8, y + 10, x + 9, y + 12, 0xFF151515);  // 구멍 꼬리
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, CONTAINER_BACKGROUND);
        int x = this.leftPos;
        int y = this.topPos;
        // 9x3 슬롯 상단 컨테이너 영역 그리기 (y: 0~71)
        graphics.blit(CONTAINER_BACKGROUND, x, y, 0, 0, this.imageWidth, 71);
        // 플레이어 인벤토리 영역 그리기 (y: 126~221, 높이 95)
        graphics.blit(CONTAINER_BACKGROUND, x, y + 71, 0, 126, this.imageWidth, 95);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
        
        // 페이지가 활성화된 경우 (한 번이라도 강화하여 2레벨 이상이 되었을 때) 현재 페이지 / 전체 페이지 표시
        if (menu.inventoryExtLevel() > 1) {
            int maxPage = Math.min(10, ((menu.inventoryExtLevel() - 1) / 3) + 2);
            Component pageText = Component.literal((menu.currentPage() + 1) + " / " + maxPage);
            int textWidth = this.font.width(pageText);
            // 두 버튼 사이의 정중앙에 화이트 섀도우 텍스트 렌더링
            graphics.drawString(this.font, pageText, (this.imageWidth - textWidth) / 2, 174, 0xFFFFFFFF, true);
        }
    }
}
