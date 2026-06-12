package com.nogeon.economyland.item;

import com.nogeon.economyland.entity.FarmerScarecrowEntity;
import com.nogeon.economyland.entity.ModEntities;
import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.SkillNode;
import com.nogeon.economyland.state.EconomyState;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class FarmerScarecrowItem extends Item {
    public FarmerScarecrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        int scarecrowLevel = profile.job(JobType.FARMER).nodeLevel(SkillNode.FARMER_SUNLIT_STEP);
        if (profile.selectedJob() != JobType.FARMER || scarecrowLevel <= 0) {
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.scarecrow.need_skill"), true);
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockPos placePos = clickedPos.relative(context.getClickedFace());
        boolean insideOwnedLand = state.isInsideAnyOwnedLand(player.getUUID(), level.dimension(), clickedPos)
            || state.isInsideAnyOwnedLand(player.getUUID(), level.dimension(), placePos);
        if (!insideOwnedLand) {
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.scarecrow.need_land"), true);
            return InteractionResult.FAIL;
        }

        FarmerScarecrowEntity scarecrow = ModEntities.FARMER_SCARECROW.get().create(serverLevel);
        if (scarecrow == null) {
            return InteractionResult.FAIL;
        }
        scarecrow.moveTo(placePos.getX() + 0.5D, placePos.getY(), placePos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
        scarecrow.setup(player.getUUID(), scarecrowLevel);

        if (!serverLevel.addFreshEntity(scarecrow)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        serverLevel.playSound(null, placePos, SoundEvents.ARMOR_STAND_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, scarecrow.getX(), scarecrow.getY() + 1.0D, scarecrow.getZ(), 18, 0.35D, 0.35D, 0.35D, 0.03D);
        player.displayClientMessage(Component.translatable("message.nogeon_economy_land.scarecrow.placed"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nogeon_economy_land.farmer_scarecrow.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.nogeon_economy_land.farmer_scarecrow.desc2").withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
