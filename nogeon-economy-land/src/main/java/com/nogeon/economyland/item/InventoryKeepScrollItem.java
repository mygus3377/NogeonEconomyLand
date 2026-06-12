package com.nogeon.economyland.item;

import com.nogeon.economyland.state.EconomyState;
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

public final class InventoryKeepScrollItem extends Item {
    public InventoryKeepScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nogeon_economy_land.inventory_keep_scroll.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.nogeon_economy_land.inventory_keep_scroll.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("Use to register this scroll to your wallet.").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.nogeon_economy_land.inventory_keep_scroll.usage").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            EconomyState state = EconomyState.get(serverPlayer.server);
            var profile = state.profile(serverPlayer.getUUID());
            profile.addInventoryKeepCharges(1);
            stack.shrink(1);
            state.setDirty();
            serverPlayer.displayClientMessage(Component.literal("Inventory Save registered. Owned: " + profile.inventoryKeepCharges()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
