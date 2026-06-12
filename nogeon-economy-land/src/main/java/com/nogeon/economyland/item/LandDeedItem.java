package com.nogeon.economyland.item;

import com.nogeon.economyland.land.LandEvents;
import com.nogeon.economyland.land.LandType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class LandDeedItem extends Item {
    private final LandType type;

    public LandDeedItem(LandType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public LandType landType() {
        return type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            LandEvents.handleDeedClick(serverPlayer, true, null);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        LandEvents.handleDeedClick(player, true, context.getClickedPos());
        return InteractionResult.CONSUME;
    }
}
