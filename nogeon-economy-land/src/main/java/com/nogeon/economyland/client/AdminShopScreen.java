package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.AdminShopMenu;
import com.nogeon.economyland.menu.ShopLine;
import com.nogeon.economyland.network.AdminShopAddPacket;
import com.nogeon.economyland.network.AdminShopRemovePacket;
import com.nogeon.economyland.network.AdminShopResetPacket;
import com.nogeon.economyland.network.ModNetwork;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class AdminShopScreen extends AbstractContainerScreen<AdminShopMenu> {
    private static final int FULL_WIDTH = 378;
    private static final int COMPACT_WIDTH = 208;
    private static final int SCREEN_HEIGHT = 288;
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int MAX_VISIBLE_ROWS = 4;
    private static final int ROW_START_Y = 190;
    private static final int ROW_SPACING = 22;
    private static boolean lastDeliveryTab;
    private static int lastScrollOffset;

    private final List<Button> removeButtons = new ArrayList<>();
    private EditBox priceBox;
    private EditBox countBox;
    private EditBox limitBox;
    private ItemStack stagedStack = ItemStack.EMPTY;
    private String editingEntryId = "";
    private boolean deliveryTab = lastDeliveryTab;
    private int scrollOffset = lastScrollOffset;
    private Button saveButton;
    private Button clearButton;
    private Button buyTabButton;
    private Button sellTabButton;
    private Button toggleSizeButton;
    private boolean compactMode;
    private String draftPrice = "1000";
    private String draftCount = "1";
    private String draftLimit = "32";

    private Button gachaRarityButton;
    private int stagedRarity;

    public AdminShopScreen(AdminShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = FULL_WIDTH;
        imageHeight = SCREEN_HEIGHT;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        if (isGachaEditor()) {
            deliveryTab = false;
            lastDeliveryTab = false;
        }
        imageWidth = compactMode ? COMPACT_WIDTH : FULL_WIDTH;
        imageHeight = SCREEN_HEIGHT;
        super.init();
        removeButtons.clear();

        buyTabButton = addRenderableWidget(Button.builder(Component.translatable(isGachaEditor()
                ? "gui.nogeon_economy_land.admin.gacha_rewards_tab"
                : "gui.nogeon_economy_land.admin.buy_tab"), button -> switchTab(false))
            .bounds(leftPos + 16, topPos + 24, 72, 18)
            .build());
        sellTabButton = addRenderableWidget(Button.builder(Component.translatable("gui.nogeon_economy_land.admin.sell_tab"), button -> switchTab(true))
            .bounds(leftPos + 92, topPos + 24, 72, 18)
            .build());
        toggleSizeButton = addRenderableWidget(Button.builder(Component.translatable(compactMode
                ? "gui.nogeon_economy_land.admin.expand"
                : "gui.nogeon_economy_land.admin.compact"),
            button -> toggleCompactMode())
            .bounds(leftPos + imageWidth - 66, topPos + 24, 50, 18)
            .build());

        priceBox = new EditBox(font, leftPos + 74, topPos + 72, 104, 18, Component.translatable(isGachaEditor()
            ? "gui.nogeon_economy_land.admin.gacha_weight"
            : "gui.nogeon_economy_land.admin.price"));
        priceBox.setValue(draftPrice);
        priceBox.setFilter(this::isDigits);
        addRenderableWidget(priceBox);

        countBox = new EditBox(font, leftPos + 74, topPos + 108, 52, 18, Component.translatable("gui.nogeon_economy_land.admin.count"));
        countBox.setValue(draftCount);
        countBox.setFilter(this::isDigits);
        addRenderableWidget(countBox);

        limitBox = new EditBox(font, leftPos + 130, topPos + 108, 48, 18, Component.translatable("gui.nogeon_economy_land.admin.limit"));
        limitBox.setValue(draftLimit);
        limitBox.setFilter(this::isDigits);
        addRenderableWidget(limitBox);

        if (isGachaEditor()) {
            gachaRarityButton = addRenderableWidget(Button.builder(Component.empty(), button -> cycleRarity())
                .bounds(leftPos + 22, topPos + 108, 48, 18)
                .build());
        }

        saveButton = addRenderableWidget(Button.builder(Component.translatable("gui.nogeon_economy_land.admin.save"), button -> saveCurrent())
            .bounds(leftPos + 16, topPos + 144, 56, 18)
            .build());
        clearButton = addRenderableWidget(Button.builder(Component.translatable("gui.nogeon_economy_land.admin.clear"), button -> clearSelection())
            .bounds(leftPos + 76, topPos + 144, 44, 18)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.nogeon_economy_land.admin.reset_current"),
            button -> {
                rememberViewState();
                ModNetwork.CHANNEL.sendToServer(new AdminShopResetPacket(menu.kindId(), menu.traderDatabaseId(), deliveryTab));
            })
            .bounds(leftPos + 124, topPos + 144, 56, 18)
            .build());

        for (int row = 0; row < MAX_VISIBLE_ROWS; row++) {
            final int rowIndex = row;
            Button removeButton = addRenderableWidget(Button.builder(Component.translatable("gui.nogeon_economy_land.admin.remove"),
                button -> removeVisibleLine(rowIndex))
                .bounds(leftPos + 140, topPos + ROW_START_Y + row * ROW_SPACING + 2, 40, 16)
                .build());
            removeButtons.add(removeButton);
        }

        updateTabState();
        updateRowButtons();
        updateActionState();
    }

    private void cycleRarity() {
        stagedRarity = (stagedRarity + 1) % 4;
        updateActionState();
    }

    private String rarityName(int rarity) {
        return switch (rarity) {
            case 1 -> "희귀";
            case 2 -> "영웅";
            case 3 -> "전설";
            default -> "일반";
        };
    }

    private boolean isDigits(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isGachaEditor() {
        return "gacha".equals(menu.kindId());
    }

    private long readLong(String value, long fallback) {
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Math.max(1L, Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int readInt(String value, int fallback) {
        if (value.isEmpty()) {
            return fallback;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xF0181714);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xF026261F);
        graphics.fill(x + 12, y + 52, x + 184, y + 172, 0xCC171B18);
        graphics.fill(x + 12, y + 180, x + 184, y + imageHeight - 16, 0xFF171B18);
        graphics.fill(x + 22, y + 66, x + 54, y + 98, 0xFF2B312C);
        graphics.fill(x + 23, y + 67, x + 53, y + 97, 0xFF111411);
        if (!compactMode) {
            graphics.fill(x + 192, y + 52, x + imageWidth - 14, y + 206, 0xCC171B18);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateRowButtons();
        updateActionState();
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
        graphics.drawCenteredString(font, title, imageWidth / 2, 10, 0xFFE8E1C4);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.staging"), 16, 56, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable(isGachaEditor()
            ? "gui.nogeon_economy_land.admin.gacha_weight"
            : "gui.nogeon_economy_land.admin.price"), 74, 56, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.count"), 74, 96, 0xFF98A49C, false);
        if (!isGachaEditor()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.limit"), 130, 96, 0xFF98A49C, false);
        } else {
            graphics.drawString(font, "등급", 22, 96, 0xFF98A49C, false);
        }
        graphics.drawString(font, Component.translatable(isGachaEditor()
            ? "gui.nogeon_economy_land.admin.gacha_current_rewards"
            : "gui.nogeon_economy_land.admin.current_items"), 16, 184, 0xFF98A49C, false);
        if (compactMode) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.compact_hint"), 16, 272, 0xFF6F7B72, false);
        } else {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.inventory_title"), 198, 56, 0xFF98A49C, false);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.inventory_hint"), 198, 68, 0xFF6F7B72, false);
        }
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.scroll_hint"), 16, 272, 0xFF6F7B72, false);

        if (!stagedStack.isEmpty()) {
            graphics.renderItem(stagedStack, 30, 74);
            graphics.renderItemDecorations(font, stagedStack, 30, 74);
            graphics.drawString(font, font.plainSubstrByWidth(stagedStack.getHoverName().getString(), 104), 74, 76, 0xFFE8E1C4, false);
            if (!editingEntryId.isBlank()) {
                graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.editing"), 74, 86, 0xFFFFD56A, false);
            }
        } else {
            graphics.drawCenteredString(font, "!", 38, 72, 0xFF6F7B72);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.empty_slot"), 74, 60, 0xFFFFD56A, false);
        }

        if (!compactMode) {
            drawInventory(graphics);
        }

        List<ShopLine> lines = visibleLines();
        for (int row = 0; row < lines.size(); row++) {
            ShopLine line = lines.get(row);
            int y = ROW_START_Y + row * ROW_SPACING;
            if (line.id().equals(editingEntryId)) {
                graphics.fill(16, y - 2, 136, y + ROW_SPACING - 4, 0x553A4239);
            }
            graphics.renderItem(line.stack(), 18, y);
            graphics.renderItemDecorations(font, line.stack(), 18, y);
            
            // Item Name (Reduced width to avoid price overlap)
            graphics.drawString(font, font.plainSubstrByWidth(line.stack().getHoverName().getString(), 50), 38, y, 0xFFE8E1C4, false);
            
            // Price and Info
            graphics.drawString(font, (isGachaEditor() ? "W " : "") + CREDIT_FORMAT.format(line.price()) + (isGachaEditor() ? "" : " C"), 90, y, 0xFFFFD56A, false);
            String limitText = isGachaEditor()
                ? Component.translatable("gui.nogeon_economy_land.admin.gacha_count_short").getString() + ": " + line.stack().getCount()
                : line.delivery()
                ? "§7[§a" + Component.translatable("gui.nogeon_economy_land.admin.sell_tab").getString() + "§7]"
                : Component.translatable("gui.nogeon_economy_land.admin.limit_short").getString() + ": " + line.remaining();
            graphics.drawString(font, font.plainSubstrByWidth(limitText, 50), 90, y + 9, 0xFF98A49C, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int inventorySlot = inventorySlotAt(mouseX, mouseY);
        if (inventorySlot >= 0 && minecraft != null && minecraft.player != null) {
            ItemStack stack = minecraft.player.getInventory().getItem(inventorySlot);
            if (!stack.isEmpty()) {
                stagedStack = stack.copy();
                editingEntryId = "";
                stagedRarity = stagedStack.getOrCreateTag().getInt("NoGeonGachaRarity");
                countBox.setValue(String.valueOf(stagedStack.getCount()));
                updateActionState();
                
                if (button == 1) { // Right click to register immediately
                    saveCurrent();
                }
                return true;
            }
        }

        ShopLine line = lineAt(mouseX, mouseY);
        if (line != null) {
            stagedStack = line.stack();
            editingEntryId = line.id();
            stagedRarity = stagedStack.getOrCreateTag().getInt("NoGeonGachaRarity");
            priceBox.setValue(String.valueOf(line.price()));
            countBox.setValue(String.valueOf(line.stack().getCount()));
            limitBox.setValue(line.delivery() ? "0" : String.valueOf(Math.max(1, line.remaining())));
            updateActionState();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int maxScroll = Math.max(0, filteredLines().size() - MAX_VISIBLE_ROWS);
        if (maxScroll <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (delta > 0.0D) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        } else if (delta < 0.0D) {
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
        rememberViewState();
        updateRowButtons();
        return true;
    }

    private void saveCurrent() {
        if (stagedStack.isEmpty()) {
            return;
        }
        rememberDraftValues();
        rememberViewState();
        int count = Math.max(1, Math.min(stagedStack.getMaxStackSize(), readInt(countBox.getValue(), stagedStack.getCount())));
        ItemStack saveStack = stagedStack.copyWithCount(count);
        if (isGachaEditor()) {
            saveStack.getOrCreateTag().putInt("NoGeonGachaRarity", stagedRarity);
            saveStack.getOrCreateTag().putBoolean("NoGeonGachaJackpot", stagedRarity == 3);
        }
        ModNetwork.CHANNEL.sendToServer(new AdminShopAddPacket(
            menu.kindId(),
            menu.traderDatabaseId(),
            editingEntryId,
            saveStack,
            readLong(priceBox.getValue(), isGachaEditor() ? 10L : 1000L),
            deliveryTab || isGachaEditor() ? 0 : readInt(limitBox.getValue(), 32),
            deliveryTab && !isGachaEditor()
        ));
    }

    private void switchTab(boolean delivery) {
        if (deliveryTab == delivery) {
            return;
        }
        rememberDraftValues();
        deliveryTab = isGachaEditor() ? false : delivery;
        scrollOffset = 0;
        rememberViewState();
        clearSelection();
        draftLimit = delivery ? "0" : "32";
        limitBox.setValue(draftLimit);
        updateTabState();
        updateRowButtons();
    }

    private void clearSelection() {
        stagedStack = ItemStack.EMPTY;
        editingEntryId = "";
        draftCount = "1";
        countBox.setValue(draftCount);
        updateActionState();
    }

    private void toggleCompactMode() {
        rememberDraftValues();
        compactMode = !compactMode;
        if (minecraft != null) {
            init(minecraft, width, height);
        }
    }

    private void rememberDraftValues() {
        if (priceBox != null) {
            draftPrice = priceBox.getValue();
        }
        if (countBox != null) {
            draftCount = countBox.getValue();
        }
        if (limitBox != null) {
            draftLimit = limitBox.getValue();
        }
    }

    private void rememberViewState() {
        lastDeliveryTab = deliveryTab;
        lastScrollOffset = scrollOffset;
    }

    private void updateActionState() {
        if (saveButton != null) {
            saveButton.active = !stagedStack.isEmpty();
        }
        if (clearButton != null) {
            clearButton.active = !stagedStack.isEmpty() || !editingEntryId.isBlank();
        }
        if (limitBox != null) {
            limitBox.setEditable(!deliveryTab);
            limitBox.active = !deliveryTab;
        }
        if (gachaRarityButton != null) {
            gachaRarityButton.setMessage(Component.literal(rarityName(stagedRarity)));
        }
    }

    private void updateTabState() {
        if (buyTabButton != null) {
            buyTabButton.active = deliveryTab;
        }
        if (sellTabButton != null) {
            sellTabButton.visible = !isGachaEditor();
            sellTabButton.active = !deliveryTab;
        }
        if (limitBox != null) {
            limitBox.visible = !isGachaEditor();
        }
    }

    private void updateRowButtons() {
        List<ShopLine> lines = visibleLines();
        for (int row = 0; row < removeButtons.size(); row++) {
            Button button = removeButtons.get(row);
            boolean visible = row < lines.size();
            button.visible = visible;
            button.active = visible;
            button.setPosition(leftPos + 140, topPos + ROW_START_Y + row * ROW_SPACING + 2);
        }
    }

    private void removeVisibleLine(int rowIndex) {
        List<ShopLine> lines = visibleLines();
        if (rowIndex < 0 || rowIndex >= lines.size()) {
            return;
        }
        ShopLine line = lines.get(rowIndex);
        rememberViewState();
        ModNetwork.CHANNEL.sendToServer(new AdminShopRemovePacket(menu.kindId(), menu.traderDatabaseId(), line.id(), line.delivery()));
    }

    private List<ShopLine> filteredLines() {
        List<ShopLine> lines = new ArrayList<>();
        for (ShopLine line : menu.lines()) {
            if (line.delivery() == deliveryTab) {
                lines.add(line);
            }
        }
        return lines;
    }

    private List<ShopLine> visibleLines() {
        List<ShopLine> lines = filteredLines();
        int maxScroll = Math.max(0, lines.size() - MAX_VISIBLE_ROWS);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
            rememberViewState();
        }
        return lines.subList(scrollOffset, Math.min(lines.size(), scrollOffset + MAX_VISIBLE_ROWS));
    }

    private ShopLine lineAt(double mouseX, double mouseY) {
        if (mouseX < leftPos + 16 || mouseX > leftPos + 140) {
            return null;
        }
        List<ShopLine> lines = visibleLines();
        for (int row = 0; row < lines.size(); row++) {
            int rowY = topPos + ROW_START_Y + row * ROW_SPACING;
            if (mouseY >= rowY - 2 && mouseY <= rowY + ROW_SPACING - 4) {
                return lines.get(row);
            }
        }
        return null;
    }

    private void drawInventory(GuiGraphics graphics) {
        if (compactMode || minecraft == null || minecraft.player == null) {
            return;
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderInventorySlot(graphics, 9 + row * 9 + column, 200 + column * 18, 82 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderInventorySlot(graphics, column, 200 + column * 18, 140);
        }
    }

    private void renderInventorySlot(GuiGraphics graphics, int slot, int x, int y) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF2B312C);
        graphics.fill(x, y, x + 16, y + 16, 0xFF111411);
        ItemStack stack = minecraft.player.getInventory().getItem(slot);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(font, stack, x, y);
        }
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        if (compactMode) {
            return -1;
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                int x = leftPos + 200 + column * 18;
                int y = topPos + 82 + row * 18;
                if (mouseX >= x && mouseX <= x + 16 && mouseY >= y && mouseY <= y + 16) {
                    return 9 + row * 9 + column;
                }
            }
        }
        for (int column = 0; column < 9; column++) {
            int x = leftPos + 200 + column * 18;
            int y = topPos + 140;
            if (mouseX >= x && mouseX <= x + 16 && mouseY >= y && mouseY <= y + 16) {
                return column;
            }
        }
        return -1;
    }

    private ItemStack tooltipStack(int mouseX, int mouseY) {
        if (!stagedStack.isEmpty() && mouseX >= leftPos + 30 && mouseX <= leftPos + 46 && mouseY >= topPos + 74 && mouseY <= topPos + 90) {
            return stagedStack;
        }
        ShopLine line = lineAt(mouseX, mouseY);
        if (line != null && mouseX >= leftPos + 18 && mouseX <= leftPos + 34) {
            return line.stack();
        }
        int inventorySlot = inventorySlotAt(mouseX, mouseY);
        if (inventorySlot >= 0 && minecraft != null && minecraft.player != null) {
            return minecraft.player.getInventory().getItem(inventorySlot);
        }
        return ItemStack.EMPTY;
    }
}
