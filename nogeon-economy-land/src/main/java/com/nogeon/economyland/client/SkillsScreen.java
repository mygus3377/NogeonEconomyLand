package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.SkillsMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.OpenWalletPacket;
import com.nogeon.economyland.network.UpgradeSkillPacket;
import com.nogeon.economyland.network.ResetSkillsPacket;
import com.nogeon.economyland.player.SkillNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public final class SkillsScreen extends AbstractContainerScreen<SkillsMenu> {
    private static final int VIEW_X = 18;
    private static final int VIEW_Y = 40;
    private static final int VIEW_WIDTH = 464;
    private static final int VIEW_HEIGHT = 174;
    private static final int DETAIL_Y = 224;
    private static final float CONTENT_WIDTH = 900.0F;
    private static final float CONTENT_HEIGHT = 470.0F;
    private static final float TREE_PADDING = 12.0F;
    private static final int SCROLLBAR_WIDTH = 8;
    private static final float MIN_ZOOM = 0.40F;
    private static final float MAX_ZOOM = 1.50F;
    private static final float DEFAULT_ZOOM = 0.45F;

    private static float rememberedZoom = DEFAULT_ZOOM;
    private static float rememberedPanX = Float.NaN;
    private static float rememberedPanY = Float.NaN;

    private float zoom = rememberedZoom;
    private float panX;
    private float panY;
    private boolean draggingTree;
    private boolean draggingScrollbar;
    private double lastDragX;
    private double lastDragY;
    private boolean showHelp = false;
    private int helpScrollOffset = 0;

    public SkillsScreen(SkillsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 500;
        imageHeight = 320;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.nogeon_economy_land.wallet_tab"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenWalletPacket()))
            .bounds(leftPos + 18, topPos + 286, 64, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal("스킬 초기화 (10만 C)"),
            button -> ModNetwork.CHANNEL.sendToServer(new ResetSkillsPacket(menu.jobId())))
            .bounds(leftPos + 90, topPos + 286, 120, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal("도움말"),
            button -> {
                showHelp = !showHelp;
                helpScrollOffset = 0;
            })
            .bounds(leftPos + 220, topPos + 286, 60, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal("-"),
            button -> setZoom(zoom - 0.15F, leftPos + VIEW_X + VIEW_WIDTH / 2, topPos + VIEW_Y + VIEW_HEIGHT / 2))
            .bounds(leftPos + 348, topPos + 286, 20, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal("1:1"),
            button -> resetView())
            .bounds(leftPos + 372, topPos + 286, 40, 20)
            .build());
        addRenderableWidget(Button.builder(Component.literal("+"),
            button -> setZoom(zoom + 0.15F, leftPos + VIEW_X + VIEW_WIDTH / 2, topPos + VIEW_Y + VIEW_HEIGHT / 2))
            .bounds(leftPos + 416, topPos + 286, 20, 20)
            .build());

        if (Float.isNaN(rememberedPanX) || Float.isNaN(rememberedPanY)) {
            resetView();
        } else {
            panX = rememberedPanX;
            panY = rememberedPanY;
            clampPan();
        }
    }

    private void resetView() {
        zoom = DEFAULT_ZOOM;
        panX = (VIEW_WIDTH - CONTENT_WIDTH * zoom) / 2.0F;
        panY = TREE_PADDING;
        clampPan();
        rememberView();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xF0121418);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xF01D2127);
        graphics.fill(x + VIEW_X, y + VIEW_Y, x + VIEW_X + VIEW_WIDTH, y + VIEW_Y + VIEW_HEIGHT, 0xFF15191D);
        graphics.fill(x + VIEW_X + 1, y + VIEW_Y + 1, x + VIEW_X + VIEW_WIDTH - 1, y + VIEW_Y + VIEW_HEIGHT - 1, 0xFF11161A);
        graphics.fill(x + 18, y + DETAIL_Y, x + imageWidth - 18, y + 270, 0xFF1B2025);
        graphics.fill(x + 18, y + 274, x + imageWidth - 18, y + 306, 0xFF171C21);

        graphics.enableScissor(viewLeft(), viewTop(), viewRight(), viewBottom());
        graphics.pose().pushPose();
        graphics.pose().translate(viewLeft() + panX, viewTop() + panY, 0.0F);
        graphics.pose().scale(zoom, zoom, 1.0F);

        for (SkillNode node : SkillNode.forJobId(menu.jobId())) {
            for (SkillNode prerequisite : node.prerequisites()) {
                drawConnector(graphics, prerequisite, node);
            }
        }
        for (SkillNode node : SkillNode.forJobId(menu.jobId())) {
            drawNode(graphics, node);
        }

        graphics.pose().popPose();
        graphics.disableScissor();
        drawScrollbar(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawHoverPanel(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
        if (!showHelp) {
            renderSkillFloatingTooltip(graphics, mouseX, mouseY);
        }
        renderHelpOverlay(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFFE7E3D1);
        graphics.drawCenteredString(font, Component.translatable("job.nogeon_economy_land." + menu.jobId())
            .append(Component.literal(" Lv." + menu.jobLevel())), 142, 28, 0xFFB9C6BE);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.skill_points")
            .append(": ").append(String.valueOf(menu.skillPoints())), 308, 28, 0xFFFFD56A, false);
        graphics.drawString(font, Component.literal(String.format(Locale.ROOT, "x%.2f", zoom)), 456, 28, 0xFFB5C1CC, false);
        graphics.drawString(font, Component.literal("휠: 확대/축소  |  Shift+휠 / 스크롤바: 위아래 이동"), 90, 276, 0xFF97A39B, false);
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int left = scrollbarLeft();
        int top = viewTop();
        int bottom = viewBottom();
        int handleHeight = scrollbarHandleHeight();
        int handleTop = scrollbarHandleTop();
        graphics.fill(left, top, left + SCROLLBAR_WIDTH, bottom, 0xFF20262D);
        graphics.fill(left + 1, top + 1, left + SCROLLBAR_WIDTH - 1, bottom - 1, 0xFF11161A);
        graphics.fill(left + 1, handleTop, left + SCROLLBAR_WIDTH - 1, handleTop + handleHeight, 0xFF8A8268);
        graphics.fill(left + 2, handleTop + 1, left + SCROLLBAR_WIDTH - 2, handleTop + handleHeight - 1,
            draggingScrollbar ? 0xFFE0B25C : 0xFFB9A46B);
    }

    private void drawConnector(GuiGraphics graphics, SkillNode parent, SkillNode child) {
        int parentCenterX = parent.x() + nodeSize(parent) / 2;
        int parentCenterY = parent.y() + nodeSize(parent) / 2;
        int childCenterX = child.x() + nodeSize(child) / 2;
        int childCenterY = child.y() + nodeSize(child) / 2;
        int midY = parentCenterY + (childCenterY - parentCenterY) / 2;

        boolean parentUnlocked = menu.level(parent) > 0;
        boolean childUnlocked = menu.level(child) > 0;
        boolean canUpgradeChild = menu.canUpgrade(child);

        int color;
        int glowColor = 0;
        boolean useGlow = false;

        if (parentUnlocked && childUnlocked) {
            color = 0xFF00FFCC; // 찬란한 청록색 네온
            glowColor = 0x4000FFCC;
            useGlow = true;
        } else if (parentUnlocked && canUpgradeChild) {
            color = 0xFFFFB900; // 해금 대기 가능 골드 네온
            glowColor = 0x40FFB900;
            useGlow = true;
        } else {
            color = 0xFF4B5259; // 잠김 어두운 회색
        }

        if (useGlow) {
            // Glow Effect (좌우상하 1px씩 넓게 채워 번지는 느낌 선사)
            graphics.fill(parentCenterX - 1, parentCenterY - (parentCenterY < midY ? 0 : 1), parentCenterX + 3, midY + (parentCenterY < midY ? 1 : 0), glowColor);
            graphics.fill(Math.min(parentCenterX, childCenterX) - 1, midY - 1, Math.max(parentCenterX, childCenterX) + 3, midY + 3, glowColor);
            graphics.fill(childCenterX - 1, midY - (midY < childCenterY ? 1 : 0), childCenterX + 3, childCenterY + (midY < childCenterY ? 0 : 1), glowColor);
        }

        // Core Line (2px 중심선)
        graphics.fill(parentCenterX, parentCenterY, parentCenterX + 2, midY, color);
        graphics.fill(Math.min(parentCenterX, childCenterX), midY, Math.max(parentCenterX, childCenterX) + 2, midY + 2, color);
        graphics.fill(childCenterX, midY, childCenterX + 2, childCenterY, color);
    }

    private void drawNode(GuiGraphics graphics, SkillNode node) {
        int x = node.x();
        int y = node.y();
        int size = nodeSize(node);
        int labelWidth = node.large() ? 128 : 102;
        int labelX = Mth.clamp(x + size / 2 - labelWidth / 2, 12, (int) CONTENT_WIDTH - labelWidth - 12);
        boolean unlocked = menu.level(node) > 0;
        boolean available = menu.canUpgrade(node);

        int outerColor;
        int innerColor;
        int glowColor = 0;
        boolean useGlow = false;

        if (node.large()) {
            if (unlocked) {
                outerColor = 0xFFE0B25C; // 골드 네온
                innerColor = 0xFF221A0F; // 다크 오렌지 브라운
                glowColor = 0x50E0B25C;
                useGlow = true;
            } else if (available) {
                outerColor = 0xFFBCA16B; // 골드 대기 상태
                innerColor = 0xFF191F26;
                glowColor = 0x25BCA16B;
                useGlow = true;
            } else {
                outerColor = 0xFF5A4D3B; // 잠김 다크 골드
                innerColor = 0xFF161616;
            }
        } else {
            if (unlocked) {
                outerColor = 0xFF00FFCC; // 시안 네온
                innerColor = 0xFF0B2120; // 딥 네이비 청록
                glowColor = 0x5000FFCC;
                useGlow = true;
            } else if (available) {
                outerColor = 0xFF00AA88; // 청록 대기 상태
                innerColor = 0xFF11262B;
                glowColor = 0x2500AA88;
                useGlow = true;
            } else {
                outerColor = 0xFF46505A; // 잠김 다크 실버
                innerColor = 0xFF1B2025;
            }
        }

        // 네온 글로우 외곽선 그리기
        if (useGlow) {
            graphics.fill(x - 2, y - 2, x + size + 2, y + size + 2, glowColor);
            graphics.fill(x - 1, y - 1, x + size + 1, y + size + 1, glowColor);
        }

        // 메인 노드 사각형 박스
        graphics.fill(x, y, x + size, y + size, outerColor);
        graphics.fill(x + 2, y + 2, x + size - 2, y + size - 2, innerColor);

        // 노드 중앙의 스킬 레벨 숫자 렌더링
        int curLevel = menu.level(node);
        String centerChar = String.valueOf(curLevel);
        int centerColor = unlocked ? 0xFFFFFFFF : (available ? 0xFFB5C1CC : 0xFF7F8C8D);

        int textWidth = font.width(centerChar);
        int textOffsetX = (size - textWidth) / 2;
        int textOffsetY = (size - 8) / 2;
        graphics.drawString(font, centerChar, x + textOffsetX, y + textOffsetY, centerColor, false);

        // 하단 텍스트 영역 렌더링 (겹침 방지를 위해 오프셋 확장)
        int textY = y + size + 9;

        // 1. [Lv.X] 레벨 뱃지 표기
        String lvText = "Lv." + curLevel;
        int lvWidth = font.width(lvText);
        int lvColor = unlocked ? (node.large() ? 0xFFFFD56A : 0xFF00FFCC) : 0xFF7F8C8D;
        graphics.drawString(font, lvText, labelX + (labelWidth - lvWidth) / 2, textY, lvColor, false);
        textY += 11;

        // 2. 타이틀 텍스트 표기
        List<FormattedCharSequence> titleLines = font.split(Component.translatable(node.titleKey()), node.large() ? 96 : 84);
        for (int index = 0; index < Math.min(2, titleLines.size()); index++) {
            int lineWidth = font.width(titleLines.get(index));
            graphics.drawString(font, titleLines.get(index), labelX + (labelWidth - lineWidth) / 2, textY + index * 10, 0xFFE7DFC9, false);
        }
    }

    private void drawHoverPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int panelX = x + 24;
        int panelY = y + DETAIL_Y + 8;

        // 헥스테크 보더 라인 및 내부 그라데이션 다크 프레임 (Y 시작 오프셋을 6px 위로 올려 52px 높이 확보!)
        graphics.fill(x + 18, y + DETAIL_Y - 6, x + imageWidth - 18, y + 270, 0xFF3C3525); // 브론즈 골드 아웃라인
        graphics.fill(x + 19, y + DETAIL_Y - 5, x + imageWidth - 19, y + 269, 0xFF14171A); // 다크 내부 프레임

        if (showHelp) {
            return;
        }

        SkillNode hovered = hoveredNode(mouseX, mouseY);
        if (hovered == null) {
            graphics.drawCenteredString(font, Component.literal("노드에 마우스를 올리면 상세 정보가 보입니다."), x + imageWidth / 2, panelY + 15, 0xFF7F8C8D);
            return;
        }

        int curLevel = menu.level(hovered);

        // 1. 스킬명 및 노드 타입 데코레이션
        Component titleComp = Component.translatable(hovered.titleKey());
        String typeText = hovered.large() ? " [특수 능력]" : " [기본 스탯]";
        int typeColor = hovered.large() ? 0xFFFFD56A : 0xFF00FFCC;

        graphics.drawString(font, titleComp, panelX, panelY, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.literal(typeText), panelX + font.width(titleComp), panelY, typeColor, false);

        // 2. 현재 레벨 표시
        String levelStr = "현재 Lv." + curLevel;
        graphics.drawString(font, Component.literal(levelStr), panelX + 375 - font.width(levelStr), panelY, 0xFFB5C1CC, false);

        // 3. 업그레이드 비용 / 제한 가이드
        int cost = menu.upgradeCost(hovered);
        boolean canUpgrade = menu.canUpgrade(hovered);

        int capLevel = hovered.large() ? Math.min(hovered.maxLevel(), menu.jobLevel() / 5) : hovered.maxLevel();
        boolean capReached = curLevel >= capLevel;

        String costText;
        int costColor;
        if (capReached) {
            costText = "레벨 제한 도달 (Lv." + capLevel + ")";
            costColor = 0xFFFF5555; // 레드 경고
        } else {
            costText = "필요 포인트: " + cost + " SP";
            costColor = canUpgrade ? 0xFFFFD56A : 0xFFE74C3C; // 가능(골드) / 불가(레드)
        }
        graphics.drawString(font, Component.literal(costText), panelX + 384, panelY, costColor, false);

        // 4. 스킬 설명 렌더링
        Font currentFont = font;
        List<FormattedCharSequence> descLines = currentFont.split(Component.translatable(hovered.descriptionKey()), 320); // 3x3 공간을 배려해 텍스트 폭을 370에서 320으로 컴팩트하게 축소!
        int textOffset = panelY + 14;
        for (int index = 0; index < Math.min(2, descLines.size()); index++) {
            graphics.drawString(currentFont, descLines.get(index), panelX, textOffset + index * 10, 0xFFA9B3BC, false);
        }

        // 5. 무한 계수 및 5레벨 캡 가이드라인 표기 (맨 하단 라인)
        if (hovered.large()) {
            String capGuideText = "[특수] 5레벨마다 1Lv 한도 상승 | 현재 최대 한계: Lv." + capLevel;
            graphics.drawString(font, Component.literal(capGuideText), panelX, panelY + 36, 0xFF50C8FF, false); // 시안 블루
        } else {
            String normalGuideText = "[스탯] 레벨 제한 없음 | 스킬 레벨에 비례하여 스탯이 영구 상승합니다.";
            graphics.drawString(font, Component.literal(normalGuideText), panelX, panelY + 36, 0xFF97A39B, false); // 실버 그레이
        }

        // 농사 75레벨 허수아비 스킬 노드일 때 우측 조합법 미니어처 렌더링!
        if (hovered == SkillNode.FARMER_SUNLIT_STEP) {
            int gridStartX = x + imageWidth - 110;
            int gridStartY = y + DETAIL_Y - 1;
            
            // "조합법" 타이틀
            graphics.drawString(font, "조합법", gridStartX, gridStartY - 9, 0xFFFFD56A, false);
            
            net.minecraft.world.item.ItemStack pumpkin = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CARVED_PUMPKIN);
            net.minecraft.world.item.ItemStack stick = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK);
            net.minecraft.world.item.ItemStack leather = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER);
            net.minecraft.world.item.ItemStack hay = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.HAY_BLOCK);
            
            net.minecraft.world.item.ItemStack[][] recipe = {
                {net.minecraft.world.item.ItemStack.EMPTY, pumpkin, net.minecraft.world.item.ItemStack.EMPTY},
                {stick, leather, stick},
                {net.minecraft.world.item.ItemStack.EMPTY, hay, net.minecraft.world.item.ItemStack.EMPTY}
            };
            
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int slotX = gridStartX + col * 17;
                    int slotY = gridStartY + row * 17;
                    graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF11161A);
                    
                    // 1px Border (0xFF3C3525)
                    graphics.fill(slotX, slotY, slotX + 16, slotY + 1, 0xFF3C3525);
                    graphics.fill(slotX, slotY + 15, slotX + 16, slotY + 16, 0xFF3C3525);
                    graphics.fill(slotX, slotY, slotX + 1, slotY + 16, 0xFF3C3525);
                    graphics.fill(slotX + 15, slotY, slotX + 16, slotY + 16, 0xFF3C3525);
                    
                    net.minecraft.world.item.ItemStack stack = recipe[row][col];
                    if (!stack.isEmpty()) {
                        graphics.renderItem(stack, slotX, slotY);
                    }
                }
            }
            
            // 화살표 ->
            int arrowX = gridStartX + 54;
            int arrowY = gridStartY + 20;
            graphics.drawString(font, "->", arrowX, arrowY, 0xFF00FFCC, false);
            
            // 결과 슬롯
            int resX = gridStartX + 70;
            int resY = gridStartY + 16;
            graphics.fill(resX, resY, resX + 18, resY + 18, 0xFF0E1311);
            
            // 1px Gold Border (0xFFFFD56A)
            graphics.fill(resX, resY, resX + 18, resY + 1, 0xFFFFD56A);
            graphics.fill(resX, resY + 17, resX + 18, resY + 18, 0xFFFFD56A);
            graphics.fill(resX, resY, resX + 1, resY + 18, 0xFFFFD56A);
            graphics.fill(resX + 17, resY, resX + 18, resY + 18, 0xFFFFD56A);
            
            net.minecraft.world.item.ItemStack resultStack = new net.minecraft.world.item.ItemStack(com.nogeon.economyland.item.ModItems.FARMER_SCARECROW.get());
            graphics.renderItem(resultStack, resX + 1, resY + 1);
        }
    }

    private void renderSkillFloatingTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        SkillNode hovered = hoveredNode(mouseX, mouseY);
        if (hovered == null) {
            return;
        }
        int curLevel = menu.level(hovered);
        int capLevel = hovered.large() ? Math.min(hovered.maxLevel(), menu.jobLevel() / 5) : hovered.maxLevel();
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable(hovered.titleKey()).append(" Lv." + curLevel + "/" + hovered.maxLevel()).getVisualOrderText());
        lines.add(Component.literal(hovered.large() ? "Special" : "Stat").getVisualOrderText());
        List<FormattedCharSequence> descLines = font.split(Component.translatable(hovered.descriptionKey()), 230);
        for (int index = 0; index < Math.min(8, descLines.size()); index++) {
            lines.add(descLines.get(index));
        }
        if (descLines.size() > 8) {
            lines.add(Component.literal("...").getVisualOrderText());
        }
        Component keyHint = activeSkillKeyHint(hovered);
        if (keyHint != null) {
            lines.add(keyHint.getVisualOrderText());
        }
        lines.add(Component.literal(hovered.large()
            ? "Max 10 / 3 SP per level / effect capped for Lv.300 balance"
            : "Max 30 / 1 SP per level / effect follows node description").getVisualOrderText());
        lines.add(Component.literal("SP: " + menu.upgradeCost(hovered)).getVisualOrderText());
        if (curLevel >= capLevel) {
            lines.add(Component.literal("Limit: Lv." + capLevel).getVisualOrderText());
        }

        int tooltipHeight = lines.size() * 10 + 8;
        int tooltipY = Math.min(mouseY + 12, height - tooltipHeight - 6);
        graphics.renderTooltip(font, lines, mouseX + 12, Math.max(6, tooltipY));
    }

    private Component activeSkillKeyHint(SkillNode node) {
        return switch (node) {
            case MINER_STONE_SKIN, HUNTER_QUICK_DRAW, FISHER_CALM_WATER, FARMER_FIELD_SNACK ->
                Component.literal("Key: ").append(ClientModEvents.JOB_ABILITY_PRIMARY_KEY.getTranslatedKeyMessage());
            case MINER_EYE_OPENING, HUNTER_WILD_STEP, COOK_MASTER_RECIPE ->
                Component.literal("Key: ").append(ClientModEvents.JOB_ABILITY_SECONDARY_KEY.getTranslatedKeyMessage());
            default -> null;
        };
    }

    private SkillNode hoveredNode(int mouseX, int mouseY) {
        if (!insideTree(mouseX, mouseY)) {
            return null;
        }

        double worldX = (mouseX - viewLeft() - panX) / zoom;
        double worldY = (mouseY - viewTop() - panY) / zoom;
        for (SkillNode node : SkillNode.forJobId(menu.jobId())) {
            int x = node.x();
            int y = node.y();
            int size = nodeSize(node);
            if (worldX >= x && worldX < x + size && worldY >= y && worldY < y + size) {
                return node;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showHelp) {
            if (mouseX >= leftPos + 18 && mouseX < leftPos + 482 && mouseY >= topPos + 40 && mouseY < topPos + 270) {
                return true;
            }
        }
        if (button == 0 && insideScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            updateScrollbar(mouseY);
            return true;
        }

        if (button == 1 && insideTree(mouseX, mouseY)) {
            draggingTree = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }

        if (button == 0) {
            SkillNode hovered = hoveredNode((int) mouseX, (int) mouseY);
            if (hovered != null) {
                if (menu.canUpgrade(hovered)) {
                    ModNetwork.CHANNEL.sendToServer(new UpgradeSkillPacket(menu.jobId(), hovered.id()));
                }
                return true;
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

        if (draggingTree && button == 1) {
            panX += (float) (mouseX - lastDragX);
            panY += (float) (mouseY - lastDragY);
            lastDragX = mouseX;
            lastDragY = mouseY;
            clampPan();
            rememberView();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
        }
        if (button == 1) {
            draggingTree = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showHelp) {
            int maxScroll = getMaxHelpScroll();
            helpScrollOffset = Mth.clamp(helpScrollOffset - (int) delta * 2, 0, maxScroll);
            return true;
        }
        if (insideTree(mouseX, mouseY)) {
            if (Screen.hasShiftDown()) {
                scrollVertically((float) delta * 18.0F);
                return true;
            }
            setZoom(zoom + (float) delta * 0.10F, (int) mouseX, (int) mouseY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void scrollVertically(float amount) {
        panY = Mth.clamp(panY + amount, minPanY(), maxPanY());
        rememberView();
    }

    private void setZoom(float targetZoom, int anchorX, int anchorY) {
        float previousZoom = zoom;
        zoom = Mth.clamp(targetZoom, MIN_ZOOM, MAX_ZOOM);
        if (Math.abs(previousZoom - zoom) < 0.001F) {
            return;
        }

        float worldX = (anchorX - viewLeft() - panX) / previousZoom;
        float worldY = (anchorY - viewTop() - panY) / previousZoom;
        panX = anchorX - viewLeft() - worldX * zoom;
        panY = anchorY - viewTop() - worldY * zoom;
        clampPan();
        rememberView();
    }

    private void clampPan() {
        panX = Mth.clamp(panX, minPanX(), maxPanX());
        panY = Mth.clamp(panY, minPanY(), maxPanY());
    }

    private void rememberView() {
        rememberedZoom = zoom;
        rememberedPanX = panX;
        rememberedPanY = panY;
    }

    private boolean insideTree(double mouseX, double mouseY) {
        return mouseX >= viewLeft() && mouseX < viewRight() && mouseY >= viewTop() && mouseY < viewBottom();
    }

    private int viewLeft() {
        return leftPos + VIEW_X;
    }

    private int viewTop() {
        return topPos + VIEW_Y;
    }

    private int viewRight() {
        return viewLeft() + VIEW_WIDTH;
    }

    private int viewBottom() {
        return viewTop() + VIEW_HEIGHT;
    }

    private int scrollbarLeft() {
        return viewRight() + 6;
    }

    private boolean insideScrollbar(double mouseX, double mouseY) {
        return mouseX >= scrollbarLeft() && mouseX < scrollbarLeft() + SCROLLBAR_WIDTH
            && mouseY >= viewTop() && mouseY < viewBottom();
    }

    private int scrollbarHandleHeight() {
        float contentHeight = CONTENT_HEIGHT * zoom;
        if (contentHeight <= VIEW_HEIGHT) {
            return VIEW_HEIGHT;
        }
        return Math.max(26, Math.round(VIEW_HEIGHT * VIEW_HEIGHT / contentHeight));
    }

    private int scrollbarHandleTop() {
        int handleHeight = scrollbarHandleHeight();
        if (handleHeight >= VIEW_HEIGHT) {
            return viewTop();
        }
        return viewTop() + Math.round((VIEW_HEIGHT - handleHeight) * scrollProgress());
    }

    private float scrollProgress() {
        float minPanY = minPanY();
        float maxPanY = maxPanY();
        if (Math.abs(maxPanY - minPanY) < 0.001F) {
            return 0.0F;
        }
        return Mth.clamp((maxPanY - panY) / (maxPanY - minPanY), 0.0F, 1.0F);
    }

    private void updateScrollbar(double mouseY) {
        int handleHeight = scrollbarHandleHeight();
        if (handleHeight >= VIEW_HEIGHT) {
            panY = maxPanY();
            rememberView();
            return;
        }
        float progress = (float) ((mouseY - viewTop() - handleHeight / 2.0F) / (VIEW_HEIGHT - handleHeight));
        panY = Mth.lerp(Mth.clamp(progress, 0.0F, 1.0F), maxPanY(), minPanY());
        rememberView();
    }

    private float minPanX() {
        float contentWidth = CONTENT_WIDTH * zoom;
        if (contentWidth + TREE_PADDING * 2.0F <= VIEW_WIDTH) {
            return (VIEW_WIDTH - contentWidth) / 2.0F;
        }
        return VIEW_WIDTH - contentWidth - TREE_PADDING;
    }

    private float maxPanX() {
        float contentWidth = CONTENT_WIDTH * zoom;
        if (contentWidth + TREE_PADDING * 2.0F <= VIEW_WIDTH) {
            return (VIEW_WIDTH - contentWidth) / 2.0F;
        }
        return TREE_PADDING;
    }

    private float minPanY() {
        float contentHeight = CONTENT_HEIGHT * zoom;
        if (contentHeight + TREE_PADDING * 2.0F <= VIEW_HEIGHT) {
            return (VIEW_HEIGHT - contentHeight) / 2.0F;
        }
        return VIEW_HEIGHT - contentHeight - TREE_PADDING;
    }

    private float maxPanY() {
        float contentHeight = CONTENT_HEIGHT * zoom;
        if (contentHeight + TREE_PADDING * 2.0F <= VIEW_HEIGHT) {
            return (VIEW_HEIGHT - contentHeight) / 2.0F;
        }
        return TREE_PADDING;
    }

    private int nodeSize(SkillNode node) {
        return node.large() ? 38 : 24;
    }

    private void renderHelpOverlay(GuiGraphics graphics) {
        if (!showHelp) {
            return;
        }
        int ox = leftPos + 18;
        int oy = topPos + 40;
        int ow = 464;
        int oh = 230;

        // 반투명 다크 배경 및 골드 테두리 렌더링
        graphics.fill(ox, oy, ox + ow, oy + oh, 0xF50A0C0E);
        graphics.fill(ox, oy, ox + ow, oy + 1, 0xFF3C3525);
        graphics.fill(ox, oy + oh - 1, ox + ow, oy + oh, 0xFF3C3525);
        graphics.fill(ox, oy, ox + 1, oy + oh, 0xFF3C3525);
        graphics.fill(ox + ow - 1, oy, ox + ow, oy + oh, 0xFF3C3525);

        List<FormattedCharSequence> lines = getHelpLines();
        int maxLines = 21; // 뷰포트 높이 내 출력 최대 줄수
        int startY = oy + 10;
        int drawCount = Math.min(maxLines, lines.size() - helpScrollOffset);

        for (int i = 0; i < drawCount; i++) {
            int lineIndex = i + helpScrollOffset;
            graphics.drawString(font, lines.get(lineIndex), ox + 12, startY + i * 10, 0xFFE7DFC9, false);
        }

        // 스크롤바 렌더링
        if (lines.size() > maxLines) {
            int sbX = ox + ow - 6;
            int sbY = oy + 2;
            int sbH = oh - 4;
            graphics.fill(sbX, sbY, sbX + 4, sbY + sbH, 0xFF15191D);

            int handleH = Math.max(12, sbH * maxLines / lines.size());
            int maxOffset = lines.size() - maxLines;
            int handleY = sbY + (sbH - handleH) * helpScrollOffset / maxOffset;
            graphics.fill(sbX, handleY, sbX + 4, handleY + handleH, 0xFFFFD56A);
        }
    }

    private int getMaxHelpScroll() {
        List<FormattedCharSequence> lines = getHelpLines();
        int maxLines = 21;
        return Math.max(0, lines.size() - maxLines);
    }

    private List<FormattedCharSequence> getHelpLines() {
        List<Component> rawLines = new ArrayList<>();
        String jobId = menu.jobId();
        if ("farmer".equals(jobId)) {
            rawLines.add(Component.literal("§e[ 농부 스킬 상세 가이드 ]"));
            rawLines.add(Component.literal(""));
            rawLines.add(Component.literal("§61. 농산물 납품가 증가 I / II"));
            rawLines.add(Component.literal(" - 농산물 납품 획득 크레딧이 스킬 레벨당 1%씩 상시 증가합니다. (중첩 시 최대 +60%)"));
            rawLines.add(Component.literal("§62. 농부 경험치 / 수확량 증가"));
            rawLines.add(Component.literal(" - 농부 경험치가 레벨당 1% 증가하고, 작물 수확 시 레벨당 1.5%(최대 45%) 확률로 수확량이 1개 추가됩니다."));
            rawLines.add(Component.literal("§63. 씨앗 환급"));
            rawLines.add(Component.literal(" - 인간 트랙터 자동 재심기 시 레벨당 1.5% 확률(최대 45%)로 씨앗 소모를 방지합니다."));
            rawLines.add(Component.literal("§64. 성장 촉진"));
            rawLines.add(Component.literal(" - 주변 12m 안의 작물이 추가 성장 틱을 받을 확률이 레벨당 1.25%(최대 40%) 증가합니다."));
            rawLines.add(Component.literal("§65. 풍요로운 손길 (Lv.25 해금)"));
            rawLines.add(Component.literal(" - 수확 시 8%+레벨당 5%(최대 60%) 확률로 주변 작물이 추가 성장합니다."));
            rawLines.add(Component.literal("§66. 허수아비 (Lv.50 해금)"));
            rawLines.add(Component.literal(" - carved pumpkin, stick, leather, hay block으로 제작하여 농지에 설치합니다."));
            rawLines.add(Component.literal(" - 주변 18~45m 내 몬스터 스폰을 차단합니다. 레벨이 오를수록 반경이 증가합니다."));
            rawLines.add(Component.literal("§67. 인간 트랙터 (Lv.75 해금)"));
            rawLines.add(Component.literal(" - 붙어있는 같은 작물을 최대 45개까지 연쇄 수확하고 자동 재심기합니다."));
            rawLines.add(Component.literal(" - 재심기된 토지는 20초간 비옥해져 1초마다 작물이 추가 성장합니다."));
            rawLines.add(Component.literal("§68. 대지의 기적 (Lv.100 해금)"));
            rawLines.add(Component.literal(" - 주변 작물의 성장을 허용하고 주변 농지의 수분을 보충합니다."));
            rawLines.add(Component.literal(" - 수확 시 3%+레벨당 2% 확률로 '+' 등급 농산물을 생산하며 납품가가 최대 2배가 됩니다."));
        } else if ("fisher".equals(jobId)) {
            rawLines.add(Component.literal("§e[ 어부 스킬 상세 가이드 ]"));
            rawLines.add(Component.literal(""));
            rawLines.add(Component.literal("§61. 어부 납품가 증가 I / II"));
            rawLines.add(Component.literal(" - 수산물 납품 가격이 레벨당 1%씩 상시 증가합니다. (중첩 시 최대 +60%)"));
            rawLines.add(Component.literal("§62. 어부 경험치 / 낚싯대 효율 증가"));
            rawLines.add(Component.literal(" - 어부 경험치가 레벨당 1% 증가하고, 낚시 성공 시 레벨당 1.5%(최대 45%) 확률로 내구도를 소비하지 않습니다."));
            rawLines.add(Component.literal("§63. 입질 대기 시간 감소"));
            rawLines.add(Component.literal(" - 입질 대기 시간이 레벨에 따라 최대 10틱 감소합니다. 핫스팟은 +10틱, 어장은 +18틱 추가 감소합니다."));
            rawLines.add(Component.literal("§64. 더블 드랍 확률 증가"));
            rawLines.add(Component.literal(" - 낚시 성공 시 레벨당 1.5% 확률(최대 45%)로 동일 물고기를 1개 더 낚습니다."));
            rawLines.add(Component.literal("§65. 물결 읽기 (Lv.25 해금)"));
            rawLines.add(Component.literal(" - 주변 수면에 생기는 버블 '핫스팟'에 찌를 던지면 입질 시간이 추가로 10틱 감소합니다."));
            rawLines.add(Component.literal("§66. 숙련된 낚싯줄 (Lv.50 해금)"));
            rawLines.add(Component.literal(" - 낚시 성공 시 25%+레벨당 5%(최대 75%) 확률로 추가 보상을 낚습니다."));
            rawLines.add(Component.literal(" - 추가 보상은 물고기, 보물, 쓰레기 중에서 나오며 레벨이 오를수록 희귀 보상 비중이 증가합니다."));
            rawLines.add(Component.literal("§67. 미끼 뿌리기 (Lv.75 해금 / 마우스 5)"));
            rawLines.add(Component.literal(" - 낚시 성공 시 흐름 게이지를 10%+레벨당 4%(최대 50%) 충전합니다."));
            rawLines.add(Component.literal(" - 100% 충전 시 물 위에 최대 180초 어장을 만들며, 어장 안에서는 입질 시간이 +18틱 감소합니다."));
            rawLines.add(Component.literal("§68. 보물 찾기 (Lv.100 해금)"));
            rawLines.add(Component.literal(" - 낚시 성공 시 6%+레벨당 5.8%(최대 35%) 확률로 심해 크레이트를 낚습니다."));
            rawLines.add(Component.literal(" - 레벨이 오를수록 높은 등급 크레이트 확률이 증가하며, 개봉 시 1~3개 보상을 획득합니다."));
        } else if ("miner".equals(jobId)) {
            rawLines.add(Component.literal("§e[ 광부 스킬 상세 가이드 ]"));
            rawLines.add(Component.literal(""));
            rawLines.add(Component.literal("§61. 판매/납품가 증가 I / II"));
            rawLines.add(Component.literal(" - 광물 및 원석 납품 가격이 레벨당 1%씩 상시 증가합니다. (중첩 시 최대 +60%)"));
            rawLines.add(Component.literal("§62. 광부 경험치 / 채굴 속도 증가"));
            rawLines.add(Component.literal(" - 광부 경험치가 레벨당 1% 증가하고, 채굴 속도가 레벨당 2%씩 증가합니다."));
            rawLines.add(Component.literal(" - 레벨에 따라 성급함 I~III 효과가 자동 적용됩니다."));
            rawLines.add(Component.literal("§63. 더블 드랍 확률 증가"));
            rawLines.add(Component.literal(" - 천연 광석 채광 시 기본 8% + 레벨당 1.5% 확률(최대 60%)로 원석을 2배 획득합니다."));
            rawLines.add(Component.literal("§64. 광물 사냥꾼"));
            rawLines.add(Component.literal(" - 일반 암석(돌, 심층암, 네더랙 등) 파괴 시 레벨당 0.5% 확률(최대 5%)로 무작위 광물을 추가 획득합니다."));
            rawLines.add(Component.literal("§65. 내구도 효율 증가"));
            rawLines.add(Component.literal(" - 채광 시 기본 10% + 레벨당 1.5% 확률(최대 60%)로 곡괭이 내구도 소모를 되돌립니다."));
            rawLines.add(Component.literal("§66. 우월한 신체 (Lv.25 해금 / 마우스 5)"));
            rawLines.add(Component.literal(" - 직업 스킬 키 1로 ON/OFF하며, 채광 시 주변 동일 광석 또는 암석을 추가로 연쇄 채굴합니다."));
            rawLines.add(Component.literal(" - 레벨 비례 채굴량이 증가하며, 광석은 최대 6블록, 암석은 최대 4블록까지 추가 채굴됩니다."));
            rawLines.add(Component.literal("§67. 선지자의 보물 (Lv.75 해금)"));
            rawLines.add(Component.literal(" - 광석 채굴 시 0.05%+레벨당 0.028%(최대 1%) 확률로 강화의 보석을 발견합니다."));
            rawLines.add(Component.literal(" - 암석 채굴 시에도 낮은 확률(광석 확률의 10%)로 같은 보상이 발동합니다."));
            rawLines.add(Component.literal("§68. 개안 (Lv.100 해금 / 마우스 4)"));
            rawLines.add(Component.literal(" - 직업 스킬 키 2로 ON/OFF하며, 주변 8+레벨*2m(최대 28m) 내 광석을 벽 너머로 표시합니다."));
            rawLines.add(Component.literal(" - 활성 중 매초 최대 체력의 5% 피해를 받고 회복이 차단되며, 체력이 부족하면 자동 해제됩니다."));
        } else if ("cook".equals(jobId)) {
            rawLines.add(Component.literal("§e[ 요리사 스킬 상세 가이드 ]"));
            rawLines.add(Component.literal(""));
            rawLines.add(Component.literal("§61. 요리사 납품가 증가 I / II"));
            rawLines.add(Component.literal(" - 가공 음식 및 요리 납품 단가가 레벨당 1%씩 상시 증가합니다. (최대 +60%)"));
            rawLines.add(Component.literal("§62. 요리사 경험치 증가"));
            rawLines.add(Component.literal(" - 요리 조리 시 얻는 직업 경험치가 레벨당 1%씩 상시 증폭됩니다."));
            rawLines.add(Component.literal("§63. 요리 조율 / 재료 보존"));
            rawLines.add(Component.literal(" - 요리 완성 시 레벨당 1.5%(최대 45%) 확률로 완성 음식을 1개 더 얻습니다."));
            rawLines.add(Component.literal(" - 재료 보존 비법은 같은 확률로 제작 재료 소모를 되돌립니다."));
            rawLines.add(Component.literal("§64. 손맛 (Lv.25 해금)"));
            rawLines.add(Component.literal(" - 직접 조리한 음식의 회복량/포만감이 레벨당 5%(최대 50%) 증가합니다."));
            rawLines.add(Component.literal(" - 완성 시 15%+레벨당 5%(최대 65%) 확률로 명품 요리가 되어 섭취 시 전투 버프를 줍니다."));
            rawLines.add(Component.literal("§65. 따뜻한 한 끼 (Lv.50 해금)"));
            rawLines.add(Component.literal(" - 조리된 음식으로 허기를 100% 채우면 신속/성급함/재생/저항 중 1종을 획득합니다."));
            rawLines.add(Component.literal(" - 지속시간은 레벨당 60초(최대 10분), 버프 등급은 4/7/10레벨에서 상승합니다."));
            rawLines.add(Component.literal("§66. 숙성 (Lv.75 해금)"));
            rawLines.add(Component.literal(" - 직접 조리한 음식을 인벤토리에 보관하면 60초마다 숙성도가 오릅니다."));
            rawLines.add(Component.literal(" - 섭취 시 숙성도 단계당 회복량이 8% 증가하며, 최대 보너스는 80%입니다."));
            rawLines.add(Component.literal("§67. 나만의 레시피 (Lv.100 해금 / 마우스 4)"));
            rawLines.add(Component.literal(" - 직업 스킬 키 2로 레시피 조율 창을 열고 6대 특수 버프 중 1~2개를 요리에 각인합니다."));
        } else if ("hunter".equals(jobId)) {
            rawLines.add(Component.literal("§e[ 사냥꾼 스킬 상세 가이드 ]"));
            rawLines.add(Component.literal(""));
            rawLines.add(Component.literal("§61. 사냥꾼 납품가 증가 I / II"));
            rawLines.add(Component.literal(" - 전리품 납품 시 획득 크레딧이 레벨당 1%씩 상시 증가합니다. (최대 +60%)"));
            rawLines.add(Component.literal("§62. 사냥 루틴"));
            rawLines.add(Component.literal(" - 사냥꾼 경험치가 레벨당 1% 증가하고, 처치 드랍이 레벨당 1.5%(최대 45%) 확률로 1개씩 증가합니다."));
            rawLines.add(Component.literal("§63. 병기 최적화 / 급소 파악"));
            rawLines.add(Component.literal(" - 처치 시 레벨당 1.25%(최대 35%) 확률로 모든 전리품을 한 번 더 복사합니다."));
            rawLines.add(Component.literal("§64. 추적자의 감각 (Lv.25 해금 / 마우스 5)"));
            rawLines.add(Component.literal(" - 허기를 소모해 주변 12~42m 몬스터와 동물을 윤곽선/파티클로 감지합니다."));
            rawLines.add(Component.literal(" - 처치 시 이동속도 I을 4초+레벨*2초 동안 얻습니다."));
            rawLines.add(Component.literal("§65. 갈증 (Lv.50 해금)"));
            rawLines.add(Component.literal(" - 공격 시 10%+레벨당 5%(최대 60%) 확률로 출혈을 걸어 2초마다 피해와 둔화를 줍니다."));
            rawLines.add(Component.literal(" - 처치 시 최대 60% 확률로 힘 I을 3초+레벨*2초(최대 23초) 얻습니다."));
            rawLines.add(Component.literal("§66. 사냥감의 표식 (Lv.75 해금 / 마우스 4)"));
            rawLines.add(Component.literal(" - 감지된 대상을 표식 처리해 받는 피해를 최대 60% 늘리고 처치 보상을 2배로 받습니다."));
            rawLines.add(Component.literal(" - 일반 처치 시에도 저항 I을 5초+레벨*3초 동안 얻습니다."));
            rawLines.add(Component.literal("§67. 먹이사슬의 정점 (Lv.100 해금)"));
            rawLines.add(Component.literal(" - 추적 중 3초마다 바뀌는 약점 방향을 맞히면 1.4+레벨*0.12배 피해, 회복, 신속 III를 얻습니다."));
        } else if ("engineer".equals(jobId)) {
            rawLines.add(Component.literal("§e[ 공학자 스킬 상세 가이드 ]"));
            rawLines.add(Component.literal(""));
            rawLines.add(Component.literal("§61. 공업 납품가 증가 I / II"));
            rawLines.add(Component.literal(" - Create 기계 부품 및 가공 보석류 납품 단가가 레벨당 1%씩 증가합니다. (중첩 시 최대 +60%)"));
            rawLines.add(Component.literal("§62. 회전 속도 학습 / 톱니바퀴 조율 / 정밀 조립법"));
            rawLines.add(Component.literal(" - 공학 경험치가 레벨당 1% 증가하고, 공학 특수 보상 발동률이 각 노드 레벨당 1.5%씩 증가합니다."));
            rawLines.add(Component.literal("§63. 자원 압축 (Lv.25 해금 / 마우스 5)"));
            rawLines.add(Component.literal(" - 단축키 입력 시 암석/광물 64개를 압축해 보석류 및 강화 보석을 일정 확률로 획득합니다."));
            rawLines.add(Component.literal(" - 스킬 레벨이 올라갈수록 더 좋은 보석이 나올 가중치가 증가합니다."));
            rawLines.add(Component.literal("§64. 정밀 가동 (Lv.50 해금)"));
            rawLines.add(Component.literal(" - 본인이 건설 가능한 산업 구역의 Create 계열 기계 주변에서 작업 효율이 극대화됩니다."));
            rawLines.add(Component.literal(" - 주변 약 8m 수평/4m 수직 범위의 산업 기계를 감지하며, 기계 수에 따라 공장 관리 보너스도 얻습니다."));
            rawLines.add(Component.literal(" - 성급함 II 및 신속 I 버프를 얻습니다. (5레벨 이상 마스터 시 성급함 III 및 신속 II)"));
            rawLines.add(Component.literal(" - 매초 렌치/곡괭이 내구도를 수리하고 2초마다 레벨 비례 확률로 허기와 수분을 회복합니다."));
            rawLines.add(Component.literal("§65. 공정 최적화 (Lv.75 해금)"));
            rawLines.add(Component.literal(" - Create 기계 부품 및 TACZ 탄약 제작 시 레벨당 10% 확률로 무작위 재료 1개를 돌려받습니다."));
            rawLines.add(Component.literal(" - TACZ 총기 사격 시 레벨당 3% 확률로 탄약을 절약하며, 드론 가동 중에는 +30%가 추가됩니다."));
            rawLines.add(Component.literal("§66. 영구 기관: 오토 스크랩 드론 (Lv.100 해금 / 마우스 4)"));
            rawLines.add(Component.literal(" - 단축키로 드론을 가동/회수합니다. 작동에는 §e동력(0~100%)§f이 필요합니다."));
            rawLines.add(Component.literal(" §e[드론 이동 \\u0026 탑승 비행]"));
            rawLines.add(Component.literal("   - 드론 우클릭 시 탑승하고, 웅크리기(Shift) + 우클릭 시 제어창을 엽니다."));
            rawLines.add(Component.literal("   - W/S/A/D: 이동, Space: 상승, Shift: 하마. (탑승 비행 중 20틱당 1% 동력 소모)"));
            rawLines.add(Component.literal(" §e[동력 충전 방식]"));
            rawLines.add(Component.literal("   - 무선 발전: 산업 구역 Create 기계 주변에 있을 때 0.5초마다 +2% 충전."));
            rawLines.add(Component.literal("   - 태양 발전: 낮 시간대에 실외 하늘 노출 시 100틱당 1% 충전."));
            rawLines.add(Component.literal("   - 자연 대기: 비전투 및 비탑승 상태로 대기 시 40틱당 1% 복구."));
            rawLines.add(Component.literal("   - 렌치 충전: 렌치를 들고 드론 우클릭 시 동력 20% 긴급 복구."));
            rawLines.add(Component.literal("   - 연료 충전: Shift+드론 우클릭 제어창의 [동력 발전]에서 수동 충전 또는 자동 발전 연료 지정."));
            rawLines.add(Component.literal("     * 연료 효율: 조약돌 1%, 석탄 5%, 블록 45%, 보석 80%, Create 톱니바퀴 40%."));
            rawLines.add(Component.literal(" §e[드론 핵심 업그레이드] (우클릭 제어창에서 크레딧으로 개방)"));
            rawLines.add(Component.literal("   - 송신기(Transmitter): 송수신 원격 제어/공급 유효 반경 범위 확장."));
            rawLines.add(Component.literal("     * 안테나 링크 등록: Shift + 빈손 우클릭으로 보관함/아이템 핸들러 블록을 등록."));
            rawLines.add(Component.literal("   - 부스터(Booster): 탑승 비행 중 달리기(Sprint) 시 강력 가속 기동(15틱 지속). 레벨당 가속도 증가 및 동력 소모 대폭 감소 (10% -> 4%)."));
            rawLines.add(Component.literal("   - 센서(Sensor): 지원 사격의 적 자동 탐지 범위 증가 (기본 12m, 레벨당 +5m)."));
            rawLines.add(Component.literal("   - 그랩 장치(Grabber): 주변 드롭 아이템 무선 흡입 범위 증가 (기본 12m, 레벨당 +4m)."));
            rawLines.add(Component.literal(" §e[드론 액티브 및 자동 기능]"));
            rawLines.add(Component.literal("   - 지원 사격: 적 감지 시 장착된 TACZ 총기 또는 기본 레이저로 자동 버스트 사격."));
            rawLines.add(Component.literal("   - 나노 복구: 0.5초마다 착용 장비 내구도 1% 수리 (-1% 동력) 및 체력 손실 시 2초마다 1 HP 치유 (-2% 동력)."));
            rawLines.add(Component.literal("   - 동력 쉴드: 5초마다 최대 체력 30% 한도 안에서 흡수 보호막을 보충합니다 (-4% 동력)."));
            rawLines.add(Component.literal("   - 진공 흡입: 드롭 아이템 자동 무선 회수 (-0.2%/개 동력)."));
            rawLines.add(Component.literal("   - 고철 분해/매각: 드론 우클릭 제어창의 [장비 분해]를 통해 쓰지 않는 아이템 분해/매각 가능."));
        }

        List<FormattedCharSequence> formatted = new ArrayList<>();
        for (Component raw : rawLines) {
            formatted.addAll(font.split(raw, 430));
        }
        return formatted;
    }
}
