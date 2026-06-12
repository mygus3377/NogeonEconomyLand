package com.nogeon.economyland.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class EnhancementGuardScrollItem extends Item {
    public EnhancementGuardScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nogeon_economy_land.enhancement_guard_scroll.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.nogeon_economy_land.enhancement_guard_scroll.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("\uc774\uc81c \uc0ac\uc6a9\ud558\uc9c0 \uc54a\ub294 \uc608\uc804 \uac15\ud654 \ud558\ub77d \ubc29\uc9c0\uad8c\uc785\ub2c8\ub2e4.").withStyle(ChatFormatting.RED));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.literal("\uc774\uc81c \uc0ac\uc6a9\ud558\uc9c0 \uc54a\ub294 \uc608\uc804 \uac15\ud654 \ud558\ub77d \ubc29\uc9c0\uad8c\uc785\ub2c8\ub2e4.").withStyle(ChatFormatting.RED), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
