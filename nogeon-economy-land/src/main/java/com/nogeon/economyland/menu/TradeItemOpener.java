package com.nogeon.economyland.menu;

import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.trade.TradeSession;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

public final class TradeItemOpener {
    private TradeItemOpener() {
    }

    public static void open(ServerPlayer player) {
        EconomyState state = EconomyState.get(player.server);
        TradeSession session = state.tradeSession(player.getUUID());
        if (session == null) {
            return;
        }
        String partnerId = session.partner(player.getUUID()).toString();
        List<ItemStack> items = state.tradeOfferItems(player);
        TradeItemMenu snapshot = new TradeItemMenu(0, partnerId, items);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new TradeItemMenu(containerId, partnerId, items),
            Component.translatable("screen.nogeon_economy_land.trade_item")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
