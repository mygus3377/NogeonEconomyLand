package com.nogeon.economyland.menu;

import com.nogeon.economyland.item.SmithingService;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.shop.ShopItemProtection;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.JobProgress;
import com.nogeon.economyland.player.JobType;
import com.nogeon.economyland.player.SkillNodeStat;
import com.nogeon.economyland.state.EconomyState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public final class SmithOpener {
    private SmithOpener() {
    }

    public static void open(ServerPlayer player, int selectedSlot, Component status) {
        open(player, selectedSlot, status, false);
    }

    public static void open(ServerPlayer player, int selectedSlot, Component status, boolean shopMode) {
        open(player, selectedSlot, status, shopMode, false);
    }

    public static void open(ServerPlayer player, int selectedSlot, Component status, boolean shopMode, boolean scrollMode) {
        open(player, selectedSlot, status, shopMode, scrollMode, false);
    }

    public static void open(ServerPlayer player, int selectedSlot, Component status, boolean shopMode, boolean scrollMode, boolean deliveryMode) {
        int resolvedSlot = SmithingService.normalizeSelectedSlot(player, selectedSlot);
        Component resolvedStatus = status == null
            ? SmithingService.defaultStatus(SmithingService.stackForSlot(player, resolvedSlot))
            : status;
        
        List<ShopLine> shopLines = new ArrayList<>();
        for (ShopEntry entry : SmithingService.shopItems()) {
            shopLines.add(new ShopLine("smith", entry.id(), entry.stack(), SmithingService.shopPrice(entry), -1, false));
        }

        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        JobProgress minerJob = profile.job(JobType.MINER);
        long bonusPercent = minerJob.bonusPercent(SkillNodeStat.DELIVERY_PRICE);

        List<ShopLine> deliveryLines = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (!invStack.isEmpty() && com.nogeon.economyland.shop.DynamicPriceLogic.shouldAccept(com.nogeon.economyland.entity.TraderKind.SMITH, invStack)) {
                long basePrice = Math.round(com.nogeon.economyland.shop.DynamicPriceLogic.calculatePrice(com.nogeon.economyland.entity.TraderKind.SMITH, invStack) * 1.8D);
                long totalPaid = basePrice + (basePrice * bonusPercent / 100L);
                String nbtSuffix = invStack.hasTag() ? ":" + Integer.toHexString(invStack.getTag().hashCode()) : "";
                String dynamicId = "dynamic:" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(invStack.getItem()) + nbtSuffix;
                
                if (deliveryLines.stream().noneMatch(line -> line.id().equals(dynamicId))) {
                    deliveryLines.add(new ShopLine("smith", dynamicId, invStack.copy(), totalPaid, -1, true));
                }
            }
        }

        int lowDowngradeScrolls = profile.enhancementDowngradeCharges(10);
        int midDowngradeScrolls = profile.enhancementDowngradeCharges(15);
        int highDowngradeScrolls = profile.enhancementDowngradeCharges(17);
        int highestDowngradeScrolls = profile.enhancementDowngradeCharges(20);
        int resetProtectionScrolls = profile.enhancementResetProtectionCharges();

        SmithMenu snapshot = new SmithMenu(0, resolvedSlot, shopMode, scrollMode, deliveryMode, shopLines, deliveryLines, resolvedStatus,
            lowDowngradeScrolls, midDowngradeScrolls, highDowngradeScrolls, highestDowngradeScrolls, resetProtectionScrolls);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new SmithMenu(containerId, resolvedSlot, shopMode, scrollMode, deliveryMode, shopLines, deliveryLines, resolvedStatus,
                lowDowngradeScrolls, midDowngradeScrolls, highDowngradeScrolls, highestDowngradeScrolls, resetProtectionScrolls),
            Component.translatable("screen.nogeon_economy_land.smith")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
