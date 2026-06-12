package com.nogeon.economyland.client;

import com.nogeon.economyland.entity.ScrapDroneEntity;
import com.nogeon.economyland.item.SmithingService;
import com.nogeon.economyland.menu.DeconstructMenu;
import com.nogeon.economyland.network.DeconstructActionPacket;
import com.nogeon.economyland.network.ModNetwork;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class DeconstructScreen extends AbstractContainerScreen<DeconstructMenu> {
    private static final int PANEL_LEFT_X = 16;
    private static final int PANEL_TOP_Y = 66;
    private static final int PANEL_LEFT_W = 220;
    private static final int PANEL_RIGHT_X = 248;
    private static final int PANEL_RIGHT_W = 170;
    private static final int PANEL_H = 220;
    private static final int STATUS_X = 16;
    private static final int STATUS_Y = 292;
    private static final int STATUS_W = 324;
    private static final int STATUS_H = 24;
    private static final int PREVIEW_BOX_X = PANEL_LEFT_X + 16;
    private static final int PREVIEW_BOX_Y = PANEL_TOP_Y + 20;
    private static final int PREVIEW_BOX_SIZE = 50;
    private static final int SLOT_SIZE = 18;
    private static final int INVENTORY_X = PANEL_RIGHT_X + 4;
    private static final int INVENTORY_Y = PANEL_TOP_Y + 30;
    private static final int MODULE_CARD_X = PANEL_LEFT_X + 8;
    private static final int MODULE_CARD_W = PANEL_LEFT_W - 16;
    private static final int MODULE_CARD_H = 38;
    private static final int CARD_GAP = 4;
    private static final int NAME_CARD_H = 46;
    private static final int STAT_CARD_H = 48;
    private static final int BG = 0xFB0B1012;
    private static final int PANEL_BG = 0xFF0F171A;
    private static final int CARD_BG = 0xFF121D21;
    private static final int CARD_ACTIVE = 0xFF14322D;
    private static final int PANEL_BORDER = 0xFF24353A;
    private static final int ACCENT = 0xFF19E6D3;
    private static final int ACCENT_2 = 0xFF0BA6D6;
    private static final int TEXT = 0xFFE6F4F1;
    private static final int MUTED = 0xFF87A8A1;
    private static final int WARN = 0xFFFFC54D;
    private static final int DANGER = 0xFFFF6767;
    private static final int GOOD = 0xFF5EF2A2;

    private HextechButton tabDeconstructButton;
    private HextechButton tabGeneratorButton;
    private HextechButton tabUpgradeButton;
    private HextechButton tabStatButton;
    private HextechButton primaryActionButton;
    private HextechButton autoRegisterButton;
    private HextechButton autoClearButton;
    private HextechButton repairButton;
    private HextechButton buyInvButton;
    private HextechButton openStorageButton;
    private HextechButton buyTransButton;
    private HextechButton buyBoostButton;
    private HextechButton buySensorButton;
    private HextechButton buyGrabberButton;
    private HextechButton applyNameButton;
    private HextechButton upgradeAttackButton;
    private HextechButton upgradeHealthButton;
    private HextechButton upgradeRangeButton;
    private HextechButton magnetToggleButton;
    private HextechButton helpButton;
    private HextechButton closeButton;
    private EditBox nameEditBox;

    private int selectedSlot;
    private boolean selectionChanged;
    private int currentTab;
    private ItemStack hoveredCostStack = ItemStack.EMPTY;
    private List<Component> hoveredCostTooltip = Collections.emptyList();

    public DeconstructScreen(DeconstructMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 434;
        this.imageHeight = 322;
        this.inventoryLabelY = 10_000;
        this.titleLabelY = 10_000;
        this.selectedSlot = menu.selectedSlot();
        this.selectionChanged = false;
        this.currentTab = Math.max(0, Math.min(3, menu.currentTab()));
    }

    @Override
    protected void init() {
        super.init();
        tabDeconstructButton = addRenderableWidget(hexButton(Component.literal("분해"), button -> switchTab(0), 0, 0, 52, 18));
        tabGeneratorButton = addRenderableWidget(hexButton(Component.literal("발전"), button -> switchTab(1), 0, 0, 52, 18));
        tabUpgradeButton = addRenderableWidget(hexButton(Component.literal(menu.isDroneBroken() ? "수리" : "기능"), button -> switchTab(2), 0, 0, 52, 18));
        tabStatButton = addRenderableWidget(hexButton(Component.literal("스탯"), button -> switchTab(3), 0, 0, 52, 18));
        primaryActionButton = addRenderableWidget(hexButton(Component.literal("실행"), button -> sendAction(currentTab), 0, 0, 84, 18));
        autoRegisterButton = addRenderableWidget(hexButton(Component.literal("자동 등록"), button -> sendAction(2), 0, 0, 68, 18));
        autoClearButton = addRenderableWidget(hexButton(Component.literal("해제"), button -> sendAction(3), 0, 0, 36, 18));
        repairButton = addRenderableWidget(hexButtonBuilder(Component.literal("긴급 복구"), button -> sendAction(4), 0, 0, 84, 18).danger(true).build());
        buyInvButton = addRenderableWidget(hexButton(Component.literal("해금"), button -> sendAction(5), 0, 0, 44, 16));
        openStorageButton = addRenderableWidget(hexButton(Component.literal("열기"), button -> sendAction(17), 0, 0, 44, 16));
        buyTransButton = addRenderableWidget(hexButton(Component.literal("해금"), button -> sendAction(6), 0, 0, 44, 16));
        buyBoostButton = addRenderableWidget(hexButton(Component.literal("해금"), button -> sendAction(7), 0, 0, 44, 16));
        buySensorButton = addRenderableWidget(hexButton(Component.literal("해금"), button -> sendAction(15), 0, 0, 44, 16));
        buyGrabberButton = addRenderableWidget(hexButton(Component.literal("해금"), button -> sendAction(16), 0, 0, 44, 16));
        applyNameButton = addRenderableWidget(hexButton(Component.literal("적용"), button -> sendAction(11, nameEditBox.getValue()), 0, 0, 44, 16));
        upgradeAttackButton = addRenderableWidget(hexButton(Component.literal("강화"), button -> sendAction(12), 0, 0, 44, 16));
        upgradeHealthButton = addRenderableWidget(hexButton(Component.literal("강화"), button -> sendAction(13), 0, 0, 44, 16));
        upgradeRangeButton = addRenderableWidget(hexButton(Component.literal("강화"), button -> sendAction(14), 0, 0, 44, 16));
        magnetToggleButton = addRenderableWidget(hexButton(Component.literal("자석 ON"), button -> sendAction(18), 0, 0, 44, 16));
        helpButton = addRenderableWidget(hexButton(Component.literal("?"), button -> { }, 0, 0, 16, 16));
        closeButton = addRenderableWidget(hexButton(Component.literal("닫기"), button -> onClose(), 0, 0, 72, 18));

        nameEditBox = new EditBox(this.font, 0, 0, 120, 16, Component.literal("드론 이름"));
        nameEditBox.setMaxLength(14);
        nameEditBox.setValue(menu.droneName());
        addRenderableWidget(nameEditBox);
        refreshButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (nameEditBox != null) {
            nameEditBox.tick();
        }
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        if ((currentTab == 0 || currentTab == 1) && selectedStack().isEmpty()) {
            selectedSlot = fallbackSelectedSlot();
            selectionChanged = true;
        }
        refreshButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        hoveredCostStack = ItemStack.EMPTY;
        hoveredCostTooltip = Collections.emptyList();
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, BG);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFF10171A);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, 0xFF0D1316);
        drawBorder(graphics, x, y, imageWidth, imageHeight, ACCENT);
        graphics.fill(x + 12, y + 34, x + imageWidth - 12, y + 35, 0x5528C6D9);

        Component displayTitle = menu.isSmithyMode() ? Component.literal("대장간 장비 분해") : title;
        Component displayDesc = menu.isSmithyMode() 
            ? Component.literal("장비를 분해하여 유용한 자재와 재련 재료를 추출합니다.")
            : Component.literal("드론 운영, 업그레이드, 동력 제어를 한 화면에서 관리합니다.");

        graphics.drawString(font, displayTitle, x + 18, y + 14, ACCENT, false);
        graphics.drawString(font, displayDesc, x + 18, y + 27, MUTED, false);

        if (!menu.isSmithyMode()) {
            drawActiveTabFrame(graphics, activeTabButton());
        }
        drawPanel(graphics, x + PANEL_LEFT_X, y + PANEL_TOP_Y, PANEL_LEFT_W, PANEL_H);
        drawPanel(graphics, x + PANEL_RIGHT_X, y + PANEL_TOP_Y, PANEL_RIGHT_W, PANEL_H);
        drawPanel(graphics, x + STATUS_X, y + STATUS_Y, STATUS_W, STATUS_H);

        if (menu.isDroneBroken() && (currentTab == 2 || currentTab == 3)) {
            renderBrokenTab(graphics, mouseX, mouseY);
        } else if (currentTab == 0) {
            renderDeconstructTab(graphics);
        } else if (currentTab == 1) {
            renderFuelTab(graphics);
        } else if (currentTab == 2) {
            renderModulesTab(graphics, mouseX, mouseY);
        } else {
            renderStatsTab(graphics, mouseX, mouseY);
        }

        renderInventoryPanel(graphics);
        if (menu.isSmithyMode()) {
            renderBlacksmithPanel(graphics);
        } else {
            renderTelemetryPanel(graphics);
        }
        renderStatusPanel(graphics, currentStatus(selectedStack()));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        if (helpButton != null && insideAbsolute(mouseX, mouseY, helpButton.getX(), helpButton.getY(), helpButton.getWidth(), helpButton.getHeight())) {
            graphics.renderComponentTooltip(font, helpLines(), mouseX, mouseY);
            return;
        }

        ItemStack tooltipStack = tooltipStack(mouseX, mouseY);
        if (!tooltipStack.isEmpty()) {
            graphics.renderTooltip(font, tooltipStack, mouseX, mouseY);
        } else if (!hoveredCostStack.isEmpty()) {
            graphics.renderComponentTooltip(font, hoveredCostTooltip, mouseX, mouseY);
        }
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
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameEditBox != null && nameEditBox.isVisible() && nameEditBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                sendAction(11, nameEditBox.getValue());
                return true;
            }
            if (nameEditBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (nameEditBox != null && nameEditBox.isVisible() && nameEditBox.isFocused() && nameEditBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void renderDeconstructTab(GuiGraphics graphics) {
        int cardX = leftPos + MODULE_CARD_X;
        int top = leftPos - leftPos + topPos + PANEL_TOP_Y + 8;
        drawCard(graphics, cardX, top, MODULE_CARD_W, 88, ACCENT);
        drawPreviewBox(graphics, leftPos + PREVIEW_BOX_X, topPos + PREVIEW_BOX_Y, selectedStack());

        ItemStack stack = selectedStack();
        graphics.drawString(font, Component.literal("선택 장비"), cardX + 68, top + 10, TEXT, false);
        if (stack.isEmpty()) {
            graphics.drawString(font, Component.literal("오른쪽 작업 인벤토리에서 장비를 선택하세요."), cardX + 68, top + 24, MUTED, false);
            graphics.drawString(font, Component.literal("청록색 테두리는 현재 선택 장비입니다."), cardX + 68, top + 36, MUTED, false);
        } else {
            graphics.drawString(font, Component.literal(ellipsize(stack.getHoverName().getString(), 124)), cardX + 68, top + 24, TEXT, false);
            boolean canDeconstruct = SmithingService.canDeconstruct(stack);
            graphics.drawString(font, Component.literal(canDeconstruct ? "분해 가능 장비" : "분해 불가 아이템"), cardX + 68, top + 36, canDeconstruct ? GOOD : DANGER, false);
            graphics.drawString(font, Component.literal(canDeconstruct ? "하단 버튼으로 즉시 분해합니다." : "재료 환원 가능한 장비만 처리됩니다."), cardX + 68, top + 48, MUTED, false);
        }

        int infoY = top + 96;
        drawCard(graphics, cardX, infoY, MODULE_CARD_W, 74, ACCENT_2);
        graphics.drawString(font, Component.literal("분해 워크플로우"), cardX + 12, infoY + 10, TEXT, false);
        graphics.drawString(font, Component.literal("1. 장비 선택  2. 상태 확인  3. 분해 실행"), cardX + 12, infoY + 26, MUTED, false);
        graphics.drawString(font, Component.literal("탭을 이동해도 선택 슬롯은 유지됩니다."), cardX + 12, infoY + 42, WARN, false);
    }

    private void renderFuelTab(GuiGraphics graphics) {
        int cardX = leftPos + MODULE_CARD_X;
        int top = topPos + PANEL_TOP_Y + 8;
        drawCard(graphics, cardX, top, MODULE_CARD_W, 88, ACCENT);
        drawPreviewBox(graphics, leftPos + PREVIEW_BOX_X, topPos + PREVIEW_BOX_Y, selectedStack());

        ItemStack stack = selectedStack();
        graphics.drawString(font, Component.literal("동력 재보급"), cardX + 68, top + 10, TEXT, false);
        if (stack.isEmpty()) {
            graphics.drawString(font, Component.literal("오른쪽 인벤토리에서 연료를 선택하세요."), cardX + 68, top + 24, MUTED, false);
            graphics.drawString(font, Component.literal("선택 후 생산 또는 자동 등록을 누르면 됩니다."), cardX + 68, top + 36, MUTED, false);
        } else {
            double charge = fuelValue(selectedStack());
            graphics.drawString(font, Component.literal(ellipsize(stack.getHoverName().getString(), 124)), cardX + 68, top + 24, TEXT, false);
            graphics.drawString(font, Component.literal("예상 충전량  +" + charge + "%"), cardX + 68, top + 36, GOOD, false);
            graphics.drawString(font, Component.literal("자동 연료 등록 시 드론이 스스로 사용합니다."), cardX + 68, top + 48, MUTED, false);
        }

        int controlY = top + 96;
        drawCard(graphics, cardX, controlY, MODULE_CARD_W, 74, ACCENT_2);
        graphics.drawString(font, Component.literal("연료 자동화"), cardX + 12, controlY + 10, TEXT, false);
        ItemStack autoFuel = resolveItemStack(menu.autoFuelItem());
        if (autoFuel.isEmpty()) {
            graphics.drawString(font, Component.literal("현재 등록된 자동 연료가 없습니다."), cardX + 12, controlY + 24, MUTED, false);
        } else {
            graphics.renderItem(autoFuel, cardX + 12, controlY + 20);
            graphics.drawString(font, Component.literal("등록 연료: " + ellipsize(autoFuel.getHoverName().getString(), 116)), cardX + 34, controlY + 24, TEXT, false);
        }
        graphics.drawString(font, Component.literal("선택한 연료로 생산, 등록, 해제를 처리합니다."), cardX + 12, controlY + 40, MUTED, false);
        graphics.drawString(font, Component.literal("자동 연료는 탑승 중에도 유지됩니다."), cardX + 12, controlY + 52, WARN, false);
    }

    private void renderBrokenTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int cardX = leftPos + MODULE_CARD_X;
        int top = topPos + PANEL_TOP_Y + 8;
        drawCard(graphics, cardX, top, MODULE_CARD_W, 232, DANGER);
        graphics.drawString(font, Component.literal("긴급 수리 프로토콜"), cardX + 12, top + 10, DANGER, false);
        graphics.drawString(font, Component.literal("드론이 파손되어 모듈 조작과 스탯 강화를 진행할 수 없습니다."), cardX + 12, top + 26, TEXT, false);
        graphics.drawString(font, Component.literal("필요 자재를 준비한 뒤 아래 복구 버튼을 눌러 기체를 되살리세요."), cardX + 12, top + 38, MUTED, false);

        List<CostEntry> repairCosts = repairCosts();
        graphics.drawString(font, Component.literal("복구 자재"), cardX + 12, top + 64, TEXT, false);
        renderCostIcons(graphics, cardX + 12, top + 78, repairCosts, mouseX, mouseY);
        graphics.drawString(font, Component.literal(ellipsize(joinCostSummary(repairCosts), 188)), cardX + 12, top + 124, MUTED, false);

        int lineY = top + 144;
        for (CostEntry cost : repairCosts) {
            int owned = countPlayerItem(cost.item);
            int color = owned >= cost.required ? GOOD : DANGER;
            graphics.drawString(font, Component.literal(itemName(cost.item) + "  " + owned + "/" + cost.required), cardX + 12, lineY, color, false);
            lineY += 12;
        }
    }

    private void renderModulesTab(GuiGraphics graphics, int mouseX, int mouseY) {
        renderModuleCard(graphics, mouseX, mouseY, 0, 5, menu.inventoryUpgradeLevel(), menu.inventoryUpgradeLevel() >= 5, moduleHeader(5, menu.inventoryUpgradeLevel()), moduleStatus(5, menu.inventoryUpgradeLevel()), moduleCosts(5, menu.inventoryUpgradeLevel()));
        renderModuleCard(graphics, mouseX, mouseY, 1, 6, menu.transmitterUpgradeLevel(), menu.transmitterUpgradeLevel() >= 5, moduleHeader(6, menu.transmitterUpgradeLevel()), moduleStatus(6, menu.transmitterUpgradeLevel()), moduleCosts(6, menu.transmitterUpgradeLevel()));
        renderModuleCard(graphics, mouseX, mouseY, 2, 7, menu.boosterUpgradeLevel(), menu.boosterUpgradeLevel() >= 5, moduleHeader(7, menu.boosterUpgradeLevel()), moduleStatus(7, menu.boosterUpgradeLevel()), moduleCosts(7, menu.boosterUpgradeLevel()));
        renderModuleCard(graphics, mouseX, mouseY, 3, 15, menu.sensorUpgradeLevel(), menu.sensorUpgradeLevel() >= 5, moduleHeader(15, menu.sensorUpgradeLevel()), moduleStatus(15, menu.sensorUpgradeLevel()), moduleCosts(15, menu.sensorUpgradeLevel()));
        renderModuleCard(graphics, mouseX, mouseY, 4, 16, menu.grabberUpgradeLevel(), menu.grabberUpgradeLevel() >= 5, moduleHeader(16, menu.grabberUpgradeLevel()), moduleStatus(16, menu.grabberUpgradeLevel()), moduleCosts(16, menu.grabberUpgradeLevel()));
    }

    private void renderStatsTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int nameX = leftPos + MODULE_CARD_X;
        int nameY = topPos + PANEL_TOP_Y + 8;
        drawCard(graphics, nameX, nameY, MODULE_CARD_W, NAME_CARD_H, ACCENT);
        graphics.drawString(font, Component.literal("드론 명칭 개조"), nameX + 12, nameY + 10, TEXT, false);
        graphics.drawString(font, Component.literal("현재 이름: " + ellipsize(menu.droneName(), 130)), nameX + 12, nameY + 24, MUTED, false);
        graphics.drawString(font, Component.literal("최대 14자 입력"), nameX + 12, nameY + 36, MUTED, false);

        renderStatCard(graphics, mouseX, mouseY, 0, 12, menu.statAttack(), "공격 스탯", "지원 사격 화력 상승", statCosts(12, menu.statAttack()));
        renderStatCard(graphics, mouseX, mouseY, 1, 13, menu.statHealth(), "체력 스탯", "드론 내구도 증가", statCosts(13, menu.statHealth()));
        renderStatCard(graphics, mouseX, mouseY, 2, 14, menu.statRange(), "자력 스탯", "아이템 흡입 범위 증가", statCosts(14, menu.statRange()));
    }

    private void renderModuleCard(GuiGraphics graphics, int mouseX, int mouseY, int index, int actionType, int level, boolean maxed, String titleText, String detailText, List<CostEntry> costs) {
        int x = leftPos + MODULE_CARD_X;
        int y = topPos + PANEL_TOP_Y + 8 + index * (MODULE_CARD_H + CARD_GAP);
        drawCard(graphics, x, y, MODULE_CARD_W, MODULE_CARD_H, level > 0 ? ACCENT : PANEL_BORDER);
        graphics.drawString(font, Component.literal(titleText), x + 10, y + 6, level > 0 ? ACCENT : TEXT, false);
        if (maxed) {
            graphics.drawString(font, Component.literal("최대 단계"), x + 10, y + 20, WARN, false);
        } else if (!costs.isEmpty()) {
            renderCostIcons(graphics, x + 10, y + 18, costs, mouseX, mouseY);
        } else {
            graphics.drawString(font, Component.literal("추가 소모 없음"), x + 10, y + 20, WARN, false);
        }
    }

    private void renderStatCard(GuiGraphics graphics, int mouseX, int mouseY, int index, int actionType, int level, String titleText, String detailText, List<CostEntry> costs) {
        int x = leftPos + MODULE_CARD_X;
        int y = topPos + PANEL_TOP_Y + 8 + NAME_CARD_H + CARD_GAP + index * (STAT_CARD_H + CARD_GAP);
        drawCard(graphics, x, y, MODULE_CARD_W, STAT_CARD_H, level >= 5 ? WARN : ACCENT_2);
        graphics.drawString(font, Component.literal(titleText + "  Lv." + level + " / 5"), x + 10, y + 6, TEXT, false);
        graphics.drawString(font, Component.literal(detailText), x + 10, y + 18, MUTED, false);
        if (level >= 5) {
            graphics.drawString(font, Component.literal("최대 단계 도달"), x + 10, y + 30, WARN, false);
        } else {
            renderCostIcons(graphics, x + 10, y + 28, costs, mouseX, mouseY);
        }
    }

    private void renderInventoryPanel(GuiGraphics graphics) {
        int x = leftPos + PANEL_RIGHT_X + 4;
        int y = topPos + PANEL_TOP_Y + 8;
        int w = PANEL_RIGHT_W - 8;
        drawCard(graphics, x, y, w, 92, ACCENT_2);
        graphics.drawString(font, Component.literal("작업 인벤토리"), x + 10, y + 10, TEXT, false);
        graphics.drawString(font, Component.literal("장비·연료·강화 재료 선택"), x + 10, y + 22, MUTED, false);
        renderInventoryGrid(graphics);
    }

    private void renderTelemetryPanel(GuiGraphics graphics) {
        int x = leftPos + PANEL_RIGHT_X + 4;
        int y = topPos + PANEL_TOP_Y + 108;
        int w = PANEL_RIGHT_W - 8;
        drawCard(graphics, x, y, w, 102, ACCENT);
        ScrapDroneEntity drone = findClientDrone();
        graphics.drawString(font, Component.literal("드론 실시간 상태"), x + 10, y + 10, TEXT, false);
        graphics.drawString(font, Component.literal(ellipsize(menu.droneName(), 108)), x + 10, y + 22, ACCENT, false);

        drawBattery(graphics, x + 10, y + 38, drone);
        int textX = x + 34;
        graphics.drawString(font, Component.literal("상태: " + droneState(drone)), textX, y + 38, TEXT, false);
        graphics.drawString(font, Component.literal("동력: " + chargeText(drone)), textX, y + 50, MUTED, false);
        graphics.drawString(font, Component.literal("모듈: " + moduleSummary()), textX, y + 62, MUTED, false);
        ItemStack autoFuel = resolveItemStack(menu.autoFuelItem());
        graphics.drawString(font, Component.literal("자동 연료: " + (autoFuel.isEmpty() ? "미등록" : ellipsize(autoFuel.getHoverName().getString(), 82))), textX, y + 74, MUTED, false);
        graphics.drawString(font, Component.literal("탑승: " + ((drone != null && drone.getFirstPassenger() != null) ? "비행 중" : "대기")), textX, y + 86, MUTED, false);
    }

    private void renderBlacksmithPanel(GuiGraphics graphics) {
        int x = leftPos + PANEL_RIGHT_X + 4;
        int y = topPos + PANEL_TOP_Y + 108;
        int w = PANEL_RIGHT_W - 8;
        drawCard(graphics, x, y, w, 102, ACCENT);
        graphics.drawString(font, Component.literal("대장간 분해소"), x + 10, y + 10, TEXT, false);
        graphics.drawString(font, Component.literal("추출 등급: 100%"), x + 10, y + 24, GOOD, false);
        graphics.drawString(font, Component.literal("수수료: 무료"), x + 10, y + 36, GOOD, false);
        
        graphics.drawString(font, Component.literal("장비 분해 시 강화 레벨 및"), x + 10, y + 54, MUTED, false);
        graphics.drawString(font, Component.literal("소켓 상태에 비례해"), x + 10, y + 66, MUTED, false);
        graphics.drawString(font, Component.literal("재련 재료를 환원받습니다."), x + 10, y + 78, MUTED, false);
    }

    private void renderStatusPanel(GuiGraphics graphics, Component status) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(status, STATUS_W - 14);
        int x = leftPos + STATUS_X + 8;
        int y = topPos + STATUS_Y + 6;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), x, y + i * 10, ACCENT);
        }
    }

    private void drawPreviewBox(GuiGraphics graphics, int x, int y, ItemStack stack) {
        drawCard(graphics, x, y, PREVIEW_BOX_SIZE, PREVIEW_BOX_SIZE, ACCENT);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 17, y + 17);
            graphics.renderItemDecorations(font, stack, x + 17, y + 17);
        }
    }

    private void drawBattery(GuiGraphics graphics, int x, int y, ScrapDroneEntity drone) {
        drawBorder(graphics, x, y, 16, 46, ACCENT);
        graphics.fill(x + 4, y - 2, x + 12, y, ACCENT);
        graphics.fill(x + 1, y + 1, x + 15, y + 45, 0xFF0A0F10);
        if (drone == null) {
            return;
        }
        int charge = drone.getCharge();
        int fillHeight = Math.max(0, (int) (42 * (charge / 100.0F)));
        int color = charge > 50 ? GOOD : (charge > 20 ? WARN : DANGER);
        graphics.fill(x + 3, y + 43 - fillHeight, x + 13, y + 43, color);
    }

    private void renderInventoryGrid(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                renderInventoryCell(graphics, 9 + row * 9 + column, leftPos + INVENTORY_X + column * SLOT_SIZE, topPos + INVENTORY_Y + row * SLOT_SIZE);
            }
        }
        for (int column = 0; column < 9; column++) {
            renderInventoryCell(graphics, column, leftPos + INVENTORY_X + column * SLOT_SIZE, topPos + INVENTORY_Y + 62);
        }
    }

    private void renderInventoryCell(GuiGraphics graphics, int slot, int left, int top) {
        ItemStack stack = minecraft.player.getInventory().getItem(slot);
        boolean selected = slot == selectedSlot;
        boolean validForDeconstruct = !stack.isEmpty() && SmithingService.canDeconstruct(stack);
        int border = selected ? ACCENT : (currentTab == 0 && validForDeconstruct ? 0xFF1E8B71 : PANEL_BORDER);
        int fill = selected ? 0xFF15362E : 0xFF0A0F10;
        graphics.fill(left, top, left + SLOT_SIZE, top + SLOT_SIZE, border);
        graphics.fill(left + 1, top + 1, left + SLOT_SIZE - 1, top + SLOT_SIZE - 1, fill);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, left + 1, top + 1);
            graphics.renderItemDecorations(font, stack, left + 1, top + 1);
            if (currentTab == 0 && !validForDeconstruct) {
                graphics.fill(left + 1, top + 1, left + SLOT_SIZE - 1, top + SLOT_SIZE - 1, 0x88000000);
            }
        }
    }

    private void renderCostIcons(GuiGraphics graphics, int x, int y, List<CostEntry> costs, int mouseX, int mouseY) {
        for (int i = 0; i < costs.size(); i++) {
            CostEntry cost = costs.get(i);
            int chipX = x + (i % 3) * 42;
            int chipY = y + (i / 3) * 18;
            int owned = countPlayerItem(cost.item);
            int border = owned >= cost.required ? GOOD : DANGER;
            graphics.fill(chipX, chipY, chipX + 38, chipY + 16, border);
            graphics.fill(chipX + 1, chipY + 1, chipX + 37, chipY + 15, 0xFF0A0F10);
            graphics.renderItem(new ItemStack(cost.item), chipX + 2, chipY + 0);
            graphics.drawString(font, Component.literal(shortCount(cost.required)), chipX + 18, chipY + 5, owned >= cost.required ? GOOD : DANGER, false);
            trackCostHover(mouseX, mouseY, chipX, chipY, 38, 16, cost.item, cost.required, owned);
        }
    }

    private void trackCostHover(int mouseX, int mouseY, int x, int y, int width, int height, Item item, int required, int owned) {
        if (!insideAbsolute(mouseX, mouseY, x, y, width, height)) {
            return;
        }
        hoveredCostStack = new ItemStack(item);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(hoveredCostStack.getHoverName());
        tooltip.add(Component.literal("필요: " + required));
        tooltip.add(Component.literal("보유: " + owned));
        hoveredCostTooltip = tooltip;
    }

    private Component currentStatus(ItemStack stack) {
        return selectionChanged ? SmithingService.defaultStatus(stack) : menu.status();
    }

    private ItemStack selectedStack() {
        if (minecraft == null || minecraft.player == null || selectedSlot < 0 || selectedSlot >= minecraft.player.getInventory().getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return minecraft.player.getInventory().getItem(selectedSlot);
    }

    private int fallbackSelectedSlot() {
        if (minecraft == null || minecraft.player == null) {
            return -1;
        }
        for (int slot = 0; slot < minecraft.player.getInventory().getContainerSize(); slot++) {
            if (SmithingService.canDeconstruct(minecraft.player.getInventory().getItem(slot))) {
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

    private void refreshButtons() {
        if (tabDeconstructButton == null) {
            return;
        }
        layoutTabs();
        helpButton.setX(leftPos + imageWidth - 28);
        helpButton.setY(topPos + 12);
        closeButton.setX(leftPos + imageWidth - 90);
        closeButton.setY(topPos + STATUS_Y);

        primaryActionButton.visible = currentTab == 0 || currentTab == 1;
        primaryActionButton.active = currentTab == 0 ? SmithingService.canDeconstruct(selectedStack()) : !selectedStack().isEmpty();
        primaryActionButton.setMessage(Component.literal(currentTab == 1 ? "동력 생산" : "분해 실행"));
        if (currentTab == 1) {
            primaryActionButton.setWidth(68);
            primaryActionButton.setX(leftPos + MODULE_CARD_X + MODULE_CARD_W - 76);
        } else {
            primaryActionButton.setWidth(84);
            primaryActionButton.setX(leftPos + MODULE_CARD_X + MODULE_CARD_W - 92);
        }
        primaryActionButton.setY(topPos + PANEL_TOP_Y + PANEL_H - 26);

        autoRegisterButton.visible = currentTab == 1;
        autoRegisterButton.active = !selectedStack().isEmpty();
        autoRegisterButton.setX(leftPos + MODULE_CARD_X + 12);
        autoRegisterButton.setY(topPos + PANEL_TOP_Y + PANEL_H - 26);

        autoClearButton.visible = currentTab == 1;
        autoClearButton.active = !menu.autoFuelItem().isEmpty();
        autoClearButton.setX(leftPos + MODULE_CARD_X + 84);
        autoClearButton.setY(topPos + PANEL_TOP_Y + PANEL_H - 26);

        boolean broken = menu.isDroneBroken() && (currentTab == 2 || currentTab == 3);
        repairButton.visible = broken;
        repairButton.active = broken;
        repairButton.setX(leftPos + MODULE_CARD_X + MODULE_CARD_W - 92);
        repairButton.setY(topPos + PANEL_TOP_Y + PANEL_H - 26);

        boolean moduleTab = currentTab == 2 && !menu.isDroneBroken();
        layoutModuleButtons(moduleTab);

        boolean statTab = currentTab == 3 && !menu.isDroneBroken();
        nameEditBox.visible = statTab;
        applyNameButton.visible = statTab;
        applyNameButton.active = statTab;
        if (statTab) {
            int nameY = topPos + PANEL_TOP_Y + 8;
            nameEditBox.setX(leftPos + MODULE_CARD_X + 12);
            nameEditBox.setY(nameY + 30);
            nameEditBox.setWidth(148);
            applyNameButton.setX(leftPos + MODULE_CARD_X + MODULE_CARD_W - 56);
            applyNameButton.setY(nameY + 30);
        }
        layoutStatButtons(statTab);
    }

    private void layoutTabs() {
        int x = leftPos + 16;
        int y = topPos + 42;
        tabDeconstructButton.setX(x);
        tabDeconstructButton.setY(y);
        tabGeneratorButton.setX(x + 56);
        tabGeneratorButton.setY(y);
        tabUpgradeButton.setX(x + 112);
        tabUpgradeButton.setY(y);
        tabStatButton.setX(x + 168);
        tabStatButton.setY(y);

        if (menu.isSmithyMode()) {
            tabDeconstructButton.visible = false;
            tabGeneratorButton.visible = false;
            tabUpgradeButton.visible = false;
            tabStatButton.visible = false;

            tabDeconstructButton.active = false;
            tabGeneratorButton.active = false;
            tabUpgradeButton.active = false;
            tabStatButton.active = false;
        } else {
            tabDeconstructButton.visible = true;
            tabGeneratorButton.visible = true;
            tabUpgradeButton.visible = true;
            tabStatButton.visible = true;

            tabDeconstructButton.active = currentTab != 0;
            tabGeneratorButton.active = currentTab != 1;
            tabUpgradeButton.active = currentTab != 2;
            tabStatButton.active = currentTab != 3;
        }
    }

    private void layoutModuleButtons(boolean visible) {
        buyInvButton.visible = visible;
        buyTransButton.visible = visible;
        buyBoostButton.visible = visible;
        buySensorButton.visible = visible;
        buyGrabberButton.visible = visible;
        openStorageButton.visible = visible && menu.inventoryUpgradeLevel() > 0;

        positionModuleButton(buyInvButton, 0, 0);
        positionStorageButton(openStorageButton, 0);
        positionModuleButton(buyTransButton, 1, 0);
        positionModuleButton(buyBoostButton, 2, 0);
        positionModuleButton(buySensorButton, 3, 0);
        positionModuleButton(buyGrabberButton, 4, 0);

        configureModuleButton(buyInvButton, 5, menu.inventoryUpgradeLevel(), true);
        configureModuleButton(buyTransButton, 6, menu.transmitterUpgradeLevel(), false);
        configureModuleButton(buyBoostButton, 7, menu.boosterUpgradeLevel(), false);
        configureModuleButton(buySensorButton, 15, menu.sensorUpgradeLevel(), false);
        configureModuleButton(buyGrabberButton, 16, menu.grabberUpgradeLevel(), false);

        openStorageButton.active = visible && menu.inventoryUpgradeLevel() > 0;
        openStorageButton.setTooltip(openStorageButton.active ? Tooltip.create(Component.literal("드론 전용 보관함 열기")) : null);
    }

    private void layoutStatButtons(boolean visible) {
        upgradeAttackButton.visible = visible;
        upgradeHealthButton.visible = visible;
        upgradeRangeButton.visible = visible;
        magnetToggleButton.visible = visible;
        if (!visible) {
            return;
        }
        positionStatButton(upgradeAttackButton, 0);
        positionStatButton(upgradeHealthButton, 1);
        positionStatButton(upgradeRangeButton, 2);
        configureStatButton(upgradeAttackButton, 12, menu.statAttack());
        configureStatButton(upgradeHealthButton, 13, menu.statHealth());
        configureStatButton(upgradeRangeButton, 14, menu.statRange());

        boolean disabled = menu.isMagnetDisabled();
        magnetToggleButton.active = visible;
        magnetToggleButton.setMessage(Component.literal(disabled ? "자석 OFF" : "자석 ON"));
        magnetToggleButton.danger(disabled);
        magnetToggleButton.setTooltip(Tooltip.create(Component.literal(disabled ? "드론의 자석(진공 흡입) 기능이 비활성화되어 있습니다. 클릭하여 켭니다." : "드론의 자석(진공 흡입) 기능이 활성화되어 있습니다. 클릭하여 끕니다.")));
        magnetToggleButton.setX(upgradeRangeButton.getX() - 48);
        magnetToggleButton.setY(upgradeRangeButton.getY());
    }

    private void positionModuleButton(HextechButton button, int index, int stackRow) {
        int y = topPos + PANEL_TOP_Y + 8 + index * (MODULE_CARD_H + CARD_GAP) + 10 + stackRow * 16;
        button.setX(leftPos + MODULE_CARD_X + MODULE_CARD_W - 46);
        button.setY(y);
    }

    private void positionStorageButton(HextechButton button, int index) {
        int y = topPos + PANEL_TOP_Y + 8 + index * (MODULE_CARD_H + CARD_GAP) + 10;
        button.setX(leftPos + MODULE_CARD_X + MODULE_CARD_W - 90);
        button.setY(y);
    }

    private void positionStatButton(HextechButton button, int index) {
        int y = topPos + PANEL_TOP_Y + 8 + NAME_CARD_H + CARD_GAP + index * (STAT_CARD_H + CARD_GAP) + 14;
        button.setX(leftPos + MODULE_CARD_X + MODULE_CARD_W - 46);
        button.setY(y);
    }

    private void configureModuleButton(HextechButton button, int actionType, int level, boolean inventoryModule) {
        boolean maxed = level >= 5;
        boolean unlocked = level > 0;
        button.active = !maxed;
        if (!unlocked) {
            button.setMessage(Component.literal("해금"));
        } else if (maxed) {
            button.setMessage(Component.literal("MAX"));
        } else {
            button.setMessage(Component.literal("강화"));
        }
        List<CostEntry> costs = moduleCosts(actionType, level);
        button.setTooltip(maxed ? null : Tooltip.create(Component.literal(joinCostSummary(costs))));
        if (inventoryModule && unlocked && !maxed) {
            button.setTooltip(Tooltip.create(Component.literal("다음 비용: " + joinCostSummary(costs))));
        }
    }

    private void configureStatButton(HextechButton button, int actionType, int level) {
        boolean maxed = level >= 5;
        button.active = !maxed;
        button.setMessage(Component.literal(maxed ? "MAX" : "강화"));
        button.setTooltip(maxed ? null : Tooltip.create(Component.literal(joinCostSummary(statCosts(actionType, level)))));
    }

    private void switchTab(int tab) {
        currentTab = Math.max(0, Math.min(3, tab));
        refreshButtons();
    }

    private void sendAction(int actionType) {
        sendAction(actionType, "");
    }

    private void sendAction(int actionType, String extraText) {
        ModNetwork.CHANNEL.sendToServer(new DeconstructActionPacket(selectedSlot, actionType, extraText, currentTab));
    }

    private HextechButton hexButton(Component message, net.minecraft.client.gui.components.Button.OnPress onPress, int x, int y, int width, int height) {
        return HextechButton.hextechBuilder(message, onPress).bounds(x, y, width, height).build();
    }

    private HextechButton.Builder hexButtonBuilder(Component message, net.minecraft.client.gui.components.Button.OnPress onPress, int x, int y, int width, int height) {
        return HextechButton.hextechBuilder(message, onPress).bounds(x, y, width, height);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_BG);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF10181C);
        drawBorder(graphics, x, y, width, height, PANEL_BORDER);
    }

    private void drawCard(GuiGraphics graphics, int x, int y, int width, int height, int accentColor) {
        graphics.fill(x, y, x + width, y + height, CARD_BG);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, accentColor == PANEL_BORDER ? 0xFF11191C : CARD_ACTIVE);
        drawBorder(graphics, x, y, width, height, accentColor);
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void drawActiveTabFrame(GuiGraphics graphics, HextechButton button) {
        if (button == null) {
            return;
        }
        graphics.fill(button.getX() - 2, button.getY() - 2, button.getX() + button.getWidth() + 2, button.getY() - 1, ACCENT);
        graphics.fill(button.getX() - 2, button.getY() + button.getHeight() + 1, button.getX() + button.getWidth() + 2, button.getY() + button.getHeight() + 2, ACCENT_2);
    }

    private HextechButton activeTabButton() {
        return switch (currentTab) {
            case 0 -> tabDeconstructButton;
            case 1 -> tabGeneratorButton;
            case 2 -> tabUpgradeButton;
            default -> tabStatButton;
        };
    }

    private ScrapDroneEntity findClientDrone() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return null;
        }
        for (net.minecraft.world.entity.Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof ScrapDroneEntity drone && drone.getOwnerUuid().isPresent() && drone.getOwnerUuid().get().equals(minecraft.player.getUUID())) {
                return drone;
            }
        }
        return null;
    }

    private List<CostEntry> moduleCosts(int actionType, int currentLevel) {
        List<CostEntry> costs = new ArrayList<>();
        switch (actionType) {
            case 5 -> {
                if (currentLevel == 0) {
                    addCost(costs, Items.IRON_INGOT, 64);
                    addCost(costs, Items.COPPER_INGOT, 32);
                    addCost(costs, getCogwheelItem(), 10);
                } else if (currentLevel == 1) {
                    addCost(costs, Items.IRON_INGOT, 128);
                    addCost(costs, Items.COPPER_INGOT, 64);
                    addCost(costs, getCogwheelItem(), 20);
                    addCost(costs, getBrassIngotItem(), 5);
                } else if (currentLevel == 2) {
                    addCost(costs, Items.IRON_INGOT, 256);
                    addCost(costs, Items.COPPER_INGOT, 128);
                    addCost(costs, getCogwheelItem(), 30);
                    addCost(costs, getBrassIngotItem(), 10);
                    addCost(costs, getElectronTubeItem(), 5);
                } else if (currentLevel == 3) {
                    addCost(costs, Items.IRON_INGOT, 512);
                    addCost(costs, Items.GOLD_INGOT, 20);
                    addCost(costs, Items.DIAMOND, 10);
                    addCost(costs, getElectronTubeItem(), 10);
                    addCost(costs, getPrecisionMechanismItem(), 2);
                } else if (currentLevel == 4) {
                    addCost(costs, Items.IRON_INGOT, 1024);
                    addCost(costs, Items.DIAMOND, 20);
                    addCost(costs, getPrecisionMechanismItem(), 10);
                    addCost(costs, getSturdySheetItem(), 5);
                }
            }
            case 6 -> {
                if (currentLevel == 0) {
                    addCost(costs, Items.GOLD_INGOT, 32);
                    addCost(costs, Items.REDSTONE, 64);
                    addCost(costs, getCogwheelItem(), 10);
                } else if (currentLevel == 1) {
                    addCost(costs, Items.GOLD_INGOT, 64);
                    addCost(costs, Items.REDSTONE, 128);
                    addCost(costs, getCogwheelItem(), 20);
                    addCost(costs, getBrassIngotItem(), 10);
                } else if (currentLevel == 2) {
                    addCost(costs, Items.GOLD_INGOT, 128);
                    addCost(costs, Items.REDSTONE, 256);
                    addCost(costs, Items.DIAMOND, 5);
                    addCost(costs, getElectronTubeItem(), 10);
                } else if (currentLevel == 3) {
                    addCost(costs, Items.GOLD_INGOT, 256);
                    addCost(costs, Items.REDSTONE, 512);
                    addCost(costs, Items.ENDER_PEARL, 10);
                    addCost(costs, getElectronTubeItem(), 20);
                    addCost(costs, getPrecisionMechanismItem(), 5);
                } else if (currentLevel == 4) {
                    addCost(costs, Items.GOLD_INGOT, 512);
                    addCost(costs, Items.REDSTONE, 1024);
                    addCost(costs, Items.EMERALD, 20);
                    addCost(costs, getPrecisionMechanismItem(), 10);
                    addCost(costs, getSturdySheetItem(), 5);
                }
            }
            case 7 -> {
                if (currentLevel == 0) {
                    addCost(costs, Items.COPPER_INGOT, 64);
                    addCost(costs, Items.PISTON, 10);
                    addCost(costs, getCogwheelItem(), 10);
                } else if (currentLevel == 1) {
                    addCost(costs, Items.COPPER_INGOT, 128);
                    addCost(costs, Items.PISTON, 20);
                    addCost(costs, getCogwheelItem(), 20);
                    addCost(costs, getBrassIngotItem(), 10);
                } else if (currentLevel == 2) {
                    addCost(costs, Items.COPPER_INGOT, 256);
                    addCost(costs, Items.PISTON, 30);
                    addCost(costs, Items.IRON_INGOT, 100);
                    addCost(costs, getElectronTubeItem(), 5);
                } else if (currentLevel == 3) {
                    addCost(costs, Items.COPPER_INGOT, 512);
                    addCost(costs, Items.PISTON, 40);
                    addCost(costs, Items.IRON_INGOT, 200);
                    addCost(costs, getElectronTubeItem(), 10);
                    addCost(costs, getPrecisionMechanismItem(), 5);
                } else if (currentLevel == 4) {
                    addCost(costs, Items.COPPER_INGOT, 1024);
                    addCost(costs, Items.PISTON, 60);
                    addCost(costs, Items.DIAMOND, 20);
                    addCost(costs, getPrecisionMechanismItem(), 10);
                    addCost(costs, getSturdySheetItem(), 5);
                }
            }
            case 15 -> {
                if (currentLevel == 0) {
                    addCost(costs, Items.REDSTONE, 64);
                    addCost(costs, Items.COPPER_INGOT, 32);
                    addCost(costs, getCogwheelItem(), 10);
                } else if (currentLevel == 1) {
                    addCost(costs, Items.REDSTONE, 128);
                    addCost(costs, Items.COPPER_INGOT, 64);
                    addCost(costs, getCogwheelItem(), 20);
                    addCost(costs, getBrassIngotItem(), 10);
                } else if (currentLevel == 2) {
                    addCost(costs, Items.REDSTONE, 256);
                    addCost(costs, Items.COPPER_INGOT, 128);
                    addCost(costs, Items.LAPIS_LAZULI, 64);
                    addCost(costs, getElectronTubeItem(), 5);
                } else if (currentLevel == 3) {
                    addCost(costs, Items.REDSTONE, 512);
                    addCost(costs, Items.COPPER_INGOT, 256);
                    addCost(costs, Items.LAPIS_LAZULI, 128);
                    addCost(costs, getElectronTubeItem(), 10);
                    addCost(costs, getPrecisionMechanismItem(), 5);
                } else if (currentLevel == 4) {
                    addCost(costs, Items.REDSTONE, 1024);
                    addCost(costs, Items.DIAMOND, 20);
                    addCost(costs, Items.EMERALD, 30);
                    addCost(costs, getPrecisionMechanismItem(), 10);
                    addCost(costs, getSturdySheetItem(), 5);
                }
            }
            case 16 -> {
                if (currentLevel == 0) {
                    addCost(costs, Items.IRON_INGOT, 64);
                    addCost(costs, Items.PISTON, 10);
                    addCost(costs, getCogwheelItem(), 10);
                } else if (currentLevel == 1) {
                    addCost(costs, Items.IRON_INGOT, 128);
                    addCost(costs, Items.PISTON, 20);
                    addCost(costs, getCogwheelItem(), 20);
                    addCost(costs, getBrassIngotItem(), 10);
                } else if (currentLevel == 2) {
                    addCost(costs, Items.IRON_INGOT, 256);
                    addCost(costs, Items.PISTON, 30);
                    addCost(costs, getElectronTubeItem(), 5);
                } else if (currentLevel == 3) {
                    addCost(costs, Items.IRON_INGOT, 512);
                    addCost(costs, Items.PISTON, 40);
                    addCost(costs, getElectronTubeItem(), 10);
                    addCost(costs, getPrecisionMechanismItem(), 5);
                } else if (currentLevel == 4) {
                    addCost(costs, Items.IRON_INGOT, 1024);
                    addCost(costs, Items.PISTON, 60);
                    addCost(costs, Items.DIAMOND, 10);
                    addCost(costs, getPrecisionMechanismItem(), 10);
                    addCost(costs, getSturdySheetItem(), 5);
                    addCost(costs, Items.EMERALD, 30);
                }
            }
            default -> {
            }
        }
        return costs;
    }

    private List<CostEntry> statCosts(int actionType, int currentLevel) {
        List<CostEntry> costs = new ArrayList<>();
        switch (actionType) {
            case 12 -> {
                if (currentLevel == 1) {
                    addCost(costs, Items.IRON_INGOT, 64);
                    addCost(costs, Items.COPPER_INGOT, 32);
                    addCost(costs, getCogwheelItem(), 10);
                } else if (currentLevel == 2) {
                    addCost(costs, Items.IRON_INGOT, 128);
                    addCost(costs, Items.COPPER_INGOT, 64);
                    addCost(costs, Items.REDSTONE, 64);
                    addCost(costs, Items.PISTON, 10);
                    addCost(costs, getElectronTubeItem(), 5);
                } else if (currentLevel == 3) {
                    addCost(costs, Items.IRON_INGOT, 256);
                    addCost(costs, Items.COPPER_INGOT, 128);
                    addCost(costs, Items.REDSTONE, 128);
                    addCost(costs, Items.DIAMOND, 10);
                    addCost(costs, getPrecisionMechanismItem(), 5);
                } else if (currentLevel == 4) {
                    addCost(costs, Items.IRON_INGOT, 512);
                    addCost(costs, Items.DIAMOND, 20);
                    addCost(costs, getPrecisionMechanismItem(), 10);
                    addCost(costs, getSturdySheetItem(), 5);
                }
            }
            case 13 -> {
                if (currentLevel == 1) {
                    addCost(costs, Items.IRON_INGOT, 64);
                    addCost(costs, Items.COPPER_INGOT, 32);
                    addCost(costs, Items.PISTON, 10);
                } else if (currentLevel == 2) {
                    addCost(costs, Items.IRON_INGOT, 128);
                    addCost(costs, Items.COPPER_INGOT, 64);
                    addCost(costs, Items.PISTON, 20);
                    addCost(costs, getCogwheelItem(), 10);
                    addCost(costs, getBrassIngotItem(), 5);
                } else if (currentLevel == 3) {
                    addCost(costs, Items.IRON_INGOT, 256);
                    addCost(costs, Items.COPPER_INGOT, 128);
                    addCost(costs, Items.PISTON, 30);
                    addCost(costs, Items.DIAMOND, 5);
                    addCost(costs, getElectronTubeItem(), 10);
                } else if (currentLevel == 4) {
                    addCost(costs, Items.IRON_INGOT, 512);
                    addCost(costs, Items.PISTON, 50);
                    addCost(costs, Items.DIAMOND, 20);
                    addCost(costs, getPrecisionMechanismItem(), 10);
                    addCost(costs, getSturdySheetItem(), 5);
                    addCost(costs, Items.EMERALD, 30);
                }
            }
            case 14 -> {
                if (currentLevel == 1) {
                    addCost(costs, Items.REDSTONE, 64);
                    addCost(costs, Items.LAPIS_LAZULI, 32);
                    addCost(costs, getCogwheelItem(), 10);
                } else if (currentLevel == 2) {
                    addCost(costs, Items.REDSTONE, 128);
                    addCost(costs, Items.LAPIS_LAZULI, 64);
                    addCost(costs, Items.ENDER_PEARL, 10);
                    addCost(costs, getElectronTubeItem(), 5);
                } else if (currentLevel == 3) {
                    addCost(costs, Items.REDSTONE, 256);
                    addCost(costs, Items.LAPIS_LAZULI, 128);
                    addCost(costs, Items.ENDER_PEARL, 20);
                    addCost(costs, Items.DIAMOND, 5);
                    addCost(costs, getPrecisionMechanismItem(), 5);
                } else if (currentLevel == 4) {
                    addCost(costs, Items.REDSTONE, 512);
                    addCost(costs, Items.LAPIS_LAZULI, 256);
                    addCost(costs, Items.ENDER_PEARL, 40);
                    addCost(costs, getPrecisionMechanismItem(), 10);
                    addCost(costs, getSturdySheetItem(), 5);
                    addCost(costs, Items.EMERALD, 30);
                }
            }
            default -> {
            }
        }
        return costs;
    }

    private List<CostEntry> repairCosts() {
        List<CostEntry> costs = new ArrayList<>();
        addCost(costs, Items.IRON_INGOT, 5 + (menu.hasInventoryUpgrade() ? 5 : 0));
        int copper = (menu.hasInventoryUpgrade() ? 2 : 0) + (menu.hasBoosterUpgrade() ? 5 : 0);
        int gold = menu.hasTransmitterUpgrade() ? 1 : 0;
        int redstone = menu.hasTransmitterUpgrade() ? 1 : 0;
        int piston = menu.hasBoosterUpgrade() ? 1 : 0;
        if (copper > 0) addCost(costs, Items.COPPER_INGOT, copper);
        if (gold > 0) addCost(costs, Items.GOLD_INGOT, gold);
        if (redstone > 0) addCost(costs, Items.REDSTONE, redstone);
        if (piston > 0) addCost(costs, Items.PISTON, piston);
        addCost(costs, getCogwheelItem(), 1);
        return costs;
    }

    private void addCost(List<CostEntry> costs, Item item, int required) {
        if (required > 0) {
            costs.add(new CostEntry(item, required));
        }
    }

    private String moduleHeader(int actionType, int level) {
        return switch (actionType) {
            case 5 -> "보관함 모듈  Lv." + level + (level > 0 ? "  " + (level * 9) + "칸" : "  잠금");
            case 6 -> "안테나 모듈  Lv." + level + (level > 0 ? "  자동 전송" : "  잠금");
            case 7 -> "부스터 모듈  Lv." + level + (level > 0 ? "  기동 강화" : "  잠금");
            case 15 -> "센서 모듈  Lv." + level + (level > 0 ? "  HUD 연동" : "  잠금");
            case 16 -> "자재 암 모듈  Lv." + level + (level > 0 ? "  상호작용" : "  잠금");
            default -> "모듈";
        };
    }

    private String moduleStatus(int actionType, int level) {
        if (level >= 5) {
            return "최대 단계 도달";
        }
        List<CostEntry> costs = moduleCosts(actionType, level);
        return costs.isEmpty() ? "추가 비용 없음" : "다음 비용: " + joinCostSummary(costs);
    }

    private String joinCostSummary(List<CostEntry> costs) {
        if (costs.isEmpty()) {
            return "없음";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < costs.size(); i++) {
            if (i > 0) {
                builder.append(" · ");
            }
            CostEntry cost = costs.get(i);
            builder.append(itemName(cost.item)).append(" x").append(cost.required);
        }
        return builder.toString();
    }

    private String shortCount(int value) {
        if (value >= 1000) {
            return (value / 1000) + "K";
        }
        return Integer.toString(value);
    }

    private String ellipsize(String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String suffix = "...";
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width(suffix))) + suffix;
    }

    private double fuelValue(ItemStack stack) {
        ScrapDroneEntity drone = findClientDrone();
        if (stack.isEmpty()) {
            return 0.0D;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return drone != null ? drone.getFuelPowerValue(itemId) : 0.5D;
    }

    private String droneState(ScrapDroneEntity drone) {
        if (drone == null) {
            return "오프라인";
        }
        if (drone.getCharge() <= 0) {
            return "방전";
        }
        if (drone.getFirstPassenger() != null) {
            return "탑승 비행";
        }
        return "대기";
    }

    private String chargeText(ScrapDroneEntity drone) {
        if (drone == null) {
            return "- - %";
        }
        int charge = drone.getCharge();
        if (charge <= 0) {
            return "0% (복구 필요)";
        }
        boolean sunOk = drone.level().isDay() && drone.level().canSeeSky(drone.blockPosition());
        if (drone.getFirstPassenger() != null) {
            return charge + "%  (-1.0%/s)";
        }
        return charge + "%  " + (sunOk ? "(+0.2%/s)" : "(-0.2%/s)");
    }

    private String moduleSummary() {
        StringBuilder builder = new StringBuilder();
        if (menu.hasInventoryUpgrade()) builder.append("보관 ");
        if (menu.hasTransmitterUpgrade()) builder.append("안테나 ");
        if (menu.hasBoosterUpgrade()) builder.append("부스터 ");
        if (menu.hasSensorUpgrade()) builder.append("센서 ");
        if (menu.hasGrabberUpgrade()) builder.append("자재암 ");
        return builder.length() == 0 ? "없음" : builder.toString().trim();
    }

    private ItemStack tooltipStack(int mouseX, int mouseY) {
        if ((currentTab == 0 || currentTab == 1) && insideAbsolute(mouseX, mouseY, leftPos + PREVIEW_BOX_X, topPos + PREVIEW_BOX_Y, PREVIEW_BOX_SIZE, PREVIEW_BOX_SIZE)) {
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
                if (inside(relativeX, relativeY, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + row * SLOT_SIZE, SLOT_SIZE, SLOT_SIZE)) {
                    return 9 + row * 9 + column;
                }
            }
        }
        for (int column = 0; column < 9; column++) {
            if (inside(relativeX, relativeY, INVENTORY_X + column * SLOT_SIZE, INVENTORY_Y + 62, SLOT_SIZE, SLOT_SIZE)) {
                return column;
            }
        }
        return -1;
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean insideAbsolute(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private List<Component> helpLines() {
        List<Component> helpLines = new ArrayList<>();
        helpLines.add(Component.literal("§6§l[오토 스크랩 드론 운영 및 업그레이드 상세 가이드]"));
        helpLines.add(Component.literal("§7드론의 기체 조작, 각 모듈 및 스탯의 상세 정보입니다."));
        helpLines.add(Component.literal(""));
        helpLines.add(Component.literal("§e§l[기본 기동 방식]"));
        helpLines.add(Component.literal(" - §b탑승 비행§7: §f드론 우클릭§7으로 매달려 비행합니다."));
        helpLines.add(Component.literal("   * §fW/S/A/D§7: 수평 비행 이동, §fSpace§7: 고도 상승, §fShift§7: 하마. (20틱당 1% 동력 소소)"));
        helpLines.add(Component.literal(" - §b제어 화면§7: §fShift(웅크리기) + 드론 우클릭§7으로 원격 분해/발전/모듈 제어 창을 엽니다."));
        helpLines.add(Component.literal(""));
        helpLines.add(Component.literal("§e§l[핵심 5대 모듈 상세 기능]"));
        helpLines.add(Component.literal(" §61. 보관함 모듈 (Lv.1~5)"));
        helpLines.add(Component.literal("  - 드론 자체 보관 공간을 해금합니다. §f레벨당 9칸씩 확장§7되어 최대 §f45칸(Lv.5)§7까지 확보됩니다."));
        helpLines.add(Component.literal("  - 모듈 탭의 §e[열기]§7 버튼으로 드론 전용 보관함을 열 수 있습니다."));
        helpLines.add(Component.literal(" §62. 안테나 모듈 (Lv.1~5)"));
        helpLines.add(Component.literal("  - 드론과의 §f원격 송수신 및 공급 유효 반경 범위§7가 비약적으로 넓어집니다."));
        helpLines.add(Component.literal("  - §fShift + 빈손 우클릭§7으로 보관함/아이템 핸들러 블록을 무선 전송 위치로 등록합니다."));
        helpLines.add(Component.literal(" §63. 부스터 모듈 (Lv.1~5)"));
        helpLines.add(Component.literal("  - 탑승 비행 중 §f달리기(Sprint) 키§7 입력 시, 강력한 §b부스터 가속 기동(15틱 지속)§7을 실행합니다."));
        helpLines.add(Component.literal("  - 강화 레벨당 §f가속 속도가 대폭 증가§7하며, 가속 시의 §e동력 소모가 10%에서 최대 4%까지 축소§7됩니다."));
        helpLines.add(Component.literal(" §44. 센서 모듈 (Lv.1~5)"));
        helpLines.add(Component.literal("  - 지원 사격 시 §f적 자동 포착 및 사격 가능 범위§7가 늘어납니다. (기본 12m, §f레벨당 +5m§7)"));
        helpLines.add(Component.literal(" §a5. 자재 암 모듈 (Lv.1~5)"));
        helpLines.add(Component.literal("  - 플레이어 주변의 드롭 아이템을 무선으로 회수하는 §b진공 흡입 범위§7가 증가합니다. (기본 12m, §f레벨당 +4m§7)"));
        helpLines.add(Component.literal("  - §e[스탯] 탭§7에서 §b자력 스탯 옆의 자석 버튼§7으로 이 흡입 효과를 언제든지 끄고 켤 수 있습니다."));
        helpLines.add(Component.literal(""));
        helpLines.add(Component.literal("§e§l[핵심 3대 스탯 강화]"));
        helpLines.add(Component.literal(" §c1. 공격 스탯 (Lv.1~5)"));
        helpLines.add(Component.literal("  - 지원 사격 시 적을 타격하는 §c탄환 최종 공격력§7과 §b기반 명중 보정값§7이 레벨당 비례해 상승합니다."));
        helpLines.add(Component.literal(" §d2. 체력 스탯 (Lv.1~5)"));
        helpLines.add(Component.literal("  - 드론의 쉴드 보호막 강화 및 §d최대 내구도(HP)§7가 크게 늘어납니다. (기본 20 HP, §f레벨당 +10 HP§7, 최대 70 HP)"));
        helpLines.add(Component.literal(" §b3. 자력 스탯 (Lv.1~5)"));
        helpLines.add(Component.literal("  - 자재 암의 무선 회수 주기가 단축되고, 주변 드롭 아이템을 끌어당기는 힘(자력 벡터)이 강화됩니다."));
        helpLines.add(Component.literal(""));
        helpLines.add(Component.literal("§a* 각 업그레이드 비용 아이콘 위에 마우스를 올리면 필요한 재료 수량과 현재 보유 수량을 볼 수 있습니다."));
        return helpLines;
    }

    private ItemStack resolveItemStack(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private int countPlayerItem(Item item) {
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }
        return com.nogeon.economyland.player.ExtendedInventoryDelivery.countAllOwnedClient(
            minecraft.player,
            new ItemStack(item),
            null,
            ""
        );
    }

    private String itemName(Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    private Item getCreateItem(String path, Item fallback) {
        ResourceLocation id = ResourceLocation.tryParse("create:" + path);
        if (id == null) {
            return fallback;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? fallback : item;
    }

    private Item getCogwheelItem() {
        return getCreateItem("cogwheel", Items.COPPER_INGOT);
    }

    private Item getBrassIngotItem() {
        return getCreateItem("brass_ingot", Items.GOLD_INGOT);
    }

    private Item getElectronTubeItem() {
        return getCreateItem("electron_tube", Items.COMPARATOR);
    }

    private Item getPrecisionMechanismItem() {
        return getCreateItem("precision_mechanism", Items.CLOCK);
    }

    private Item getSturdySheetItem() {
        return getCreateItem("sturdy_sheet", Items.NETHERITE_INGOT);
    }

    private static final class CostEntry {
        private final Item item;
        private final int required;

        private CostEntry(Item item, int required) {
            this.item = item;
            this.required = required;
        }
    }
}
