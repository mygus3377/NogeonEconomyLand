package com.nogeon.economyland.menu;

import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.menu.GachaRewardAdminOpener;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.TraderShopState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class AdminShopOpener {
    private AdminShopOpener() {
    }

    public static void open(ServerPlayer player, TraderKind kind) {
        open(player, kind, "");
    }

    public static void open(ServerPlayer player, TraderKind kind, String traderDatabaseId) {
        if (kind == TraderKind.GACHA) {
            GachaRewardAdminOpener.open(player, traderDatabaseId);
            return;
        }
        if (!kind.supportsInventoryShop() && kind != TraderKind.GACHA) {
            AdminActionOpener.open(player, kind);
            return;
        }

        EconomyState state = EconomyState.get(player.server);
        TraderShopState traderState = TraderShopState.get(player.server);
        List<ShopLine> lines = new ArrayList<>();
        for (ShopEntry entry : traderState.shopEntries(state, kind, traderDatabaseId)) {
            lines.add(new ShopLine(kind.id(), entry.id(), entry.stack(), entry.price(), entry.dailyLimit(), false));
        }
        for (ShopEntry entry : traderState.deliveryEntries(state, kind, traderDatabaseId)) {
            lines.add(new ShopLine(kind.id(), entry.id(), entry.stack(), entry.price(), 0, true));
        }
        AdminShopMenu snapshot = new AdminShopMenu(0, kind.id(), traderDatabaseId, lines);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new AdminShopMenu(containerId, kind.id(), traderDatabaseId, lines),
            Component.translatable(kind == TraderKind.GACHA
                ? "screen.nogeon_economy_land.admin_gacha_rewards"
                : "screen.nogeon_economy_land.admin_shop_kind", Component.translatable(kind.translationKey()))
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
