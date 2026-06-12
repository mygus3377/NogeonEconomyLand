package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.AuctionLine;
import com.nogeon.economyland.menu.AuctionMenu;
import com.nogeon.economyland.network.AuctionBuyPacket;
import com.nogeon.economyland.network.AuctionCancelPacket;
import com.nogeon.economyland.network.AuctionCreatePacket;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.OpenAuctionPacket;
import com.nogeon.economyland.network.OpenWalletPacket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

public final class AuctionScreen extends AbstractContainerScreen<AuctionMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int ROWS_PER_PAGE = 6;
    private static final int INVENTORY_SLOT_COUNT = 36;
    private static final int SIDEBAR_LEFT = 16;
    private static final int SIDEBAR_TOP = 34;
    private static final int SIDEBAR_WIDTH = 82;
    private static final int CONTENT_LEFT = 110;
    private static final int CONTENT_TOP = 34;
    private static final int CONTENT_WIDTH = 228;
    private static final int PREVIEW_LEFT = 350;
    private static final int PREVIEW_TOP = 34;
    private static final int PREVIEW_WIDTH = 134;
    private static final int ROW_TOP = 84;
    private static final int ROW_HEIGHT = 28;
    private static final int PICKER_LEFT = CONTENT_LEFT + 10;
    private static final int PICKER_TOP = 102;
    private static final int PICKER_SLOT_SIZE = 18;
    private static final int PICKER_SLOT_GAP = 20;
    private static final int DIALOG_WIDTH = 210;
    private static final int DIALOG_HEIGHT = 122;
    private static final int CONTEXT_MENU_WIDTH = 92;
    private static final int CONTEXT_MENU_ROW_HEIGHT = 18;

    private final HextechButton[] actionButtons = new HextechButton[ROWS_PER_PAGE];
    private final HextechButton[] categoryButtons = new HextechButton[AuctionCategory.values().length];
    private final List<Integer> visibleLineIndices = new ArrayList<>();
    private EditBox searchBox;
    private EditBox quantityBox;
    private EditBox priceBox;
    private HextechButton registerButton;
    private HextechButton walletButton;
    private HextechButton refreshButton;
    private HextechButton allListingsButton;
    private HextechButton myListingsButton;
    private HextechButton previousPageButton;
    private HextechButton nextPageButton;
    private HextechButton dialogPrimaryButton;
    private HextechButton dialogSecondaryButton;
    private AuctionCategory category = AuctionCategory.ALL;
    private ListingDialogStep dialogStep = ListingDialogStep.NONE;
    private boolean mineOnly;
    private int page;
    private int selectedAuctionId = -1;
    private int previewAuctionId = -1;
    private int selectedInventorySlot = -1;
    private int selectedListingCount = 1;
    private int contextMenuAuctionId = -1;
    private int contextMenuX;
    private int contextMenuY;
    private ItemStack equippedPreviewStack = ItemStack.EMPTY;

    public AuctionScreen(AuctionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 500;
        imageHeight = 352;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();

        int dialogLeft = leftPos + CONTENT_LEFT + 9;
        int dialogTop = topPos + 110;
        int pageCenterX = leftPos + CONTENT_LEFT + CONTENT_WIDTH / 2;

        allListingsButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.auction_all"),
            button -> setMineOnly(false))
            .bounds(leftPos + SIDEBAR_LEFT, topPos + SIDEBAR_TOP, SIDEBAR_WIDTH, 18)
            .build());
        myListingsButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.auction_my_listings"),
            button -> setMineOnly(true))
            .bounds(leftPos + SIDEBAR_LEFT, topPos + SIDEBAR_TOP + 22, SIDEBAR_WIDTH, 18)
            .build());
 
        for (int index = 0; index < AuctionCategory.values().length; index++) {
            AuctionCategory value = AuctionCategory.values()[index];
            categoryButtons[index] = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable(value.translationKey),
                button -> setCategory(value))
                .bounds(leftPos + SIDEBAR_LEFT, topPos + SIDEBAR_TOP + 54 + index * 19, SIDEBAR_WIDTH, 18)
                .build());
        }
 
        // 검색창을 CONTENT 패널의 좌우 대칭 및 수평 행 정렬로 리뉴얼 (헥스테크 테두리와 일치하도록 좌표 미세조정)
        searchBox = new EditBox(font, leftPos + CONTENT_LEFT + 14, topPos + CONTENT_TOP + 8, 200, 14,
            Component.translatable("gui.nogeon_economy_land.auction_search"));
        searchBox.setMaxLength(32);
        searchBox.setBordered(false); // 기본 흰색/검은색 테두리 제거!
        searchBox.setTextColor(0xFFF0F3F8); // 연한 백색 글자색
        searchBox.setResponder(ignored -> refreshVisibleLines());
        addRenderableWidget(searchBox);
 
        quantityBox = new EditBox(font, dialogLeft + 18, dialogTop + 58, DIALOG_WIDTH - 36, 18,
            Component.translatable("gui.nogeon_economy_land.auction_quantity"));
        quantityBox.setFilter(value -> value.matches("\\d*"));
        quantityBox.setMaxLength(3);
        quantityBox.setVisible(false);
        quantityBox.setResponder(ignored -> updateButtons());
        addRenderableWidget(quantityBox);
 
        priceBox = new EditBox(font, dialogLeft + 18, dialogTop + 58, DIALOG_WIDTH - 36, 18,
            Component.translatable("gui.nogeon_economy_land.auction_price"));
        priceBox.setFilter(value -> value.matches("\\d*"));
        priceBox.setMaxLength(12);
        priceBox.setVisible(false);
        priceBox.setResponder(ignored -> updateButtons());
        addRenderableWidget(priceBox);
 
        // 하단 3종 버튼의 Y축 삐짐(경계선 336px 탈출) 방지 및 단정한 여백 대칭 정렬
        registerButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.auction_choose_item"),
            button -> openInventoryPicker())
            .bounds(leftPos + PREVIEW_LEFT + 12, topPos + PREVIEW_TOP + 242, 110, 20)
            .build());
        refreshButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.auction_refresh"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenAuctionPacket()))
            .bounds(leftPos + PREVIEW_LEFT + 12, topPos + PREVIEW_TOP + 266, 53, 20)
            .build());
        walletButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.wallet_tab"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenWalletPacket()))
            .bounds(leftPos + PREVIEW_LEFT + 69, topPos + PREVIEW_TOP + 266, 53, 20)
            .build());
 
        dialogPrimaryButton = addRenderableWidget(HextechButton.hextechBuilder(Component.empty(),
            button -> advanceDialog())
            .bounds(dialogLeft + 18, dialogTop + 90, 82, 20)
            .build());
        dialogPrimaryButton.visible = false;
        dialogSecondaryButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.auction_confirm_cancel"),
            button -> closeDialog())
            .bounds(dialogLeft + 110, dialogTop + 90, 82, 20)
            .danger(true)
            .build());
        dialogSecondaryButton.visible = false;
 
        for (int slot = 0; slot < ROWS_PER_PAGE; slot++) {
            int y = topPos + ROW_TOP + 4 + slot * ROW_HEIGHT;
            final int row = slot;
            actionButtons[slot] = addRenderableWidget(HextechButton.hextechBuilder(Component.empty(),
                button -> handleLineAction(row))
                .bounds(leftPos + CONTENT_LEFT + CONTENT_WIDTH - 60, y, 54, 18)
                .build());
        }
 
        previousPageButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("<"),
            button -> setPage(page - 1))
            .bounds(pageCenterX - 54, topPos + 318, 20, 18)
            .build());
        nextPageButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal(">"),
            button -> setPage(page + 1))
            .bounds(pageCenterX + 34, topPos + 318, 20, 18)
            .build());

        refreshVisibleLines();
        setInitialFocus(searchBox);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        searchBox.tick();
        quantityBox.tick();
        priceBox.tick();
        if (selectedInventorySlot >= 0 && inventoryStack(selectedInventorySlot).isEmpty()) {
            selectedInventorySlot = -1;
            selectedListingCount = 1;
            if (dialogStep != ListingDialogStep.NONE) {
                closeDialog();
            }
        }
        updateButtons();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dialogStep == ListingDialogStep.PICKER) {
            int inventorySlot = inventorySlotAt(mouseX, mouseY);
            if (inventorySlot >= 0) {
                ItemStack stack = inventoryStack(inventorySlot);
                if (!stack.isEmpty()) {
                    selectedInventorySlot = inventorySlot;
                    selectedListingCount = stack.getCount();
                    if (stack.getCount() > 1) {
                        openQuantityDialog();
                    } else {
                        openPriceDialog();
                    }
                } else {
                    closeDialog();
                }
                return true;
            }
            closeDialog();
            return true;
        }

        if (dialogStep == ListingDialogStep.QUANTITY || dialogStep == ListingDialogStep.PRICE_CONFIRM) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return true;
        }

        if (contextMenuOpen()) {
            int optionIndex = contextOptionAt(mouseX, mouseY);
            if (optionIndex >= 0) {
                applyContextOption(optionIndex);
            }
            closeContextMenu();
            return true;
        }

        if (button == 1) {
            int row = hoveredRow(mouseX, mouseY);
            if (row >= 0) {
                AuctionLine line = currentLine(row);
                if (line != null) {
                    selectedAuctionId = line.auctionId();
                    openContextMenu(line, mouseX, mouseY);
                    return true;
                }
            }
        }

        if (isInSelectedItemArea(mouseX, mouseY)) {
            openInventoryPicker();
            return true;
        }

        int hoveredRow = hoveredRow(mouseX, mouseY);
        if (hoveredRow >= 0) {
            AuctionLine line = currentLine(hoveredRow);
            if (line != null) {
                selectedAuctionId = line.auctionId();
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((dialogStep != ListingDialogStep.NONE || contextMenuOpen()) && keyCode == 256) {
            closeContextMenu();
            closeDialog();
            return true;
        }
        if ((dialogStep == ListingDialogStep.QUANTITY || dialogStep == ListingDialogStep.PRICE_CONFIRM)
            && (keyCode == 257 || keyCode == 335)) {
            if (dialogPrimaryButton.active) {
                advanceDialog();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void openInventoryPicker() {
        if (!hasSelectableInventoryItem()) {
            return;
        }
        closeContextMenu();
        dialogStep = ListingDialogStep.PICKER;
        updateButtons();
    }

    private void openQuantityDialog() {
        dialogStep = ListingDialogStep.QUANTITY;
        ItemStack stack = inventoryStack(selectedInventorySlot);
        quantityBox.setValue(Integer.toString(stack.isEmpty() ? 1 : stack.getCount()));
        setFocused(quantityBox);
        setInitialFocus(quantityBox);
        updateButtons();
    }

    private void openPriceDialog() {
        dialogStep = ListingDialogStep.PRICE_CONFIRM;
        priceBox.setValue("");
        setFocused(priceBox);
        setInitialFocus(priceBox);
        updateButtons();
    }

    private void closeDialog() {
        dialogStep = ListingDialogStep.NONE;
        quantityBox.setValue("");
        priceBox.setValue("");
        setFocused(searchBox);
        updateButtons();
    }

    private void advanceDialog() {
        if (dialogStep == ListingDialogStep.QUANTITY) {
            int quantity = parsedQuantity();
            ItemStack stack = inventoryStack(selectedInventorySlot);
            if (stack.isEmpty() || quantity <= 0 || quantity > stack.getCount()) {
                updateButtons();
                return;
            }
            selectedListingCount = quantity;
            openPriceDialog();
            return;
        }
        if (dialogStep == ListingDialogStep.PRICE_CONFIRM) {
            sendRegisterFromSelectedSlot();
            return;
        }
        if (dialogStep == ListingDialogStep.BUY_CONFIRM) {
            AuctionLine line = previewLine();
            if (line == null) {
                for (AuctionLine l : menu.lines()) {
                    if (l.auctionId() == selectedAuctionId) {
                        line = l;
                        break;
                    }
                }
            }
            if (line != null) {
                ModNetwork.CHANNEL.sendToServer(new AuctionBuyPacket(line.auctionId()));
            }
            closeDialog();
            return;
        }
        if (dialogStep == ListingDialogStep.CANCEL_CONFIRM) {
            AuctionLine line = null;
            for (AuctionLine l : menu.lines()) {
                if (l.auctionId() == selectedAuctionId) {
                    line = l;
                    break;
                }
            }
            if (line != null) {
                ModNetwork.CHANNEL.sendToServer(new AuctionCancelPacket(line.auctionId()));
            }
            closeDialog();
        }
    }

    private void sendRegisterFromSelectedSlot() {
        long price = parsedPrice();
        if (price <= 0L || selectedInventorySlot < 0 || selectedListingCount <= 0) {
            updateButtons();
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new AuctionCreatePacket(selectedInventorySlot, selectedListingCount, price));
        selectedInventorySlot = -1;
        selectedListingCount = 1;
        closeDialog();
    }

    private void setMineOnly(boolean value) {
        mineOnly = value;
        refreshVisibleLines();
    }

    private void setCategory(AuctionCategory value) {
        category = value;
        refreshVisibleLines();
    }

    private int parsedQuantity() {
        if (quantityBox.getValue().isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(quantityBox.getValue());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private long parsedPrice() {
        if (priceBox.getValue().isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(priceBox.getValue());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private void handleLineAction(int slot) {
        AuctionLine line = currentLine(slot);
        if (line == null) {
            return;
        }
        selectedAuctionId = line.auctionId();
        closeContextMenu();
        if (line.mine()) {
            dialogStep = ListingDialogStep.CANCEL_CONFIRM;
        } else {
            dialogStep = ListingDialogStep.BUY_CONFIRM;
        }
        updateButtons();
    }

    private void refreshVisibleLines() {
        String searchTerm = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        visibleLineIndices.clear();
        for (int index = 0; index < menu.lines().size(); index++) {
            AuctionLine line = menu.lines().get(index);
            ItemStack stack = lineStack(line);
            if (mineOnly && !line.mine()) {
                continue;
            }
            if (!category.matches(stack)) {
                continue;
            }
            if (!searchTerm.isBlank() && !matchesSearch(line, stack, searchTerm)) {
                continue;
            }
            visibleLineIndices.add(index);
        }
        boolean selectedStillVisible = false;
        for (int index : visibleLineIndices) {
            if (menu.lines().get(index).auctionId() == selectedAuctionId) {
                selectedStillVisible = true;
                break;
            }
        }
        if (!selectedStillVisible) {
            selectedAuctionId = visibleLineIndices.isEmpty() ? -1 : menu.lines().get(visibleLineIndices.get(0)).auctionId();
        }
        if (previewLine() == null) {
            previewAuctionId = -1;
            equippedPreviewStack = ItemStack.EMPTY;
        }
        setPage(page);
    }

    private boolean matchesSearch(AuctionLine line, ItemStack stack, String searchTerm) {
        String itemName = itemName(line, stack).getString().toLowerCase(Locale.ROOT);
        return itemName.contains(searchTerm)
            || line.sellerName().toLowerCase(Locale.ROOT).contains(searchTerm)
            || line.itemId().toLowerCase(Locale.ROOT).contains(searchTerm);
    }

    private void setPage(int targetPage) {
        page = Mth.clamp(targetPage, 0, Math.max(0, pageCount() - 1));
        updateButtons();
    }

    private void updateButtons() {
        boolean modalClosed = dialogStep == ListingDialogStep.NONE && !contextMenuOpen();
        ItemStack stack = inventoryStack(selectedInventorySlot);
        int maxCount = stack.isEmpty() ? 0 : stack.getCount();

        for (int slotIndex = 0; slotIndex < ROWS_PER_PAGE; slotIndex++) {
            AuctionLine line = currentLine(slotIndex);
            Button button = actionButtons[slotIndex];
            button.visible = line != null;
            button.active = modalClosed && line != null;
            button.setMessage(line == null ? Component.empty()
                : Component.translatable(line.mine() ? "gui.nogeon_economy_land.auction_cancel" : "gui.nogeon_economy_land.auction_buy"));
        }

        previousPageButton.active = modalClosed && page > 0;
        nextPageButton.active = modalClosed && page + 1 < pageCount();
        allListingsButton.active = modalClosed && mineOnly;
        myListingsButton.active = modalClosed && !mineOnly;
        for (int index = 0; index < categoryButtons.length; index++) {
            categoryButtons[index].active = modalClosed && category != AuctionCategory.values()[index];
        }

        registerButton.active = modalClosed && hasSelectableInventoryItem();
        refreshButton.active = modalClosed;
        walletButton.active = modalClosed;
        searchBox.active = modalClosed;

        quantityBox.setVisible(dialogStep == ListingDialogStep.QUANTITY);
        quantityBox.active = dialogStep == ListingDialogStep.QUANTITY;
        priceBox.setVisible(dialogStep == ListingDialogStep.PRICE_CONFIRM);
        priceBox.active = dialogStep == ListingDialogStep.PRICE_CONFIRM;

        dialogPrimaryButton.visible = dialogStep == ListingDialogStep.QUANTITY 
            || dialogStep == ListingDialogStep.PRICE_CONFIRM 
            || dialogStep == ListingDialogStep.BUY_CONFIRM 
            || dialogStep == ListingDialogStep.CANCEL_CONFIRM;
        dialogSecondaryButton.visible = dialogPrimaryButton.visible;
        if (dialogStep == ListingDialogStep.QUANTITY) {
            dialogPrimaryButton.setMessage(Component.translatable("gui.nogeon_economy_land.auction_quantity_next"));
            int quantity = parsedQuantity();
            dialogPrimaryButton.active = quantity > 0 && quantity <= maxCount;
        } else if (dialogStep == ListingDialogStep.PRICE_CONFIRM) {
            dialogPrimaryButton.setMessage(Component.translatable("gui.nogeon_economy_land.auction_confirm_submit"));
            dialogPrimaryButton.active = parsedPrice() > 0L && selectedListingCount > 0 && !stack.isEmpty();
        } else if (dialogStep == ListingDialogStep.BUY_CONFIRM) {
            dialogPrimaryButton.setMessage(Component.translatable("gui.nogeon_economy_land.auction_buy"));
            dialogPrimaryButton.active = true;
        } else if (dialogStep == ListingDialogStep.CANCEL_CONFIRM) {
            dialogPrimaryButton.setMessage(Component.translatable("gui.nogeon_economy_land.auction_cancel"));
            dialogPrimaryButton.active = true;
        } else {
            dialogPrimaryButton.active = false;
        }
        dialogSecondaryButton.active = dialogPrimaryButton.visible;
    }

    private AuctionLine currentLine(int slot) {
        int index = page * ROWS_PER_PAGE + slot;
        if (index < 0 || index >= visibleLineIndices.size()) {
            return null;
        }
        return menu.lines().get(visibleLineIndices.get(index));
    }

    private int pageCount() {
        return Math.max(1, (visibleLineIndices.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
    }

    private int hoveredRow(double mouseX, double mouseY) {
        int rowLeft = leftPos + CONTENT_LEFT + 6;
        int rowRight = leftPos + CONTENT_LEFT + CONTENT_WIDTH - 6;
        if (mouseX < rowLeft || mouseX > rowRight) {
            return -1;
        }
        for (int slot = 0; slot < ROWS_PER_PAGE; slot++) {
            int y = topPos + ROW_TOP + slot * ROW_HEIGHT;
            if (mouseY >= y && mouseY <= y + 24 && currentLine(slot) != null) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int panelBottom = y + imageHeight - 16;

        // 1. 프리미엄 헥스테크 칠흑 및 미드나이트 그린 테두리
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E); // 칠흑
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141918); // 그린 내벽
        
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF00FFCC); // 상단 Cyan 네온
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 하단 Blue 네온
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF00FFCC); // 좌측
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 우측

        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 24, 0xFF0E1311); // 타이틀 바 칠흑
        drawCustomBorder(graphics, x + 1, y + 1, imageWidth - 2, 23, 0xFF1B2C27); // 타이틀 바 하단 미드나이트 그린선

        drawPanel(graphics, x + SIDEBAR_LEFT, y + SIDEBAR_TOP, x + SIDEBAR_LEFT + SIDEBAR_WIDTH, panelBottom, 0xFF0E1311);
        drawPanel(graphics, x + CONTENT_LEFT, y + CONTENT_TOP, x + CONTENT_LEFT + CONTENT_WIDTH, panelBottom, 0xFF0E1311);
        drawPanel(graphics, x + PREVIEW_LEFT, y + PREVIEW_TOP, x + PREVIEW_LEFT + PREVIEW_WIDTH, panelBottom, 0xFF0E1311);

        // 검색창의 헥스테크 테두리 및 칠흑 배경 직접 드로잉 (EditBox 흰색 보더 대체)
        int sbX = x + CONTENT_LEFT + 10;
        int sbY = y + CONTENT_TOP + 6;
        int sbW = 208;
        int sbH = 18;
        graphics.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0xFF0E1311); // 칠흑 배경
        drawCustomBorder(graphics, sbX, sbY, sbW, sbH, 0xFF1B2C27); // 기본 미드나이트 그린선
        if (searchBox != null && searchBox.isFocused()) {
            drawCustomBorder(graphics, sbX, sbY, sbW, sbH, 0xFF00FFCC); // 포커스 시 시안 네온 강조!
        }

        // 검색창 및 목록 리뉴얼에 맞춰 헤더 상자 Y좌표 연동 (ROW_TOP - 20)
        graphics.fill(x + CONTENT_LEFT + 6, y + ROW_TOP - 20, x + CONTENT_LEFT + CONTENT_WIDTH - 6, y + ROW_TOP - 4, 0xFF142421);
        drawCustomBorder(graphics, x + CONTENT_LEFT + 6, y + ROW_TOP - 20, CONTENT_WIDTH - 12, 16, 0xFF1B2C27);
 
        for (int slot = 0; slot < ROWS_PER_PAGE; slot++) {
            AuctionLine line = currentLine(slot);
            int rowTop = y + ROW_TOP + slot * ROW_HEIGHT;
            int fill = 0xFF0E1311;
            boolean isSelected = line != null && line.auctionId() == selectedAuctionId;
            boolean isHovered = dialogStep == ListingDialogStep.NONE && !contextMenuOpen() && slot == hoveredRow(mouseX, mouseY);
            
            if (isSelected) {
                fill = 0xFF142924; // 시안-그린 딤 하이라이트
            } else if (isHovered) {
                fill = 0xFF1A3831; // 밝은 그린 피드백
            }
            
            graphics.fill(x + CONTENT_LEFT + 6, rowTop, x + CONTENT_LEFT + CONTENT_WIDTH - 6, rowTop + 24, fill);
            
            if (isSelected) {
                drawCustomBorder(graphics, x + CONTENT_LEFT + 6, rowTop, CONTENT_WIDTH - 12, 24, 0xFF00FFCC); // 선택 시 뚜렷한 시안 네온선
            } else {
                graphics.fill(x + CONTENT_LEFT + 7, rowTop + 23, x + CONTENT_LEFT + CONTENT_WIDTH - 7, rowTop + 24, 0xFF162521); // 기본 미드나이트 그린선
            }
        }
 
        ItemStack dollPreviewStack = currentPreviewStack(mouseX, mouseY);
        graphics.fill(x + PREVIEW_LEFT + 12, y + PREVIEW_TOP + 22, x + PREVIEW_LEFT + PREVIEW_WIDTH - 12, y + PREVIEW_TOP + 148, 0xFF0E1311);
        drawCustomBorder(graphics, x + PREVIEW_LEFT + 12, y + PREVIEW_TOP + 22, PREVIEW_WIDTH - 24, 126, 0xFF1B2C27);
        graphics.fill(x + PREVIEW_LEFT + 18, y + PREVIEW_TOP + 30, x + PREVIEW_LEFT + PREVIEW_WIDTH - 18, y + PREVIEW_TOP + 140, 0xFF080B0A);
        
        renderPaperDoll(graphics, x + PREVIEW_LEFT, y + PREVIEW_TOP, dollPreviewStack, mouseX, mouseY);
        
        graphics.fill(x + PREVIEW_LEFT + 12, y + PREVIEW_TOP + 154, x + PREVIEW_LEFT + PREVIEW_WIDTH - 12, panelBottom - 12, 0xFF0E1311);
        drawCustomBorder(graphics, x + PREVIEW_LEFT + 12, y + PREVIEW_TOP + 154, PREVIEW_WIDTH - 24, panelBottom - 12 - (y + PREVIEW_TOP + 154), 0xFF1B2C27);
 
        // Render auction list items in background to stay behind dialogs
        for (int slot = 0; slot < ROWS_PER_PAGE; slot++) {
            AuctionLine line = currentLine(slot);
            if (line == null) continue;
            ItemStack stack = lineStack(line);
            int rowY = y + ROW_TOP + slot * ROW_HEIGHT + 4;
            graphics.renderItem(stack, x + CONTENT_LEFT + 10, rowY);
            if (!stack.isEmpty()) {
                graphics.renderItemDecorations(font, stack, x + CONTENT_LEFT + 10, rowY);
            }
            graphics.drawString(font, trim(itemName(line, stack), 84), x + CONTENT_LEFT + 32, rowY + 1, 0xFFF0F3F8, false);
            graphics.drawString(font, trim(sellerLabel(line), 86), x + CONTENT_LEFT + 32, rowY + 11, line.mine() ? 0xFF85D09E : 0xFF93A5BA, false);
            // 가격 그리기 X좌표를 116으로 당기고 trim 폭을 48로 좁혀 버튼(168~)과 겹침 완벽 소멸
            graphics.drawString(font, trim(Component.literal(CREDIT_FORMAT.format(line.price()) + " C"), 48), x + CONTENT_LEFT + 116, rowY + 7, 0xFFFFD56A, false);
        }

        if (visibleLineIndices.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_empty"), x + CONTENT_LEFT + 14, y + ROW_TOP + 16, 0xFF93A5BA, false);
        }

        renderPreview(graphics, x, y);
    }

    private void renderPreview(GuiGraphics graphics, int x, int y) {
        AuctionLine previewLine = previewLine();
        ItemStack listingPreviewStack = previewLine == null ? ItemStack.EMPTY : lineStack(previewLine);
        Component selectedLabel = previewLine == null
            ? Component.translatable("gui.nogeon_economy_land.auction_preview_empty")
            : itemName(previewLine, listingPreviewStack);
        graphics.drawString(font, trim(selectedLabel, PREVIEW_WIDTH - 48), x + PREVIEW_LEFT + 34, y + PREVIEW_TOP + 156, 0xFFF0F3F8, false);

        if (previewLine != null) {
            graphics.renderItem(listingPreviewStack, x + PREVIEW_LEFT + 12, y + PREVIEW_TOP + 154);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_quantity", previewLine.count()), x + PREVIEW_LEFT + 34, y + PREVIEW_TOP + 168, 0xFF93A5BA, false);
            // 텍스트가 미리보기 챔버를 삐져나가지 않도록 시작 X를 18로 밀고 너비 제한을 PREVIEW_WIDTH - 36 (98px)로 단정하게 묶어줌
            graphics.drawString(font, trim(sellerLabel(previewLine), PREVIEW_WIDTH - 36), x + PREVIEW_LEFT + 18, y + PREVIEW_TOP + 182, previewLine.mine() ? 0xFF85D09E : 0xFF93A5BA, false);
            graphics.drawString(font, CREDIT_FORMAT.format(previewLine.price()) + " C", x + PREVIEW_LEFT + 18, y + PREVIEW_TOP + 194, 0xFFFFD56A, false);
        } else {
            // "목록에서 물품을 고르세요." 의 시작 X 도 18로 통일하여 왼쪽 삐짐을 영구 제거
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_no_selection"), x + PREVIEW_LEFT + 18, y + PREVIEW_TOP + 182, 0xFF93A5BA, false);
        }
    }

    private void drawCustomBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawPanel(GuiGraphics graphics, int left, int top, int right, int bottom, int innerColor) {
        graphics.fill(left, top, right, bottom, 0xFF1B2C27); // 헥스테크 그린 프레임
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, innerColor);
        graphics.fill(left + 1, top + 1, right - 1, top + 18, 0xFF0E1311); // 헤더 배경 칠흑
        graphics.fill(left + 1, top + 18, right - 1, top + 19, 0xFF00FFCC); // 헤더 분할 시안 네온선!
    }

    private void renderPaperDoll(GuiGraphics graphics, int panelLeft, int panelTop, ItemStack previewStack, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
            panelLeft + PREVIEW_WIDTH / 2,
            panelTop + 128,
            44,
            (float) (panelLeft + PREVIEW_WIDTH / 2 - mouseX),
            (float) (panelTop + 88 - mouseY),
            minecraft.player);

        EquipmentSlot previewSlot = previewSlot(previewStack);
        renderEquipmentSlot(graphics, panelLeft + 18, panelTop + 38, EquipmentSlot.HEAD, previewStack, previewSlot);
        renderEquipmentSlot(graphics, panelLeft + 18, panelTop + 61, EquipmentSlot.CHEST, previewStack, previewSlot);
        renderEquipmentSlot(graphics, panelLeft + 18, panelTop + 84, EquipmentSlot.LEGS, previewStack, previewSlot);
        renderEquipmentSlot(graphics, panelLeft + 18, panelTop + 107, EquipmentSlot.FEET, previewStack, previewSlot);
        renderEquipmentSlot(graphics, panelLeft + PREVIEW_WIDTH - 36, panelTop + 72, EquipmentSlot.OFFHAND, previewStack, previewSlot);
        renderEquipmentSlot(graphics, panelLeft + PREVIEW_WIDTH - 36, panelTop + 99, EquipmentSlot.MAINHAND, previewStack, previewSlot);
    }

    private void renderEquipmentSlot(GuiGraphics graphics, int x, int y, EquipmentSlot slot, ItemStack previewStack, EquipmentSlot previewSlot) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack stack = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemBySlot(slot);
        if (previewSlot == slot && !previewStack.isEmpty()) {
            stack = previewStack.copyWithCount(1);
        }
        int background = previewSlot == slot ? 0xFF3E5A7D : 0xFF1A2432;
        graphics.fill(x, y, x + 18, y + 18, background);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0D1219);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
        }
    }

    private void renderInventoryPicker(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = leftPos + PICKER_LEFT;
        int top = topPos + PICKER_TOP;
        int right = left + 208;
        int bottom = top + 116;

        drawPanel(graphics, left, top, right, bottom, 0xFF0E1311);
        drawCustomBorder(graphics, left, top, 208, 116, 0xFF00FFCC); // 눈부신 시안 네온 테두리!
        
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_picker_title"), left + 10, top + 6, 0xFF85D09E, false); // 세련된 그린 헤더
        drawWrappedText(graphics, Component.translatable("gui.nogeon_economy_land.auction_picker_hint"), left + 10, top + 18, 184, 0xFF9DB0C7, 2);

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = row == 3 ? column : 9 + row * 9 + column;
                int slotX = left + 14 + column * PICKER_SLOT_GAP;
                int slotY = top + 34 + row * PICKER_SLOT_GAP;
                ItemStack stack = inventoryStack(slot);
                
                boolean selected = slot == selectedInventorySlot;
                boolean hovered = mouseX >= slotX && mouseX <= slotX + PICKER_SLOT_SIZE 
                    && mouseY >= slotY && mouseY <= slotY + PICKER_SLOT_SIZE;
                
                int background = selected ? 0xFF142924 : 0xFF111614; // 선택 시 시안-그린 딤, 기본 시 칠흑
                int borderColor = selected ? 0xFF00FFCC : (hovered ? 0xFF00C8FF : 0xFF1B2C27); // 선택 시 시안 네온, 호버 시 블루 네온, 기본 미드나이트 그린
                
                graphics.fill(slotX, slotY, slotX + PICKER_SLOT_SIZE, slotY + PICKER_SLOT_SIZE, borderColor);
                graphics.fill(slotX + 1, slotY + 1, slotX + PICKER_SLOT_SIZE - 1, slotY + PICKER_SLOT_SIZE - 1, background);
                
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, slotX + 1, slotY + 1);
                    graphics.renderItemDecorations(font, stack, slotX + 1, slotY + 1);
                }
            }
        }
    }

    private void renderDialog(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = leftPos + CONTENT_LEFT + 9;
        int top = topPos + 110;
        int right = left + DIALOG_WIDTH;
        int bottom = top + DIALOG_HEIGHT;
        
        ItemStack stack;
        AuctionLine targetLine = null;
        if (dialogStep == ListingDialogStep.BUY_CONFIRM || dialogStep == ListingDialogStep.CANCEL_CONFIRM) {
            for (AuctionLine l : menu.lines()) {
                if (l.auctionId() == selectedAuctionId) {
                    targetLine = l;
                    break;
                }
            }
            stack = targetLine != null ? lineStack(targetLine) : ItemStack.EMPTY;
        } else {
            stack = inventoryStack(selectedInventorySlot);
        }

        drawPanel(graphics, left, top, right, bottom, 0xFF0E1311);
        drawCustomBorder(graphics, left, top, DIALOG_WIDTH, DIALOG_HEIGHT, 0xFF00FFCC); // 눈부신 시안 네온 테두리!

        Component title;
        if (dialogStep == ListingDialogStep.QUANTITY) {
            title = Component.translatable("gui.nogeon_economy_land.auction_quantity_title");
        } else if (dialogStep == ListingDialogStep.PRICE_CONFIRM) {
            title = Component.translatable("gui.nogeon_economy_land.auction_confirm_title");
        } else if (dialogStep == ListingDialogStep.BUY_CONFIRM) {
            title = Component.translatable("gui.nogeon_economy_land.auction_buy_title");
        } else {
            title = Component.translatable("gui.nogeon_economy_land.auction_cancel_title");
        }
        graphics.drawString(font, title, left + 12, top + 6, 0xFF85D09E, false); // 세련된 그린 헤더

        if (!stack.isEmpty()) {
            graphics.fill(left + 13, top + 25, left + 31, top + 43, 0xFF111614);
            drawCustomBorder(graphics, left + 13, top + 25, 18, 18, 0xFF1B2C27);
            graphics.renderItem(stack, left + 14, top + 26);
            graphics.drawString(font, trim(stack.getHoverName(), DIALOG_WIDTH - 44), left + 36, top + 27, 0xFFF0F3F8, false);
        }

        if (dialogStep == ListingDialogStep.QUANTITY) {
            drawWrappedText(graphics,
                Component.translatable("gui.nogeon_economy_land.auction_quantity_question", stack.getCount()),
                left + 14,
                top + 46,
                DIALOG_WIDTH - 28,
                0xFF9DB0C7,
                2);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_quantity_label"), left + 18, top + 62, 0xFF85D09E, false);
        } else if (dialogStep == ListingDialogStep.PRICE_CONFIRM) {
            drawWrappedText(graphics,
                Component.translatable("gui.nogeon_economy_land.auction_confirm_question", selectedListingCount),
                left + 14,
                top + 46,
                DIALOG_WIDTH - 28,
                0xFF9DB0C7,
                2);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_price"), left + 18, top + 62, 0xFF85D09E, false);
        } else if (dialogStep == ListingDialogStep.BUY_CONFIRM && targetLine != null) {
            drawWrappedText(graphics,
                Component.translatable("gui.nogeon_economy_land.auction_buy_question"),
                left + 14,
                top + 46,
                DIALOG_WIDTH - 28,
                0xFF9DB0C7,
                2);
            
            // 수량 및 가격 정보 안내
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_dialog_quantity").append(": " + targetLine.count() + "개"), left + 18, top + 60, 0xFF9DB0C7, false);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_dialog_price").append(": ").append(CREDIT_FORMAT.format(targetLine.price()) + " C"), left + 18, top + 72, 0xFFFFD56A, false);
        } else if (dialogStep == ListingDialogStep.CANCEL_CONFIRM && targetLine != null) {
            drawWrappedText(graphics,
                Component.translatable("gui.nogeon_economy_land.auction_cancel_question"),
                left + 14,
                top + 46,
                DIALOG_WIDTH - 28,
                0xFF9DB0C7,
                2);
            
            // 수량 및 가격 정보 안내
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_dialog_quantity").append(": " + targetLine.count() + "개"), left + 18, top + 60, 0xFF9DB0C7, false);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_dialog_price").append(": ").append(CREDIT_FORMAT.format(targetLine.price()) + " C"), left + 18, top + 72, 0xFFFFD56A, false);
        }
    }

    private void renderContextMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        int optionCount = contextOptionCount();
        if (optionCount == 0) {
            return;
        }
        int left = contextMenuX;
        int top = contextMenuY;
        int right = left + CONTEXT_MENU_WIDTH;
        int bottom = top + optionCount * CONTEXT_MENU_ROW_HEIGHT;
        
        graphics.fill(left, top, right, bottom, 0xFF0E1311);
        drawCustomBorder(graphics, left, top, CONTEXT_MENU_WIDTH, optionCount * CONTEXT_MENU_ROW_HEIGHT, 0xFF00FFCC); // 눈부신 시안 네온 테두리!

        for (int index = 0; index < optionCount; index++) {
            int rowTop = top + index * CONTEXT_MENU_ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX <= right 
                && mouseY >= rowTop && mouseY < rowTop + CONTEXT_MENU_ROW_HEIGHT;
            
            int fill = hovered ? 0xFF142924 : 0xFF0E1311; // 호버 시 시안-그린 딤 피드백
            int textColor = hovered ? 0xFF00FFCC : 0xFF9DB0C7; // 호버 시 시안 텍스트, 평소에는 미드나이트 실버
            
            graphics.fill(left + 1, rowTop + 1, right - 1, rowTop + CONTEXT_MENU_ROW_HEIGHT - 1, fill);
            graphics.drawString(font, contextOptionLabel(index), left + 8, rowTop + 5, textColor, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.flush();

        int x = leftPos;
        int y = topPos;

        // 모달 팝업이 활성화되었을 때 Z-depth를 에스컬레이터 방식으로 단계적 보정하여 맨 위에 그리도록 함
        if (dialogStep != ListingDialogStep.NONE) {
            graphics.pose().pushPose();
            // Z축 오프셋을 300으로 올려서 뒷배경의 3D 캐릭터 모델과 아이템들을 완벽하게 딤 처리로 덮습니다.
            graphics.pose().translate(0.0F, 0.0F, 300.0F);

            // 딤(어둡게) 배경 그리기
            graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xCC080B0D);

            if (dialogStep == ListingDialogStep.PICKER) {
                renderInventoryPicker(graphics, mouseX, mouseY);
            } else {
                renderDialog(graphics, mouseX, mouseY);
            }

            // 모달 안의 입력 박스(EditBox)와 다이얼로그 버튼들을 높은 Z-depth 상태에서 강제로 추가 렌더링
            if (dialogStep == ListingDialogStep.QUANTITY) {
                quantityBox.render(graphics, mouseX, mouseY, partialTick);
            } else if (dialogStep == ListingDialogStep.PRICE_CONFIRM) {
                priceBox.render(graphics, mouseX, mouseY, partialTick);
            }

            if (dialogPrimaryButton.visible) {
                dialogPrimaryButton.render(graphics, mouseX, mouseY, partialTick);
            }
            if (dialogSecondaryButton.visible) {
                dialogSecondaryButton.render(graphics, mouseX, mouseY, partialTick);
            }

            graphics.pose().popPose();
        }

        // 우클릭 컨텍스트 메뉴는 그 위에 떠야 하므로 Z-depth를 400으로 주어 렌더링
        if (contextMenuOpen()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            renderContextMenu(graphics, mouseX, mouseY);
            graphics.pose().popPose();
        }

        ItemStack tooltipStack = tooltipStack(mouseX, mouseY);
        if (!tooltipStack.isEmpty()) {
            graphics.renderTooltip(font, tooltipStack, mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int pageCenterX = CONTENT_LEFT + CONTENT_WIDTH / 2;
        graphics.drawCenteredString(font, title, imageWidth / 2, 8, 0xFFE8E1C4); // 헥스테크 골드 타이틀
        // 검색 글자 수평 정렬 배치 (CONTENT_LEFT + 12, Y좌표는 검색 상자와 평행한 CONTENT_TOP + 10)
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_search"), CONTENT_LEFT + 12, CONTENT_TOP + 10, 0xFFD3DDEF, false);
        // 헤더 텍스트 Y좌표를 ROW_TOP - 16 연동에 맞게 정렬 (ROW_TOP = 84이므로 Y = 68)
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_col_item"), CONTENT_LEFT + 12, ROW_TOP - 16, 0xFF85D09E, false); // 헤더들 그린 톤
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_col_seller"), CONTENT_LEFT + 62, ROW_TOP - 16, 0xFF85D09E, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_col_price"), CONTENT_LEFT + 150, ROW_TOP - 16, 0xFF85D09E, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_preview"), PREVIEW_LEFT + 10, PREVIEW_TOP + 5, 0xFF85D09E, false);
        drawWrappedText(graphics, Component.translatable("gui.nogeon_economy_land.auction_hint"), SIDEBAR_LEFT + 6, 292, 70, 0xFF9DB0C7, 3);
        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.auction_page", page + 1, pageCount()), pageCenterX, 322, 0xFF9CB0CA);

        ItemStack selectedStack = inventoryStack(selectedInventorySlot);
        // "등록할 물품"의 시작 X좌표를 18로 우측으로 밀어 왼쪽 삐짐 영구 방지
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_selected_item"), PREVIEW_LEFT + 18, PREVIEW_TOP + 210, 0xFF85D09E, false);
        
        // 미드나이트 그린 챔버 스타일 테두리와 칠흑 배경
        graphics.fill(PREVIEW_LEFT + 12, PREVIEW_TOP + 222, PREVIEW_LEFT + PREVIEW_WIDTH - 12, PREVIEW_TOP + 252, 0xFF1B2C27);
        graphics.fill(PREVIEW_LEFT + 13, PREVIEW_TOP + 223, PREVIEW_LEFT + PREVIEW_WIDTH - 13, PREVIEW_TOP + 251, 0xFF0E1311);
        
        if (!selectedStack.isEmpty()) {
            int shownCount = dialogStep == ListingDialogStep.PRICE_CONFIRM ? selectedListingCount : selectedStack.getCount();
            
            // 아이템 슬롯 주변에 얇은 시안 네온 보더 적용
            graphics.fill(PREVIEW_LEFT + 15, PREVIEW_TOP + 226, PREVIEW_LEFT + 33, PREVIEW_TOP + 244, 0xFF00FFCC);
            graphics.fill(PREVIEW_LEFT + 16, PREVIEW_TOP + 227, PREVIEW_LEFT + 32, PREVIEW_TOP + 243, 0xFF0E1311);
            
            graphics.renderItem(selectedStack, PREVIEW_LEFT + 16, PREVIEW_TOP + 227);
            graphics.drawString(font, trim(selectedStack.getHoverName(), PREVIEW_WIDTH - 42), PREVIEW_LEFT + 38, PREVIEW_TOP + 228, 0xFFD3DDEF, false);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.auction_quantity", shownCount), PREVIEW_LEFT + 38, PREVIEW_TOP + 239, 0xFF93A5BA, false);
        } else {
            // "등록 버튼을 눌러 판매할 물품을 고르세요." 의 X좌표를 18로 밀고, 너비를 PREVIEW_WIDTH - 36 (98px)로 묶어 우측 삐짐 방지
            drawWrappedText(graphics, Component.translatable("gui.nogeon_economy_land.auction_pick_prompt"), PREVIEW_LEFT + 18, PREVIEW_TOP + 229, PREVIEW_WIDTH - 36, 0xFF93A5BA, 2);
        }

        // 모달 가이드 힌트 텍스트들의 시작 X를 18로 밀어주고 너비를 PREVIEW_WIDTH - 36로 정돈해 단정하게 정렬
        if (dialogStep == ListingDialogStep.PICKER) {
            drawWrappedText(graphics, Component.translatable("gui.nogeon_economy_land.auction_picker_hint"), PREVIEW_LEFT + 18, PREVIEW_TOP + 310, PREVIEW_WIDTH - 36, 0xFF9DB0C7, 3);
        } else if (dialogStep == ListingDialogStep.QUANTITY || dialogStep == ListingDialogStep.PRICE_CONFIRM) {
            drawWrappedText(graphics, Component.translatable("gui.nogeon_economy_land.auction_confirm_hint"), PREVIEW_LEFT + 18, PREVIEW_TOP + 310, PREVIEW_WIDTH - 36, 0xFF9DB0C7, 3);
        } else if (contextMenuOpen()) {
            drawWrappedText(graphics, Component.translatable("gui.nogeon_economy_land.auction_context_hint"), PREVIEW_LEFT + 18, PREVIEW_TOP + 310, PREVIEW_WIDTH - 36, 0xFF9DB0C7, 3);
        }

        EquipmentSlot slot = previewSlot(currentPreviewStack(-1, -1));
        if (slot != null) {
            graphics.drawString(font,
                Component.translatable("gui.nogeon_economy_land.auction_equipment_preview", Component.translatable(slotTranslationKey(slot))),
                PREVIEW_LEFT + 12,
                PREVIEW_TOP + 268,
                0xFF93A5BA,
                false);
        }
    }

    private void drawWrappedText(GuiGraphics graphics, Component text, int x, int y, int width, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, width);
        int count = Math.min(lines.size(), maxLines);
        for (int index = 0; index < count; index++) {
            graphics.drawString(font, lines.get(index), x, y + index * 10, color, false);
        }
    }

    private String slotTranslationKey(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "gui.nogeon_economy_land.auction_slot_head";
            case CHEST -> "gui.nogeon_economy_land.auction_slot_chest";
            case LEGS -> "gui.nogeon_economy_land.auction_slot_legs";
            case FEET -> "gui.nogeon_economy_land.auction_slot_feet";
            case OFFHAND -> "gui.nogeon_economy_land.auction_slot_offhand";
            default -> "gui.nogeon_economy_land.auction_slot_mainhand";
        };
    }

    private AuctionLine previewLine() {
        for (AuctionLine line : menu.lines()) {
            if (line.auctionId() == previewAuctionId) {
                return line;
            }
        }
        return null;
    }

    private ItemStack currentPreviewStack(int mouseX, int mouseY) {
        if (dialogStep == ListingDialogStep.PICKER) {
            int hoveredInventorySlot = inventorySlotAt(mouseX, mouseY);
            ItemStack hoveredStack = inventoryStack(hoveredInventorySlot);
            if (!hoveredStack.isEmpty()) {
                return hoveredStack.copyWithCount(1);
            }
        }
        if (dialogStep == ListingDialogStep.QUANTITY || dialogStep == ListingDialogStep.PRICE_CONFIRM) {
            ItemStack stack = inventoryStack(selectedInventorySlot);
            return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        }
        if (!equippedPreviewStack.isEmpty()) {
            return equippedPreviewStack;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack tooltipStack(int mouseX, int mouseY) {
        if (dialogStep == ListingDialogStep.PICKER) {
            int inventorySlot = inventorySlotAt(mouseX, mouseY);
            ItemStack stack = inventoryStack(inventorySlot);
            if (!stack.isEmpty()) {
                return stack;
            }
        }

        for (int slot = 0; slot < ROWS_PER_PAGE; slot++) {
            AuctionLine line = currentLine(slot);
            if (line == null) {
                continue;
            }
            int iconX = leftPos + CONTENT_LEFT + 10;
            int iconY = topPos + ROW_TOP + slot * ROW_HEIGHT + 4;
            if (mouseX >= iconX && mouseX <= iconX + 16 && mouseY >= iconY && mouseY <= iconY + 16) {
                return lineStack(line);
            }
        }

        AuctionLine previewLine = previewLine();
        if (previewLine != null) {
            int previewX = leftPos + PREVIEW_LEFT + 12;
            int previewY = topPos + PREVIEW_TOP + 154;
            if (mouseX >= previewX && mouseX <= previewX + 16 && mouseY >= previewY && mouseY <= previewY + 16) {
                return lineStack(previewLine);
            }
        }

        int selectedX = leftPos + PREVIEW_LEFT + 16;
        int selectedY = topPos + PREVIEW_TOP + 227;
        if (mouseX >= selectedX && mouseX <= selectedX + 16 && mouseY >= selectedY && mouseY <= selectedY + 16) {
            return inventoryStack(selectedInventorySlot);
        }
        return ItemStack.EMPTY;
    }

    private boolean isInSelectedItemArea(double mouseX, double mouseY) {
        int left = leftPos + PREVIEW_LEFT + 12;
        int right = leftPos + PREVIEW_LEFT + PREVIEW_WIDTH - 12;
        int top = topPos + PREVIEW_TOP + 222;
        int bottom = topPos + PREVIEW_TOP + 252;
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        if (dialogStep != ListingDialogStep.PICKER) {
            return -1;
        }
        int left = leftPos + PICKER_LEFT + 14;
        int top = topPos + PICKER_TOP + 34;
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                int slotX = left + column * PICKER_SLOT_GAP;
                int slotY = top + row * PICKER_SLOT_GAP;
                if (mouseX >= slotX && mouseX <= slotX + PICKER_SLOT_SIZE && mouseY >= slotY && mouseY <= slotY + PICKER_SLOT_SIZE) {
                    return row == 3 ? column : 9 + row * 9 + column;
                }
            }
        }
        return -1;
    }

    private boolean hasSelectableInventoryItem() {
        return firstSelectableInventorySlot() >= 0;
    }

    private int firstSelectableInventorySlot() {
        for (int slot = 0; slot < INVENTORY_SLOT_COUNT; slot++) {
            if (!inventoryStack(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private ItemStack inventoryStack(int slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || slot < 0 || slot >= INVENTORY_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return minecraft.player.getInventory().getItem(slot);
    }

    private void openContextMenu(AuctionLine line, double mouseX, double mouseY) {
        ItemStack stack = lineStack(line);
        int options = contextOptionCount(line, stack);
        contextMenuAuctionId = line.auctionId();
        contextMenuX = Mth.clamp((int) mouseX, leftPos + CONTENT_LEFT + 8, leftPos + CONTENT_LEFT + CONTENT_WIDTH - CONTEXT_MENU_WIDTH - 8);
        contextMenuY = Mth.clamp((int) mouseY, topPos + ROW_TOP, topPos + ROW_TOP + ROWS_PER_PAGE * ROW_HEIGHT - options * CONTEXT_MENU_ROW_HEIGHT);
    }

    private void closeContextMenu() {
        contextMenuAuctionId = -1;
    }

    private boolean contextMenuOpen() {
        return getContextMenuLine() != null;
    }

    private AuctionLine getContextMenuLine() {
        for (AuctionLine line : menu.lines()) {
            if (line.auctionId() == contextMenuAuctionId) {
                return line;
            }
        }
        return null;
    }

    private int contextOptionCount() {
        AuctionLine line = getContextMenuLine();
        return line == null ? 0 : contextOptionCount(line, lineStack(line));
    }

    private int contextOptionCount(AuctionLine line, ItemStack stack) {
        return previewSlot(stack) == null ? 1 : 2;
    }

    private int contextOptionAt(double mouseX, double mouseY) {
        int count = contextOptionCount();
        if (count == 0) {
            return -1;
        }
        int left = contextMenuX;
        int top = contextMenuY;
        int right = left + CONTEXT_MENU_WIDTH;
        int bottom = top + count * CONTEXT_MENU_ROW_HEIGHT;
        if (mouseX < left || mouseX > right || mouseY < top || mouseY > bottom) {
            return -1;
        }
        return ((int) mouseY - top) / CONTEXT_MENU_ROW_HEIGHT;
    }

    private Component contextOptionLabel(int optionIndex) {
        AuctionLine line = getContextMenuLine();
        if (line == null) {
            return Component.empty();
        }
        if (optionIndex == 0) {
            return Component.translatable("gui.nogeon_economy_land.auction_context_details");
        }
        boolean alreadyPreviewing = previewAuctionId == line.auctionId() && !equippedPreviewStack.isEmpty();
        return Component.translatable(alreadyPreviewing
            ? "gui.nogeon_economy_land.auction_context_clear"
            : "gui.nogeon_economy_land.auction_context_try_on");
    }

    private void applyContextOption(int optionIndex) {
        AuctionLine line = getContextMenuLine();
        if (line == null) {
            return;
        }
        if (optionIndex == 0) {
            previewAuctionId = line.auctionId();
            equippedPreviewStack = ItemStack.EMPTY;
            return;
        }
        if (previewAuctionId == line.auctionId() && !equippedPreviewStack.isEmpty()) {
            previewAuctionId = line.auctionId();
            equippedPreviewStack = ItemStack.EMPTY;
            return;
        }
        ItemStack stack = lineStack(line);
        if (!stack.isEmpty() && previewSlot(stack) != null) {
            previewAuctionId = line.auctionId();
            equippedPreviewStack = stack.copyWithCount(1);
        }
    }

    private Component sellerLabel(AuctionLine line) {
        return line.mine()
            ? Component.translatable("gui.nogeon_economy_land.auction_my_listing")
            : Component.translatable("gui.nogeon_economy_land.auction_seller", line.sellerName());
    }

    private Component itemName(AuctionLine line, ItemStack stack) {
        return stack.isEmpty() ? Component.translatable(line.itemKey()) : stack.getHoverName();
    }

    private Component trim(Component text, int width) {
        return Component.literal(font.plainSubstrByWidth(text.getString(), width));
    }

    private ItemStack lineStack(AuctionLine line) {
        return line.stack();
    }

    private EquipmentSlot previewSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item instanceof ArmorItem armorItem) {
            return armorItem.getEquipmentSlot();
        }
        if (item instanceof ShieldItem) {
            return EquipmentSlot.OFFHAND;
        }
        if (isHandEquipment(stack)) {
            return EquipmentSlot.MAINHAND;
        }
        return null;
    }

    private static boolean isWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem
            || item instanceof BowItem
            || item instanceof CrossbowItem
            || item instanceof ProjectileWeaponItem
            || item instanceof TridentItem;
    }

    private static boolean isTool(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof DiggerItem
            || item instanceof FishingRodItem
            || item instanceof TieredItem && !isWeapon(stack);
    }

    private static boolean isHandEquipment(ItemStack stack) {
        return isWeapon(stack) || isTool(stack) || stack.getItem() instanceof ShieldItem;
    }

    private enum ListingDialogStep {
        NONE,
        PICKER,
        QUANTITY,
        PRICE_CONFIRM,
        BUY_CONFIRM,
        CANCEL_CONFIRM
    }

    private enum AuctionCategory {
        ALL("gui.nogeon_economy_land.auction_category_all") {
            @Override
            boolean matches(ItemStack stack) {
                return true;
            }
        },
        WEAPON("gui.nogeon_economy_land.auction_category_weapon") {
            @Override
            boolean matches(ItemStack stack) {
                return isWeapon(stack);
            }
        },
        ARMOR("gui.nogeon_economy_land.auction_category_armor") {
            @Override
            boolean matches(ItemStack stack) {
                return stack.getItem() instanceof ArmorItem;
            }
        },
        TOOL("gui.nogeon_economy_land.auction_category_tool") {
            @Override
            boolean matches(ItemStack stack) {
                return isTool(stack);
            }
        },
        FOOD("gui.nogeon_economy_land.auction_category_food") {
            @Override
            boolean matches(ItemStack stack) {
                return stack.isEdible();
            }
        },
        BLOCK("gui.nogeon_economy_land.auction_category_block") {
            @Override
            boolean matches(ItemStack stack) {
                return stack.getItem() instanceof BlockItem;
            }
        },
        MISC("gui.nogeon_economy_land.auction_category_misc") {
            @Override
            boolean matches(ItemStack stack) {
                return !stack.isEmpty()
                    && !isWeapon(stack)
                    && !(stack.getItem() instanceof ArmorItem)
                    && !isTool(stack)
                    && !stack.isEdible()
                    && !(stack.getItem() instanceof BlockItem);
            }
        };

        private final String translationKey;

        AuctionCategory(String translationKey) {
            this.translationKey = translationKey;
        }

        abstract boolean matches(ItemStack stack);
    }
}
