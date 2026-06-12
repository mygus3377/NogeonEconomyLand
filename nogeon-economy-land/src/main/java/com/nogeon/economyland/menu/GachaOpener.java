package com.nogeon.economyland.menu;

import com.nogeon.economyland.state.GachaRewardResult;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class GachaOpener {
    private GachaOpener() {
    }

    public static void open(ServerPlayer player, String actionId, String categoryId, long pricePerRoll, int selectedCount) {
        open(player, actionId, categoryId, pricePerRoll, selectedCount, List.of(), null);
    }

    public static void open(ServerPlayer player, String traderDatabaseId, String actionId, String categoryId, long pricePerRoll, int selectedCount) {
        open(player, traderDatabaseId, actionId, categoryId, pricePerRoll, selectedCount, List.of(), null);
    }

    public static void open(ServerPlayer player, String actionId, String categoryId, long pricePerRoll, int selectedCount, List<GachaRewardResult> results) {
        open(player, actionId, categoryId, pricePerRoll, selectedCount, results, null);
    }

    public static void open(ServerPlayer player, String actionId, String categoryId, long pricePerRoll, int selectedCount, List<GachaRewardResult> results, UUID celebrationToken) {
        open(player, "", actionId, categoryId, pricePerRoll, selectedCount, results, celebrationToken);
    }

    public static void open(ServerPlayer player, String traderDatabaseId, String actionId, String categoryId, long pricePerRoll, int selectedCount, List<GachaRewardResult> results, UUID celebrationToken) {
        GachaMenu snapshot = new GachaMenu(0, traderDatabaseId, actionId, categoryId, pricePerRoll, selectedCount, results, celebrationToken);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new GachaMenu(containerId, traderDatabaseId, actionId, categoryId, pricePerRoll, selectedCount, results, celebrationToken),
            Component.translatable("screen.nogeon_economy_land.gacha_machine")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }
}
