package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.GachaStorageMenu;
import com.nogeon.economyland.network.GachaTakeStoredPacket;
import com.nogeon.economyland.network.GachaClaimStoredPacket;
import com.nogeon.economyland.network.GachaCelebratePacket;
import com.nogeon.economyland.network.ModNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class GachaStorageScreen extends AbstractContainerScreen<GachaStorageMenu> {
    private static final int ROWS = 3;
    private final List<HextechButton> takeButtons = new ArrayList<>();
    private int scrollOffset;

    public GachaStorageScreen(GachaStorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 352;
        imageHeight = 246;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        takeButtons.clear();
        for (int row = 0; row < ROWS; row++) {
            final int rowIndex = row;
            HextechButton btn = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("꺼내기"),
                button -> take(rowIndex))
                .bounds(leftPos + 266, topPos + 44 + row * 24, 58, 18)
                .build());
            takeButtons.add(btn);
        }
        
        // 일괄 꺼내기 및 닫기 버튼을 대칭 정렬로 이식
        addRenderableWidget(HextechButton.hextechBuilder(Component.literal("모두 꺼내기"), button -> claimAll())
            .bounds(leftPos + 24, topPos + 218, 140, 20)
            .danger(true)
            .build());
            
        addRenderableWidget(HextechButton.hextechBuilder(Component.literal("닫기"), button -> onClose())
            .bounds(leftPos + 188, topPos + 218, 140, 20)
            .build());
            
        updateButtons();
    }

    private void claimAll() {
        ModNetwork.CHANNEL.sendToServer(new GachaClaimStoredPacket());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;
        
        // 칠흑의 프리미엄 헥스테크 배경 그리기
        graphics.fill(x, y, x + w, y + h, 0xFA0B0F0E);
        
        // 시안 네온 외곽 테두리
        int neonColor = 0xFF00FFCC;
        graphics.fill(x, y, x + w, y + 1, neonColor);
        graphics.fill(x, y + h - 1, x + w, y + h, neonColor);
        graphics.fill(x, y, x + 1, y + h, neonColor);
        graphics.fill(x + w - 1, y, x + w, y + h, neonColor);
        
        // 상단 리스트 영역 뒷배경 챔버
        graphics.fill(x + 20, y + 36, x + 332, y + 118, 0xFF14201D);
        
        // 하단 인벤토리 구역 챔버
        graphics.fill(x + 20, y + 122, x + 332, y + 212, 0xFF0B1210);
        
        // 인벤토리 구역 시안 코너 데코레이션
        graphics.fill(x + 20, y + 122, x + 30, y + 123, neonColor);
        graphics.fill(x + 20, y + 122, x + 21, y + 132, neonColor);
        graphics.fill(x + 322, y + 122, x + 332, y + 123, neonColor);
        graphics.fill(x + 331, y + 122, x + 332, y + 132, neonColor);
        
        // 플레이어 인벤토리 격실 슬롯 테두리 36칸 드로잉
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                int sx = x + 95 + c * 18;
                int sy = y + 128 + r * 18;
                
                graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF111A18); // 슬롯 내부 칠흑
                
                // 프레임 민트그린-테크
                int border = 0xFF1B2C27;
                graphics.fill(sx - 1, sy - 1, sx + 17, sy, border);
                graphics.fill(sx - 1, sy + 16, sx + 17, sy + 17, border);
                graphics.fill(sx - 1, sy - 1, sx, sy + 17, border);
                graphics.fill(sx + 16, sy - 1, sx + 17, sy + 17, border);
            }
        }
        for (int c = 0; c < 9; c++) {
            int sx = x + 95 + c * 18;
            int sy = y + 188;
            
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF111A18);
            
            int border = 0xFF1B2C27;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy, border);
            graphics.fill(sx - 1, sy + 16, sx + 17, sy + 17, border);
            graphics.fill(sx - 1, sy - 1, sx, sy + 17, border);
            graphics.fill(sx + 16, sy - 1, sx + 17, sy + 17, border);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateButtons();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        ItemStack tooltip = hoveredStack(mouseX, mouseY);
        if (!tooltip.isEmpty()) {
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFFFFD56A);
        graphics.drawString(font, Component.literal("보관 수: " + menu.rewards().size()), 24, 24, 0xFF00FFCC, false);
        graphics.drawString(font, Component.literal("인벤토리"), 95, 120, 0xFF769B8E, false);
        
        List<ItemStack> visible = visibleRewards();
        for (int row = 0; row < visible.size(); row++) {
            ItemStack stack = visible.get(row);
            int y = 44 + row * 24;
            
            // 리스트 항목 세련된 칠흑-에메랄드배경
            graphics.fill(26, y - 4, 326, y + 18, 0xFF1A2824);
            graphics.fill(26, y - 4, 326, y - 3, 0xFF2C443D);
            graphics.fill(26, y + 17, 326, y + 18, 0xFF2C443D);
            
            graphics.renderItem(stack, 32, y);
            graphics.renderItemDecorations(font, stack, 32, y);
            
            // 총기 한글 명칭 깨짐 방지
            Component displayName = GachaCelebratePacket.getGachaItemName(stack);
            graphics.drawString(font, font.plainSubstrByWidth(displayName.getString(), 170), 56, y, 0xFFE8E1C4, false);
            graphics.drawString(font, "x" + stack.getCount(), 230, y, 0xFFFFD56A, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, menu.rewards().size() - ROWS);
        if (max <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        scrollOffset = Mth.clamp(scrollOffset + (delta < 0.0D ? 1 : -1), 0, max);
        return true;
    }

    private List<ItemStack> visibleRewards() {
        int max = Math.max(0, menu.rewards().size() - ROWS);
        scrollOffset = Mth.clamp(scrollOffset, 0, max);
        return menu.rewards().subList(scrollOffset, Math.min(menu.rewards().size(), scrollOffset + ROWS));
    }

    private void take(int rowIndex) {
        int index = scrollOffset + rowIndex;
        if (index >= 0 && index < menu.rewards().size()) {
            ModNetwork.CHANNEL.sendToServer(new GachaTakeStoredPacket(index));
        }
    }

    private void updateButtons() {
        int visible = visibleRewards().size();
        for (int row = 0; row < takeButtons.size(); row++) {
            takeButtons.get(row).visible = row < visible;
            takeButtons.get(row).active = row < visible;
        }
    }

    private ItemStack hoveredStack(int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (x < 32 || x >= 248) {
            return ItemStack.EMPTY;
        }
        int row = (y - 44) / 24;
        List<ItemStack> visible = visibleRewards();
        return row >= 0 && row < visible.size() ? visible.get(row) : ItemStack.EMPTY;
    }
}

