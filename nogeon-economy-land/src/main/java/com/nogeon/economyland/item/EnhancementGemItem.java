package com.nogeon.economyland.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class EnhancementGemItem extends Item {
    private final int tier;
    private final int bonusPercent;
    private final ChatFormatting color;

    public EnhancementGemItem(int tier, int bonusPercent, ChatFormatting color, Properties properties) {
        super(properties);
        this.tier = tier;
        this.bonusPercent = bonusPercent;
        this.color = color;
    }

    public int tier() {
        return tier;
    }

    public int bonusPercent() {
        return bonusPercent;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (tier >= 4 && entity.level() instanceof ServerLevel level && entity.tickCount % particleInterval() == 0) {
            double x = entity.getX();
            double y = entity.getY() + 0.35D;
            double z = entity.getZ();
            double radius = 0.18D + tier * 0.035D;

            level.sendParticles(ParticleTypes.ENCHANT, x, y + 0.12D, z, tier + 2, radius, 0.22D, radius, 0.02D);
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, Math.max(1, tier - 3), radius, 0.18D, radius, 0.01D);
            if (tier >= 5) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 0.05D, z, 2, radius * 0.8D, 0.12D, radius * 0.8D, 0.01D);
            }
            if (tier >= 6) {
                level.sendParticles(ParticleTypes.FIREWORK, x, y + 0.2D, z, 2, radius, 0.20D, radius, 0.02D);
                double angle = (entity.tickCount % 60) * 0.104719755D;
                double sparkleX = x + Math.cos(angle) * 0.28D;
                double sparkleZ = z + Math.sin(angle) * 0.28D;
                level.sendParticles(ParticleTypes.END_ROD, sparkleX, y + 0.18D, sparkleZ, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                level.sendParticles(ParticleTypes.WAX_ON, x, y + 0.08D, z, 3, radius * 0.7D, 0.10D, radius * 0.7D, 0.02D);
            }
        }
        return false;
    }

    private int particleInterval() {
        return switch (tier) {
            case 6 -> 3;
            case 5 -> 5;
            default -> 8;
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        if (tier >= 6) {
            return animatedRainbowName();
        }
        return Component.translatable(getDescriptionId(stack)).withStyle(color);
    }

    private Component animatedRainbowName() {
        String name = "\uc644\ubcbd\ud55c \uac15\ud654\uc758 \ubcf4\uc11d";
        long time = System.currentTimeMillis();
        MutableComponent result = Component.empty();
        for (int i = 0; i < name.length(); i++) {
            float hue = ((time % 3600L) / 3600.0F + i * 0.075F) % 1.0F;
            int rgb = hsvToRgb(hue, 0.95F, 1.0F);
            result.append(Component.literal(String.valueOf(name.charAt(i))).withStyle(style -> style.withColor(TextColor.fromRgb(rgb))));
        }
        return result;
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        int sector = (int) (hue * 6.0F);
        float fraction = hue * 6.0F - sector;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - fraction * saturation);
        float t = value * (1.0F - (1.0F - fraction) * saturation);
        float r;
        float g;
        float b;
        switch (sector % 6) {
            case 0 -> {
                r = value;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = value;
                b = p;
            }
            case 2 -> {
                r = p;
                g = value;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = value;
            }
            case 4 -> {
                r = t;
                g = p;
                b = value;
            }
            default -> {
                r = value;
                g = p;
                b = q;
            }
        }
        return ((int) (r * 255.0F) << 16) | ((int) (g * 255.0F) << 8) | (int) (b * 255.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("\ub300\uc7a5\uac04 \uac15\ud654 \uc2dc \uc120\ud0dd\ud558\uc5ec \uc0ac\uc6a9\ud569\ub2c8\ub2e4.").withStyle(ChatFormatting.GRAY));
        if (tier >= 6) {
            tooltip.add(Component.literal("\uc120\ud0dd\ud55c \uac15\ud654\ub97c \ubc18\ub4dc\uc2dc \uc131\uacf5\uc2dc\ud0b5\ub2c8\ub2e4.").withStyle(color));
            tooltip.add(Component.literal("\uc601\ub871\ud55c \ube5b\uc774 \ubcf4\uc11d \ud45c\uba74\uc744 \ub530\ub77c \ud750\ub985\ub2c8\ub2e4.").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        } else {
            tooltip.add(Component.literal("\uac15\ud654 \uc131\uacf5\ub960 \uae30\ubcf8 +" + bonusPercent + "%p").withStyle(color));
            tooltip.add(Component.literal("\uac15\ud654 \ub2e8\uacc4\uac00 \ub192\uc744\uc218\ub85d \uc2e4\uc81c \ubcf4\ub108\uc2a4\uac00 \uac10\uc18c\ud569\ub2c8\ub2e4.").withStyle(ChatFormatting.DARK_AQUA));
        }
        tooltip.add(Component.literal("+1~5 100%, +6~10 80%, +11~15 60%, +16~17 40%, +18~20 25%").withStyle(ChatFormatting.GRAY));
    }
}
