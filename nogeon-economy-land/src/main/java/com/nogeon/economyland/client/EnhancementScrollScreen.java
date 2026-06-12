package com.nogeon.economyland.client;

import com.nogeon.economyland.item.SmithingService;
import com.nogeon.economyland.menu.EnhancementScrollMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.SmithActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class EnhancementScrollScreen extends AbstractContainerScreen<EnhancementScrollMenu> {
    private static final int PREVIEW_X = 24;
    private static final int PREVIEW_Y = 74;
    private static final int INVENTORY_X = 206;
    private static final int INVENTORY_Y = 74;
    private static final int SLOT_SIZE = 18;
    private static final int SCROLL_BUTTON_X = 24;
    private static final int SCROLL_BUTTON_Y = 126;
    private static final int SCROLL_BUTTON_W = 54;
    private static final int SCROLL_BUTTON_H = 13;
    private static final int SCROLL_BUTTON_GAP_X = 56;
    private static final int SCROLL_BUTTON_GAP_Y = 13;
    private static final int STATUS_Y = 210;
    private static final int DESCRIPTION_Y = 230;

    private final HextechButton[] scrollButtons = new HextechButton[15];
    private int selectedSlot;
    private boolean selectionChanged;

    public EnhancementScrollScreen(EnhancementScrollMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 388;
        imageHeight = 264;
        inventoryLabelY = 10_000;
        selectedSlot = menu.selectedSlot();
    }

    @Override
    protected void init() {
        super.init();
        for (int level = 1; level <= scrollButtons.length; level++) {
            final int targetLevel = level;
            scrollButtons[level - 1] = addRenderableWidget(HextechButton.hextechBuilder(Component.empty(), button ->
                ModNetwork.CHANNEL.sendToServer(new SmithActionPacket("scroll_" + targetLevel, selectedSlot, "", false, true)))
                .bounds(leftPos + SCROLL_BUTTON_X + ((level - 1) % 3) * SCROLL_BUTTON_GAP_X,
                    topPos + SCROLL_BUTTON_Y + ((level - 1) / 3) * SCROLL_BUTTON_GAP_Y,
                    SCROLL_BUTTON_W, SCROLL_BUTTON_H)
                .build());
        }
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.smith_close"), button -> onClose())
            .bounds(leftPos + 300, topPos + 224, 64, 18)
            .danger(true)
            .build());
        refreshButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (selectedStack().isEmpty()) {
            selectedSlot = fallbackSelectedSlot();
            selectionChanged = true;
        }
        refreshButtons();
    }

    private void drawCustomBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawCyberAccents(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        int len = 6;
        // Top-Left
        graphics.fill(x, y, x + len, y + 1, color);
        graphics.fill(x, y, x + 1, y + len, color);
        // Top-Right
        graphics.fill(x + w - len, y, x + w, y + 1, color);
        graphics.fill(x + w - 1, y, x + w, y + len, color);
        // Bottom-Left
        graphics.fill(x, y + h - 1, x + len, y + h, color);
        graphics.fill(x, y + h - len, x + 1, y + h, color);
        // Bottom-Right
        graphics.fill(x + w - len, y + h - 1, x + w, y + h, color);
        graphics.fill(x + w - 1, y + h - len, x + w, y + h, color);
    }

    private void framedPanel(GuiGraphics graphics, int left, int top, int right, int bottom, int border, int fill) {
        graphics.fill(left, top, right, bottom, 0xFF0A0C0A);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, border);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, fill);
        graphics.fill(left + 2, top + 2, right - 2, top + 3, 0x33FFFFFF);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        int themeNeonColor = 0xFFFF8C00;
        int secondaryNeonColor = 0xFFFFAA00;
        
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141918);
        
        graphics.fill(x, y, x + imageWidth, y + 1, themeNeonColor);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, secondaryNeonColor);
        graphics.fill(x, y, x + 1, y + imageHeight, themeNeonColor);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, secondaryNeonColor);
        
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 20, 0xFF0E1311);
        drawCustomBorder(graphics, x + 1, y + 1, imageWidth - 2, 20, 0xFF2A2218);

        framedPanel(graphics, x + 18, y + 52, x + 196, y + 198, 0xFF2A2218, 0xFF0E110F);
        framedPanel(graphics, x + 200, y + 52, x + imageWidth - 18, y + 198, 0xFF2A2218, 0xFF0E110F);
        framedPanel(graphics, x + 18, y + 202, x + imageWidth - 18, y + imageHeight - 18, 0xFF2A2218, 0xFF0E110F);

        framedPanel(graphics, x + PREVIEW_X, y + PREVIEW_Y, x + PREVIEW_X + 48, y + PREVIEW_Y + 48, themeNeonColor, 0xFF0A0C0A);
        drawCyberAccents(graphics, x + PREVIEW_X, y + PREVIEW_Y, 48, 48, secondaryNeonColor);
        ItemStack stack = selectedStack();
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + PREVIEW_X + 16, y + PREVIEW_Y + 16);
            graphics.renderItemDecorations(font, stack, x + PREVIEW_X + 16, y + PREVIEW_Y + 16);
        }
        renderInventoryGrid(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        ItemStack tooltipStack = tooltipStack(mouseX, mouseY);
        if (!tooltipStack.isEmpty()) {
            graphics.renderTooltip(font, tooltipStack, mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack stack = selectedStack();
        int level = SmithingService.level(stack);
        graphics.drawCenteredString(font, title, imageWidth / 2, 10, 0xFFF2E3BC);
        graphics.drawString(font, trim(Component.translatable("gui.nogeon_economy_land.smith_scroll_subtitle"), 160), 24, 52, 0xFF9FA79A, false);
        graphics.drawString(font, trim(Component.translatable("gui.nogeon_economy_land.smith_inventory_hint"), 160), 206, 52, 0xFF9FA79A, false);
        graphics.drawString(font, trim(stack.isEmpty() ? Component.translatable("gui.nogeon_economy_land.smith_empty_hand") : SmithingService.displayName(stack), 104), 84, 76, 0xFFE8E1C4, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_current_level", level), 84, 94, 0xFFE8E1C4, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_scroll_pick_hint"), 24, 122, 0xFFD9BF7E, false);

        int nextScroll = nextUsableScrollLevel(stack);
        if (nextScroll > 0) {
            int count = countItem(SmithingService.scrollItem(nextScroll));
            graphics.drawString(font, Component.literal("다음 목표: +" + nextScroll), 206, 162, 0xFFF4E3B0, false);
            graphics.drawString(font, Component.literal("필요: +" + nextScroll + " 강화권 x1"), 206, 176, 0xFFFFD56A, false);
            graphics.drawString(font, Component.literal("보유: x" + count), 206, 190, count > 0 ? 0xFF8ED79E : 0xFFD47B7B, false);
        } else if (!stack.isEmpty() && SmithingService.canEnhance(stack)) {
            graphics.drawString(font, Component.literal("사용 가능한 강화권 없음"), 206, 162, 0xFFD47B7B, false);
        }

        graphics.drawString(font, trim(currentStatus(stack), 340), 24, STATUS_Y, 0xFFE8E1C4, false);
        graphics.drawString(font, trim(Component.translatable("gui.nogeon_economy_land.smith_scroll_desc"), 340), 24, DESCRIPTION_Y, 0xFF98A49C, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int inventorySlot = inventorySlotAt(mouseX, mouseY);
            if (inventorySlot >= 0 && minecraft != null && !minecraft.player.getInventory().getItem(inventorySlot).isEmpty()) {
                selectedSlot = inventorySlot;
                selectionChanged = selectedSlot != menu.selectedSlot();
                refreshButtons();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderInventoryGrid(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderInventoryCell(graphics, 9 + row * 9 + column, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + row * SLOT_SIZE);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderInventoryCell(graphics, column, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + 62);
        }
    }

    private void renderInventoryCell(GuiGraphics graphics, int slot, int x, int y) {
        ItemStack stack = minecraft.player.getInventory().getItem(slot);
        int left = leftPos + x;
        int top = topPos + y;
        boolean selected = slot == selectedSlot;
        
        int cellBorder = selected ? 0xFFFF8C00 : 0xFF2A2218;
        int cellBg = selected ? 0xFF261D15 : 0xFF0E110F;
        
        graphics.fill(left, top, left + SLOT_SIZE, top + SLOT_SIZE, cellBorder);
        graphics.fill(left + 1, top + 1, left + SLOT_SIZE - 1, top + SLOT_SIZE - 1, cellBg);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, left + 1, top + 1);
            graphics.renderItemDecorations(font, stack, left + 1, top + 1);
        }
        if (selected) {
            graphics.fill(left, top, left + SLOT_SIZE, top + 1, 0xFFFFAA00);
            graphics.fill(left, top + SLOT_SIZE - 1, left + SLOT_SIZE, top + SLOT_SIZE, 0xFFFFAA00);
            graphics.fill(left, top, left + 1, top + SLOT_SIZE, 0xFFFFAA00);
            graphics.fill(left + SLOT_SIZE - 1, top, left + SLOT_SIZE, top + SLOT_SIZE, 0xFFFFAA00);
        }
    }

    private ItemStack selectedStack() {
        if (minecraft == null || minecraft.player == null || selectedSlot < 0 || selectedSlot >= minecraft.player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return minecraft.player.getInventory().getItem(selectedSlot);
    }

    private Component currentStatus(ItemStack stack) {
        return selectionChanged ? SmithingService.defaultStatus(stack) : menu.status();
    }

    public void refreshFromSyncedInventory() {
        if (selectedStack().isEmpty()) {
            selectedSlot = fallbackSelectedSlot();
        }
        refreshButtons();
    }

    private int fallbackSelectedSlot() {
        if (minecraft == null || minecraft.player == null) {
            return -1;
        }
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            if (SmithingService.canEnhance(minecraft.player.getInventory().getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private void refreshButtons() {
        ItemStack stack = selectedStack();
        int level = SmithingService.level(stack);
        for (int index = 0; index < scrollButtons.length; index++) {
            HextechButton button = scrollButtons[index];
            if (button == null) continue;
            int targetLevel = index + 1;
            button.setMessage(Component.literal("+" + targetLevel + " x" + countItem(SmithingService.scrollItem(targetLevel))));
            button.active = SmithingService.canEnhance(stack)
                && level < targetLevel
                && countItem(SmithingService.scrollItem(targetLevel)) > 0;
        }
    }

    private int countItem(Item item) {
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }
        int count = 0;
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int nextUsableScrollLevel(ItemStack stack) {
        if (!SmithingService.canEnhance(stack)) {
            return -1;
        }
        int level = SmithingService.level(stack);
        for (int targetLevel = Math.max(1, level + 1); targetLevel <= 15; targetLevel++) {
            return targetLevel;
        }
        return -1;
    }

    private ItemStack tooltipStack(int mouseX, int mouseY) {
        if (insideBox(mouseX, mouseY, leftPos + PREVIEW_X + 16, topPos + PREVIEW_Y + 16, 16, 16)) {
            return selectedStack();
        }
        int inventorySlot = inventorySlotAt(mouseX, mouseY);
        if (inventorySlot >= 0 && minecraft != null) {
            return minecraft.player.getInventory().getItem(inventorySlot);
        }
        return ItemStack.EMPTY;
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        int relativeX = Mth.floor(mouseX) - leftPos;
        int relativeY = Mth.floor(mouseY) - topPos;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                if (insideBox(relativeX, relativeY, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + row * SLOT_SIZE, SLOT_SIZE, SLOT_SIZE)) {
                    return 9 + row * 9 + column;
                }
            }
        }
        for (int column = 0; column < 9; column++) {
            if (insideBox(relativeX, relativeY, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + 62, SLOT_SIZE, SLOT_SIZE)) {
                return column;
            }
        }
        return -1;
    }

    private boolean insideBox(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private Component trim(Component text, int width) {
        return Component.literal(font.plainSubstrByWidth(text.getString(), width)).withStyle(text.getStyle());
    }
}
