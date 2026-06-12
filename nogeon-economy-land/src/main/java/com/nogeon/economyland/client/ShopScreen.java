package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.ShopLine;
import com.nogeon.economyland.menu.ShopMenu;
import com.nogeon.economyland.network.BuyShopItemPacket;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.OpenWalletPacket;
import com.nogeon.economyland.network.OpenJobChangePacket;
import com.nogeon.economyland.network.TraderActionPacket;
import com.nogeon.economyland.shop.ShopItemProtection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ShopScreen extends AbstractContainerScreen<ShopMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int VIEW_X = 18;
    private static final int VIEW_Y = 60;
    private static final int VIEW_WIDTH = 304;
    private static final int VIEW_HEIGHT = 114;
    private static final int ROW_HEIGHT = 19;
    private static final int VISIBLE_ROWS = 6;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final int PREVIEW_Y = 184;
    private static final int PREVIEW_HEIGHT = 44;
    private static final int PREVIEW_COLUMNS = 6;

    private static boolean lastSellMode;
    private static int lastBuyScroll;
    private static int lastSellScroll;

    private boolean sellMode = lastSellMode;
    private int buyScroll = lastBuyScroll;
    private int sellScroll = lastSellScroll;
    private boolean draggingScrollbar;
    private boolean draggingMiniScrollbar;
    private RowData pendingRow;
    private int pendingQuantity = 1;
    private boolean pendingDelivery;
    private boolean pendingNormalSell;

    private enum StorageType {
        INVENTORY,
        BACKPACK,
        STORAGE
    }
    private StorageType storageMode = StorageType.INVENTORY;
    private int storageScroll = 0;

    private final List<Integer> selectedInventorySlots = new ArrayList<>();
    private final List<Integer> selectedBackpackSlots = new ArrayList<>();
    private final List<Integer> selectedStorageSlots = new ArrayList<>();

    private final Map<String, Integer> cartItems = new LinkedHashMap<>();
    private Button cartBuyButton;
    private Button cartClearButton;

    private EditBox modalQuantityField;
    private Button walletButton;
    private Button buyTabButton;
    private Button sellTabButton;
    private Button jobButton;
    private Button modalMinusTenButton;
    private Button modalMinusOneButton;
    private Button modalPlusOneButton;
    private Button modalPlusTenButton;
    private Button modalConfirmButton;
    private Button modalCancelButton;
    private Button sellSelectedButton;
    private Button sellAllButton;
    private Button deliverySelectedButton;
    private Button deliveryAllButton;

    private Button invTabSubButton;
    private Button backpackTabSubButton;
    private Button storageTabSubButton;
    private final List<Button> rowButtons = new ArrayList<>();
    private final List<Button> rowSellNormalButtons = new ArrayList<>();
    private final List<Button> rowSellDeliveryButtons = new ArrayList<>();

    public ShopScreen(ShopMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 352;
        imageHeight = 278;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        walletButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.wallet_tab"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenWalletPacket()))
            .bounds(leftPos + 278, topPos + 26, 56, 20)
            .build());
        buyTabButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.shop_buy_tab"),
            button -> switchMode(false))
            .bounds(leftPos + 18, topPos + 26, 66, 20)
            .build());
        sellTabButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.shop_sell_tab"),
            button -> switchMode(true))
            .bounds(leftPos + 88, topPos + 26, 66, 20)
            .build());
        jobButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("전직"),
            button -> sendJobChange())
            .bounds(leftPos + 158, topPos + 26, 54, 20)
            .build());

        rowButtons.clear();
        rowSellNormalButtons.clear();
        rowSellDeliveryButtons.clear();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int rowIndex = row;
            Button buyBtn = addRenderableWidget(HextechButton.hextechBuilder(Component.empty(),
                ignored -> onRowAction(rowIndex, false, false))
                .bounds(leftPos + 270, topPos + 58 + row * ROW_HEIGHT, 58, 18)
                .build());
            rowButtons.add(buyBtn);

            Button sellNormalBtn = addRenderableWidget(HextechButton.hextechBuilder(Component.empty(),
                ignored -> onRowAction(rowIndex, true, true))
                .bounds(leftPos + 246, topPos + 58 + row * ROW_HEIGHT, 42, 18)
                .build());
            rowSellNormalButtons.add(sellNormalBtn);

            Button sellDeliveryBtn = addRenderableWidget(HextechButton.hextechBuilder(Component.empty(),
                ignored -> onRowAction(rowIndex, true, false))
                .bounds(leftPos + 290, topPos + 58 + row * ROW_HEIGHT, 42, 18)
                .build());
            rowSellDeliveryButtons.add(sellDeliveryBtn);
        }

        modalMinusTenButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("-10"),
            button -> adjustPendingQuantity(-10))
            .bounds(leftPos + 80, topPos + 121, 34, 18)
            .danger(true)
            .build());
        modalMinusOneButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("-1"),
            button -> adjustPendingQuantity(-1))
            .bounds(leftPos + 116, topPos + 121, 30, 18)
            .danger(true)
            .build());
        modalPlusOneButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("+1"),
            button -> adjustPendingQuantity(1))
            .bounds(leftPos + 206, topPos + 121, 30, 18)
            .build());
        modalPlusTenButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("+10"),
            button -> adjustPendingQuantity(10))
            .bounds(leftPos + 238, topPos + 121, 34, 18)
            .build());
        modalConfirmButton = addRenderableWidget(HextechButton.hextechBuilder(Component.empty(),
            button -> confirmPendingAction())
            .bounds(leftPos + 138, topPos + 145, 64, 18)
            .build());
        modalCancelButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.cancel"),
            button -> closeQuantityDialog())
            .bounds(leftPos + 206, topPos + 145, 64, 18)
            .danger(true)
            .build());

        sellSelectedButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("선택 판매"),
            button -> sellSelectedInventory(false))
            .bounds(leftPos + 206, topPos + PREVIEW_Y + 41, 58, 19)
            .build());

        sellAllButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("전부 판매"),
            button -> sellAllInventory(false))
            .bounds(leftPos + 268, topPos + PREVIEW_Y + 41, 58, 19)
            .danger(true)
            .build());

        deliverySelectedButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("선택 납품"),
            button -> sellSelectedInventory(true))
            .bounds(leftPos + 206, topPos + PREVIEW_Y + 62, 58, 19)
            .build());

        deliveryAllButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("전부 납품"),
            button -> sellAllInventory(true))
            .bounds(leftPos + 268, topPos + PREVIEW_Y + 62, 58, 19)
            .danger(true)
            .build());

        invTabSubButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("인벤"),
            button -> { storageMode = StorageType.INVENTORY; storageScroll = 0; refreshControls(); })
            .bounds(leftPos + 92, topPos + PREVIEW_Y - 11, 30, 13)
            .build());

        backpackTabSubButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("배낭"),
            button -> { storageMode = StorageType.BACKPACK; storageScroll = 0; refreshControls(); })
            .bounds(leftPos + 124, topPos + PREVIEW_Y - 11, 30, 13)
            .build());

        storageTabSubButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("보관"),
            button -> { storageMode = StorageType.STORAGE; storageScroll = 0; refreshControls(); })
            .bounds(leftPos + 156, topPos + PREVIEW_Y - 11, 30, 13)
            .build());

        cartBuyButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("일괄 구매"),
            button -> buyAllCartItems())
            .bounds(leftPos + 258, topPos + 114, 72, 18)
            .build());

        cartClearButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("비우기"),
            button -> clearCart())
            .bounds(leftPos + 258, topPos + 134, 72, 18)
            .danger(true)
            .build());

        modalQuantityField = new EditBox(font, leftPos + 148, topPos + 123, 56, 14, Component.literal(""));
        modalQuantityField.setValue(String.valueOf(pendingQuantity));
        modalQuantityField.setResponder(text -> {
            if (text.isEmpty()) {
                pendingQuantity = 1;
                return;
            }
            try {
                int val = Integer.parseInt(text);
                int limit = quantityDialogLimit();
                pendingQuantity = Mth.clamp(val, 1, limit);
            } catch (NumberFormatException ignored) {
            }
        });
        modalQuantityField.setTextColor(0xFFFFD56A);
        modalQuantityField.setBordered(true);
        modalQuantityField.active = false;
        modalQuantityField.visible = false;
        addRenderableWidget(modalQuantityField);

        refreshControls();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        // 1. 프리미엄 헥스테크 칠흑 및 미드나이트 그린 테두리
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E); // 칠흑
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141918); // 그린 내벽
        
        graphics.fill(x, y, x + imageWidth, y + 1, 0xFF00FFCC); // 상단 Cyan 네온
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 하단 Blue 네온
        graphics.fill(x, y, x + 1, y + imageHeight, 0xFF00FFCC); // 좌측
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF00C8FF); // 우측

        // 메인 상품 리스트 챔버
        graphics.fill(x + 16, y + 56, x + imageWidth - 16, y + 176, 0xFF0E1311);
        drawCustomBorder(graphics, x + 16, y + 56, imageWidth - 32, 120, 0xFF1B2C27);

        // 하단 인벤토리 프리뷰 챔버
        graphics.fill(x + 16, y + PREVIEW_Y, x + imageWidth - 16, y + PREVIEW_Y + 84, 0xFF0E1311);
        drawCustomBorder(graphics, x + 16, y + PREVIEW_Y, imageWidth - 32, 84, 0xFF1B2C27);
        
        drawScrollbar(graphics);

        if (hasPendingDialog()) {
            graphics.fill(x + 16, y + 56, x + imageWidth - 16, y + PREVIEW_Y + 84, 0xCC080B0D);     
            graphics.fill(x + 72, y + 84, x + imageWidth - 72, y + 168, 0xFA0B0F0E); // 모달 칠흑
            graphics.fill(x + 73, y + 85, x + imageWidth - 73, y + 167, 0xFA141918); // 모달 그린 내벽
            drawCustomBorder(graphics, x + 72, y + 84, imageWidth - 144, 84, 0xFF00FFCC); // 모달 시안 네온 라인 테두리!
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
        ItemStack tooltipStack = tooltipStack(mouseX, mouseY);
        if (!tooltipStack.isEmpty()) {
            graphics.renderTooltip(font, tooltipStack, mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hasPendingDialog()) {
            ItemStack preview = pendingRow.previewStack();
            graphics.drawCenteredString(font, Component.translatable(pendingDelivery
                ? (pendingNormalSell ? "gui.nogeon_economy_land.shop_sell_dialog_title" : "gui.nogeon_economy_land.shop_delivery_dialog_title")
                : "gui.nogeon_economy_land.shop_buy_dialog_title"), imageWidth / 2, 90, 0xFFE8E1C4);
            graphics.renderItem(preview, 82, 95);
            graphics.renderItemDecorations(font, preview, 82, 95);
            drawClippedText(graphics, pendingRow.line().stack().getHoverName(), 106, 98, 140, 0xFFE8E1C4);
            
            // Quantity center display is handled by the modalQuantityField EditBox now
            
            long totalPrice;
            if (pendingDelivery) {
                if (pendingNormalSell) {
                    int bundleSize = Math.max(1, pendingRow.line().count());
                    long basePrice = pendingRow.line().price();
                    if (!pendingRow.line().id().startsWith("dynamic:")) {
                        basePrice = Math.max(1L, Math.round((double) basePrice / bundleSize));
                    }
                    long normalUnit = Math.round((double) basePrice / 1.8D * 2.4D);
                    totalPrice = normalUnit * pendingQuantity;
                } else {
                    if (pendingRow.line().id().startsWith("dynamic:")) {
                        totalPrice = pendingRow.line().price() * pendingRow.line().count() * pendingQuantity;
                    } else {
                        totalPrice = pendingRow.line().price() * pendingQuantity;
                    }
                }
            } else {
                totalPrice = pendingRow.line().price() * pendingQuantity;
            }

            graphics.drawString(font, CREDIT_FORMAT.format(totalPrice) + " C", 82, 149, 0xFFFFD56A, false);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.shop_available_short")
                .append(": ").append(String.valueOf(quantityDialogLimit())), 82, 160, 0xFF98A49C, false);
            return;
        }

        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFFE8E1C4);
        if (!isJobTrader()) {
            drawClippedText(graphics, Component.translatable(sellMode
                ? "gui.nogeon_economy_land.shop_sell_hint"
                : "gui.nogeon_economy_land.shop_buy_hint"), 164, 33, 106, 0xFF98A49C);
        }

        if (sellMode) {
            List<RowData> rows = visibleRows();
            for (int row = 0; row < rows.size(); row++) {
                RowData data = rows.get(row);
                int y = VIEW_Y + row * ROW_HEIGHT;
                ItemStack stack = data.previewStack();
                graphics.renderItem(stack, 24, y - 2);
                graphics.renderItemDecorations(font, stack, 24, y - 2);
                drawClippedText(graphics, data.line().stack().getHoverName(), 46, y + 2, 112, 0xFFE8E1C4);

                int bundleSize = Math.max(1, data.line().count());
                long basePrice = data.line().price();
                if (!data.line().id().startsWith("dynamic:")) {
                    basePrice = Math.max(1L, Math.round((double) basePrice / bundleSize));
                }
                long normalPrice = Math.round((double) basePrice / 1.8D * 2.4D);
                
                long deliveryPrice;
                if (data.line().id().startsWith("dynamic:")) {
                    deliveryPrice = data.line().price() * bundleSize;
                } else {
                    deliveryPrice = data.line().price();
                }
                
                String priceText = "낱개: " + CREDIT_FORMAT.format(normalPrice) + "C | 납품: " + CREDIT_FORMAT.format(deliveryPrice) + "C";
                drawClippedText(graphics, Component.literal(priceText), 154, y - 2, 140, 0xFFFFD56A);
                
                String ownedText = "보유: " + data.owned() + " | 가능: " + data.maxBundles();
                drawClippedText(graphics, Component.literal(ownedText), 154, y + 8, 140, 0xFF98A49C);
                
                if (data.line().maxSaturation() > 0) {
                    renderSaturationBar(graphics, 46, y + 13, 110, data.line().currentSaturation(), data.line().maxSaturation());
                }
            }
        } else {
            // 1. 상품 그리드 렌더링 (8열 x 4행)
            List<RowData> allBuyRows = buyRows();
            int startIdx = buyScroll * 8;
            for (int i = 0; i < 32; i++) {
                int idx = startIdx + i;
                int col = i % 8;
                int row = i / 8;
                int cellX = 20 + col * 18;
                int cellY = 62 + row * 18;

                graphics.fill(cellX, cellY, cellX + 18, cellY + 18, 0xFF222724);
                drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFF1D2220);

                if (idx < allBuyRows.size()) {
                    RowData data = allBuyRows.get(idx);
                    ItemStack stack = data.previewStack();
                    graphics.renderItem(stack, cellX + 1, cellY + 1);
                    graphics.renderItemDecorations(font, stack, cellX + 1, cellY + 1);

                    if (data.line().remaining() <= 0) {
                        graphics.fill(cellX + 1, cellY + 1, cellX + 17, cellY + 17, 0x99000000);
                    }
                }
            }

            // 2. 장바구니 렌더링 (3열 x 3행)
            int cartIdx = 0;
            for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
                if (cartIdx >= 9) break;
                String entryId = entry.getKey();
                int qty = entry.getValue();

                ShopLine line = null;
                for (ShopLine l : menu.lines()) {
                    if (l.id().equals(entryId)) {
                        line = l;
                        break;
                    }
                }
                if (line == null) continue;

                int col = cartIdx % 3;
                int row = cartIdx / 3;
                int cellX = 196 + col * 18;
                int cellY = 62 + row * 18;

                graphics.fill(cellX, cellY, cellX + 18, cellY + 18, 0xFF222724);
                drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFF1B2C27);

                ItemStack stack = line.stack().copy();
                stack.setCount(line.count() * qty);
                graphics.renderItem(stack, cellX + 1, cellY + 1);
                graphics.renderItemDecorations(font, stack, cellX + 1, cellY + 1);

                cartIdx++;
            }
            for (int i = cartIdx; i < 9; i++) {
                int col = i % 3;
                int row = i / 3;
                int cellX = 196 + col * 18;
                int cellY = 62 + row * 18;
                graphics.fill(cellX, cellY, cellX + 18, cellY + 18, 0xFF141918);
                drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFF0E1311);
            }

            // 3. 장바구니 정보 텍스트 렌더링
            long totalCartPrice = 0;
            int totalCartQty = 0;
            for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
                ShopLine line = null;
                for (ShopLine l : menu.lines()) {
                    if (l.id().equals(entry.getKey())) {
                        line = l;
                        break;
                    }
                }
                if (line != null) {
                    totalCartPrice += line.price() * entry.getValue();
                    totalCartQty += entry.getValue();
                }
            }

            graphics.drawString(font, "장바구니", 258, 64, 0xFFE8E1C4, false);
            graphics.drawString(font, "총수량: " + totalCartQty + "개", 258, 76, 0xFF98A49C, false);
            graphics.drawString(font, "합계 금액:", 258, 88, 0xFF98A49C, false);
            graphics.drawString(font, CREDIT_FORMAT.format(totalCartPrice) + " C", 258, 100, 0xFFFFD56A, false);
        }

        drawInventoryPreview(graphics);
        renderSellSelectionSummary(graphics);
    }

    private void renderSaturationBar(GuiGraphics graphics, int x, int y, int width, int current, int max) {
        int height = 2;
        graphics.fill(x, y, x + width, y + height, 0xFF10140F);
        float progress = Math.min(1.0F, (float) current / max);
        int fillWidth = Math.round(width * progress);
        int color = progress >= 0.9F ? 0xFFFFD56A : 0xFF7DDAFF;
        graphics.fill(x, y, x + fillWidth, y + height, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hasPendingDialog()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 0 && insideScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            updateScrollbar(mouseY);
            return true;
        }
        if (button == 0 && insideMiniScrollbar(mouseX, mouseY)) {
            draggingMiniScrollbar = true;
            updateMiniScrollbar(mouseY);
            return true;
        }
        if (!sellMode) {
            // 상품 그리드 클릭
            int gridX = (int) mouseX - (leftPos + 20);
            int gridY = (int) mouseY - (topPos + 62);
            if (gridX >= 0 && gridX < 8 * 18 && gridY >= 0 && gridY < 4 * 18) {
                int col = gridX / 18;
                int row = gridY / 18;
                int idx = buyScroll * 8 + row * 8 + col;
                List<RowData> allBuyRows = buyRows();
                if (idx < allBuyRows.size()) {
                    RowData data = allBuyRows.get(idx);
                    ShopLine line = data.line();
                    int maxLimit = line.remaining();
                    if (maxLimit <= 0) {
                        return true;
                    }
                    int currentInCart = cartItems.getOrDefault(line.id(), 0);
                    if (currentInCart >= maxLimit) {
                        return true;
                    }
                    if (currentInCart == 0 && cartItems.size() >= 9) {
                        minecraft.player.displayClientMessage(Component.literal("§c장바구니는 최대 9 종류까지만 담을 수 있습니다."), true);
                        return true;
                    }
                    cartItems.put(line.id(), currentInCart + 1);
                    refreshControls();
                    return true;
                }
            }

            // 장바구니 클릭
            int cartX = (int) mouseX - (leftPos + 196);
            int cartY = (int) mouseY - (topPos + 62);
            if (cartX >= 0 && cartX < 3 * 18 && cartY >= 0 && cartY < 3 * 18) {
                int col = cartX / 18;
                int row = cartY / 18;
                int idx = row * 3 + col;
                if (idx < cartItems.size()) {
                    List<String> keys = new ArrayList<>(cartItems.keySet());
                    String entryId = keys.get(idx);
                    ShopLine line = null;
                    for (ShopLine l : menu.lines()) {
                        if (l.id().equals(entryId)) {
                            line = l;
                            break;
                        }
                    }
                    if (line != null) {
                        int currentInCart = cartItems.getOrDefault(entryId, 0);
                        if (button == 0) { // 좌클릭: 증가
                            if (currentInCart < line.remaining()) {
                                cartItems.put(entryId, currentInCart + 1);
                            }
                        } else if (button == 1) { // 우클릭: 감소
                            if (currentInCart > 1) {
                                cartItems.put(entryId, currentInCart - 1);
                            } else {
                                cartItems.remove(entryId);
                            }
                        }
                        refreshControls();
                        return true;
                    }
                }
            }
        }
        if (sellMode && button == 0) {
            int gridX = (int) mouseX - (leftPos + 24);
            int gridY = (int) mouseY - (topPos + PREVIEW_Y + 8);
            if (gridX >= 0 && gridX < 9 * 18 && gridY >= 0 && gridY < 4 * 18) {
                int col = gridX / 18;
                int row = gridY / 18;
                if (storageMode == StorageType.INVENTORY) {
                    int slotIndex = (row == 3) ? col : (9 + row * 9 + col);
                    ItemStack stack = minecraft.player.getInventory().getItem(slotIndex);
                    if (!stack.isEmpty()) {
                        if (ShopItemProtection.isSellBlocked(stack)) {
                            return true;
                        }
                        RowData matching = findMatchingSellRow(stack);
                        if (matching != null) {
                            if (selectedInventorySlots.contains(slotIndex)) {
                                selectedInventorySlots.remove(Integer.valueOf(slotIndex));
                            } else {
                                selectedInventorySlots.add(slotIndex);
                            }
                            refreshControls();
                            return true;
                        }
                    }
                } else if (storageMode == StorageType.BACKPACK) {
                    int slotIndex = storageScroll * 9 + row * 9 + col; // 배낭 스크롤 오프셋 반영!
                    ItemStack backpack = findBackpackStack();
                    if (!backpack.isEmpty()) {
                        var cap = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER);
                        if (cap.isPresent()) {
                            IItemHandler handler = cap.orElse(null);
                            if (handler != null && slotIndex < handler.getSlots()) {
                                ItemStack stack = handler.getStackInSlot(slotIndex);
                                if (!stack.isEmpty() && !ShopItemProtection.isSellBlocked(stack)) {
                                    RowData matching = findMatchingSellRow(stack);
                                    if (matching != null) {
                                        if (selectedBackpackSlots.contains(slotIndex)) {
                                            selectedBackpackSlots.remove(Integer.valueOf(slotIndex));
                                        } else {
                                            selectedBackpackSlots.add(slotIndex);
                                        }
                                        refreshControls();
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                } else if (storageMode == StorageType.STORAGE) {
                    int slotIndex = storageScroll * 9 + row * 9 + col;
                    int extLevel = menu.extInventoryNbt().contains("inventoryExtLevel") 
                        ? Math.max(1, menu.extInventoryNbt().getInt("inventoryExtLevel")) 
                        : 1;
                    int unlockedSlots = Math.min(270, Math.max(0, extLevel * 9));
                    if (slotIndex >= unlockedSlots) {
                        return true; // 잠긴 슬롯 클릭 시 즉시 동작 차단!
                    }

                    ItemStack[] extItems = com.nogeon.economyland.player.ExtendedInventoryDelivery.load(menu.extInventoryNbt());
                    if (slotIndex < extItems.length) {
                        ItemStack stack = extItems[slotIndex];
                        if (!stack.isEmpty() && !ShopItemProtection.isSellBlocked(stack)) {
                            RowData matching = findMatchingSellRow(stack);
                            if (matching != null) {
                                if (selectedStorageSlots.contains(slotIndex)) {
                                    selectedStorageSlots.remove(Integer.valueOf(slotIndex));
                                } else {
                                    selectedStorageSlots.add(slotIndex);
                                }
                                refreshControls();
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            updateScrollbar(mouseY);
            return true;
        }
        if (draggingMiniScrollbar && button == 0) {
            updateMiniScrollbar(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
            draggingMiniScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (hasPendingDialog()) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        if (sellMode) {
            int gridX = (int) mouseX - (leftPos + 24);
            int gridY = (int) mouseY - (topPos + PREVIEW_Y + 8);
            if (gridX >= 0 && gridX < 9 * 18 && gridY >= 0 && gridY < 4 * 18) {
                if (storageMode == StorageType.STORAGE) {
                    int extLevel = menu.extInventoryNbt().contains("inventoryExtLevel") 
                        ? Math.max(1, menu.extInventoryNbt().getInt("inventoryExtLevel")) 
                        : 1;
                    int unlockedSlots = Math.min(270, Math.max(0, extLevel * 9));
                    int unlockedRows = (unlockedSlots + 8) / 9;
                    int maxScroll = Math.max(0, unlockedRows - 4);
                    storageScroll = Mth.clamp(storageScroll + (delta < 0 ? 1 : -1), 0, maxScroll);
                    refreshControls();
                    return true;
                } else if (storageMode == StorageType.BACKPACK) {
                    ItemStack backpack = findBackpackStack();
                    if (!backpack.isEmpty()) {
                        var cap = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER);
                        if (cap.isPresent()) {
                            IItemHandler handler = cap.orElse(null);
                            if (handler != null) {
                                int slots = handler.getSlots();
                                int bagRows = (slots + 8) / 9;
                                int maxScroll = Math.max(0, bagRows - 4);
                                storageScroll = Mth.clamp(storageScroll + (delta < 0 ? 1 : -1), 0, maxScroll);
                                refreshControls();
                                return true;
                            }
                        }
                    }
                }
            }
        } else {
            int gridX = (int) mouseX - (leftPos + 20);
            int gridY = (int) mouseY - (topPos + 62);
            if (gridX >= 0 && gridX < 8 * 18 && gridY >= 0 && gridY < 4 * 18) {
                scrollBy(delta < 0 ? 1 : -1);
                return true;
            }
        }
        if (insideList(mouseX, mouseY) || insideScrollbar(mouseX, mouseY)) {
            scrollBy(delta < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void switchMode(boolean nextSellMode) {
        if (sellMode == nextSellMode) {
            return;
        }
        sellMode = nextSellMode;
        lastSellMode = sellMode;
        pendingRow = null;
        pendingQuantity = 1;
        selectedInventorySlots.clear();
        selectedBackpackSlots.clear();
        selectedStorageSlots.clear();
        refreshControls();
    }

    private void onRowAction(int rowIndex, boolean deliveryAction, boolean normalSellAction) {
        List<RowData> rows = visibleRows();
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return;
        }
        RowData data = rows.get(rowIndex);
        int maxLimit = deliveryAction 
            ? (normalSellAction ? data.owned() : data.maxBundles()) 
            : data.line().remaining();
        if (maxLimit > 0) {
            pendingDelivery = deliveryAction;
            pendingNormalSell = normalSellAction;
            openQuantityDialog(data);
        }
    }

    private void refreshControls() {
        int maxScroll = Math.max(0, activeRows().size() - VISIBLE_ROWS);
        if (sellMode) {
            sellScroll = Mth.clamp(sellScroll, 0, maxScroll);
            lastSellScroll = sellScroll;
        } else {
            buyScroll = Mth.clamp(buyScroll, 0, maxScroll);
            lastBuyScroll = buyScroll;
        }

        boolean dialogOpen = hasPendingDialog();
        buyTabButton.active = !dialogOpen && sellMode;
        sellTabButton.active = !dialogOpen && !sellMode;
        walletButton.active = !dialogOpen;
        jobButton.visible = !dialogOpen && isJobTrader();
        jobButton.active = !dialogOpen && isJobTrader();

        List<RowData> rows = visibleRows();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            Button buyBtn = rowButtons.get(row);
            Button sellNormalBtn = rowSellNormalButtons.get(row);
            Button sellDeliveryBtn = rowSellDeliveryButtons.get(row);

            buyBtn.setY(topPos + VIEW_Y - 2 + row * ROW_HEIGHT);
            sellNormalBtn.setY(topPos + VIEW_Y - 2 + row * ROW_HEIGHT);
            sellDeliveryBtn.setY(topPos + VIEW_Y - 2 + row * ROW_HEIGHT);

            if (row < rows.size()) {
                RowData data = rows.get(row);
                if (sellMode) {
                    buyBtn.visible = false;
                    buyBtn.active = false;
                    
                    sellNormalBtn.visible = !dialogOpen;
                    sellNormalBtn.active = !dialogOpen && data.owned() > 0;
                    sellNormalBtn.setMessage(Component.translatable("gui.nogeon_economy_land.sell_normal_btn"));

                    sellDeliveryBtn.visible = !dialogOpen;
                    sellDeliveryBtn.active = !dialogOpen && data.maxBundles() > 0;
                    sellDeliveryBtn.setMessage(Component.translatable("gui.nogeon_economy_land.sell_delivery_btn"));
                } else {
                    // 구매 모드에서는 기존 리스트 형태의 개별 구매 버튼은 노출하지 않음 (8x4 그리드 전용)
                    buyBtn.visible = false;
                    buyBtn.active = false;

                    sellNormalBtn.visible = false;
                    sellNormalBtn.active = false;
                    sellDeliveryBtn.visible = false;
                    sellDeliveryBtn.active = false;
                }
            } else {
                buyBtn.visible = false;
                buyBtn.active = false;
                sellNormalBtn.visible = false;
                sellNormalBtn.active = false;
                sellDeliveryBtn.visible = false;
                sellDeliveryBtn.active = false;
            }
        }

        int dialogLimit = quantityDialogLimit();
        pendingQuantity = Mth.clamp(pendingQuantity, 1, dialogLimit);
        modalMinusTenButton.visible = dialogOpen;
        modalMinusOneButton.visible = dialogOpen;
        modalPlusOneButton.visible = dialogOpen;
        modalPlusTenButton.visible = dialogOpen;
        modalConfirmButton.visible = dialogOpen;
        modalCancelButton.visible = dialogOpen;
        if (modalQuantityField != null) {
            modalQuantityField.visible = dialogOpen;
            modalQuantityField.active = dialogOpen;
        }
        modalMinusTenButton.active = dialogOpen && pendingQuantity > 1;
        modalMinusOneButton.active = dialogOpen && pendingQuantity > 1;
        modalPlusOneButton.active = dialogOpen && pendingQuantity < dialogLimit;
        modalPlusTenButton.active = dialogOpen && pendingQuantity < dialogLimit;
        modalConfirmButton.active = dialogOpen && dialogLimit > 0;
        
        if (dialogOpen) {
            modalConfirmButton.setMessage(Component.translatable(pendingDelivery 
                ? (pendingNormalSell ? "gui.nogeon_economy_land.sell_normal_btn" : "gui.nogeon_economy_land.sell_delivery_btn")
                : "gui.nogeon_economy_land.buy"));
        }

        boolean buttonsVisible = !dialogOpen && sellMode;
        boolean hasSelection = !selectedInventorySlots.isEmpty() || !selectedBackpackSlots.isEmpty() || !selectedStorageSlots.isEmpty();
        long totalNormalVal = calculateTotalNormalSellValue();
        long totalDeliveryVal = calculateTotalDeliverySellValue();

        if (sellSelectedButton != null) {
            sellSelectedButton.visible = buttonsVisible;
            sellSelectedButton.active = buttonsVisible && hasSelection;
        }
        if (sellAllButton != null) {
            sellAllButton.visible = buttonsVisible;
            sellAllButton.active = buttonsVisible && totalNormalVal > 0;
        }
        if (deliverySelectedButton != null) {
            deliverySelectedButton.visible = buttonsVisible;
            deliverySelectedButton.active = buttonsVisible && hasSelection && totalDeliveryVal > 0;
        }
        if (deliveryAllButton != null) {
            deliveryAllButton.visible = buttonsVisible;
            deliveryAllButton.active = buttonsVisible && totalDeliveryVal > 0;
        }

        if (invTabSubButton != null && backpackTabSubButton != null && storageTabSubButton != null) {
            boolean subTabsVisible = !dialogOpen && sellMode;
            invTabSubButton.visible = subTabsVisible;
            backpackTabSubButton.visible = subTabsVisible;
            storageTabSubButton.visible = subTabsVisible;
            
            invTabSubButton.active = subTabsVisible && storageMode != StorageType.INVENTORY;
            backpackTabSubButton.active = subTabsVisible && storageMode != StorageType.BACKPACK;
            storageTabSubButton.active = subTabsVisible && storageMode != StorageType.STORAGE;
        }

        boolean isBuyMode = !sellMode;
        if (cartBuyButton != null) {
            cartBuyButton.visible = isBuyMode && !dialogOpen;
            cartBuyButton.active = isBuyMode && !dialogOpen && !cartItems.isEmpty();
        }
        if (cartClearButton != null) {
            cartClearButton.visible = isBuyMode && !dialogOpen;
            cartClearButton.active = isBuyMode && !dialogOpen && !cartItems.isEmpty();
        }
    }

    private List<RowData> visibleRows() {
        List<RowData> source = sellMode ? sellRows() : buyRows();
        int scroll = Math.min(activeScroll(), Math.max(0, source.size() - VISIBLE_ROWS));
        int end = Math.min(source.size(), scroll + VISIBLE_ROWS);
        return source.subList(scroll, end);
    }

    private List<RowData> buyRows() {
        List<RowData> rows = new ArrayList<>();
        for (ShopLine line : menu.lines()) {
            if (!line.delivery()) {
                rows.add(new RowData(line, 0, Math.max(0, line.remaining())));
            }
        }
        return rows;
    }

    private List<RowData> sellRows() {
        List<RowData> rows = new ArrayList<>();
        for (ShopLine line : menu.lines()) {
            if (!line.delivery()) {
                continue;
            }
            int owned = com.nogeon.economyland.player.ExtendedInventoryDelivery.countAllOwnedClient(
                minecraft.player, line.stack(), menu.extInventoryNbt());
            if (owned <= 0) {
                continue;
            }
            int bundleSize = Math.max(1, line.count());
            rows.add(new RowData(line, owned, owned / bundleSize));
        }
        return rows;
    }

    private void drawInventoryPreview(GuiGraphics graphics) {
        int panelY = PREVIEW_Y - 8;
        
        if (sellMode && storageMode == StorageType.BACKPACK) {
            ItemStack backpack = findBackpackStack();
            if (backpack.isEmpty()) {
                drawCenteredPlainText(graphics, "소지한 배낭이 없습니다", leftPos + 24 + 81, PREVIEW_Y + 36, 160, 0xFFE06C75);
                return;
            }
            
            var cap = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER);
            if (cap.isPresent()) {
                IItemHandler handler = cap.orElse(null);
                if (handler != null) {
                    for (int row = 0; row < 4; row++) {
                        for (int col = 0; col < 9; col++) {
                            int slotIndex = storageScroll * 9 + row * 9 + col; // 배낭 스크롤 반영!
                            int cellX = 24 + col * 18;
                            int cellY = PREVIEW_Y + 8 + row * 18;
                            
                            graphics.fill(cellX, cellY, cellX + 18, cellY + 18, 0xFF222724);
                            drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFF1D2220);

                            if (slotIndex < handler.getSlots()) {
                                ItemStack stack = handler.getStackInSlot(slotIndex);
                                if (!stack.isEmpty()) {
                                    graphics.renderItem(stack, cellX + 1, cellY + 1);
                                    graphics.renderItemDecorations(font, stack, cellX + 1, cellY + 1);

                                    if (ShopItemProtection.isSellBlocked(stack)) {
                                        graphics.pose().pushPose();
                                        graphics.pose().translate(0.0F, 0.0F, 200.0F);
                                        graphics.fill(cellX + 1, cellY + 1, cellX + 17, cellY + 17, 0x3F9E1313);
                                        drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFFFF3333);
                                        graphics.drawString(font, "*", cellX + 1, cellY + 1, 0xFFFFD56A, true);
                                        graphics.pose().popPose();
                                    } else {
                                        RowData matching = findMatchingSellRow(stack);
                                        if (matching != null) {
                                            if (selectedBackpackSlots.contains(slotIndex)) {
                                                graphics.fill(cellX + 1, cellY + 1, cellX + 17, cellY + 17, 0x3300FFCC);
                                                drawCustomBorder(graphics, cellX - 1, cellY - 1, 20, 20, 0xFF00FFCC);
                                            } else {
                                                drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFF00FFCC);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 배낭 4줄 초과 시 미니 스크롤바 렌더링
                    int totalSlots = handler.getSlots();
                    int bagRows = (totalSlots + 8) / 9;
                    int maxScroll = Math.max(0, bagRows - 4);
                    if (maxScroll > 0) {
                        int miniScrollX = 24 + 9 * 18 + 4; // 190
                        int miniScrollY = PREVIEW_Y + 8;
                        int miniScrollHeight = 72;
                        int miniScrollWidth = 6;
                        
                        graphics.fill(miniScrollX, miniScrollY, miniScrollX + miniScrollWidth, miniScrollY + miniScrollHeight, 0xFF0E1311);
                        drawCustomBorder(graphics, miniScrollX, miniScrollY, miniScrollWidth, miniScrollHeight, 0xFF1B2C27);
                        
                        int handleHeight = 12;
                        int handleY = miniScrollY + (miniScrollHeight - handleHeight) * storageScroll / maxScroll;
                        graphics.fill(miniScrollX + 1, handleY, miniScrollX + miniScrollWidth - 1, handleY + handleHeight, 0xFF00FFCC);
                    }
                }
            }
        } else if (sellMode && storageMode == StorageType.STORAGE) {
            ItemStack[] extItems = com.nogeon.economyland.player.ExtendedInventoryDelivery.load(menu.extInventoryNbt());
            int extLevel = menu.extInventoryNbt().contains("inventoryExtLevel") 
                ? Math.max(1, menu.extInventoryNbt().getInt("inventoryExtLevel")) 
                : 1;
            int unlockedSlots = Math.min(270, Math.max(0, extLevel * 9));
            
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotIndex = storageScroll * 9 + row * 9 + col;
                    int cellX = 24 + col * 18;
                    int cellY = PREVIEW_Y + 8 + row * 18;
                    
                    graphics.fill(cellX, cellY, cellX + 18, cellY + 18, 0xFF222724);
                    drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFF1D2220);

                    if (slotIndex >= unlockedSlots) {
                        // 미해금(잠긴) 슬롯은 반투명 검은색 및 자물쇠 아이콘 렌더링
                        graphics.fill(cellX, cellY, cellX + 18, cellY + 18, 0xBF0F0F14);
                        drawLockIcon(graphics, cellX + 1, cellY + 1);
                    } else if (slotIndex < extItems.length) {
                        ItemStack stack = extItems[slotIndex];
                        if (!stack.isEmpty()) {
                            graphics.renderItem(stack, cellX + 1, cellY + 1);
                            graphics.renderItemDecorations(font, stack, cellX + 1, cellY + 1);

                            if (ShopItemProtection.isSellBlocked(stack)) {
                                graphics.pose().pushPose();
                                graphics.pose().translate(0.0F, 0.0F, 200.0F);
                                graphics.fill(cellX + 1, cellY + 1, cellX + 17, cellY + 17, 0x3F9E1313);
                                drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFFFF3333);
                                graphics.drawString(font, "*", cellX + 1, cellY + 1, 0xFFFFD56A, true);
                                graphics.pose().popPose();
                            } else {
                                RowData matching = findMatchingSellRow(stack);
                                if (matching != null) {
                                    if (selectedStorageSlots.contains(slotIndex)) {
                                        graphics.fill(cellX + 1, cellY + 1, cellX + 17, cellY + 17, 0x3300FFCC);
                                        drawCustomBorder(graphics, cellX - 1, cellY - 1, 20, 20, 0xFF00FFCC);
                                    } else {
                                        drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFF00FFCC);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 보관함 미니 스크롤바 렌더링 (해금된 줄 범위까지만 반영)
            int unlockedRows = (unlockedSlots + 8) / 9;
            int maxScroll = Math.max(0, unlockedRows - 4);
            if (maxScroll > 0) {
                int miniScrollX = 24 + 9 * 18 + 4; // 190
                int miniScrollY = PREVIEW_Y + 8;
                int miniScrollHeight = 72;
                int miniScrollWidth = 6;
                
                graphics.fill(miniScrollX, miniScrollY, miniScrollX + miniScrollWidth, miniScrollY + miniScrollHeight, 0xFF0E1311);
                drawCustomBorder(graphics, miniScrollX, miniScrollY, miniScrollWidth, miniScrollHeight, 0xFF1B2C27);
                
                int handleHeight = 12;
                int handleY = miniScrollY + (miniScrollHeight - handleHeight) * storageScroll / maxScroll;
                graphics.fill(miniScrollX + 1, handleY, miniScrollX + miniScrollWidth - 1, handleY + handleHeight, 0xFF00FFCC);
            }
        } else {
            // INVENTORY 모드 또는 구매 모드 (기본 인벤토리 표시)
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotIndex = (row == 3) ? col : (9 + row * 9 + col);
                    ItemStack stack = minecraft.player.getInventory().getItem(slotIndex);
                    int cellX = 24 + col * 18;
                    int cellY = PREVIEW_Y + 8 + row * 18;
                    
                    graphics.fill(cellX, cellY, cellX + 18, cellY + 18, 0xFF222724);
                    drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFF1D2220);

                    if (!stack.isEmpty()) {
                        graphics.renderItem(stack, cellX + 1, cellY + 1);
                        graphics.renderItemDecorations(font, stack, cellX + 1, cellY + 1);

                        if (sellMode) {
                            if (ShopItemProtection.isSellBlocked(stack)) {
                                graphics.pose().pushPose();
                                graphics.pose().translate(0.0F, 0.0F, 200.0F);
                                graphics.fill(cellX + 1, cellY + 1, cellX + 17, cellY + 17, 0x3F9E1313);
                                drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFFFF3333);
                                graphics.drawString(font, "*", cellX + 1, cellY + 1, 0xFFFFD56A, true);
                                graphics.pose().popPose();
                            } else {
                                RowData matching = findMatchingSellRow(stack);
                                if (matching != null) {
                                    if (selectedInventorySlots.contains(slotIndex)) {
                                        graphics.fill(cellX + 1, cellY + 1, cellX + 17, cellY + 17, 0x3300FFCC);
                                        drawCustomBorder(graphics, cellX - 1, cellY - 1, 20, 20, 0xFF00FFCC);
                                    } else {
                                        drawCustomBorder(graphics, cellX, cellY, 18, 18, 0xFF00FFCC);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderSellSelectionSummary(GuiGraphics graphics) {
        if (!sellMode) {
            return;
        }

        int boxX = 206;
        int boxY = PREVIEW_Y + 2;
        int boxW = 120;
        int boxH = 36;
        boolean hasSelection = hasSelectedSellSlots();
        int selectedCount = hasSelection ? calculateSelectedSellItemCount() : 0;
        long totalNormalVal = hasSelection ? calculateTotalNormalSellValue() : 0L;
        long totalDeliveryVal = hasSelection ? calculateTotalDeliverySellValue() : 0L;

        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xE60A0F0D);
        drawCustomBorder(graphics, boxX, boxY, boxW, boxH, hasSelection ? 0xFF00FFCC : 0xFF1B2C27);

        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.shop_selected_summary"), boxX + 6, boxY + 3, 0xFFE8E1C4, false);
        Component countText = Component.translatable("gui.nogeon_economy_land.shop_selected_count", selectedCount);
        drawClippedText(graphics, countText, boxX + 76, boxY + 3, 38, hasSelection ? 0xFF98F5E1 : 0xFF58645F);

        graphics.drawString(font, "판매: ", boxX + 6, boxY + 14, hasSelection ? 0xFF98A49C : 0xFF58645F, false);
        Component normalText = Component.literal(CREDIT_FORMAT.format(totalNormalVal) + " C");
        drawClippedText(graphics, normalText, boxX + 36, boxY + 14, 78, hasSelection ? 0xFFFFD56A : 0xFF58645F);

        graphics.drawString(font, "납품: ", boxX + 6, boxY + 25, hasSelection ? 0xFF98A49C : 0xFF58645F, false);
        Component deliveryText = Component.literal(CREDIT_FORMAT.format(totalDeliveryVal) + " C");
        drawClippedText(graphics, deliveryText, boxX + 36, boxY + 25, 78, hasSelection ? 0xFF7DDAFF : 0xFF58645F);
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int left = scrollbarLeft();
        int top = viewTop();
        int bottom = viewBottom();
        int handleHeight = scrollbarHandleHeight();
        int handleTop = scrollbarHandleTop();
        graphics.fill(left, top, left + SCROLLBAR_WIDTH, bottom, 0xFF0E1311); // 트랙 칠흑
        drawCustomBorder(graphics, left, top, SCROLLBAR_WIDTH, bottom - top, 0xFF1B2C27); // 트랙 프레임
        
        int handleColor = draggingScrollbar ? 0xFF00FFCC : 0xFF769B8E; // 드래깅 시 눈부신 시안 네온 피드백!
        graphics.fill(left + 1, handleTop, left + SCROLLBAR_WIDTH - 1, handleTop + handleHeight, handleColor);
    }

    private void drawClippedText(GuiGraphics graphics, Component text, int x, int y, int width, int color) {    
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), width), x, y, color, false);        
    }

    private void drawCenteredPlainText(GuiGraphics graphics, String text, int centerX, int y, int width, int color) {
        String clipped = font.plainSubstrByWidth(text, width);
        graphics.drawString(font, clipped, centerX - font.width(clipped) / 2, y, color, false);
    }

    private ItemStack tooltipStack(int mouseX, int mouseY) {
        if (hasPendingDialog()) {
            int dialogIconX = leftPos + 92;
            int dialogIconY = topPos + 107;
            if (insideIcon(mouseX, mouseY, dialogIconX, dialogIconY)) {
                return pendingRow.previewStack();
            }
            return ItemStack.EMPTY;
        }

        List<RowData> rows = visibleRows();
        for (int row = 0; row < rows.size(); row++) {
            int iconX = leftPos + 24;
            int iconY = topPos + VIEW_Y + row * ROW_HEIGHT - 2;
            if (insideIcon(mouseX, mouseY, iconX, iconY)) {
                return rows.get(row).previewStack();
            }
        }

        if (sellMode) {
            int gridX = mouseX - (leftPos + 24);
            int gridY = mouseY - (topPos + PREVIEW_Y + 8);
            if (gridX >= 0 && gridX < 9 * 18 && gridY >= 0 && gridY < 4 * 18) {
                int col = gridX / 18;
                int row = gridY / 18;
                if (storageMode == StorageType.INVENTORY) {
                    int slotIndex = (row == 3) ? col : (9 + row * 9 + col);
                    return minecraft.player.getInventory().getItem(slotIndex);
                } else if (storageMode == StorageType.BACKPACK) {
                    int slotIndex = storageScroll * 9 + row * 9 + col; // 배낭 스크롤 오프셋 반영!
                    ItemStack backpack = findBackpackStack();
                    if (!backpack.isEmpty()) {
                        var cap = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER);
                        if (cap.isPresent()) {
                            IItemHandler handler = cap.orElse(null);
                            if (handler != null && slotIndex < handler.getSlots()) {
                                return handler.getStackInSlot(slotIndex);
                            }
                        }
                    }
                } else if (storageMode == StorageType.STORAGE) {
                    int slotIndex = storageScroll * 9 + row * 9 + col;
                    ItemStack[] extItems = com.nogeon.economyland.player.ExtendedInventoryDelivery.load(menu.extInventoryNbt());
                    if (slotIndex < extItems.length) {
                        return extItems[slotIndex];
                    }
                }
            }
        } else {
            // 1. 상품 그리드 툴팁 판정
            int gridX = mouseX - (leftPos + 20);
            int gridY = mouseY - (topPos + 62);
            if (gridX >= 0 && gridX < 8 * 18 && gridY >= 0 && gridY < 4 * 18) {
                int col = gridX / 18;
                int row = gridY / 18;
                int idx = buyScroll * 8 + row * 8 + col;
                List<RowData> allBuyRows = buyRows();
                if (idx < allBuyRows.size()) {
                    return allBuyRows.get(idx).previewStack();
                }
            }

            // 2. 장바구니 툴팁 판정
            int cartX = mouseX - (leftPos + 196);
            int cartY = mouseY - (topPos + 62);
            if (cartX >= 0 && cartX < 3 * 18 && cartY >= 0 && cartY < 3 * 18) {
                int col = cartX / 18;
                int row = cartY / 18;
                int idx = row * 3 + col;
                if (idx < cartItems.size()) {
                    List<String> keys = new ArrayList<>(cartItems.keySet());
                    String entryId = keys.get(idx);
                    for (ShopLine l : menu.lines()) {
                        if (l.id().equals(entryId)) {
                            ItemStack base = l.stack().copy();
                            base.setCount(l.count() * cartItems.get(entryId));
                            return base;
                        }
                    }
                }
            }

            // 3. 하단 인벤토리 툴팁 판정
            int invX = mouseX - (leftPos + 24);
            int invY = mouseY - (topPos + PREVIEW_Y + 8);
            if (invX >= 0 && invX < 9 * 18 && invY >= 0 && invY < 4 * 18) {
                int col = invX / 18;
                int row = invY / 18;
                int slotIndex = (row == 3) ? col : (9 + row * 9 + col);
                return minecraft.player.getInventory().getItem(slotIndex);
            }
        }

        return ItemStack.EMPTY;
    }

    private boolean insideIcon(int mouseX, int mouseY, int iconX, int iconY) {
        return mouseX >= iconX && mouseX <= iconX + 16 && mouseY >= iconY && mouseY <= iconY + 16;
    }

    private boolean insideMiniScrollbar(double mouseX, double mouseY) {
        if (!sellMode || storageMode == StorageType.INVENTORY) {
            return false;
        }
        int maxScroll = 0;
        if (storageMode == StorageType.BACKPACK) {
            ItemStack backpack = findBackpackStack();
            if (!backpack.isEmpty()) {
                var cap = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER);
                if (cap.isPresent()) {
                    IItemHandler handler = cap.orElse(null);
                    if (handler != null) {
                        int slots = handler.getSlots();
                        int bagRows = (slots + 8) / 9;
                        maxScroll = Math.max(0, bagRows - 4);
                    }
                }
            }
        } else if (storageMode == StorageType.STORAGE) {
            int extLevel = menu.extInventoryNbt().contains("inventoryExtLevel") 
                ? Math.max(1, menu.extInventoryNbt().getInt("inventoryExtLevel")) 
                : 1;
            int unlockedSlots = Math.min(270, Math.max(0, extLevel * 9));
            int unlockedRows = (unlockedSlots + 8) / 9;
            maxScroll = Math.max(0, unlockedRows - 4);
        }
        if (maxScroll <= 0) {
            return false;
        }
        int miniScrollX = leftPos + 190;
        int miniScrollY = topPos + PREVIEW_Y + 8;
        return mouseX >= miniScrollX && mouseX < miniScrollX + 6 && mouseY >= miniScrollY && mouseY < miniScrollY + 72;
    }

    private void updateMiniScrollbar(double mouseY) {
        int maxScroll = 0;
        if (storageMode == StorageType.BACKPACK) {
            ItemStack backpack = findBackpackStack();
            if (!backpack.isEmpty()) {
                var cap = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER);
                if (cap.isPresent()) {
                    IItemHandler handler = cap.orElse(null);
                    if (handler != null) {
                        int slots = handler.getSlots();
                        int bagRows = (slots + 8) / 9;
                        maxScroll = Math.max(0, bagRows - 4);
                    }
                }
            }
        } else if (storageMode == StorageType.STORAGE) {
            int extLevel = menu.extInventoryNbt().contains("inventoryExtLevel") 
                ? Math.max(1, menu.extInventoryNbt().getInt("inventoryExtLevel")) 
                : 1;
            int unlockedSlots = Math.min(270, Math.max(0, extLevel * 9));
            int unlockedRows = (unlockedSlots + 8) / 9;
            maxScroll = Math.max(0, unlockedRows - 4);
        }
        if (maxScroll <= 0) {
            storageScroll = 0;
            refreshControls();
            return;
        }

        int miniScrollY = topPos + PREVIEW_Y + 8;
        int miniScrollHeight = 72;
        int handleHeight = 12;
        int travel = miniScrollHeight - handleHeight;
        if (travel <= 0) {
            storageScroll = 0;
            refreshControls();
            return;
        }

        float progress = (float) ((mouseY - miniScrollY - handleHeight / 2.0F) / travel);
        storageScroll = net.minecraft.util.Mth.clamp(Math.round(progress * maxScroll), 0, maxScroll);
        refreshControls();
    }

    private void scrollBy(int amount) {
        if (sellMode) {
            int maxScroll = Math.max(0, activeRows().size() - VISIBLE_ROWS);
            sellScroll = Mth.clamp(sellScroll + amount, 0, maxScroll);
            lastSellScroll = sellScroll;
        } else {
            int totalRows = (buyRows().size() + 7) / 8;
            int maxScroll = Math.max(0, totalRows - 4);
            buyScroll = Mth.clamp(buyScroll + amount, 0, maxScroll);
            lastBuyScroll = buyScroll;
        }
        refreshControls();
    }

    private void updateScrollbar(double mouseY) {
        int handleHeight = scrollbarHandleHeight();
        if (sellMode) {
            int travel = VIEW_HEIGHT - handleHeight;
            if (travel <= 0) {
                sellScroll = 0;
                lastSellScroll = 0;
                refreshControls();
                return;
            }
            float progress = (float) ((mouseY - viewTop() - handleHeight / 2.0F) / travel);
            int maxScroll = Math.max(0, activeRows().size() - VISIBLE_ROWS);
            sellScroll = Math.round(Mth.clamp(progress, 0.0F, 1.0F) * maxScroll);
            lastSellScroll = sellScroll;
        } else {
            int travel = 72 - handleHeight;
            if (travel <= 0) {
                buyScroll = 0;
                lastBuyScroll = 0;
                refreshControls();
                return;
            }
            float progress = (float) ((mouseY - viewTop() - handleHeight / 2.0F) / travel);
            int totalRows = (buyRows().size() + 7) / 8;
            int maxScroll = Math.max(0, totalRows - 4);
            buyScroll = Math.round(Mth.clamp(progress, 0.0F, 1.0F) * maxScroll);
            lastBuyScroll = buyScroll;
        }
        refreshControls();
    }

    private int activeScroll() {
        return sellMode ? sellScroll : buyScroll;
    }

    private List<RowData> activeRows() {
        return sellMode ? sellRows() : buyRows();
    }

    private boolean hasPendingDialog() {
        return pendingRow != null;
    }

    private boolean isJobTrader() {
        return !jobActionId().isEmpty();
    }

    private String traderKindId() {
        for (ShopLine line : menu.lines()) {
            return line.kindId();
        }
        return "";
    }

    private String jobActionId() {
        return switch (traderKindId()) {
            case "crop" -> "job_farmer";
            case "fisher" -> "job_fisher";
            case "miner" -> "job_miner";
            case "chef" -> "job_cook";
            case "hunter" -> "job_hunter";
            case "engineer" -> "job_engineer";
            default -> "";
        };
    }

    private void sendJobChange() {
        String jobId = jobId();
        if (!jobId.isEmpty()) {
            ModNetwork.CHANNEL.sendToServer(new OpenJobChangePacket(jobId));
        }
    }

    private String jobId() {
        return switch (traderKindId()) {
            case "crop" -> "farmer";
            case "fisher" -> "fisher";
            case "miner" -> "miner";
            case "chef" -> "cook";
            case "hunter" -> "hunter";
            case "engineer" -> "engineer";
            default -> "";
        };
    }

    private void openQuantityDialog(RowData row) {
        pendingRow = row;
        pendingQuantity = 1;
        if (modalQuantityField != null) {
            modalQuantityField.setValue("1");
            modalQuantityField.visible = true;
            modalQuantityField.active = true;
            modalQuantityField.setFocused(true);
        }
        refreshControls();
    }

    private void closeQuantityDialog() {
        pendingRow = null;
        pendingQuantity = 1;
        if (modalQuantityField != null) {
            modalQuantityField.visible = false;
            modalQuantityField.active = false;
            modalQuantityField.setFocused(false);
        }
        refreshControls();
    }

    private void adjustPendingQuantity(int delta) {
        if (pendingRow == null) {
            return;
        }
        pendingQuantity = Mth.clamp(pendingQuantity + delta, 1, quantityDialogLimit());
        if (modalQuantityField != null) {
            modalQuantityField.setValue(String.valueOf(pendingQuantity));
        }
        refreshControls();
    }

    private void confirmPendingAction() {
        if (pendingRow == null) {
            return;
        }
        int amount = Math.min(pendingQuantity, quantityDialogLimit());
        if (amount <= 0) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new BuyShopItemPacket(
            pendingRow.line().kindId(),
            menu.traderDatabaseId(),
            pendingRow.line().id(),
            pendingDelivery,
            amount,
            pendingNormalSell
        ));
        closeQuantityDialog();
    }

    private int quantityDialogLimit() {
        if (pendingRow == null) {
            return 1;
        }
        if (pendingDelivery) {
            return pendingNormalSell ? Math.max(1, pendingRow.owned()) : Math.max(1, pendingRow.maxBundles());
        }
        return Math.max(1, pendingRow.line().remaining());
    }

    private boolean insideList(double mouseX, double mouseY) {
        return mouseX >= viewLeft() && mouseX < viewRight() && mouseY >= viewTop() && mouseY < viewBottom();    
    }

    private boolean insideScrollbar(double mouseX, double mouseY) {
        return mouseX >= scrollbarLeft() && mouseX < scrollbarLeft() + SCROLLBAR_WIDTH
            && mouseY >= viewTop() && mouseY < viewBottom();
    }

    private int viewLeft() {
        return sellMode ? (leftPos + VIEW_X) : (leftPos + 20);
    }

    private int viewTop() {
        return sellMode ? (topPos + VIEW_Y) : (topPos + 62);
    }

    private int viewRight() {
        return sellMode ? (viewLeft() + VIEW_WIDTH) : (leftPos + 164);
    }

    private int viewBottom() {
        return sellMode ? (viewTop() + VIEW_HEIGHT) : (topPos + 134);
    }

    private int scrollbarLeft() {
        return sellMode ? (viewRight() + 6) : (leftPos + 170);
    }

    private int scrollbarHandleHeight() {
        if (sellMode) {
            int rowCount = activeRows().size();
            if (rowCount <= VISIBLE_ROWS) {
                return VIEW_HEIGHT;
            }
            return Math.max(22, Math.round((float) VIEW_HEIGHT * VISIBLE_ROWS / rowCount));
        } else {
            int totalRows = (buyRows().size() + 7) / 8;
            if (totalRows <= 4) {
                return 72;
            }
            return Math.max(12, Math.round((float) 72 * 4 / totalRows));
        }
    }

    private int scrollbarHandleTop() {
        int handleHeight = scrollbarHandleHeight();
        if (sellMode) {
            if (handleHeight >= VIEW_HEIGHT) {
                return viewTop();
            }
            int maxScroll = Math.max(1, activeRows().size() - VISIBLE_ROWS);
            float progress = (float) activeScroll() / maxScroll;
            return viewTop() + Math.round((VIEW_HEIGHT - handleHeight) * progress);
        } else {
            if (handleHeight >= 72) {
                return viewTop();
            }
            int totalRows = (buyRows().size() + 7) / 8;
            int maxScroll = Math.max(1, totalRows - 4);
            float progress = (float) buyScroll / maxScroll;
            return viewTop() + Math.round((72 - handleHeight) * progress);
        }
    }

    private ItemStack findBackpackStack() {
        java.util.List<ItemStack> list = com.nogeon.economyland.player.ExtendedInventoryDelivery.findAllBackpacks(minecraft.player);
        return list.isEmpty() ? ItemStack.EMPTY : list.get(0);
    }

    private RowData findMatchingSellRow(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (ShopLine line : menu.lines()) {
            if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                int owned = com.nogeon.economyland.player.ExtendedInventoryDelivery.countAllOwnedClient(
                    minecraft.player, line.stack(), menu.extInventoryNbt(), line.kindId());
                int bundleSize = Math.max(1, line.count());
                return new RowData(line, owned, owned / bundleSize);
            }
        }
        return null;
    }

    private boolean hasSelectedSellSlots() {
        return !selectedInventorySlots.isEmpty() || !selectedBackpackSlots.isEmpty() || !selectedStorageSlots.isEmpty();
    }

    private int calculateSelectedSellItemCount() {
        int count = 0;
        for (int slotIndex : selectedInventorySlots) {
            ItemStack stack = minecraft.player.getInventory().getItem(slotIndex);
            if (!stack.isEmpty() && !ShopItemProtection.isSellBlocked(stack) && findMatchingSellRow(stack) != null) {
                count += stack.getCount();
            }
        }

        ItemStack backpack = findBackpackStack();
        if (!backpack.isEmpty()) {
            var cap = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER);
            if (cap.isPresent()) {
                IItemHandler handler = cap.orElse(null);
                if (handler != null) {
                    for (int slotIndex : selectedBackpackSlots) {
                        if (slotIndex < handler.getSlots()) {
                            ItemStack stack = handler.getStackInSlot(slotIndex);
                            if (!stack.isEmpty() && !ShopItemProtection.isSellBlocked(stack) && findMatchingSellRow(stack) != null) {
                                count += stack.getCount();
                            }
                        }
                    }
                }
            }
        }

        ItemStack[] extItems = com.nogeon.economyland.player.ExtendedInventoryDelivery.load(menu.extInventoryNbt());
        for (int slotIndex : selectedStorageSlots) {
            if (slotIndex < extItems.length) {
                ItemStack stack = extItems[slotIndex];
                if (!stack.isEmpty() && !ShopItemProtection.isSellBlocked(stack) && findMatchingSellRow(stack) != null) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    private long calculateTotalNormalSellValue() {
        return calculateTotalSellValueInternal(false);
    }

    private long calculateTotalDeliverySellValue() {
        return calculateTotalSellValueInternal(true);
    }

    private long calculateTotalSellValueInternal(boolean isDelivery) {
        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        
        boolean hasSelection = hasSelectedSellSlots();
        
        if (hasSelection) {
            for (int slotIndex : selectedInventorySlots) {
                ItemStack stack = minecraft.player.getInventory().getItem(slotIndex);
                if (stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                    continue;
                }
                for (ShopLine line : menu.lines()) {
                    if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                        String lineId = line.id();
                        itemCounts.put(lineId, itemCounts.getOrDefault(lineId, 0) + stack.getCount());
                        break;
                    }
                }
            }
            ItemStack backpack = findBackpackStack();
            if (!backpack.isEmpty()) {
                var cap = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER);
                if (cap.isPresent()) {
                    IItemHandler handler = cap.orElse(null);
                    if (handler != null) {
                        for (int slotIndex : selectedBackpackSlots) {
                            if (slotIndex < handler.getSlots()) {
                                ItemStack stack = handler.getStackInSlot(slotIndex);
                                if (stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                                    continue;
                                }
                                for (ShopLine line : menu.lines()) {
                                    if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                                        String lineId = line.id();
                                        itemCounts.put(lineId, itemCounts.getOrDefault(lineId, 0) + stack.getCount());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ItemStack[] extItems = com.nogeon.economyland.player.ExtendedInventoryDelivery.load(menu.extInventoryNbt());
            for (int slotIndex : selectedStorageSlots) {
                if (slotIndex < extItems.length) {
                    ItemStack stack = extItems[slotIndex];
                    if (stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                        continue;
                    }
                    for (ShopLine line : menu.lines()) {
                        if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                            String lineId = line.id();
                            itemCounts.put(lineId, itemCounts.getOrDefault(lineId, 0) + stack.getCount());
                            break;
                        }
                    }
                }
            }
        } else {
            // 전부 판매 예정액 표시: 배낭(Backpack) 내부 아이템은 완전 제외!! (장착장비 36~40 슬롯 보호)
            for (int slot = 0; slot < 36; slot++) {
                ItemStack stack = minecraft.player.getInventory().getItem(slot);
                if (stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                    continue;
                }
                if (stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                    continue;
                }
                for (ShopLine line : menu.lines()) {
                    if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                        String lineId = line.id();
                        itemCounts.put(lineId, itemCounts.getOrDefault(lineId, 0) + stack.getCount());
                        break;
                    }
                }
            }
            ItemStack[] extItems = com.nogeon.economyland.player.ExtendedInventoryDelivery.load(menu.extInventoryNbt());
            for (ItemStack stack : extItems) {
                if (stack == null || stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                    continue;
                }
                for (ShopLine line : menu.lines()) {
                    if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                        String lineId = line.id();
                        itemCounts.put(lineId, itemCounts.getOrDefault(lineId, 0) + stack.getCount());
                        break;
                    }
                }
            }
        }

        long totalValue = 0;
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            String lineId = entry.getKey();
            int totalCount = entry.getValue();
            
            ShopLine line = null;
            for (ShopLine l : menu.lines()) {
                if (l.id().equals(lineId)) {
                    line = l;
                    break;
                }
            }
            if (line == null) continue;

            int bundleSize = Math.max(1, line.count());
            long basePrice = line.price();
            if (!line.id().startsWith("dynamic:")) {
                basePrice = Math.max(1L, Math.round((double) basePrice / bundleSize));
            }
            long normalPrice = Math.round((double) basePrice / 1.8D * 2.4D);
            
            if (isDelivery) {
                long deliveryPrice;
                if (line.id().startsWith("dynamic:")) {
                    deliveryPrice = line.price() * bundleSize;
                } else {
                    deliveryPrice = line.price();
                }

                int bundles = totalCount / bundleSize;
                int rem = totalCount % bundleSize;

                totalValue += (long) bundles * deliveryPrice + (long) rem * normalPrice;
            } else {
                totalValue += (long) totalCount * normalPrice;
            }
        }

        return totalValue;
    }

    private void sellSelectedInventory(boolean isDelivery) {
        if (selectedInventorySlots.isEmpty() && selectedBackpackSlots.isEmpty() && selectedStorageSlots.isEmpty()) {
            return;
        }
        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        
        for (int slotIndex : selectedInventorySlots) {
            ItemStack stack = minecraft.player.getInventory().getItem(slotIndex);
            if (stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                continue;
            }
            for (ShopLine line : menu.lines()) {
                if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                    String lineId = line.id();
                    itemCounts.put(lineId, itemCounts.getOrDefault(lineId, 0) + stack.getCount());
                    break;
                }
            }
        }
        
        ItemStack backpack = findBackpackStack();
        if (!backpack.isEmpty()) {
            var cap = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER);
            if (cap.isPresent()) {
                IItemHandler handler = cap.orElse(null);
                if (handler != null) {
                    for (int slotIndex : selectedBackpackSlots) {
                        if (slotIndex < handler.getSlots()) {
                            ItemStack stack = handler.getStackInSlot(slotIndex);
                            if (stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                                continue;
                            }
                            for (ShopLine line : menu.lines()) {
                                if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                                    String lineId = line.id();
                                    itemCounts.put(lineId, itemCounts.getOrDefault(lineId, 0) + stack.getCount());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        ItemStack[] extItems = com.nogeon.economyland.player.ExtendedInventoryDelivery.load(menu.extInventoryNbt());
        for (int slotIndex : selectedStorageSlots) {
            if (slotIndex < extItems.length) {
                ItemStack stack = extItems[slotIndex];
                if (stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                    continue;
                }
                for (ShopLine line : menu.lines()) {
                    if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                        String lineId = line.id();
                        itemCounts.put(lineId, itemCounts.getOrDefault(lineId, 0) + stack.getCount());
                        break;
                    }
                }
            }
        }

        // --- 클라이언트 측 즉시 가상 차감 (UX 극대화) ---
        for (int slotIndex : selectedInventorySlots) {
            minecraft.player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
        }
        if (!backpack.isEmpty()) {
            var cap = backpack.getCapability(ForgeCapabilities.ITEM_HANDLER);
            if (cap.isPresent()) {
                IItemHandler handler = cap.orElse(null);
                if (handler instanceof net.minecraftforge.items.IItemHandlerModifiable modifiable) {
                    for (int slotIndex : selectedBackpackSlots) {
                        if (slotIndex < modifiable.getSlots()) {
                            modifiable.setStackInSlot(slotIndex, ItemStack.EMPTY);
                        }
                    }
                }
            }
        }
        boolean storageChanged = false;
        for (int slotIndex : selectedStorageSlots) {
            if (slotIndex < extItems.length) {
                extItems[slotIndex] = ItemStack.EMPTY;
                storageChanged = true;
            }
        }
        if (storageChanged) {
            net.minecraft.nbt.CompoundTag newNbt = com.nogeon.economyland.player.ExtendedInventoryDelivery.save(extItems);
            menu.extInventoryNbt().put("Items", newNbt.getList("Items", 10));
        }
        // ------------------------------------------------

        sendSellPackets(itemCounts, isDelivery);
        selectedInventorySlots.clear();
        selectedBackpackSlots.clear();
        selectedStorageSlots.clear();
        refreshControls();
    }

    private void sellAllInventory(boolean isDelivery) {
        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                continue;
            }
            if (stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                continue;
            }
            for (ShopLine line : menu.lines()) {
                if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                    String lineId = line.id();
                    itemCounts.put(lineId, itemCounts.getOrDefault(lineId, 0) + stack.getCount());
                    break;
                }
            }
        }
        
        ItemStack[] extItems = com.nogeon.economyland.player.ExtendedInventoryDelivery.load(menu.extInventoryNbt());
        for (ItemStack stack : extItems) {
            if (stack == null || stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                continue;
            }
            for (ShopLine line : menu.lines()) {
                if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                    String lineId = line.id();
                    itemCounts.put(lineId, itemCounts.getOrDefault(lineId, 0) + stack.getCount());
                    break;
                }
            }
        }

        // --- 클라이언트 측 즉시 가상 전부 차감 (배낭 및 장착장비 보호) ---
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = minecraft.player.getInventory().getItem(slot);
            if (stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                continue;
            }
            if (stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
                continue;
            }
            for (ShopLine line : menu.lines()) {
                if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                    minecraft.player.getInventory().setItem(slot, ItemStack.EMPTY);
                    break;
                }
            }
        }
        boolean storageChanged = false;
        for (int i = 0; i < extItems.length; i++) {
            ItemStack stack = extItems[i];
            if (stack == null || stack.isEmpty() || ShopItemProtection.isSellBlocked(stack)) {
                continue;
            }
            for (ShopLine line : menu.lines()) {
                if (line.delivery() && com.nogeon.economyland.shop.DynamicPriceLogic.isSameItemForTrade(stack, line.stack(), line.kindId())) {
                    extItems[i] = ItemStack.EMPTY;
                    storageChanged = true;
                    break;
                }
            }
        }
        if (storageChanged) {
            net.minecraft.nbt.CompoundTag newNbt = com.nogeon.economyland.player.ExtendedInventoryDelivery.save(extItems);
            menu.extInventoryNbt().put("Items", newNbt.getList("Items", 10));
        }
        // --------------------------------------------------------

        sendSellPackets(itemCounts, isDelivery);
        selectedInventorySlots.clear();
        selectedBackpackSlots.clear();
        selectedStorageSlots.clear();
        refreshControls();
    }

    private void sendSellPackets(Map<String, Integer> itemCounts, boolean isDelivery) {
        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
            String lineId = entry.getKey();
            int totalCount = entry.getValue();
            
            ShopLine line = null;
            for (ShopLine l : menu.lines()) {
                if (l.id().equals(lineId)) {
                    line = l;
                    break;
                }
            }
            if (line == null) continue;

            if (isDelivery) {
                int bundleSize = Math.max(1, line.count());
                int bundles = totalCount / bundleSize;
                int rem = totalCount % bundleSize;

                if (bundles > 0) {
                    ModNetwork.CHANNEL.sendToServer(new BuyShopItemPacket(
                        line.kindId(),
                        menu.traderDatabaseId(),
                        line.id(),
                        true,
                        bundles,
                        false
                    ));
                }
                if (rem > 0) {
                    ModNetwork.CHANNEL.sendToServer(new BuyShopItemPacket(
                        line.kindId(),
                        menu.traderDatabaseId(),
                        line.id(),
                        true,
                        rem,
                        true
                    ));
                }
            } else {
                if (totalCount > 0) {
                    ModNetwork.CHANNEL.sendToServer(new BuyShopItemPacket(
                        line.kindId(),
                        menu.traderDatabaseId(),
                        line.id(),
                        true,
                        totalCount,
                        true
                    ));
                }
            }
        }
    }

    private void drawLockIcon(GuiGraphics graphics, int x, int y) {
        // 자물쇠 고리 (은빛 철광석 픽셀아트)
        graphics.fill(x + 6, y + 2, x + 10, y + 3, 0xFFB0B5B9);
        graphics.fill(x + 5, y + 3, x + 6, y + 6, 0xFFB0B5B9);
        graphics.fill(x + 10, y + 3, x + 11, y + 6, 0xFF6A6E72);

        // 자물쇠 바디 (실버 메탈 픽셀아트)
        graphics.fill(x + 3, y + 6, x + 13, y + 13, 0xFF808589);
        graphics.fill(x + 4, y + 7, x + 12, y + 8, 0xFFDDE1E5);
        graphics.fill(x + 4, y + 7, x + 5, y + 12, 0xFFDDE1E5);

        // 자물쇠 그림자 및 테두리
        graphics.fill(x + 3, y + 12, x + 13, y + 13, 0xFF4A4E52);
        graphics.fill(x + 12, y + 7, x + 13, y + 12, 0xFF4A4E52);

        // 열쇠구멍 (다크 그레이)
        graphics.fill(x + 7, y + 9, x + 9, y + 10, 0xFF151515);
        graphics.fill(x + 8, y + 10, x + 9, y + 12, 0xFF151515);
    }

    public void refreshShopLines() {
        this.refreshControls();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (modalQuantityField != null && modalQuantityField.isFocused()) {
            if (keyCode == 256) { // ESC
                modalQuantityField.setFocused(false);
                return true;
            }
            return modalQuantityField.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (modalQuantityField != null && modalQuantityField.isFocused()) {
            return modalQuantityField.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void buyAllCartItems() {
        if (cartItems.isEmpty()) {
            return;
        }
        List<com.nogeon.economyland.network.BuyCartItemsPacket.CartItem> packetItems = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
            packetItems.add(new com.nogeon.economyland.network.BuyCartItemsPacket.CartItem(entry.getKey(), entry.getValue()));
        }
        
        ModNetwork.CHANNEL.sendToServer(new com.nogeon.economyland.network.BuyCartItemsPacket(
            traderKindId(),
            menu.traderDatabaseId(),
            packetItems
        ));
        
        cartItems.clear();
        refreshControls();
    }

    private void clearCart() {
        cartItems.clear();
        refreshControls();
    }

    private static final class RowData {
        private final ShopLine line;
        private final int owned;
        private final int maxBundles;

        public RowData(ShopLine line, int owned, int maxBundles) {
            this.line = line;
            this.owned = owned;
            this.maxBundles = maxBundles;
        }

        public ShopLine line() {
            return line;
        }

        public int owned() {
            return owned;
        }

        public int maxBundles() {
            return maxBundles;
        }

        public ItemStack previewStack() {
            ItemStack base = line.stack().copy();
            base.setCount(line.count());
            return base;
        }
    }
}
