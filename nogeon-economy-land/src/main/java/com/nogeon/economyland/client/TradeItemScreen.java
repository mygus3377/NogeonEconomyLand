package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.TradeItemMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.TradeOfferItemPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class TradeItemScreen extends AbstractContainerScreen<TradeItemMenu> {
    private static final int VISIBLE_ROWS = 8;
    private final List<Button> itemButtons = new ArrayList<>();
    private int scroll;

    public TradeItemScreen(TradeItemMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 292;
        imageHeight = 248;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        itemButtons.clear();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int rowIndex = row;
            itemButtons.add(addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.trade_offer_item"),
                ignored -> offer(rowIndex))
                .bounds(leftPos + 214, topPos + 50 + row * 21, 56, 18)
                .build()));
        }
        updateButtons();
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

        // 2. 아이템 격자 리스트 챔버
        graphics.fill(x + 16, y + 44, x + imageWidth - 16, y + 224, 0xFF0E1311);
        drawCustomBorder(graphics, x + 16, y + 44, imageWidth - 32, 180, 0xFF1B2C27);
        graphics.fill(x + 16, y + 44, x + 18, y + 224, 0xFF00FFCC); // 가이드 네온 액센트

        // 아이템 각 행 구별을 위한 수평 네온 흐름선 드로잉
        for (int i = 1; i < VISIBLE_ROWS; i++) {
            int lineY = y + 44 + i * 21;
            graphics.fill(x + 18, lineY, x + imageWidth - 18, lineY + 1, 0xFF131A17);
        }
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
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_item_hint"), 22, 28, 0xFF769B8E, false);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            ItemStack stack = item(row);
            if (stack.isEmpty()) {
                continue;
            }
            int y = 52 + row * 21;
            graphics.renderItem(stack, 24, y - 2);
            graphics.renderItemDecorations(font, stack, 24, y - 2);
            drawClippedText(graphics, stack.getHoverName(), 46, y + 2, 116, 0xFFE8E1C4);
            graphics.drawString(font, "x" + stack.getCount(), 166, y + 2, 0xFFFFD56A, false);
        }
        if (menu.items().isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.trade_item_empty"), 24, 56, 0xFF4A6057, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= leftPos + 16 && mouseX < leftPos + imageWidth - 16 && mouseY >= topPos + 44 && mouseY < topPos + 224) {
            scroll = Mth.clamp(scroll + (delta < 0.0D ? 1 : -1), 0, maxScroll());
            updateButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void drawClippedText(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }

    private void offer(int row) {
        ItemStack stack = item(row);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!stack.isEmpty() && id != null) {
            ModNetwork.CHANNEL.sendToServer(new TradeOfferItemPacket(id.toString()));
        }
    }

    private ItemStack item(int row) {
        int index = scroll + row;
        return index >= 0 && index < menu.items().size() ? menu.items().get(index) : ItemStack.EMPTY;
    }

    private int maxScroll() {
        return Math.max(0, menu.items().size() - VISIBLE_ROWS);
    }

    private void updateButtons() {
        scroll = Mth.clamp(scroll, 0, maxScroll());
        for (int row = 0; row < itemButtons.size(); row++) {
            boolean visible = !item(row).isEmpty();
            itemButtons.get(row).visible = visible;
            itemButtons.get(row).active = visible;
        }
    }
}
