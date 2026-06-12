package com.nogeon.economyland.menu;

import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.shop.ShopEntry;
import com.nogeon.economyland.state.EconomyState;
import com.nogeon.economyland.state.TraderShopState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class GachaRewardAdminOpener {
    public static final int PAGE_SIZE = 40;
    private static final Map<UUID, String> LAST_CATEGORY = new HashMap<>();
    private static final Map<UUID, Integer> LAST_PAGE = new HashMap<>();

    private GachaRewardAdminOpener() {
    }

    public static void open(ServerPlayer player, String traderDatabaseId) {
        open(player, traderDatabaseId, LAST_CATEGORY.getOrDefault(player.getUUID(), GachaCategory.ITEM.id()));
    }

    public static void open(ServerPlayer player, String traderDatabaseId, String categoryId) {
        GachaCategory selectedCategory = GachaCategory.byId(categoryId);
        int page = selectedCategory.id().equals(LAST_CATEGORY.get(player.getUUID()))
            ? LAST_PAGE.getOrDefault(player.getUUID(), 0)
            : 0;
        open(player, traderDatabaseId, selectedCategory.id(), page);
    }

    public static void open(ServerPlayer player, String traderDatabaseId, String categoryId, int page) {
        EconomyState state = EconomyState.get(player.server);
        TraderShopState traderState = TraderShopState.get(player.server);
        GachaCategory selectedCategory = GachaCategory.byId(categoryId);
        List<ShopEntry> rewards = traderState.gachaRewardEntries(state, selectedCategory.id());
        int totalCount = rewards.size();
        int maxPage = Math.max(0, (totalCount - 1) / PAGE_SIZE);
        int selectedPage = Math.max(0, Math.min(page, maxPage));
        LAST_CATEGORY.put(player.getUUID(), selectedCategory.id());
        LAST_PAGE.put(player.getUUID(), selectedPage);
        int start = selectedPage * PAGE_SIZE;
        int end = Math.min(totalCount, start + PAGE_SIZE);
        List<ShopLine> lines = new ArrayList<>();
        for (ShopEntry entry : rewards.subList(start, end)) {
            lines.add(new ShopLine(selectedCategory.id(), entry.id(), entry.stack(), entry.price(), entry.dailyLimit(), false));
        }
        GachaRewardAdminMenu snapshot = new GachaRewardAdminMenu(0, traderDatabaseId, selectedCategory.id(), selectedPage, totalCount, lines);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new GachaRewardAdminMenu(containerId, traderDatabaseId, selectedCategory.id(), selectedPage, totalCount, lines),
            Component.translatable("screen.nogeon_economy_land.admin_gacha_rewards")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
