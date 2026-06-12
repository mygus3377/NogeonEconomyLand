package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.GachaCategory;
import com.nogeon.economyland.menu.GachaRewardAdminMenu;
import com.nogeon.economyland.menu.ShopLine;
import com.nogeon.economyland.network.GachaRewardAdminCategoryPacket;
import com.nogeon.economyland.network.GachaRewardAutoAddPacket;
import com.nogeon.economyland.network.GachaRewardRemovePacket;
import com.nogeon.economyland.network.GachaRewardResetPacket;
import com.nogeon.economyland.network.GachaRewardSavePacket;
import com.nogeon.economyland.network.ModNetwork;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class GachaRewardAdminScreen extends AbstractContainerScreen<GachaRewardAdminMenu> {
    private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int SLOT_SIZE = 18;
    private static final int ROWS = 6;

    private static int lastCategory = GachaCategory.ITEM.ordinal();
    private static int lastScroll = 0;

    private final List<Button> removeButtons = new ArrayList<>();
    private final List<Button> rarityCycleButtons = new ArrayList<>();
    private final List<Button> selectButtons = new ArrayList<>();
    private final List<Button> categoryButtons = new ArrayList<>();
    private EditBox weightBox;
    private EditBox countBox;
    private Button saveButton;
    private Button rarityButton;
    private Button jackpotButton;
    private Button previousPageButton;
    private Button nextPageButton;
    private int selectedRarity;
    private boolean jackpot;
    private int selectedCategory = lastCategory;
    private int scrollOffset = lastScroll;
    private final List<ItemStack> stagedItems = new ArrayList<>();
    private final Set<String> selectedEntryIds = new LinkedHashSet<>();
    private String editingEntryId = "";

    public GachaRewardAdminScreen(GachaRewardAdminMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 412;
        imageHeight = 312;
        inventoryLabelY = 10_000;
        selectedCategory = GachaCategory.byId(menu.categoryId()).ordinal();
        lastCategory = selectedCategory;
    }

    @Override
    protected void init() {
        super.init();
        removeButtons.clear();
        rarityCycleButtons.clear();
        selectButtons.clear();
        categoryButtons.clear();
        
        for (int index = 0; index < GachaCategory.values().length; index++) {
            GachaCategory category = GachaCategory.values()[index];
            final int categoryIndex = index;
            categoryButtons.add(addRenderableWidget(Button.builder(Component.translatable(category.translationKey()), button -> selectCategory(categoryIndex))
                .bounds(leftPos + 18 + index * 76, topPos + 24, 72, 18)
                .build()));
        }

        weightBox = new EditBox(font, leftPos + 72, topPos + 52, 70, 18, Component.literal("가중치"));
        weightBox.setValue("10");
        weightBox.setFilter(this::isDigits);
        addRenderableWidget(weightBox);
        countBox = new EditBox(font, leftPos + 148, topPos + 52, 42, 18, Component.literal("수량"));
        countBox.setValue("1");
        countBox.setFilter(this::isDigits);
        addRenderableWidget(countBox);
        rarityButton = addRenderableWidget(Button.builder(Component.literal("등급: " + rarityLabel(selectedRarity)), button -> cycleRarity())
            .bounds(leftPos + 20, topPos + 76, 86, 18)
            .build());
        jackpotButton = addRenderableWidget(Button.builder(Component.literal(jackpot ? "잭팟: ON" : "잭팟: OFF"), button -> toggleJackpot())
            .bounds(leftPos + 112, topPos + 76, 86, 18)
            .build());
        
        saveButton = addRenderableWidget(Button.builder(Component.literal("저장/추가"), button -> saveStaged())
            .bounds(leftPos + 20, topPos + 98, 70, 18)
            .build());
        addRenderableWidget(Button.builder(Component.literal("비우기"), button -> clearSelection())
            .bounds(leftPos + 96, topPos + 98, 40, 18)
            .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.nogeon_economy_land.admin.reset_current"),
            button -> ModNetwork.CHANNEL.sendToServer(new GachaRewardResetPacket(menu.traderDatabaseId(), "", currentCategory())))
            .bounds(leftPos + 142, topPos + 98, 70, 18)
            .build());

        addRenderableWidget(Button.builder(Component.literal("선택 적용"), button -> applySelected())
            .bounds(leftPos + 20, topPos + 250, 62, 18)
            .build());
        addRenderableWidget(Button.builder(Component.literal("선택 삭제"), button -> removeSelected())
            .bounds(leftPos + 88, topPos + 250, 62, 18)
            .build());
        addRenderableWidget(Button.builder(Component.literal("선택 해제"), button -> clearSelectedEntries())
            .bounds(leftPos + 156, topPos + 250, 58, 18)
            .build());

        addRenderableWidget(Button.builder(Component.literal("인벤토리 전체"), button -> addAllFromInventory())
            .bounds(leftPos + 224, topPos + 282, 100, 18)
            .build());
        addRenderableWidget(Button.builder(Component.literal("방어구 자동"), button -> autoAddArmor())
            .bounds(leftPos + 224, topPos + 250, 100, 18)
            .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.nogeon_economy_land.close"), button -> onClose())
            .bounds(leftPos + 332, topPos + 282, 58, 18)
            .build());
        previousPageButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
            .bounds(leftPos + 166, topPos + 122, 22, 16)
            .build());
        nextPageButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
            .bounds(leftPos + 190, topPos + 122, 22, 16)
            .build());

        for (int row = 0; row < ROWS; row++) {
            final int rowIndex = row;
            selectButtons.add(addRenderableWidget(Button.builder(Component.empty(),
                button -> toggleSelected(rowIndex))
                .bounds(leftPos + 18, topPos + 134 + row * 18, 16, 16)
                .build()));

            removeButtons.add(addRenderableWidget(Button.builder(Component.literal("X"),
                button -> removeReward(rowIndex))
                .bounds(leftPos + 196, topPos + 134 + row * 18, 18, 16)
                .build()));
            
            rarityCycleButtons.add(addRenderableWidget(Button.builder(Component.empty(),
                button -> cycleRowRarity(rowIndex))
                .bounds(leftPos + 148, topPos + 134 + row * 18, 46, 16)
                .build()));
        }
        updateButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xF0181714);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xF026261F);
        graphics.fill(x + 16, y + 20, x + 214, y + 120, 0x55000000); 
        graphics.fill(x + 16, y + 124, x + 214, y + 246, 0xDD171B18); 
        graphics.fill(x + 224, y + 20, x + 394, y + 246, 0xDD171B18); 
        graphics.fill(x + 24, y + 48, x + 206, y + 74, 0xFF0F120F); // Basket area
        graphics.fill(x + 18 + selectedCategory * 76, y + 44, x + 90 + selectedCategory * 76, y + 46, 0xFFFFD56A);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateButtons();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        ItemStack tooltip = tooltipStack(mouseX, mouseY);
        if (!tooltip.isEmpty()) {
            graphics.renderTooltip(font, tooltip, mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, Component.literal("전역 가챠 보상 관리"), imageWidth / 2, 8, 0xFFE8E1C4);
        graphics.drawString(font, Component.literal("카테고리를 고르고 인벤토리 아이템을 클릭해 장바구니에 넣으세요."), 20, 18, 0xFF98A49C, false);
        graphics.drawString(font, Component.literal("장바구니"), 24, 38, 0xFF98A49C, false);
        graphics.drawString(font, Component.literal("가중치"), 72, 38, 0xFF98A49C, false);
        graphics.drawString(font, Component.literal("수량"), 148, 38, 0xFF98A49C, false);
        graphics.drawString(font, Component.literal("공유 보상 목록 " + pageLabel()), 20, 122, 0xFF98A49C, false);
        graphics.drawString(font, Component.literal("인벤토리"), 230, 28, 0xFF98A49C, false);

        for (int i = 0; i < stagedItems.size() && i < 10; i++) {
            ItemStack stack = stagedItems.get(i);
            int iconX = 26 + i * 18;
            graphics.renderItem(stack, iconX, 52);
            graphics.renderItemDecorations(font, stack, iconX, 52);
        }
        
        if (!editingEntryId.isEmpty()) {
            graphics.drawString(font, Component.literal("수정 중..."), 20, 84, 0xFFFFD56A, false);
        } else if (!stagedItems.isEmpty()) {
            graphics.drawString(font, Component.literal(stagedItems.size() + "개 항목 대기 중"), 20, 84, 0xFFE8E1C4, false);
        }

        renderRewardRows(graphics);
        renderInventory(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int inventorySlot = inventorySlotAt(mouseX, mouseY);
        if (inventorySlot >= 0 && minecraft != null && minecraft.player != null) {
            ItemStack stack = minecraft.player.getInventory().getItem(inventorySlot);
            if (!stack.isEmpty()) {
                addToStaged(stack.copy());
                editingEntryId = "";
                updateButtons();
                return true;
            }
        }
        int stagedIdx = stagedIndexAt(mouseX, mouseY);
        if (stagedIdx >= 0 && stagedIdx < stagedItems.size()) {
            stagedItems.remove(stagedIdx);
            updateButtons();
            return true;
        }
        ShopLine line = lineAt(mouseX, mouseY);
        if (line != null) {
            stagedItems.clear();
            stagedItems.add(line.stack());
            editingEntryId = line.id();
            weightBox.setValue(String.valueOf(Math.max(1L, line.price())));
            countBox.setValue(String.valueOf(Math.max(1, line.stack().getCount())));
            selectedRarity = Math.max(0, Math.min(3, line.stack().getOrCreateTag().getInt("NoGeonGachaRarity")));
            jackpot = line.stack().getOrCreateTag().getBoolean("NoGeonGachaJackpot");
            updateButtons();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, filteredLines().size() - ROWS);
        if (max <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        scrollOffset = Mth.clamp(scrollOffset + (delta < 0.0D ? 1 : -1), 0, max);
        lastScroll = scrollOffset;
        return true;
    }

    private void renderRewardRows(GuiGraphics graphics) {
        List<ShopLine> lines = visibleLines();
        for (int row = 0; row < lines.size(); row++) {
            ShopLine line = lines.get(row);
            int y = 134 + row * 18;
            if (line.id().equals(editingEntryId)) {
                graphics.fill(20, y - 2, 212, y + 14, 0x554A4230);
            }
            if (selectedEntryIds.contains(line.id())) {
                graphics.fill(36, y - 2, 196, y + 14, 0x55395A42);
            }
            graphics.renderItem(line.stack(), 36, y - 2);
            graphics.renderItemDecorations(font, line.stack(), 36, y - 2);
            graphics.drawString(font, font.plainSubstrByWidth(line.stack().getHoverName().getString(), 46), 56, y, 0xFFE8E1C4, false);
            graphics.drawString(font, "W" + FORMAT.format(line.price()), 108, y, 0xFFFFD56A, false);
            
            // Rarity label is handled by buttons now
        }
    }

    private void renderInventory(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderInventorySlot(graphics, 9 + row * 9 + column, 230 + column * SLOT_SIZE, 60 + row * SLOT_SIZE);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderInventorySlot(graphics, column, 230 + column * SLOT_SIZE, 122);
        }
    }

    private void renderInventorySlot(GuiGraphics graphics, int slot, int x, int y) {
        ItemStack stack = minecraft.player.getInventory().getItem(slot);
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF262C27);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF0E110F);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        }
    }

    private void addToStaged(ItemStack stack) {
        if (stagedItems.size() >= 10) return;
        stagedItems.add(stack);
    }

    private void saveStaged() {
        if (stagedItems.isEmpty()) return;
        long weight = readLong(weightBox.getValue(), 10L);
        int count = readInt(countBox.getValue(), 1);
        
        for (ItemStack staged : stagedItems) {
            ItemStack stack = staged.copyWithCount(Math.max(1, Math.min(staged.getMaxStackSize(), count)));
            ModNetwork.CHANNEL.sendToServer(new GachaRewardSavePacket(menu.traderDatabaseId(), "", currentCategory(), editingEntryId, stack, weight, selectedRarity, jackpot));
            // Only use editingEntryId for the first item if there are multiple? 
            // Actually, if editingEntryId is set, stagedItems usually has 1 item.
        }
        if (editingEntryId.isEmpty()) {
            stagedItems.clear();
        }
    }

    private void cycleRowRarity(int rowIndex) {
        List<ShopLine> lines = visibleLines();
        if (rowIndex >= 0 && rowIndex < lines.size()) {
            ShopLine line = lines.get(rowIndex);
            ItemStack stack = line.stack().copy();
            int current = stack.getOrCreateTag().getInt("NoGeonGachaRarity");
            int next = (current + 1) % 4;
            stack.getOrCreateTag().putInt("NoGeonGachaRarity", next);
            ModNetwork.CHANNEL.sendToServer(new GachaRewardSavePacket(menu.traderDatabaseId(), "", currentCategory(), line.id(), stack, line.price(), next, stack.getOrCreateTag().getBoolean("NoGeonGachaJackpot")));
        }
    }

    private void addAllFromInventory() {
        if (minecraft == null || minecraft.player == null) return;
        long weight = readLong(weightBox.getValue(), 10L);
        for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = minecraft.player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                ModNetwork.CHANNEL.sendToServer(new GachaRewardSavePacket(menu.traderDatabaseId(), "", currentCategory(), "", stack.copy(), weight, selectedRarity, false));
            }
        }
    }

    private void autoAddArmor() {
        selectedCategory = GachaCategory.ARMOR.ordinal();
        lastCategory = selectedCategory;
        scrollOffset = 0;
        lastScroll = 0;
        selectedEntryIds.clear();
        ModNetwork.CHANNEL.sendToServer(new GachaRewardAutoAddPacket(menu.traderDatabaseId(), GachaCategory.ARMOR.id()));
    }

    private void removeReward(int rowIndex) {
        List<ShopLine> lines = visibleLines();
        if (rowIndex >= 0 && rowIndex < lines.size()) {
            ModNetwork.CHANNEL.sendToServer(new GachaRewardRemovePacket(menu.traderDatabaseId(), lines.get(rowIndex).id()));
            selectedEntryIds.remove(lines.get(rowIndex).id());
        }
    }

    private void toggleSelected(int rowIndex) {
        List<ShopLine> lines = visibleLines();
        if (rowIndex < 0 || rowIndex >= lines.size()) {
            return;
        }
        String entryId = lines.get(rowIndex).id();
        if (!selectedEntryIds.remove(entryId)) {
            selectedEntryIds.add(entryId);
        }
        updateButtons();
    }

    private void applySelected() {
        if (selectedEntryIds.isEmpty()) {
            return;
        }
        long weight = readLong(weightBox.getValue(), 10L);
        int count = readInt(countBox.getValue(), 1);
        for (ShopLine line : filteredLines()) {
            if (!selectedEntryIds.contains(line.id())) {
                continue;
            }
            ItemStack stack = line.stack().copyWithCount(Math.max(1, Math.min(line.stack().getMaxStackSize(), count)));
            stack.getOrCreateTag().putInt("NoGeonGachaRarity", selectedRarity);
            stack.getOrCreateTag().putBoolean("NoGeonGachaJackpot", jackpot);
            ModNetwork.CHANNEL.sendToServer(new GachaRewardSavePacket(menu.traderDatabaseId(), "", currentCategory(), line.id(), stack, weight, selectedRarity, jackpot));
        }
    }

    private void removeSelected() {
        if (selectedEntryIds.isEmpty()) {
            return;
        }
        for (String entryId : List.copyOf(selectedEntryIds)) {
            ModNetwork.CHANNEL.sendToServer(new GachaRewardRemovePacket(menu.traderDatabaseId(), entryId));
        }
        selectedEntryIds.clear();
    }

    private void clearSelectedEntries() {
        selectedEntryIds.clear();
        updateButtons();
    }

    private void clearSelection() {
        stagedItems.clear();
        editingEntryId = "";
        weightBox.setValue("10");
        countBox.setValue("1");
        selectedRarity = 0;
        jackpot = false;
        updateButtons();
    }

    private void selectCategory(int category) {
        selectedCategory = category;
        lastCategory = selectedCategory;
        scrollOffset = 0;
        lastScroll = 0;
        selectedEntryIds.clear();
        clearSelection();
        ModNetwork.CHANNEL.sendToServer(new GachaRewardAdminCategoryPacket(menu.traderDatabaseId(), currentCategory(), 0));
    }

    private void changePage(int delta) {
        int maxPage = Math.max(0, (menu.totalCount() - 1) / 40);
        int nextPage = Mth.clamp(menu.page() + delta, 0, maxPage);
        if (nextPage == menu.page()) {
            return;
        }
        scrollOffset = 0;
        lastScroll = 0;
        selectedEntryIds.clear();
        ModNetwork.CHANNEL.sendToServer(new GachaRewardAdminCategoryPacket(menu.traderDatabaseId(), currentCategory(), nextPage));
    }

    private String pageLabel() {
        if (menu.totalCount() <= 0) {
            return "0";
        }
        return (menu.page() + 1) + "/" + Math.max(1, (menu.totalCount() + 39) / 40) + " (" + menu.totalCount() + ")";
    }

    private String currentCategory() {
        return GachaCategory.values()[selectedCategory].id();
    }

    private String rarityLabel(int rarity) {
        return switch (rarity) {
            case 1 -> "희귀";
            case 2 -> "영웅";
            case 3 -> "전설";
            default -> "일반";
        };
    }

    private void cycleRarity() {
        selectedRarity = (selectedRarity + 1) % 4;
        updateButtons();
    }

    private void toggleJackpot() {
        jackpot = !jackpot;
        updateButtons();
    }

    private List<ShopLine> filteredLines() {
        List<ShopLine> result = new ArrayList<>();
        String category = currentCategory();
        for (ShopLine line : menu.lines()) {
            if (line.kindId().equals(category)) {
                result.add(line);
            }
        }
        return result;
    }

    private List<ShopLine> visibleLines() {
        List<ShopLine> lines = filteredLines();
        int max = Math.max(0, lines.size() - ROWS);
        scrollOffset = Mth.clamp(scrollOffset, 0, max);
        return lines.subList(scrollOffset, Math.min(lines.size(), scrollOffset + ROWS));
    }

    private ShopLine lineAt(double mouseX, double mouseY) {
        int x = Mth.floor(mouseX) - leftPos;
        int y = Mth.floor(mouseY) - topPos;
        if (x < 36 || x >= 148) {
            return null;
        }
        int row = (y - 132) / 18;
        if (row < 0 || row >= visibleLines().size()) {
            return null;
        }
        return visibleLines().get(row);
    }

    private int stagedIndexAt(double mouseX, double mouseY) {
        int x = Mth.floor(mouseX) - leftPos;
        int y = Mth.floor(mouseY) - topPos;
        if (y < 48 || y >= 74) return -1;
        int idx = (x - 24) / 18;
        if (idx >= 0 && idx < 10) return idx;
        return -1;
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        int x = Mth.floor(mouseX) - leftPos;
        int y = Mth.floor(mouseY) - topPos;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                if (inside(x, y, 230 + column * SLOT_SIZE, 60 + row * SLOT_SIZE, SLOT_SIZE, SLOT_SIZE)) {
                    return 9 + row * 9 + column;
                }
            }
        }
        for (int column = 0; column < 9; column++) {
            if (inside(x, y, 230 + column * SLOT_SIZE, 122, SLOT_SIZE, SLOT_SIZE)) {
                return column;
            }
        }
        return -1;
    }

    private ItemStack tooltipStack(int mouseX, int mouseY) {
        int slot = inventorySlotAt(mouseX, mouseY);
        if (slot >= 0 && minecraft != null && minecraft.player != null) {
            return minecraft.player.getInventory().getItem(slot);
        }
        int stagedIdx = stagedIndexAt(mouseX, mouseY);
        if (stagedIdx >= 0 && stagedIdx < stagedItems.size()) {
            return stagedItems.get(stagedIdx);
        }
        ShopLine line = lineAt(mouseX, mouseY);
        return line == null ? ItemStack.EMPTY : line.stack();
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean isDigits(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private long readLong(String value, long fallback) {
        try {
            return value.isBlank() ? fallback : Math.max(1L, Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int readInt(String value, int fallback) {
        try {
            return value.isBlank() ? fallback : Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void updateButtons() {
        if (saveButton != null) {
            saveButton.active = !stagedItems.isEmpty();
        }
        if (rarityButton != null) {
            rarityButton.setMessage(Component.literal("등급: " + rarityLabel(selectedRarity)));
        }
        if (jackpotButton != null) {
            jackpotButton.setMessage(Component.literal(jackpot ? "잭팟: ON" : "잭팟: OFF"));
        }
        for (int index = 0; index < categoryButtons.size(); index++) {
            categoryButtons.get(index).active = index != selectedCategory;
        }
        if (previousPageButton != null) {
            previousPageButton.active = menu.page() > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.active = menu.page() + 1 < Math.max(1, (menu.totalCount() + 39) / 40);
        }
        List<ShopLine> visible = visibleLines();
        for (int row = 0; row < removeButtons.size(); row++) {
            Button remove = removeButtons.get(row);
            Button cycle = rarityCycleButtons.get(row);
            Button select = selectButtons.get(row);
            boolean active = row < visible.size();
            remove.visible = active;
            remove.active = active;
            cycle.visible = active;
            cycle.active = active;
            select.visible = active;
            select.active = active;
            if (active) {
                int rarity = visible.get(row).stack().getOrCreateTag().getInt("NoGeonGachaRarity");
                cycle.setMessage(Component.literal(rarityLabel(rarity)));
                select.setMessage(Component.literal(selectedEntryIds.contains(visible.get(row).id()) ? "✓" : ""));
            }
        }
    }
}
