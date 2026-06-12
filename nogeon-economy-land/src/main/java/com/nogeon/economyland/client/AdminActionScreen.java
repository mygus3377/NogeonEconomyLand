package com.nogeon.economyland.client;

import com.nogeon.economyland.item.GunCatalog;
import com.nogeon.economyland.menu.AdminActionMenu;
import com.nogeon.economyland.menu.TraderActionLine;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

public final class AdminActionScreen extends AbstractContainerScreen<AdminActionMenu> {
    private static final NumberFormat CREDIT_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);
    private static final int DEFAULT_HEIGHT = 228;
    private static final int GACHA_HEIGHT = 316;
    private static final int LAND_HEIGHT = 300;

    public AdminActionScreen(AdminActionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 340;
        imageHeight = isLandMenu() ? LAND_HEIGHT : isGachaMenu() ? GACHA_HEIGHT : DEFAULT_HEIGHT;
        inventoryLabelY = 10_000;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xF0181714);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xF025251F);
        graphics.fill(x + 16, y + 52, x + imageWidth - 16, y + imageHeight - 28, 0xFF171B18);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isGachaMenu()) {
            renderGachaLabels(graphics);
            return;
        }
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFFE8E1C4);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.action_hint"), 22, 28, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.action_readonly"), 22, 40, 0xFFD9B56F, false);

        for (LayoutRow row : layoutRows()) {
            if (row.headerKey() != null) {
                graphics.drawString(font, Component.translatable(row.headerKey()), 24, row.y(), 0xFF8FBF9B, false);
                graphics.fill(24, row.y() + 10, 318, row.y() + 11, 0xFF2E4533);
                continue;
            }

            TraderActionLine line = row.line();
            if (line == null) {
                continue;
            }
            graphics.drawString(font, Component.translatable(line.labelKey()), 24, row.y(), 0xFFE8E1C4, false);
            graphics.drawString(font, Component.translatable(line.descriptionKey()), 24, row.y() + 10, 0xFF98A49C, false);
            if (line.price() > 0) {
                graphics.drawString(font, CREDIT_FORMAT.format(line.price()) + " C", 198, row.y(), 0xFFFFD56A, false);
            }
        }
    }

    private boolean isLandMenu() {
        return "land".equals(menu.kindId());
    }

    private boolean isGachaMenu() {
        return "gacha".equals(menu.kindId());
    }

    private void renderGachaLabels(GuiGraphics graphics) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 12, 0xFFE8E1C4);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.action_hint"), 22, 28, 0xFF98A49C, false);
        graphics.drawString(font, Component.translatable("gui.nogeon_economy_land.admin.action_readonly"), 22, 40, 0xFFD9B56F, false);

        int y = 62;
        for (TraderActionLine line : menu.lines()) {
            int band = gachaBand(line.actionId());
            List<Item> guns = GunCatalog.gachaItems(band);
            graphics.drawString(font, Component.translatable(line.labelKey()), 24, y, 0xFFE8E1C4, false);
            graphics.drawString(font, CREDIT_FORMAT.format(line.price()) + " C", 244, y, 0xFFFFD56A, false);
            graphics.drawString(font, Component.translatable(line.descriptionKey()), 24, y + 12, 0xFF98A49C, false);
            graphics.drawString(font, gachaCategoriesLabel(), 24, y + 24, 0xFF8FBF9B, false);
            graphics.drawString(font, gunPoolSummary(guns), 24, y + 36, 0xFF7FA8D8, false);
            graphics.fill(24, y + 48, 318, y + 49, 0xFF2E4533);
            y += 58;
        }
    }

    private Component gachaCategoriesLabel() {
        return Component.literal(
            Component.translatable("gui.nogeon_economy_land.gacha_category_weapon").getString()
                + " / "
                + Component.translatable("gui.nogeon_economy_land.gacha_category_armor").getString()
                + " / "
                + Component.translatable("gui.nogeon_economy_land.gacha_category_item").getString()
                + " / "
                + Component.translatable("gui.nogeon_economy_land.gacha_category_gun").getString()
        );
    }

    private String gunPoolSummary(List<Item> guns) {
        if (guns.isEmpty()) {
            return "총기 풀: 0종";
        }
        StringBuilder builder = new StringBuilder("총기 풀: ")
            .append(guns.size())
            .append("종");
        int previewCount = Math.min(3, guns.size());
        if (previewCount > 0) {
            builder.append(" | ");
            for (int index = 0; index < previewCount; index++) {
                if (index > 0) {
                    builder.append(", ");
                }
                String name = Component.translatable(guns.get(index).getDescriptionId()).getString();
                builder.append(name.length() > 12 ? name.substring(0, 12) + "…" : name);
            }
        }
        return builder.toString();
    }

    private int gachaBand(String actionId) {
        return switch (actionId) {
            case "gacha_legend" -> 3;
            case "gacha_high" -> 2;
            case "gacha_middle" -> 1;
            default -> 0;
        };
    }

    private List<LayoutRow> layoutRows() {
        if (!isLandMenu()) {
            List<LayoutRow> rows = new ArrayList<>();
            int y = 66;
            for (TraderActionLine line : menu.lines()) {
                rows.add(LayoutRow.action(line, y));
                y += 24;
            }
            return rows;
        }

        List<TraderActionLine> utilityLines = new ArrayList<>();
        List<TraderActionLine> landDeeds = new ArrayList<>();
        List<TraderActionLine> socialClasses = new ArrayList<>();
        for (TraderActionLine line : menu.lines()) {
            if (line.actionId().endsWith("_deed")) {
                landDeeds.add(line);
            } else if (line.actionId().startsWith("class_")) {
                socialClasses.add(line);
            } else {
                utilityLines.add(line);
            }
        }

        List<LayoutRow> rows = new ArrayList<>();
        int y = 64;
        for (TraderActionLine line : utilityLines) {
            rows.add(LayoutRow.action(line, y));
            y += 24;
        }
        if (!landDeeds.isEmpty()) {
            rows.add(LayoutRow.header("gui.nogeon_economy_land.land_section_deeds", y));
            y += 16;
            for (TraderActionLine line : landDeeds) {
                rows.add(LayoutRow.action(line, y));
                y += 22;
            }
        }
        if (!socialClasses.isEmpty()) {
            rows.add(LayoutRow.header("gui.nogeon_economy_land.land_section_classes", y));
            y += 16;
            for (TraderActionLine line : socialClasses) {
                rows.add(LayoutRow.action(line, y));
                y += 22;
            }
        }
        return rows;
    }

    private record LayoutRow(TraderActionLine line, String headerKey, int y) {
        private static LayoutRow action(TraderActionLine line, int y) {
            return new LayoutRow(line, null, y);
        }

        private static LayoutRow header(String headerKey, int y) {
            return new LayoutRow(null, headerKey, y);
        }
    }
}