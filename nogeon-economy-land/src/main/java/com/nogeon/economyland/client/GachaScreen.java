package com.nogeon.economyland.client;

import com.nogeon.economyland.menu.GachaMenu;
import com.nogeon.economyland.menu.GachaCategory;
import com.nogeon.economyland.network.GachaCelebratePacket;
import com.nogeon.economyland.network.GachaRollPacket;
import com.nogeon.economyland.network.ModNetwork;
import com.nogeon.economyland.network.OpenGachaStoragePacket;
import com.nogeon.economyland.state.GachaRewardResult;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import com.nogeon.economyland.client.HextechButton;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class GachaScreen extends AbstractContainerScreen<GachaMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int BUTTON_WIDTH = 36;
    private static final int BUTTON_HEIGHT = 20;
    private static final int CATEGORY_WIDTH = 52;
    private static final int RESULTS_PER_ROW = 5;

    private final RandomSource random = RandomSource.create();
    private final List<HextechButton> countButtons = new ArrayList<>();
    private final List<HextechButton> categoryButtons = new ArrayList<>();
    private HextechButton rollButton;
    private HextechButton reRollButton;
    private String selectedCategoryId;
    private int selectedCount;
    private int animationTicks;
    private int revealedCount;
    private int suspenseTicks;
    private boolean celebrationSent;
    private boolean[] revealedSlots = new boolean[0];
    private List<Integer> revealOrder = List.of();
    private int lastRevealedIndex = -1;
    private int revealFlashTicks = 0;
    private int screenFlashTicks = 0;

    public GachaScreen(GachaMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 392;
        imageHeight = 338;
        inventoryLabelY = 10_000;
        selectedCategoryId = menu.categoryId();
        selectedCount = menu.selectedCount();
    }

    @Override
    protected void init() {
        super.init();
        countButtons.clear();
        categoryButtons.clear();
        selectedCategoryId = menu.categoryId();
        selectedCount = menu.selectedCount();
        animationTicks = 0;
        suspenseTicks = 0;
        celebrationSent = false;
        resetRevealState();
 
        int startX = leftPos + 24;
        int startY = topPos + 90;
        for (int count = 1; count <= 10; count++) {
            int index = count - 1;
            int row = index / 5;
            int column = index % 5;
            int selectedValue = count;
            HextechButton button = HextechButton.hextechBuilder(Component.literal(String.valueOf(selectedValue)), ignored -> selectCount(selectedValue))
                .bounds(startX + column * (BUTTON_WIDTH + 4), startY + row * (BUTTON_HEIGHT + 8), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
            countButtons.add(addRenderableWidget(button));
        }
 
        int categoryY = topPos + 148;
        int categoryX = leftPos + 24;
        for (GachaCategory category : GachaCategory.values()) {
            HextechButton button = HextechButton.hextechBuilder(Component.translatable(category.translationKey()), ignored -> selectCategory(category.id()))
                .bounds(categoryX, categoryY, CATEGORY_WIDTH, 20)
                .build();
            categoryButtons.add(addRenderableWidget(button));
            categoryX += CATEGORY_WIDTH + 2;
        }
 
        rollButton = addRenderableWidget(HextechButton.hextechBuilder(rollLabel(), button -> rollSelectedCount())
            .bounds(leftPos + 258, topPos + 132, 104, 24)
            .build());
            
        addRenderableWidget(HextechButton.hextechBuilder(Component.literal("보관함 회수"),
            button -> ModNetwork.CHANNEL.sendToServer(new OpenGachaStoragePacket()))
            .bounds(leftPos + 24, topPos + 298, 100, 20)
            .build());
            
        reRollButton = addRenderableWidget(HextechButton.hextechBuilder(Component.literal("다시 뽑기"),
            button -> rollSelectedCount())
            .bounds(leftPos + 146, topPos + 298, 100, 20)
            .build());
            
        addRenderableWidget(HextechButton.hextechBuilder(Component.translatable("gui.nogeon_economy_land.gacha_close"), button -> onClose())
            .bounds(leftPos + 268, topPos + 298, 100, 20)
            .danger(true)
            .build());
            
        updateButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (reRollButton != null) {
            reRollButton.visible = menu.hasResults();
            reRollButton.active = menu.hasResults() && (revealedCount >= revealOrder.size());
        }
        if (revealFlashTicks > 0) {
            revealFlashTicks--;
        }
        if (screenFlashTicks > 0) {
            screenFlashTicks--;
        }
        if (!menu.hasResults()) {
            animationTicks = 0;
            resetRevealState();
            return;
        }

        animationTicks++;
        if (revealedCount >= revealOrder.size()) {
            return;
        }

        if (hasFinaleSuspense()) {
            suspenseTicks++;
            playSuspensePulse();
            if (suspenseTicks < 28) {
                return;
            }
        }

        int interval = revealedCount == menu.results().size() - 1 ? 9 : 5;
        if (animationTicks % interval != 0) {
            return;
        }

        int revealIndex = revealOrder.get(revealedCount);
        GachaRewardResult result = menu.results().get(revealIndex);
        playRevealSound(result);
        revealedSlots[revealIndex] = true;
        revealedCount++;
        
        lastRevealedIndex = revealIndex;
        revealFlashTicks = 8;
        if (result.jackpot() || result.rarity() >= 2) {
            screenFlashTicks = 5;
        }
        
        suspenseTicks = 0;
        if (revealedCount == revealOrder.size() && menu.hasCelebrationToken() && !celebrationSent) {
            ModNetwork.CHANNEL.sendToServer(new GachaCelebratePacket(menu.celebrationToken()));
            celebrationSent = true;
        }
    }

    private void drawCustomBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        
        int themeNeonColor = switch (menu.actionId()) {
            case "gacha_middle" -> 0xFF00FF88; // 중급: 에메랄드 네온
            case "gacha_high" -> 0xFF00C8FF;   // 상급: 사파이어 네온
            case "gacha_legend" -> 0xFFFF8C00; // 전설: 파이어 오렌지 네온
            default -> 0xFF00FFCC;             // 일반: 시안 네온
        };
        
        int secondaryNeonColor = switch (menu.actionId()) {
            case "gacha_middle" -> 0xFF8ED4A2;
            case "gacha_high" -> 0xFF7EB5FF;
            case "gacha_legend" -> 0xFFFFBE5C;
            default -> 0xFF98D7AA;
        };

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFA0B0F0E); // 칠흑
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFA141918); // 미드나이트 그레이 내벽
        
        graphics.fill(x, y, x + imageWidth, y + 1, themeNeonColor); // 상단 테마 네온
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, secondaryNeonColor); // 하단 보조 네온
        graphics.fill(x, y, x + 1, y + imageHeight, themeNeonColor); // 좌측
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, secondaryNeonColor); // 우측

        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 24, 0xFF0E1311); // 타이틀 바 칠흑
        drawCustomBorder(graphics, x + 1, y + 1, imageWidth - 2, 23, accentColor(0xFF1B2C27)); // 타이틀 바 하단 경계선

        framedPanel(graphics, x + 18, y + 38, x + imageWidth - 18, y + 78, accentColor(0xFF1B2C27), 0xFF0E1311);
        framedPanel(graphics, x + 18, y + 82, x + 242, y + 176, accentColor(0xFF1B2C27), 0xFF0E1311);
        framedPanel(graphics, x + 248, y + 82, x + imageWidth - 18, y + 176, themeNeonColor, 0xFF0E1311);
        framedPanel(graphics, x + 18, y + 188, x + imageWidth - 18, y + 292, accentColor(0xFF1B2C27), 0xFF0E1311);
        framedPanel(graphics, x + 18, y + 296, x + imageWidth - 18, y + 320, accentColor(0xFF1B2C27), 0xFF0E1311);
        
        graphics.fill(x + 24, y + 186, x + imageWidth - 24, y + 188, themeNeonColor);

        drawMachineGlow(graphics, x + 262, y + 88, x + imageWidth - 24, y + 170);
        drawFeaturedResult(graphics, x + 248, y + 82, x + imageWidth - 18, y + 176);
        drawResults(graphics, x + 24, y + 198);
        
        if (screenFlashTicks > 0) {
            int alpha = (int) (screenFlashTicks / 5.0f * 140);
            graphics.fill(x, y, x + imageWidth, y + imageHeight, (alpha << 24) | 0xFFFFFF);
        }
    }

    private void framedPanel(GuiGraphics graphics, int left, int top, int right, int bottom, int border, int fill) {
        graphics.fill(left, top, right, bottom, 0xFF0A0C0A); // 그림자 테두리
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, border); // 네온 보더
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, fill); // 칠흑 속배경
        
        graphics.fill(left + 2, top + 2, right - 2, top + 3, 0x33FFFFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderResultTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFFF2E3BC);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gacha_machine_hint"), 24, 28, 0xFF9FA79A, false);
        graphics.drawString(font, Component.translatable(tierTitleKey()), 24, 49, 0xFFEADCB8, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gacha_count").append(": ").append(String.valueOf(selectedCount)), 24, 63, 0xFFE8E1C4, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gacha_price_each").append(": ").append(CREDIT_FORMAT.format(menu.pricePerRoll())).append(" C"), 216, 49, 0xFFFFD56A, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gacha_total_price").append(": ").append(CREDIT_FORMAT.format(menu.pricePerRoll() * selectedCount)).append(" C"), 216, 63, 0xFFE8E1C4, false);
        if (!menu.hasResults()) {
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gacha_select_count"), 24, 84, 0xFFE8E1C4, false);
            graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gacha_select_category"), 24, 138, 0xFFE8E1C4, false);
            drawClippedText(graphics, Component.translatable("gui.nogeon_economy_land.gacha_selected_category")
                .append(": ").append(Component.translatable(selectedCategory().translationKey())), 254, 94, 108, 0xFFE8E1C4);
            drawClippedText(graphics, Component.translatable("gui.nogeon_economy_land.gacha_roll_summary", selectedCount), 254, 108, 108, 0xFFE8E1C4);
        }
        drawClippedText(graphics, statusLabel(), 254, 162, 108, accentColor(0xFF98D7AA));
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.gacha_results"), 24, 180, 0xFFEADCB8, false);
    }

    private void drawClippedText(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), width), x, y, color, false);
    }

    private void selectCount(int count) {
        selectedCount = count;
        updateButtons();
    }

    private void selectCategory(String categoryId) {
        selectedCategoryId = categoryId;
        updateButtons();
    }

    private void updateButtons() {
        boolean setupVisible = !menu.hasResults();
        for (int index = 0; index < countButtons.size(); index++) {
            HextechButton btn = countButtons.get(index);
            btn.visible = setupVisible;
            btn.active = setupVisible && selectedCount != index + 1;
        }
        for (int index = 0; index < categoryButtons.size(); index++) {
            GachaCategory category = GachaCategory.values()[index];
            HextechButton btn = categoryButtons.get(index);
            btn.visible = setupVisible;
            btn.active = setupVisible && !category.id().equals(selectedCategoryId);
        }
        if (rollButton != null) {
            rollButton.visible = setupVisible;
            rollButton.active = setupVisible;
            rollButton.setMessage(rollLabel());
        }
        if (reRollButton != null) {
            reRollButton.visible = !setupVisible;
            reRollButton.active = !setupVisible && (revealedCount >= revealOrder.size());
        }
    }

    private Component rollLabel() {
        return Component.translatable("gui.nogeon_economy_land.gacha_roll_button", selectedCount);
    }

    private void rollSelectedCount() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new GachaRollPacket(menu.actionId(), menu.traderDatabaseId(), selectedCategoryId, selectedCount));
        if (rollButton != null) {
            rollButton.active = false;
        }
    }

    private void drawMachineGlow(GuiGraphics graphics, int left, int top, int right, int bottom) {
        int shimmer = hasFinaleSuspense() 
            ? (int) (90 + Math.sin(animationTicks * 0.8f) * 60) 
            : (int) (40 + Math.sin(animationTicks * 0.3f) * 20);
            
        int baseColor = accentColor(0x00FFD56A);
        if (hasFinaleSuspense()) {
            baseColor = 0xFFFF4500;
        }
        
        graphics.fill(left, top, right, bottom, 0xFF0A0C0A);
        graphics.fill(left + 4, top + 4, right - 4, bottom - 4, (shimmer << 24) | (baseColor & 0x00FFFFFF));
    }

    private void drawFeaturedResult(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.fill(left + 10, top + 10, right - 10, bottom - 10, 0xFF0A0D0B);
        
        int chamberBg = hasFinaleSuspense() ? 0xFF2A1612 : 0xFF0F1512;
        graphics.fill(left + 14, top + 14, right - 14, bottom - 14, chamberBg);
        
        GachaRewardResult featured = featuredResult();
        if (featured == null) {
            return;
        }
        
        if (hasFinaleSuspense()) {
            int pulse = (int) (100 + Math.sin(animationTicks * 0.8f) * 80);
            graphics.fill(left + 24, top + 22, right - 24, bottom - 22, (pulse << 24) | 0xFFFFAA00);
            
            graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.gacha_featured_finale"), (left + right) / 2, top + 18, 0xFFFF5555);
            graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.gacha_featured_finale_hint"), (left + right) / 2, top + 34, 0xFFFFAA00);
            
            int rainbowColor = (animationTicks * 20) % 255;
            int textRainbow = 0xFF000000 | (rainbowColor << 16) | ((255 - rainbowColor) << 8) | 255;
            graphics.drawCenteredString(font, Component.literal("✴ ? ✴"), (left + right) / 2, top + 52, textRainbow);
            return;
        }

        ItemStack stack = rewardStack(featured);
        graphics.renderItem(stack, (left + right) / 2 - 8, top + 34);
        graphics.renderItemDecorations(font, stack, (left + right) / 2 - 8, top + 34);
        
        int ringColor = rarityColor(featured.rarity());
        drawCustomBorder(graphics, (left + right) / 2 - 10, top + 32, 20, 20, ringColor);

        graphics.drawCenteredString(font, Component.translatable("gui.nogeon_economy_land.gacha_featured_now"), (left + right) / 2, top + 18, 0xFFEADCB8);
        graphics.drawCenteredString(font, shortName(featured), (left + right) / 2, top + 58, ringColor);
        graphics.drawCenteredString(font, Component.literal("x" + featured.count()), (left + right) / 2, top + 70, 0xFFFFD56A);
    }

    private void drawResults(GuiGraphics graphics, int startX, int startY) {
        List<GachaRewardResult> results = menu.results();
        int slots = Math.max(10, results.size());
        for (int index = 0; index < slots; index++) {
            int row = index / RESULTS_PER_ROW;
            int column = index % RESULTS_PER_ROW;
            int x = startX + column * 70;
            int y = startY + row * 44;
            GachaRewardResult result = index < results.size() ? results.get(index) : null;
            boolean revealed = result != null && index < revealedSlots.length && revealedSlots[index];
            drawResultSlot(graphics, x, y, result, revealed);
        }
    }

    private void drawResultSlot(GuiGraphics graphics, int x, int y, GachaRewardResult result, boolean revealed) {
        int border = result != null ? rarityBorder(result) : 0xFF2C3229;
        
        graphics.fill(x, y, x + 64, y + 38, 0xFF0E1311);
        graphics.fill(x + 1, y + 1, x + 63, y + 37, border);
        
        int innerBg = 0xFF121613;
        graphics.fill(x + 3, y + 3, x + 61, y + 35, innerBg);

        if (result == null) {
            graphics.drawCenteredString(font, Component.literal("-"), x + 32, y + 14, 0xFF3C4D45);
            return;
        }

        if (!revealed) {
            String[] spinSymbols = {"✦", "✴", "✧", "❈", "★", "?", "✥", "✦", "✷"};
            String symbol = "?";
            int textColor = 0xFFEADCB8;
            
            if (menu.hasResults() && revealedCount < revealOrder.size()) {
                int symbolIndex = (animationTicks + x + y) % spinSymbols.length;
                symbol = spinSymbols[symbolIndex];
                
                int pulseColor = (animationTicks * 25) % 255;
                textColor = 0xFF000000 | (pulseColor << 16) | ((255 - pulseColor) << 8) | 255;
            } else {
                symbol = "?";
                textColor = 0xFF769B8E;
            }
            
            int glowColor = accentColor(0x2298D7AA);
            if (menu.hasResults() && revealedCount < revealOrder.size()) {
                int alpha = (int) (40 + Math.sin(animationTicks * 0.4f) * 25);
                glowColor = (alpha << 24) | (accentColor(0x00FFFFFF) & 0x00FFFFFF);
            }
            graphics.fill(x + 3, y + 3, x + 61, y + 35, glowColor);
            
            graphics.drawCenteredString(font, Component.literal(symbol), x + 32, y + 14, textColor);
            return;
        }

        ItemStack stack = rewardStack(result);
        graphics.renderItem(stack, x + 8, y + 10);
        graphics.renderItemDecorations(font, stack, x + 8, y + 10);
        
        int nameColor = rarityColor(result.rarity());
        graphics.drawString(font, font.plainSubstrByWidth(shortName(result).getString(), 32), x + 28, y + 8, nameColor, false);
        graphics.drawString(font, "x" + result.count(), x + 28, y + 20, 0xFFFFD56A, false);

        int index = menu.results().indexOf(result);
        if (index == lastRevealedIndex && revealFlashTicks > 0) {
            int expansion = (8 - revealFlashTicks) * 2;
            int flashColor = result.jackpot() ? 0xFFFFAA00 : (result.rarity() >= 2 ? 0xFFAA00AA : 0xFF00FFCC);
            int flashAlpha = (int) (revealFlashTicks / 8.0f * 255);
            int finalColor = (flashAlpha << 24) | (flashColor & 0x00FFFFFF);
            drawCustomBorder(graphics, x - expansion, y - expansion, 64 + expansion * 2, 38 + expansion * 2, finalColor);
        }
    }

    private int rarityColor(int rarity) {
        return switch (rarity) {
            case 1 -> 0xFF5555FF; // Rare: Blue
            case 2 -> 0xFFAA00AA; // Epic: Purple
            case 3 -> 0xFFFFAA00; // Legendary: Gold
            default -> 0xFFFFFFFF; // Common: White
        };
    }

    private String rarityName(int rarity) {
        return switch (rarity) {
            case 1 -> "희귀";
            case 2 -> "영웅";
            case 3 -> "전설";
            default -> "일반";
        };
    }

    private Component shortName(GachaRewardResult result) {
        ItemStack stack = rewardStack(result);
        Component nameComp = com.nogeon.economyland.network.GachaCelebratePacket.getGachaItemName(stack);
        String text;
        if (nameComp.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
            text = net.minecraft.client.resources.language.I18n.get(translatable.getKey());
        } else {
            text = nameComp.getString();
        }
        if (text.length() > 10) {
            text = text.substring(0, 10) + "...";
        }
        return Component.literal(text);
    }

    private Component statusLabel() {
        if (!menu.hasResults()) {
            return Component.translatable("gui.nogeon_economy_land.gacha_idle_hint");
        }
        if (hasFinaleSuspense()) {
            return Component.translatable("gui.nogeon_economy_land.gacha_finale_warning");
        }
        if (revealedCount < revealOrder.size()) {
            return Component.translatable("gui.nogeon_economy_land.gacha_rolling");
        }
        if (menu.results().stream().anyMatch(GachaRewardResult::jackpot)) {
            return Component.translatable("gui.nogeon_economy_land.gacha_jackpot_status");
        }
        return Component.translatable("gui.nogeon_economy_land.gacha_finished");
    }

    private void playRevealSound(GachaRewardResult result) {
        Minecraft client = minecraft;
        if (client == null || client.player == null) {
            return;
        }
        if (result.jackpot()) {
            client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.1F, 1.0F);
            client.player.playSound(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 1.0F);
            return;
        }
        float pitch = 0.85F + random.nextFloat() * 0.45F;
        client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.55F, pitch);
    }

    private void playSuspensePulse() {
        Minecraft client = minecraft;
        if (client == null || client.player == null || !hasFinaleSuspense()) {
            return;
        }
        if (suspenseTicks % 7 == 0) {
            float pitch = 0.6F + Math.min(0.45F, suspenseTicks * 0.015F);
            client.player.playSound(SoundEvents.NOTE_BLOCK_BIT.value(), 0.45F, pitch);
        }
    }

    private void renderResultTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!menu.hasResults()) {
            return;
        }
        for (int index = 0; index < menu.results().size(); index++) {
            if (index >= revealedSlots.length || !revealedSlots[index]) {
                continue;
            }
            int row = index / RESULTS_PER_ROW;
            int column = index % RESULTS_PER_ROW;
            int x = leftPos + 24 + column * 70;
            int y = topPos + 198 + row * 44;
            if (mouseX >= x && mouseX <= x + 64 && mouseY >= y && mouseY <= y + 38) {
                GachaRewardResult result = menu.results().get(index);
                graphics.renderTooltip(font, rewardStack(result), mouseX, mouseY);
                return;
            }
        }
    }

    private ItemStack rewardStack(GachaRewardResult result) {
        ItemStack stack = result.stack().copy();
        if (stack.isEmpty()) {
            stack = new ItemStack(Items.BARRIER);
        }
        stack.setCount(Math.max(1, result.count()));
        return stack;
    }

    private int rarityBorder(GachaRewardResult result) {
        return switch (result.rarity()) {
            case 3 -> 0xFFFFAA00;
            case 2 -> 0xFFAA00AA;
            case 1 -> 0xFF5DB6F2;
            default -> 0xFF4A5749;
        };
    }

    private int accentColor(int fallback) {
        return switch (menu.actionId()) {
            case "gacha_middle" -> fallback == 0xFF2A271E ? 0xFF14241B : 0xFF00FF88;
            case "gacha_high" -> fallback == 0xFF2A271E ? 0xFF0E1A29 : 0xFF00C8FF;
            case "gacha_legend" -> fallback == 0xFF2A271E ? 0xFF2E1A0F : 0xFFFF8C00;
            default -> fallback == 0xFF2A271E ? 0xFF0E1311 : 0xFF00FFCC;
        };
    }

    private boolean hasFinaleSuspense() {
        return menu.hasCelebrationToken() && !revealOrder.isEmpty() && revealedCount == revealOrder.size() - 1;
    }

    private GachaRewardResult featuredResult() {
        if (!menu.hasResults()) {
            return null;
        }
        if (revealOrder.isEmpty()) {
            return menu.results().get(0);
        }
        if (revealedCount <= 0) {
            return menu.results().get(revealOrder.get(0));
        }
        int revealPointer = Math.min(revealedCount - 1, revealOrder.size() - 1);
        int index = revealOrder.get(revealPointer);
        if (hasFinaleSuspense()) {
            index = revealOrder.get(revealOrder.size() - 1);
        }
        return menu.results().get(index);
    }

    private GachaCategory selectedCategory() {
        return GachaCategory.byId(selectedCategoryId);
    }

    private void resetRevealState() {
        if (!menu.hasResults()) {
            revealOrder = List.of();
            revealedSlots = new boolean[0];
            revealedCount = 0;
            return;
        }
        revealOrder = buildRevealOrder();
        revealedSlots = new boolean[menu.results().size()];
        Arrays.fill(revealedSlots, false);
        revealedCount = 0;
    }

    private List<Integer> buildRevealOrder() {
        List<Integer> normal = new ArrayList<>();
        List<Integer> jackpots = new ArrayList<>();
        for (int index = 0; index < menu.results().size(); index++) {
            if (menu.results().get(index).jackpot()) {
                jackpots.add(index);
            } else {
                normal.add(index);
            }
        }
        normal.addAll(jackpots);
        return List.copyOf(normal);
    }

    private String tierTitleKey() {
        return switch (menu.actionId()) {
            case "gacha_middle" -> "action.nogeon_economy_land.gacha_middle";
            case "gacha_high" -> "action.nogeon_economy_land.gacha_high";
            case "gacha_legend" -> "action.nogeon_economy_land.gacha_legend";
            default -> "action.nogeon_economy_land.gacha_basic";
        };
    }
}
