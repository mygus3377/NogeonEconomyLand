package com.nogeon.economyland.item;

import com.nogeon.economyland.state.EconomyState;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class EnhancementResetProtectionScrollItem extends Item {
    private final String displayName;

    private static final int[] RAINBOW_COLORS = {
        0xFF6666, 0xFF9944, 0xFFDD44, 0x44FF66,
        0x44DDFF, 0x6666FF, 0xDD44FF, 0xFF44AA
    };

    public EnhancementResetProtectionScrollItem(String displayName, Properties properties) {
        super(properties);
        this.displayName = displayName;
    }

    @Override
    public Component getName(ItemStack stack) {
        return buildRainbowName(displayName, true);
    }

    private static Component buildRainbowName(String text, boolean bold) {
        double phase = (System.currentTimeMillis() % 1600L) / 1600.0D * RAINBOW_COLORS.length;
        MutableComponent result = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            double pos = (phase + i * 0.55D) % RAINBOW_COLORS.length;
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
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("대장간 강화 실패 시 장비 초기화(0강)를 1회 방지합니다.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("우클릭하여 지갑에 충전 후 강화할 때 자동으로 사용됩니다.").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.EPIC;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            EconomyState state = EconomyState.get(serverPlayer.server);
            var profile = state.profile(serverPlayer.getUUID());
            profile.addEnhancementResetProtectionCharges(1);
            state.setDirty();
            serverPlayer.displayClientMessage(Component.literal("강화 초기화 방지권 등록 완료 / 보유: " + profile.enhancementResetProtectionCharges() + "개").withStyle(ChatFormatting.GREEN), true);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
