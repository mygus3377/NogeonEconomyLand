package com.nogeon.economyland.item;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.ForgeRegistries;

public final class ShadyWizardSpawnerItem extends Item {
    public ShadyWizardSpawnerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("ars_nouveau", "shady_wizard"));
        if (type == null) {
            if (context.getPlayer() != null) {
                context.getPlayer().sendSystemMessage(Component.literal("§c[오류] Ars Nouveau 모드가 활성화되어 있지 않아 희미한 마법사를 소환할 수 없습니다."));
            }
            return InteractionResult.FAIL;
        }

        Entity entity = type.create(level);
        if (entity == null) {
            return InteractionResult.FAIL;
        }

        entity.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, context.getRotation(), 0.0F);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(entity);

        if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
