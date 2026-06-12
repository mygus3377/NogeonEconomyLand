package com.nogeon.economyland.client;

import net.minecraft.sounds.SoundEvents;
import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.item.SmithingService;
import com.nogeon.economyland.menu.ShopLine;
import com.nogeon.economyland.menu.SmithMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.SmithActionPacket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
public final class SmithScreen extends AbstractContainerScreen<SmithMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int PREVIEW_X = 24;
    private static final int PREVIEW_Y = 74;
    private static final int INVENTORY_X = 206;
    private static final int INVENTORY_Y = 74;
    private static final int SHOP_X = 24;
    private static final int SHOP_Y = 78;
    private static final int SLOT_SIZE = 18;
    private static final int VISIBLE_SHOP_ROWS = 4;
    private static final int SHOP_ROW_HEIGHT = 34;
    private static final int GEM_POPUP_COLUMNS = 4;
    private static final int GEM_POPUP_SLOT_SIZE = 22;
    private static final int GEM_POPUP_SLOT_GAP = 8;

    private static boolean lastShopMode;
    private static boolean lastScrollMode;
    private static boolean lastDeliveryMode;
    private static int lastShopScroll;
    private static boolean keepGemSelection;
    private static int lastGemTier;

    private HextechButton serviceTabButton;
    private HextechButton scrollTabButton;
    private HextechButton shopTabButton;
    private HextechButton deliveryTabButton;
    private HextechButton enhanceButton;
    private HextechButton repairButton;
    private HextechButton gemButton;
    private HextechButton downgradeScrollButton;
    private HextechButton buyButton;
    private HextechButton safetyLockButton;
    private final HextechButton[] scrollButtons = new HextechButton[7];
    private int selectedSlot;
    private boolean shopMode = lastShopMode;
    private boolean scrollMode = lastScrollMode;
    private boolean deliveryMode = lastDeliveryMode;
    private int shopScroll = lastShopScroll;
    private int selectedOfferIndex;
    private boolean selectionChanged;
    private boolean draggingScrollbar;
    private int selectedGemTier;
    private boolean gemPopupOpen;

    // 대장간 애니메이션 및 도파민 연출 필드
    private static boolean skipAnimation = false;
    private boolean isAnimating = false;
    private int enhanceAnimationTicks = 0;
    private String pendingAction = "";
    private HextechButton skipToggleButton;
    private int animationFlashTicks = 0;
    private long lastActionTime = 0L;

    public SmithScreen(SmithMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 388;
        imageHeight = 264;
        inventoryLabelY = 10_000;
        selectedSlot = menu.selectedSlot();
        shopMode = false;
        scrollMode = false;
        deliveryMode = false;
        selectedGemTier = keepGemSelection ? lastGemTier : 0;
        lastShopMode = false;
        lastScrollMode = false;
        lastDeliveryMode = false;
        selectionChanged = false;
    }

    @Override
    protected void init() {
        super.init();
        isAnimating = false;
        enhanceAnimationTicks = 0;
        animationFlashTicks = 0;

        serviceTabButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.smith_service_tab"),
            button -> setMode(false, false, false))
            .bounds(leftPos + 24, topPos + 24, 100, 18) // 가로 100 대칭 균형 배치
            .build());
            
        shopTabButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.smith_shop_tab"),
            button -> {
                onClose();
                ModNetwork.CHANNEL.sendToServer(new com.nogeon.economyland.network.OpenShopPacket("smith")); // 클릭 시 프리미엄 상점 호출!
            })
            .bounds(leftPos + 142, topPos + 24, 100, 18) // 가로 100 대칭 균형 배치
            .build());
            
        deliveryTabButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.shop_sell_tab"),
            button -> {
                onClose();
                ModNetwork.CHANNEL.sendToServer(new com.nogeon.economyland.network.OpenShopPacket("smith")); // 클릭 시 프리미엄 상점 호출!
            })
            .bounds(leftPos + 260, topPos + 24, 100, 18) // 가로 100 대칭 균형 배치
            .build());
            
        enhanceButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.smith_enhance"),
            button -> triggerAction("enhance"))
            .bounds(leftPos + 24, topPos + 152, 80, 20)
            .build());
            
        repairButton = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.smith_repair"),
            button -> triggerAction("repair"))
            .bounds(leftPos + 110, topPos + 152, 80, 20)
            .build());

        gemButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("*"),
            button -> {
                gemPopupOpen = !gemPopupOpen;
            })
            .bounds(leftPos + 174, topPos + 114, 18, 18)
            .build());

        downgradeScrollButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal(""),
            button -> {})
            .bounds(leftPos + 174, topPos + 134, 18, 18)
            .build());
            
        buyButton = addRenderableWidget(HextechButton.hextechBuilder(Component.empty(),
            button -> {
                ShopLine selectedOffer = selectedOffer();
                if (selectedOffer != null) {
                    if (deliveryMode) {
                        ModNetwork.CHANNEL.sendToServer(new com.nogeon.economyland.network.BuyShopItemPacket("smith", "smith", selectedOffer.id(), true, 1));
                    } else {
                        ModNetwork.CHANNEL.sendToServer(new SmithActionPacket("buy", selectedSlot, selectedOffer.id(), true));
                    }
                }
            })
            .bounds(leftPos + 238, topPos + 172, 72, 20)
            .build());
        shopTabButton.visible = false;
        shopTabButton.active = false;
        deliveryTabButton.visible = false;
        deliveryTabButton.active = false;
        buyButton.visible = false;
        buyButton.active = false;

        // 애니메이션 토글 단추 및 닫기 단추 세로 스택형 배치
        skipToggleButton = addRenderableWidget(HextechButton.hextechBuilder(
            Component.literal(skipAnimation ? "✦ 연출: SKIP" : "✦ 연출: 보기"), button -> toggleSkipAnimation())
            .bounds(leftPos + 270, topPos + 204, 94, 18)
            .danger(skipAnimation)
            .build());

        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.smith_close"), button -> onClose())
            .bounds(leftPos + 270, topPos + 224, 94, 18)
            .danger(true)
            .build());

        safetyLockButton = addRenderableWidget(HextechButton.hextechBuilder(
            safetyLockMessage(ClientConfig.safetyLock),
            button -> {
                ClientConfig.safetyLock = !ClientConfig.safetyLock;
                ClientConfig.save();
                if (button instanceof HextechButton hb) {
                    hb.setMessage(safetyLockMessage(ClientConfig.safetyLock));
                    hb.danger(!ClientConfig.safetyLock);
                }
                refreshButtons();
            })
            .bounds(leftPos + 206, topPos + 168, 162, 18)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("15강 이상 강화 시 방지권이나 보호제 없이 시도하는 것을 차단합니다.")))
            .build());
        safetyLockButton.danger(!ClientConfig.safetyLock);
            
        refreshButtons();
    }

    private void triggerAction(String action) {
        long now = System.currentTimeMillis();
        if (now - lastActionTime < 300L) {
            return;
        }
        lastActionTime = now;

        if (skipAnimation) {
            this.pendingAction = action;
            sendPendingActionPacket();
            this.pendingAction = "";
        } else {
            if (isAnimating) return;
            this.isAnimating = true;
            this.enhanceAnimationTicks = 0;
            this.pendingAction = action;
            this.animationFlashTicks = 0;
            refreshButtons();
        }
    }

    private void toggleSkipAnimation() {
        skipAnimation = !skipAnimation;
        if (skipToggleButton != null) {
            skipToggleButton.setMessage(Component.literal(skipAnimation ? "✦ 연출: SKIP" : "✦ 연출: 보기"));
            skipToggleButton.danger(skipAnimation);
        }
    }

    private Component safetyLockMessage(boolean enabled) {
        return Component.literal(enabled ? "🛡️ 안전 잠금: 활성화" : "🛡️ 안전 잠금: 비활성화");
    }

    private void sendPendingActionPacket() {
        if (pendingAction.startsWith("scroll_")) {
            int targetLevel = Integer.parseInt(pendingAction.substring(7));
            ModNetwork.CHANNEL.sendToServer(new SmithActionPacket("scroll_" + targetLevel, selectedSlot, "", false, true));
        } else if (pendingAction.equals("enhance")) {
            lastGemTier = keepGemSelection ? selectedGemTier : 0;
            ModNetwork.CHANNEL.sendToServer(new SmithActionPacket("enhance", selectedSlot, selectedGemTier > 0 ? "gem_" + selectedGemTier : "", false, false));
        } else if (pendingAction.equals("repair")) {
            ModNetwork.CHANNEL.sendToServer(new SmithActionPacket("repair", selectedSlot, "", false, false));
        }
    }

    private float randomPlayPitch() {
        if (minecraft != null && minecraft.level != null) {
            return minecraft.level.random.nextFloat() * 0.2F;
        }
        return 0.0F;
    }


    @Override
    protected void containerTick() {
        super.containerTick();
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        if (animationFlashTicks > 0) {
            animationFlashTicks--;
        }

        if (isAnimating) {
            enhanceAnimationTicks++;
            if (enhanceAnimationTicks == 1) {
                minecraft.player.playSound(SoundEvents.BEACON_ACTIVATE, 0.8F, 1.2F);
            } else if (enhanceAnimationTicks == 6 || enhanceAnimationTicks == 12) {
                minecraft.player.playSound(SoundEvents.ANVIL_PLACE, 0.5F, 1.6F + randomPlayPitch());
            } else if (enhanceAnimationTicks == 18) {
                minecraft.player.playSound(SoundEvents.ANVIL_USE, 1.0F, 0.9F);
                minecraft.player.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.3F, 1.4F);
                sendPendingActionPacket();
                animationFlashTicks = 6;
            }
            if (enhanceAnimationTicks >= 24) {
                isAnimating = false;
                enhanceAnimationTicks = 0;
                pendingAction = "";
            }
            refreshButtons();
            return;
        }

        List<ShopLine> lines = activeLines();
        selectedOfferIndex = Mth.clamp(selectedOfferIndex, 0, Math.max(0, lines.size() - 1));
        if (!shopMode && !deliveryMode && !scrollMode && selectedStack().isEmpty()) {
            selectedSlot = fallbackSelectedSlot();
            selectionChanged = true;
        }
        refreshButtons();
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

        if (shopMode || deliveryMode) {
            renderShopBackground(graphics, x, y);
        } else if (scrollMode) {
            renderScrollBackground(graphics, x, y);
        } else {
            renderServiceBackground(graphics, x, y);
        }

        if (animationFlashTicks > 0) {
            int alpha = (int) (animationFlashTicks / 6.0F * 130);
            graphics.fill(x, y, x + imageWidth, y + imageHeight, (alpha << 24) | 0xFFFFFF);
        }
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderSelectedGem(graphics);
        renderDowngradeScrollButton(graphics, mouseX, mouseY);
        if (gemPopupOpen) {
            renderGemPopup(graphics, mouseX, mouseY);
        }
        ItemStack tooltipStack = tooltipStack(mouseX, mouseY);
        if (!tooltipStack.isEmpty()) {
            graphics.renderTooltip(font, tooltipStack, mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 10, 0xFFF2E3BC);
        if (shopMode || deliveryMode) {
            renderShopLabels(graphics);
        } else if (scrollMode) {
            renderScrollLabels(graphics);
        } else {
            renderServiceLabels(graphics);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (gemPopupOpen) {
                if (insideBox(mouseX, mouseY, gemPopupLeft() + 8, gemPopupTop() + gemPopupHeight() - 15, 76, 12)) {
                    keepGemSelection = !keepGemSelection;
                    lastGemTier = keepGemSelection ? selectedGemTier : 0;
                    return true;
                }
                if (insideBox(mouseX, mouseY, gemHelpX(), gemHelpY(), 12, 12)) {
                    return true;
                }
                int tier = gemTierAt(mouseX, mouseY);
                if (tier >= 0) {
                    selectedGemTier = tier;
                    if (keepGemSelection) {
                        lastGemTier = tier;
                    }
                    gemPopupOpen = false;
                    refreshButtons();
                    return true;
                }
                gemPopupOpen = false;
                return true;
            }
            if (shopMode || deliveryMode) {
                int shopRow = shopRowAt(mouseX, mouseY);
                if (shopRow >= 0) {
                    selectedOfferIndex = shopRow;
                    refreshButtons();
                    return true;
                }
                if (insideBox(mouseX, mouseY, leftPos + 178, topPos + SHOP_Y, 12, 136)) {
                    draggingScrollbar = true;
                    updateScrollbar(mouseY);
                    return true;
                }
            } else {
                int inventorySlot = inventorySlotAt(mouseX, mouseY);
                if (inventorySlot >= 0 && minecraft != null && !minecraft.player.getInventory().getItem(inventorySlot).isEmpty()) {
                    selectedSlot = inventorySlot;
                    selectionChanged = selectedSlot != menu.selectedSlot();
                    refreshButtons();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            updateScrollbar(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (shopMode || deliveryMode) {
            scrollBy(delta < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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

    private void drawSmithingAnimation(GuiGraphics graphics, int centerX, int centerY, ItemStack stack) {
        if (enhanceAnimationTicks < 18) {
            float progress = enhanceAnimationTicks / 18.0F;
            int numParticles = 8;
            for (int i = 0; i < numParticles; i++) {
                double angle = (double) i / numParticles * Math.PI * 2 + (enhanceAnimationTicks * 0.25F);
                double radius = 24.0 * (1.0F - progress);
                int px = centerX + (int) (Math.cos(angle) * radius);
                int py = centerY + (int) (Math.sin(angle) * radius);
                graphics.fill(px - 1, py - 1, px + 2, py + 2, 0xFFFF8C00);
            }
            int alpha = (int) (40 + Math.sin(enhanceAnimationTicks * 0.6F) * 35);
            graphics.fill(centerX - 10, centerY - 10, centerX + 10, centerY + 10, (alpha << 24) | 0xFFFFAA00);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, centerX - 8, centerY - 8);
            }
        } else {
            float shockProgress = (enhanceAnimationTicks - 18) / 6.0F;
            int radius = (int) (4.0F + shockProgress * 28.0F);
            int flashAlpha = (int) ((1.0F - shockProgress) * 200);
            drawCustomBorder(graphics, centerX - radius, centerY - radius, radius * 2, radius * 2, (flashAlpha << 24) | 0xFFFFAA00);
            int sparkCount = 12;
            for (int i = 0; i < sparkCount; i++) {
                double angle = (double) i / sparkCount * Math.PI * 2;
                double dist = radius * 0.9D;
                int sx = centerX + (int) (Math.cos(angle) * dist);
                int sy = centerY + (int) (Math.sin(angle) * dist);
                graphics.fill(sx - 1, sy - 1, sx + 1, sy + 1, 0xFFFF3300);
            }
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, centerX - 8, centerY - 8);
                graphics.renderItemDecorations(font, stack, centerX - 8, centerY - 8);
            }
        }
    }

    private void renderServiceBackground(GuiGraphics graphics, int x, int y) {
        framedPanel(graphics, x + 18, y + 52, x + 196, y + 198, 0xFF2A2218, 0xFF0E110F);
        framedPanel(graphics, x + 200, y + 52, x + imageWidth - 18, y + 198, 0xFF2A2218, 0xFF0E110F);
        framedPanel(graphics, x + 18, y + 202, x + imageWidth - 18, y + imageHeight - 18, 0xFF2A2218, 0xFF0E110F);

        framedPanel(graphics, x + PREVIEW_X, y + PREVIEW_Y, x + PREVIEW_X + 48, y + PREVIEW_Y + 48, 0xFFFF8C00, 0xFF0A0C0A);
        drawCyberAccents(graphics, x + PREVIEW_X, y + PREVIEW_Y, 48, 48, 0xFFFFAA00);

        ItemStack stack = selectedStack();
        if (!stack.isEmpty() && !isAnimating) {
            graphics.renderItem(stack, x + PREVIEW_X + 16, y + PREVIEW_Y + 16);
            graphics.renderItemDecorations(font, stack, x + PREVIEW_X + 16, y + PREVIEW_Y + 16);
        }

        if (isAnimating && (pendingAction.equals("enhance") || pendingAction.equals("repair"))) {
            drawSmithingAnimation(graphics, x + PREVIEW_X + 24, y + PREVIEW_Y + 24, stack);
        }

        renderInventoryGrid(graphics);
    }

    private void renderShopBackground(GuiGraphics graphics, int x, int y) {
        framedPanel(graphics, x + 18, y + 52, x + 188, y + imageHeight - 36, 0xFF2A2218, 0xFF0E110F);
        framedPanel(graphics, x + 192, y + 52, x + imageWidth - 18, y + imageHeight - 36, 0xFF2A2218, 0xFF0E110F);
        
        List<ShopLine> lines = activeLines();
        int max = Math.max(0, lines.size() - VISIBLE_SHOP_ROWS);
        shopScroll = Mth.clamp(shopScroll, 0, max);
        lastShopScroll = shopScroll;

        for (int i = 0; i < VISIBLE_SHOP_ROWS; i++) {
            int index = shopScroll + i;
            if (index >= lines.size()) break;
            ShopLine line = lines.get(index);
            int rowY = y + SHOP_Y + i * SHOP_ROW_HEIGHT;
            boolean selected = index == selectedOfferIndex;
            framedPanel(graphics, x + SHOP_X, rowY, x + 176, rowY + 28, selected ? 0xFFFF8C00 : 0xFF2A2218, selected ? 0xFF261D15 : 0xFF0E110F);
            if (selected) {
                drawCyberAccents(graphics, x + SHOP_X, rowY, 152, 28, 0xFFFFAA00);
            }
            graphics.renderItem(line.stack(), x + SHOP_X + 6, rowY + 6);
            graphics.renderItemDecorations(font, line.stack(), x + SHOP_X + 6, rowY + 6);
        }

        renderScrollbar(graphics, x + 178, y + SHOP_Y, 12, 136, lines.size(), VISIBLE_SHOP_ROWS, shopScroll);

        ShopLine selectedOffer = selectedOffer();
        if (selectedOffer != null) {
            framedPanel(graphics, x + 214, y + 76, x + 278, y + 140, 0xFFFF8C00, 0xFF0A0C0A);
            drawCyberAccents(graphics, x + 214, y + 76, 64, 64, 0xFFFFAA00);
            graphics.renderItem(selectedOffer.stack(), x + 238, y + 100);
            graphics.renderItemDecorations(font, selectedOffer.stack(), x + 238, y + 100);
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int x, int y, int width, int height, int total, int visible, int scroll) {
        graphics.fill(x, y, x + width, y + height, 0xFF0A0C0A);
        if (total > visible) {
            int handleHeight = Math.max(12, height * visible / total);
            int handleTop = y + (height - handleHeight) * scroll / (total - visible);
            int scrollColor = draggingScrollbar ? 0xFFFF8C00 : 0xFFFFAA00;
            graphics.fill(x + 2, handleTop, x + width - 2, handleTop + handleHeight, scrollColor);
        }
    }

    private void renderScrollBackground(GuiGraphics graphics, int x, int y) {
        framedPanel(graphics, x + 18, y + 52, x + 196, y + 198, 0xFF2A2218, 0xFF0E110F);
        framedPanel(graphics, x + 200, y + 52, x + imageWidth - 18, y + 198, 0xFF2A2218, 0xFF0E110F);
        framedPanel(graphics, x + 18, y + 202, x + imageWidth - 18, y + imageHeight - 18, 0xFF2A2218, 0xFF0E110F);

        framedPanel(graphics, x + PREVIEW_X, y + PREVIEW_Y, x + PREVIEW_X + 48, y + PREVIEW_Y + 48, 0xFFFF8C00, 0xFF0A0C0A);
        drawCyberAccents(graphics, x + PREVIEW_X, y + PREVIEW_Y, 48, 48, 0xFFFFAA00);
        ItemStack stack = selectedStack();
        if (!stack.isEmpty() && !isAnimating) {
            graphics.renderItem(stack, x + PREVIEW_X + 16, y + PREVIEW_Y + 16);
            graphics.renderItemDecorations(font, stack, x + PREVIEW_X + 16, y + PREVIEW_Y + 16);
        }

        if (isAnimating && pendingAction.startsWith("scroll_")) {
            drawSmithingAnimation(graphics, x + PREVIEW_X + 24, y + PREVIEW_Y + 24, stack);
        }

        renderInventoryGrid(graphics);
    }


    private void renderServiceLabels(GuiGraphics graphics) {
        ItemStack stack = selectedStack();
        int level = SmithingService.level(stack);
        int nextLevel = SmithingService.nextLevel(stack);
        boolean canEnhance = SmithingService.canEnhance(stack) && level < SmithingService.MAX_LEVEL;
        boolean canRepair = SmithingService.canRepair(stack);
        
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_service_subtitle"), 24, 52, 0xFF9FA79A, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_inventory_hint"), 206, 52, 0xFF9FA79A, false);
        graphics.drawString(font, stack.isEmpty() ? Component.translatable("gui.nogeon_economy_land.smith_empty_hand") : SmithingService.displayName(stack), 84, 76, 0xFFE8E1C4, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_current_level", level), 84, 94, 0xFFE8E1C4, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_durability",
            Math.max(0, stack.isEmpty() ? 0 : stack.getMaxDamage() - stack.getDamageValue()),
            stack.isEmpty() ? 0 : stack.getMaxDamage()), 84, 112, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_damage_bonus", Math.round((SmithingService.damageMultiplier(stack) - 1.0F) * 100.0F)), 84, 130, 0xFFD9BF7E, false);
        
        // 겹침 방지: 상태 및 팁을 버튼 위에 우아하게 렌더링 (X=24, Y=188)
        graphics.drawString(font, currentStatus(stack), 24, 188, 0xFFE8E1C4, false);

        // 하단 패널 내부 표 형태 격자 배치 (X=28 / X=196)
        // 1열 X = 28
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_price", CREDIT_FORMAT.format(SmithingService.price(stack))), 28, 210, 0xFFFFD56A, false);
        graphics.drawString(font, Component.translatable(menuForDowngrade(stack)
            ? "gui.nogeon_economy_land.smith_fail_rule_drop"
            : "gui.nogeon_economy_land.smith_fail_rule_hold"), 28, 222, 0xFFD9BF7E, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_reset_rate", SmithingService.resetPercent(stack)), 28, 234,
            SmithingService.resetPercent(stack) > 0 ? 0xFFD47B7B : 0xFF98A49C, false);

        // 2열 X = 146
        if (!canEnhance && !canRepair && !stack.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_invalid_pick_hint"), 146, 210, 0xFF9FA79A, false);
        } else {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_next_level", nextLevel, SmithingService.MAX_LEVEL), 146, 210, 0xFFF4E3B0, false);
            int gemBonus = SmithingService.enhancementGemEffectiveBonus(selectedGemTier, nextLevel);
            int shownChance = selectedGemTier >= 6 ? 100 : Math.min(100, SmithingService.successPercent(stack) + gemBonus);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_success_rate", shownChance), 146, 222, 0xFF8ED79E, false);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_repair_price", canRepair ? CREDIT_FORMAT.format(SmithingService.repairPrice(stack)) : "-"), 146, 234, 0xFFFFD56A, false);
        }
    }

    private void renderShopLabels(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable(deliveryMode ? "gui.nogeon_economy_land.shop_sell_inventory" : "gui.nogeon_economy_land.smith_shop_subtitle"), 24, 52, 0xFF9FA79A, false);
        graphics.drawString(font, Component.translatable(deliveryMode ? "gui.nogeon_economy_land.shop_sell_tab" : "gui.nogeon_economy_land.smith_shop_title"), 24, 62, 0xFFE8E1C4, false);
        
        List<ShopLine> lines = activeLines();
        for (int i = 0; i < VISIBLE_SHOP_ROWS; i++) {
            int index = shopScroll + i;
            if (index >= lines.size()) break;
            ShopLine line = lines.get(index);
            int rowY = SHOP_Y + i * SHOP_ROW_HEIGHT;
            graphics.drawString(font, SmithingService.displayName(line.stack()), SHOP_X + 30, rowY + 4, 0xFFE8E1C4, false);
            graphics.drawString(font, CREDIT_FORMAT.format(line.price()) + " C", SHOP_X + 30, rowY + 16, 0xFFFFD56A, false);
        }
        
        ShopLine selectedOffer = selectedOffer();
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_shop_detail_title"), 214, 62, 0xFFE8E1C4, false);
        if (selectedOffer != null) {
            graphics.drawString(font, SmithingService.displayName(selectedOffer.stack()), 214, 146, 0xFFE8E1C4, false);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_price", CREDIT_FORMAT.format(selectedOffer.price())), 214, 158, 0xFFFFD56A, false);
            graphics.drawString(font, Component.translatable(deliveryMode ? "gui.nogeon_economy_land.shop_sell_inventory" : "gui.nogeon_economy_land.smith_shop_pick_hint"), 214, 194, 0xFF9FA79A, false);
            
            // 상태 텍스트를 버튼들과 겹치지 않게 우측 디테일 박스 상단 빈자리로 정렬 (X=214, Y=172)
            graphics.drawString(font, menu.status(), 214, 180, 0xFF8ED79E, false);
        }
    }

    private void renderScrollLabels(GuiGraphics graphics) {
        ItemStack stack = selectedStack();
        int level = SmithingService.level(stack);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_scroll_subtitle"), 24, 52, 0xFF9FA79A, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_inventory_hint"), 206, 52, 0xFF9FA79A, false);
        graphics.drawString(font, stack.isEmpty() ? Component.translatable("gui.nogeon_economy_land.smith_empty_hand") : SmithingService.displayName(stack), 84, 76, 0xFFE8E1C4, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_current_level", level), 84, 94, 0xFFE8E1C4, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_scroll_pick_hint"), 24, 110, 0xFFD9BF7E, false);
        
        // 겹침 방지: 상태 및 팁을 버튼 위에 렌더링
        graphics.drawString(font, currentStatus(stack), 24, 194, 0xFFE8E1C4, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.smith_scroll_desc"), 24, 210, 0xFF98A49C, false);
    }

    private void renderInventoryGrid(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        graphics.fill(leftPos + INVENTORY_X - 4, topPos + INVENTORY_Y - 4, leftPos + INVENTORY_X + 9 * SLOT_SIZE + 4, topPos + INVENTORY_Y + 3 * SLOT_SIZE + 4, 0xFF0A0C0A);
        graphics.fill(leftPos + INVENTORY_X - 4, topPos + INVENTORY_Y + 58, leftPos + INVENTORY_X + 9 * SLOT_SIZE + 4, topPos + INVENTORY_Y + 62 + SLOT_SIZE, 0xFF0A0C0A);

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

    private void renderSelectedGem(GuiGraphics graphics) {
        if (gemButton == null || !gemButton.visible) {
            return;
        }
        ItemStack gem = SmithingService.enhancementGemStack(selectedGemTier);
        int x = gemButton.getX() + 1;
        int y = gemButton.getY() + 1;
        if (!gem.isEmpty()) {
            graphics.renderItem(gem, x, y);
            graphics.renderItemDecorations(font, gem, x, y);
        } else {
            graphics.drawCenteredString(font, "-", gemButton.getX() + 9, gemButton.getY() + 5, 0xFF9FA79A);
        }
    }

    private void renderDowngradeScrollButton(GuiGraphics graphics, int mouseX, int mouseY) {
        if (downgradeScrollButton == null || !downgradeScrollButton.visible) {
            return;
        }
        ItemStack scroll = new ItemStack(ModItems.LOW_ENHANCEMENT_DOWNGRADE_SCROLL.get());
        int x = downgradeScrollButton.getX() + 1;
        int y = downgradeScrollButton.getY() + 1;
        graphics.renderItem(scroll, x, y);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 120);
        int total = menu.lowDowngradeScrolls() + menu.midDowngradeScrolls() + menu.highDowngradeScrolls() + menu.highestDowngradeScrolls() + menu.resetProtectionScrolls();
        if (total > 0) {
            graphics.drawString(font, String.valueOf(total), x + 10, y + 10, 0xFFFFFFFF, true);
        }
        graphics.pose().popPose();
        if (downgradeScrollButton.isHoveredOrFocused() || insideBox(mouseX, mouseY, downgradeScrollButton.getX(), downgradeScrollButton.getY(), 18, 18)) {
            renderDowngradeScrollPopup(graphics, mouseX, mouseY);
        }
    }

    private void renderDowngradeScrollPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 280);
        int x = downgradeScrollButton.getX() + 22;
        int y = downgradeScrollButton.getY() - 40;
        int width = 138;
        int height = 96;
        graphics.fill(x, y, x + width, y + height, 0xFF0A0C0A);
        graphics.fill(x, y, x + width, y + 1, 0xFFFFAA00);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFAA00);
        graphics.fill(x, y, x + 1, y + height, 0xFFFFAA00);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFAA00);
        graphics.drawString(font, Component.literal("방지권 등록 수"), x + 8, y + 6, 0xFFE8E1C4, false);
        renderDowngradeScrollLine(graphics, x + 8, y + 22, new ItemStack(ModItems.LOW_ENHANCEMENT_DOWNGRADE_SCROLL.get()), "+6~+10", menu.lowDowngradeScrolls(), 0xFF79A7FF);
        renderDowngradeScrollLine(graphics, x + 8, y + 36, new ItemStack(ModItems.MID_ENHANCEMENT_DOWNGRADE_SCROLL.get()), "+11~+15", menu.midDowngradeScrolls(), 0xFF79A7FF);
        renderDowngradeScrollLine(graphics, x + 8, y + 50, new ItemStack(ModItems.HIGH_ENHANCEMENT_DOWNGRADE_SCROLL.get()), "+16~+17", menu.highDowngradeScrolls(), 0xFFFFB347);
        renderDowngradeScrollLine(graphics, x + 8, y + 64, new ItemStack(ModItems.HIGHEST_ENHANCEMENT_DOWNGRADE_SCROLL.get()), "+18~+20", menu.highestDowngradeScrolls(), 0xFFFFD36A);
        renderDowngradeScrollLine(graphics, x + 8, y + 78, new ItemStack(ModItems.ENHANCEMENT_RESET_PROTECTION_SCROLL.get()), "초기화방지", menu.resetProtectionScrolls(), 0xFF8ED79E);
        graphics.pose().popPose();
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            List<Component> help = new ArrayList<>();
            help.add(Component.literal("우클릭으로 등록한 방지권 현황입니다."));
            help.add(Component.literal("+1~+6 실패는 하락하지 않습니다."));
            help.add(Component.literal("하락 방지권은 초기화를 막지 못합니다."));
            help.add(Component.literal("초기화 방지권은 0강화 리셋을 1회 면제합니다."));
            graphics.renderComponentTooltip(font, help, mouseX, mouseY);
        }
    }

    private void renderDowngradeScrollLine(GuiGraphics graphics, int x, int y, ItemStack icon, String range, int count, int color) {
        graphics.renderItem(icon, x, y - 4);
        graphics.drawString(font, Component.literal(range), x + 18, y, color, false);
        graphics.drawString(font, Component.literal("x " + count), x + 72, y, count > 0 ? 0xFFFFFFFF : 0xFF98A49C, false);
    }

    private void renderGemPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 260);
        int x = gemPopupLeft();
        int y = gemPopupTop();
        int width = gemPopupWidth();
        int height = gemPopupHeight();
        graphics.fill(x, y, x + width, y + height, 0xFF0A0C0A);
        graphics.fill(x, y, x + width, y + 1, 0xFFFFAA00);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFAA00);
        graphics.fill(x, y, x + 1, y + height, 0xFFFFAA00);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFAA00);
        graphics.drawString(font, Component.literal("\uac15\ud654\uc758 \ubcf4\uc11d"), x + 6, y + 5, 0xFFE8E1C4, false);
        graphics.fill(gemHelpX(), gemHelpY(), gemHelpX() + 12, gemHelpY() + 12, 0xFF2A2218);
        graphics.fill(gemHelpX() + 1, gemHelpY() + 1, gemHelpX() + 11, gemHelpY() + 11, 0xFF10140F);
        graphics.drawCenteredString(font, "?", gemHelpX() + 6, gemHelpY() + 2, 0xFFFFD36A);
        for (int tier = 0; tier <= 6; tier++) {
            int slotX = gemPopupSlotX(tier);
            int slotY = gemPopupSlotY(tier);
            boolean selected = tier == selectedGemTier;
            graphics.fill(slotX, slotY, slotX + GEM_POPUP_SLOT_SIZE, slotY + GEM_POPUP_SLOT_SIZE, selected ? 0xFFFFAA00 : 0xFF2A2218);
            graphics.fill(slotX + 1, slotY + 1, slotX + GEM_POPUP_SLOT_SIZE - 1, slotY + GEM_POPUP_SLOT_SIZE - 1, 0xFF10140F);
            if (tier == 0) {
                graphics.drawCenteredString(font, "-", slotX + GEM_POPUP_SLOT_SIZE / 2, slotY + 7, 0xFFB7C7B8);
                continue;
            }
            ItemStack gem = SmithingService.enhancementGemStack(tier);
            boolean hasGem = minecraft != null && minecraft.player != null && SmithingService.countEnhancementGem(minecraft.player, tier) > 0;
            graphics.renderItem(gem, slotX + 3, slotY + 3);
            if (!hasGem) {
                graphics.fill(slotX + 1, slotY + 1, slotX + GEM_POPUP_SLOT_SIZE - 1, slotY + GEM_POPUP_SLOT_SIZE - 1, 0xB0000000);
            }
            int count = minecraft != null && minecraft.player != null ? SmithingService.countEnhancementGem(minecraft.player, tier) : 0;
            if (count > 0) {
                graphics.pose().pushPose();
                graphics.pose().translate(0, 0, 120);
                graphics.drawString(font, String.valueOf(count), slotX + 13, slotY + 13, 0xFFFFFFFF, true);
                graphics.pose().popPose();
            }
        }
        int checkX = x + 8;
        int checkY = y + height - 15;
        graphics.fill(checkX, checkY, checkX + 10, checkY + 10, keepGemSelection ? 0xFFFFAA00 : 0xFF2A2218);
        graphics.fill(checkX + 1, checkY + 1, checkX + 9, checkY + 9, 0xFF10140F);
        if (keepGemSelection) {
            graphics.drawString(font, "v", checkX + 2, checkY, 0xFFFFD36A, false);
        }
        graphics.drawString(font, Component.literal("\uc5f0\uc18d \uc0ac\uc6a9"), checkX + 14, checkY + 1, 0xFFC9D7C6, false);
        if (insideBox(mouseX, mouseY, gemHelpX(), gemHelpY(), 12, 12)) {
            List<Component> help = new ArrayList<>();
            help.add(Component.literal("보석 기본 보너스는 단계가 높아질수록 효율이 감소합니다."));
            help.add(Component.literal("+1~5 100%, +6~10 80%, +11~15 60%"));
            help.add(Component.literal("+16~17 40%, +18~20 25%"));
            help.add(Component.literal("완벽한 강화의 보석은 효율 감소 없이 반드시 성공합니다."));
            graphics.renderComponentTooltip(font, help, mouseX, mouseY);
        }
        graphics.pose().popPose();
    }

    private int gemTierAt(double mouseX, double mouseY) {
        for (int tier = 0; tier <= 6; tier++) {
            int slotX = gemPopupSlotX(tier);
            int slotY = gemPopupSlotY(tier);
            if (insideBox(mouseX, mouseY, slotX, slotY, GEM_POPUP_SLOT_SIZE, GEM_POPUP_SLOT_SIZE)) {
                return tier;
            }
        }
        return -1;
    }

    private int gemPopupLeft() {
        return leftPos + 208;
    }

    private int gemPopupTop() {
        return topPos + 120;
    }

    private int gemPopupWidth() {
        return 6 + GEM_POPUP_COLUMNS * GEM_POPUP_SLOT_SIZE + (GEM_POPUP_COLUMNS - 1) * GEM_POPUP_SLOT_GAP + 6;
    }

    private ItemStack selectedStack() {
        if (minecraft == null || minecraft.player == null || selectedSlot < 0 || selectedSlot >= minecraft.player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return minecraft.player.getInventory().getItem(selectedSlot);
    }

    private Component currentStatus(ItemStack stack) {
        int level = SmithingService.level(stack);
        if (SmithingService.canEnhance(stack) && level < SmithingService.MAX_LEVEL) {
            int targetLevel = level + 1;
            if (ClientConfig.safetyLock && level >= 15 && selectedGemTier != 6) {
                boolean hasScroll = targetLevel <= 17 ? menu.highDowngradeScrolls() > 0 : menu.highestDowngradeScrolls() > 0;
                if (!hasScroll) {
                    return Component.literal("§c[안전 잠금] 하락 방지권이 필요합니다.");
                }
            }
        }
        return selectionChanged ? SmithingService.defaultStatus(stack) : menu.status();
    }

    private int fallbackSelectedSlot() {
        if (minecraft == null || minecraft.player == null) {
            return -1;
        }
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            if (SmithingService.canSmith(minecraft.player.getInventory().getItem(slot))) {
                return slot;
            }
        }
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            if (!minecraft.player.getInventory().getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private int gemPopupHeight() {
        return 18 + 2 * GEM_POPUP_SLOT_SIZE + GEM_POPUP_SLOT_GAP + 22;
    }

    private int gemHelpX() {
        return gemPopupLeft() + gemPopupWidth() - 18;
    }

    private int gemHelpY() {
        return gemPopupTop() + 4;
    }

    private int gemPopupSlotX(int tier) {
        return gemPopupLeft() + 6 + (tier % GEM_POPUP_COLUMNS) * (GEM_POPUP_SLOT_SIZE + GEM_POPUP_SLOT_GAP);
    }

    private int gemPopupSlotY(int tier) {
        return gemPopupTop() + 18 + (tier / GEM_POPUP_COLUMNS) * (GEM_POPUP_SLOT_SIZE + GEM_POPUP_SLOT_GAP);
    }

    private boolean menuForDowngrade(ItemStack stack) {
        return SmithingService.canDowngrade(stack);
    }

    private void refreshButtons() {
        ItemStack stack = selectedStack();
        int level = SmithingService.level(stack);
        
        boolean setupActive = !isAnimating;
        
        // Tab buttons
        serviceTabButton.active = setupActive && (shopMode || scrollMode || deliveryMode);
        shopTabButton.visible = false;
        shopTabButton.active = false;
        deliveryTabButton.visible = false;
        deliveryTabButton.active = false;
        
        // Service mode widgets
        boolean isService = !shopMode && !scrollMode && !deliveryMode;
        enhanceButton.visible = isService;
        repairButton.visible = isService;
        gemButton.visible = isService;
        downgradeScrollButton.visible = isService;
        
        boolean safetyLocked = false;
        if (setupActive && isService && SmithingService.canEnhance(stack) && level < SmithingService.MAX_LEVEL) {
            int targetLevel = level + 1;
            if (ClientConfig.safetyLock && level >= 15 && selectedGemTier != 6) {
                boolean hasScroll = targetLevel <= 17 ? menu.highDowngradeScrolls() > 0 : menu.highestDowngradeScrolls() > 0;
                if (!hasScroll) {
                    safetyLocked = true;
                }
            }
        }
        enhanceButton.active = setupActive && isService && SmithingService.canEnhance(stack) && level < SmithingService.MAX_LEVEL && !safetyLocked;
        repairButton.active = setupActive && isService && SmithingService.canRepair(stack);
        gemButton.active = setupActive && isService;
        downgradeScrollButton.active = setupActive && isService;
        
        if (safetyLockButton != null) {
            safetyLockButton.visible = isService;
            safetyLockButton.active = setupActive && isService;
        }
        
        // Shop/Delivery mode widgets
        buyButton.visible = shopMode || deliveryMode;
        ShopLine offer = selectedOffer();
        buyButton.active = setupActive && (shopMode || deliveryMode) && offer != null;
        buyButton.setMessage(Component.translatable(deliveryMode ? "gui.nogeon_economy_land.sell" : "gui.nogeon_economy_land.buy"));
        
        // 강화권 모드 위젯 비활성화 (smith_service_tab 리뉴얼 대응)
        
        if (skipToggleButton != null) {
            skipToggleButton.active = setupActive;
        }
    }


    private ItemStack tooltipStack(int mouseX, int mouseY) {
        if (gemPopupOpen) {
            int tier = gemTierAt(mouseX, mouseY);
            if (tier > 0) {
                return SmithingService.enhancementGemStack(tier);
            }
        }
        if (!shopMode && !deliveryMode && insideBox(mouseX, mouseY, leftPos + PREVIEW_X + 16, topPos + PREVIEW_Y + 16, 16, 16)) {
            return selectedStack();
        }
        if (!shopMode && !deliveryMode) {
            int inventorySlot = inventorySlotAt(mouseX, mouseY);
            if (inventorySlot >= 0 && minecraft != null) {
                return minecraft.player.getInventory().getItem(inventorySlot);
            }
        }
        List<ShopLine> lines = activeLines();
        for (int i = 0; i < Math.min(VISIBLE_SHOP_ROWS, lines.size() - shopScroll); i++) {
            int index = shopScroll + i;
            int iconX = leftPos + SHOP_X + 6;
            int iconY = topPos + SHOP_Y + i * SHOP_ROW_HEIGHT + 6;
            if (insideBox(mouseX, mouseY, iconX, iconY, 16, 16)) {
                return lines.get(index).stack();
            }
        }
        if ((shopMode || deliveryMode) && insideBox(mouseX, mouseY, leftPos + 238, topPos + 100, 16, 16)) {
            ShopLine selectedOffer = selectedOffer();
            return selectedOffer == null ? ItemStack.EMPTY : selectedOffer.stack();
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

    private void setMode(boolean scrollMode, boolean shopMode, boolean deliveryMode) {
        this.scrollMode = scrollMode;
        this.shopMode = shopMode;
        this.deliveryMode = deliveryMode;
        lastScrollMode = scrollMode;
        lastShopMode = shopMode;
        lastDeliveryMode = deliveryMode;
        selectedOfferIndex = 0;
        shopScroll = 0;
        refreshButtons();
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

    private List<ShopLine> activeLines() {
        return deliveryMode ? menu.deliveryLines() : menu.shopLines();
    }

    private ShopLine selectedOffer() {
        List<ShopLine> lines = activeLines();
        if (lines.isEmpty()) {
            return null;
        }
        selectedOfferIndex = Mth.clamp(selectedOfferIndex, 0, lines.size() - 1);
        return lines.get(selectedOfferIndex);
    }

    private int shopRowAt(double mouseX, double mouseY) {
        List<ShopLine> lines = activeLines();
        for (int i = 0; i < Math.min(VISIBLE_SHOP_ROWS, lines.size() - shopScroll); i++) {
            int rowX = leftPos + SHOP_X;
            int rowY = topPos + SHOP_Y + i * SHOP_ROW_HEIGHT;
            if (insideBox(mouseX, mouseY, rowX, rowY, 152, 28)) {
                return shopScroll + i;
            }
        }
        return -1;
    }

    private void scrollBy(int amount) {
        List<ShopLine> lines = activeLines();
        int max = Math.max(0, lines.size() - VISIBLE_SHOP_ROWS);
        shopScroll = Mth.clamp(shopScroll + amount, 0, max);
        lastShopScroll = shopScroll;
        refreshButtons();
    }

    private void updateScrollbar(double mouseY) {
        List<ShopLine> lines = activeLines();
        int max = Math.max(0, lines.size() - VISIBLE_SHOP_ROWS);
        if (max <= 0) {
            shopScroll = 0;
            lastShopScroll = 0;
            refreshButtons();
            return;
        }
        float progress = (float) (mouseY - (topPos + SHOP_Y)) / 136.0F;
        shopScroll = Math.round(Mth.clamp(progress, 0.0F, 1.0F) * max);
        lastShopScroll = shopScroll;
        refreshButtons();
    }
}
