package com.nogeon.economyland.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class EnhancementScrollItem extends Item {
    private final int level;

    private static final int[] RAINBOW_COLORS = {
        0xFF6666, 0xFF9944, 0xFFDD44, 0x44FF66,
        0x44DDFF, 0x6666FF, 0xDD44FF, 0xFF44AA
    };

    public EnhancementScrollItem(int level, Properties properties) {
        super(properties);
        this.level = level;
    }

    public int getScrollLevel() {
        return level;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (level >= 9) {
            return buildRainbowName(Component.translatable(this.getDescriptionId(stack)).getString(), true);
        }

        ChatFormatting color = switch (level) {
            case 1, 2 -> ChatFormatting.WHITE;
            case 3 -> ChatFormatting.YELLOW;
            case 4 -> ChatFormatting.GREEN;
            case 5 -> ChatFormatting.AQUA;
            case 6 -> ChatFormatting.LIGHT_PURPLE;
            case 7 -> ChatFormatting.GOLD;
            case 8 -> ChatFormatting.RED;
            default -> ChatFormatting.WHITE;
        };

        Component name = Component.translatable(this.getDescriptionId(stack)).withStyle(color);
        if (level >= 7) {
            name = name.copy().withStyle(ChatFormatting.BOLD);
        }
        return name;
    }

    private static Component buildRainbowName(String text, boolean bold) {
        double phase = (System.currentTimeMillis() % 1400L) / 1400.0D * RAINBOW_COLORS.length;
        MutableComponent result = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            double pos = (phase + i * 0.60D) % RAINBOW_COLORS.length;
            int colorIndex = (int) Math.floor(pos);
            int nextIndex = (colorIndex + 1) % RAINBOW_COLORS.length;
            int blended = lerpColor(RAINBOW_COLORS[colorIndex], RAINBOW_COLORS[nextIndex], (float) (pos - colorIndex));
            result.append(Component.literal(String.valueOf(text.charAt(i)))
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(blended)).withBold(bold)));
        }
        return result;
    }

    private static int lerpColor(int from, int to, float t) {
        int r1 = (from >> 16) & 0xFF, g1 = (from >> 8) & 0xFF, b1 = from & 0xFF;
        int r2 = (to >> 16) & 0xFF, g2 = (to >> 8) & 0xFF, b2 = to & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nogeon_economy_land.enhancement_scroll.desc1", level).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.nogeon_economy_land.enhancement_scroll.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        if (level >= 11) {
            tooltip.add(Component.literal("⚡━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━⚡").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(buildRainbowName("★ 신화적인 기적의 강화 주문서 ★", true));
            tooltip.add(Component.literal("⚡━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━⚡").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("  사용 시 대장간 강화 단계를 ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("+" + level).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFDD44)).withBold(true)))
                .append(Component.literal(" 강으로 확정 업그레이드!").withStyle(ChatFormatting.GRAY)));
            tooltip.add(Component.literal("  (현재 장비 레벨이 강화권 레벨보다 낮아야 사용 가능)").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("⚡━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━⚡").withStyle(ChatFormatting.DARK_GRAY));
        } else if (level >= 9) {
            tooltip.add(Component.literal("\u2726 ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable("item.nogeon_economy_land.enhancement_scroll.usage").withStyle(ChatFormatting.LIGHT_PURPLE)));
            tooltip.add(Component.literal("\u2726 \uc804\uc124\uae09 \uac15\ud654 \uc8fc\ubb38\uc11c").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFAA00)).withBold(true)));
        } else {
            tooltip.add(Component.translatable("item.nogeon_economy_land.enhancement_scroll.usage").withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        if (level >= 9) {
            return Rarity.EPIC;
        }
        if (level >= 7) {
            return Rarity.RARE;
        }
        return super.getRarity(stack);
    }
}
