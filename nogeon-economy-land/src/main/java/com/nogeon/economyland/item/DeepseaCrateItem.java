package com.nogeon.economyland.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class DeepseaCrateItem extends Item {
    private final int tier;

    public DeepseaCrateItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String grade = switch (tier) {
            case 1 -> "\ucd08\uae09";
            case 2 -> "\uc911\uae09";
            case 3 -> "\uace0\uae09";
            default -> "\uc804\uc124";
        };
        tooltip.add(Component.literal("[\ubcf4\ubb3c \ucc3e\uae30] \ub0da\uc2dc\ub85c \uac74\uc838 \uc62c\ub9b0 \uc2ec\ud574 \ud06c\ub808\uc774\ud2b8").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("\ub4e4\uace0 \uc6b0\ud074\ub9ad\ud558\uba74 " + grade + " \uac00\ucc28 \ubcf4\uc0c1\uc744 \ud68d\ub4dd").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("\uc608\uc0c1 \ubcf4\uc0c1: 1~3\uac1c").withStyle(ChatFormatting.AQUA));
    }
}
