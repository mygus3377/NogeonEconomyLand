package com.nogeon.economyland.menu;

import com.nogeon.economyland.NoGeonEconomyLand;
import com.nogeon.economyland.item.ModItems;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public final class LuckExchangeOpener {
    private static final List<LuckExchangeOffer> OFFERS = List.of(
        offer("basic_ticket", "item.nogeon_economy_land.basic_gacha_ticket", 6, ModItems.BASIC_GACHA_TICKET.get(), 3),
        offer("middle_ticket", "item.nogeon_economy_land.middle_gacha_ticket", 18, ModItems.MIDDLE_GACHA_TICKET.get(), 3),
        offer("high_ticket", "item.nogeon_economy_land.high_gacha_ticket", 45, ModItems.HIGH_GACHA_TICKET.get(), 2),
        offer("legend_ticket", "item.nogeon_economy_land.legend_gacha_ticket", 120, ModItems.LEGEND_GACHA_TICKET.get(), 1)
    );

    private LuckExchangeOpener() {
    }

    public static void open(ServerPlayer player) {
        com.nogeon.economyland.state.EconomyState state = com.nogeon.economyland.state.EconomyState.get(player.server);
        com.nogeon.economyland.player.PlayerProfile profile = state.profile(player.getUUID());
        int virtualTokens = profile.unluckyTokens();
        int itemTokens = tokenCount(player);

        LuckExchangeMenu snapshot = new LuckExchangeMenu(0, virtualTokens, itemTokens, OFFERS);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new LuckExchangeMenu(containerId, virtualTokens, itemTokens, OFFERS),
            Component.translatable("screen.nogeon_economy_land.luck_exchange")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }

    public static LuckExchangeOffer findOffer(String id) {
        for (LuckExchangeOffer offer : OFFERS) {
            if (offer.id().equals(id)) {
                return offer;
            }
        }
        return null;
    }

    public static int tokenCount(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.UNLUCKY_TOKEN.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static LuckExchangeOffer offer(String id, String labelKey, int tokenCost, Item rewardItem, int rewardCount) {
        ResourceLocation rewardId = rewardItem.builtInRegistryHolder().key().location();
        return new LuckExchangeOffer(id, labelKey, tokenCost, rewardId.toString(), rewardCount);
    }
}
