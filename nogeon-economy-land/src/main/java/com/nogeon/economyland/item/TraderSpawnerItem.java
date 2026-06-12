package com.nogeon.economyland.item;

import com.nogeon.economyland.entity.EconomyTraderEntity;
import com.nogeon.economyland.entity.ModEntities;
import com.nogeon.economyland.entity.TraderKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class TraderSpawnerItem extends Item {
    private final TraderKind traderKind;

    public TraderSpawnerItem(TraderKind traderKind, Properties properties) {
        super(properties);
        this.traderKind = traderKind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        EconomyTraderEntity trader = ModEntities.ECONOMY_TRADER.get().create(level);
        if (trader == null) {
            return InteractionResult.FAIL;
        }
        trader.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, context.getRotation(), 0.0F);
        trader.setTraderKind(traderKind);
        if (context.getItemInHand().hasTag() && context.getItemInHand().getTag().contains("TraderDatabaseId")) {
            trader.setTraderDatabaseId(context.getItemInHand().getTag().getString("TraderDatabaseId"));
        }
        level.addFreshEntity(trader);
        if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
