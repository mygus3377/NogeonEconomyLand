package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.TraderActionLine;
import com.nogeon.economyland.menu.TraderActionMenu;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.TraderActionPacket;
import com.nogeon.economyland.player.SocialClass;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TraderActionScreen extends AbstractContainerScreen<TraderActionMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int DEFAULT_HEIGHT = 226;
    private static final int SMITH_HEIGHT = 268;
    private static final int GAMBLER_HEIGHT = 250;
    private static final int GACHA_HEIGHT = 282;
    private static final int LAND_HEIGHT = 260;

    private final List<Button> actionButtons = new ArrayList<>();
    private LandTab landTab = LandTab.DEEDS;
    private Button lotteryInfoCloseButton;
    private boolean lotteryInfoOpen;

    public TraderActionScreen(TraderActionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 340;
        imageHeight = isLandMenu() ? LAND_HEIGHT : (isGamblerMenu() || "lottery".equals(menu.kindId())) ? GAMBLER_HEIGHT : isGachaMenu() ? GACHA_HEIGHT : isSmithMenu() ? SMITH_HEIGHT : DEFAULT_HEIGHT;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void init() {
        super.init();
        actionButtons.clear();
        if (isLandMenu()) {
            addRenderableWidget(HextechButton.hextechBuilder(Component.translatable(LandTab.DEEDS.translationKey), button -> setLandTab(LandTab.DEEDS))
                .bounds(leftPos + 22, topPos + 82, 84, 20)
                .build());
            addRenderableWidget(HextechButton.hextechBuilder(Component.translatable(LandTab.CLASSES.translationKey), button -> setLandTab(LandTab.CLASSES))
                .bounds(leftPos + 112, topPos + 82, 84, 20)
                .build());
        }
        for (int index = 0; index < visibleCapacity(); index++) {
            final int slot = index;
            int btnY = (isGamblerMenu() || !isLandMenu()) ? 4 : -3;
            int btnW = (isGamblerMenu() || !isLandMenu()) ? 50 : 46;
            int btnH = (isGamblerMenu() || !isLandMenu()) ? 20 : 18;
            int btnX = (isGamblerMenu() || !isLandMenu()) ? 266 : 270;
            Button button = addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.execute"),
                pressed -> runVisibleLine(slot))
                .bounds(leftPos + btnX, topPos + rowY(index) + btnY, btnW, btnH)
                .build());
            actionButtons.add(button);
        }
        lotteryInfoCloseButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("X"), button -> {
            lotteryInfoOpen = false;
            refreshActionButtons();
        }).bounds(leftPos + 290, topPos + 52, 22, 18).build());
        lotteryInfoCloseButton.visible = false;
        refreshActionButtons();
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

        // 리스트 챔버 격자 프레임
        graphics.fill(x + 16, y + 40, x + imageWidth - 16, y + imageHeight - 28, 0xFF0E1311);
        drawCustomBorder(graphics, x + 16, y + 40, imageWidth - 32, imageHeight - 68, 0xFF1B2C27);

        // 도박사 또는 복권 상인 메뉴일 때 각 게임 항목의 서브 판넬 상자 그리기
        if (isGamblerMenu() || "lottery".equals(menu.kindId())) {
            for (int i = 0; i < 3; i++) {
                int ry = y + rowY(i);
                // 연한 그린-블랙 서브 챔버 배경 fill
                graphics.fill(x + 20, ry - 4, x + imageWidth - 20, ry + 42, 0xFF0A0F0D);
                // 은은한 다크 그린 테두리
                drawCustomBorder(graphics, x + 20, ry - 4, imageWidth - 40, 46, 0xFF1B2C27);
            }
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
        if (lotteryInfoOpen) {
            renderLotteryInfo(graphics);
            lotteryInfoCloseButton.render(graphics, mouseX, mouseY, partialTick);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (lotteryInfoOpen) {
            return lotteryInfoCloseButton.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFFE8E1C4);
        graphics.drawString(font, hintText(), 22, 28, 0xFF98A49C, false);

        if (isLandMenu()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.land_home_section"), 24, 46, 0xFF8FBF9B, false);
            graphics.fill(24, 56, 318, 57, 0xFF2E4533);
            graphics.drawString(font, Component.translatable(landTab.translationKey), 24, 104, 0xFF8FBF9B, false);
            graphics.fill(24, 114, 318, 115, 0xFF2E4533);
        }

        List<TraderActionLine> lines = visibleLines();
        for (int index = 0; index < lines.size(); index++) {
            TraderActionLine line = lines.get(index);
            int y = rowY(index);
            SocialClass targetClass = targetClass(line);
            boolean locked = isLocked(targetClass);
            graphics.drawString(font, Component.translatable(line.labelKey()), 24, y, locked ? 0xFF313131 : 0xFFE8E1C4, false);
            
            Component desc = locked
                ? Component.translatable("gui.nogeon_economy_land.class_locked", Component.translatable(previousClass(targetClass).translationKey()))
                : Component.translatable(line.descriptionKey());
            int maxWidth = 235;
            List<net.minecraft.util.FormattedCharSequence> splitLines = font.split(desc, maxWidth);
            int currentY = y + 10;
            int maxDescriptionLines = isSmithMenu() ? 2 : splitLines.size();
            for (int lineIndex = 0; lineIndex < Math.min(maxDescriptionLines, splitLines.size()); lineIndex++) {
                net.minecraft.util.FormattedCharSequence seq = splitLines.get(lineIndex);
                graphics.drawString(font, seq, 24, currentY, locked ? 0xFF252525 : 0xFF98A49C, false);
                currentY += 9;
            }
            
            if (line.price() > 0) {
                graphics.drawString(font, CREDIT_FORMAT.format(stakeAmount(line)) + " C", 198, y, locked ? 0xFF313131 : 0xFFFFD56A, false);
            }
        }
    }

    private boolean isLandMenu() {
        return "land".equals(menu.kindId());
    }

    private boolean isGamblerMenu() {
        return "gambler".equals(menu.kindId());
    }

    private boolean isGachaMenu() {
        return "gacha".equals(menu.kindId());
    }

    private boolean isSmithMenu() {
        return "smith".equals(menu.kindId());
    }

    private Component hintText() {
        if (isLandMenu()) {
            return Component.translatable("gui.nogeon_economy_land.land_action_hint");
        }
        if (isGamblerMenu()) {
            return Component.translatable("gui.nogeon_economy_land.gambler_action_hint");
        }
        return Component.translatable("gui.nogeon_economy_land.trader_action_hint");
    }

    private void setLandTab(LandTab value) {
        landTab = value;
        refreshActionButtons();
    }

    private void runVisibleLine(int slot) {
        List<TraderActionLine> lines = visibleLines();
        if (slot < 0 || slot >= lines.size()) {
            return;
        }
        if ("lottery_info".equals(lines.get(slot).actionId())) {
            lotteryInfoOpen = true;
            refreshActionButtons();
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new TraderActionPacket(menu.kindId(), menu.traderDatabaseId(), lines.get(slot).actionId(), actionStake(lines.get(slot))));
    }

    private void refreshActionButtons() {
        List<TraderActionLine> lines = visibleLines();
        for (int index = 0; index < actionButtons.size(); index++) {
            Button button = actionButtons.get(index);
            button.visible = !lotteryInfoOpen && index < lines.size();
            button.active = !lotteryInfoOpen && index < lines.size() && canRun(lines.get(index));
            int btnY = (isGamblerMenu() || !isLandMenu()) ? 4 : -3;
            int btnX = (isGamblerMenu() || !isLandMenu()) ? 266 : 270;
            button.setPosition(leftPos + btnX, topPos + rowY(index) + btnY);
        }
        if (lotteryInfoCloseButton != null) {
            lotteryInfoCloseButton.visible = lotteryInfoOpen;
            lotteryInfoCloseButton.active = lotteryInfoOpen;
            lotteryInfoCloseButton.setPosition(leftPos + 290, topPos + 52);
        }
    }

    private List<TraderActionLine> visibleLines() {
        if (!isLandMenu()) {
            return menu.lines();
        }

        List<TraderActionLine> visible = new ArrayList<>();
        TraderActionLine utilityLine = null;
        List<TraderActionLine> landDeeds = new ArrayList<>();
        List<TraderActionLine> socialClasses = new ArrayList<>();
        for (TraderActionLine line : menu.lines()) {
            if (line.actionId().endsWith("_deed")) {
                landDeeds.add(line);
            } else if (line.actionId().startsWith("class_")) {
                socialClasses.add(line);
            } else {
                utilityLine = line;
            }
        }

        if (utilityLine != null) {
            visible.add(utilityLine);
        }
        visible.addAll(landTab == LandTab.DEEDS ? landDeeds : socialClasses);
        return visible;
    }

    private int visibleCapacity() {
        return isLandMenu() ? 5 : menu.lines().size();
    }

    private int rowY(int index) {
        if (!isLandMenu()) {
            if (isGamblerMenu() || "lottery".equals(menu.kindId())) {
                return 64 + index * 52;
            }
            if (isGachaMenu()) {
                return 58 + index * 44;
            }
            if (isSmithMenu()) {
                return 54 + index * 30;
            }
            return 54 + index * 24;
        }
        return index == 0 ? 60 : 120 + (index - 1) * 24;
    }

    private long stakeAmount(TraderActionLine line) {
        return line.price();
    }

    private long actionStake(TraderActionLine line) {
        return isGamblerMenu() ? 0L : line.price();
    }

    private void renderLotteryInfo(GuiGraphics graphics) {
        int x = leftPos + 28;
        int y = topPos + 48;
        int w = 284;
        int h = 142;
        graphics.fill(x, y, x + w, y + h, 0xFF0B0F0E);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF1A201E);
        drawCustomBorder(graphics, x, y, w, h, 0xFF00FFCC);
        graphics.drawCenteredString(font, Component.literal("복권 확률/보상표"), x + w / 2, y + 10, 0xFFFFD56A);
        graphics.drawString(font, Component.literal("구매 가격: " + CREDIT_FORMAT.format(1000) + " C / 1장"), x + 14, y + 26, 0xFFE8E1C4, false);
        int row = y + 44;
        lotteryRow(graphics, row, "1등", "0.01%", "1/10,000", menu.lotteryJackpot1());
        lotteryRow(graphics, row + 14, "2등", "0.05%", "1/2,000", menu.lotteryJackpot2());
        lotteryRow(graphics, row + 28, "3등", "0.20%", "1/500", 1_000_000L);
        lotteryRow(graphics, row + 42, "4등", "1.00%", "1/100", 200_000L);
        lotteryRow(graphics, row + 56, "5등", "3.00%", "-", 50_000L);
        lotteryRow(graphics, row + 70, "장려상", "5.00%", "-", 10_000L);
        graphics.drawString(font, Component.literal("낙첨/꽝: 불운의 증표 1개 지급"), x + 14, row + 88, 0xFF98A49C, false);
    }

    private void lotteryRow(GuiGraphics graphics, int y, String rank, String chance, String odds, long reward) {
        graphics.drawString(font, rank, leftPos + 42, y, 0xFFE8E1C4, false);
        graphics.drawString(font, chance, leftPos + 94, y, 0xFF98E6D7, false);
        graphics.drawString(font, odds, leftPos + 150, y, 0xFF98A49C, false);
        graphics.drawString(font, CREDIT_FORMAT.format(reward) + " C", leftPos + 216, y, 0xFFFFD56A, false);
    }

    private boolean canRun(TraderActionLine line) {
        SocialClass targetClass = targetClass(line);
        return targetClass == null || currentClass().ordinal() + 1 == targetClass.ordinal();
    }

    private boolean isLocked(SocialClass targetClass) {
        return targetClass != null && currentClass().ordinal() + 1 < targetClass.ordinal();
    }

    private SocialClass currentClass() {
        return SocialClass.byId(menu.socialClassId());
    }

    private static SocialClass targetClass(TraderActionLine line) {
        return switch (line.actionId()) {
            case "class_middle" -> SocialClass.MIDDLE;
            case "class_rich" -> SocialClass.RICH;
            case "class_tycoon" -> SocialClass.TYCOON;
            case "class_billionaire" -> SocialClass.BILLIONAIRE;
            default -> null;
        };
    }

    private static SocialClass previousClass(SocialClass socialClass) {
        return SocialClass.values()[Math.max(0, socialClass.ordinal() - 1)];
    }

    private enum LandTab {
        DEEDS("gui.nogeon_economy_land.land_section_deeds"),
        CLASSES("gui.nogeon_economy_land.land_section_classes");

        private final String translationKey;

        LandTab(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
