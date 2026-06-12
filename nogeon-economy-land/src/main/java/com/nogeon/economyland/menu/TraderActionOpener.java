package com.nogeon.economyland.menu;

import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

public final class TraderActionOpener {
    private TraderActionOpener() {
    }

    public static void open(ServerPlayer player, TraderKind kind) {
        open(player, kind, "");
    }

    public static void open(ServerPlayer player, TraderKind kind, String traderDatabaseId) {
        EconomyState state = EconomyState.get(player.server);
        PlayerProfile profile = state.profile(player.getUUID());
        List<TraderActionLine> lines = lines(kind, profile);
        String socialClassId = profile.socialClass().id();
        long lotteryJackpot1 = kind == TraderKind.LOTTERY ? state.lotteryJackpot1() : 0L;
        long lotteryJackpot2 = kind == TraderKind.LOTTERY ? state.lotteryJackpot2() : 0L;
        TraderActionMenu snapshot = new TraderActionMenu(0, kind.id(), traderDatabaseId, socialClassId, lines, lotteryJackpot1, lotteryJackpot2);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, opener) -> new TraderActionMenu(containerId, kind.id(), traderDatabaseId, socialClassId, lines, lotteryJackpot1, lotteryJackpot2),
            Component.translatable("screen.nogeon_economy_land." + kind.id() + "_action")
        ), (FriendlyByteBuf buffer) -> snapshot.write(buffer));
    }

    public static List<TraderActionLine> lines(TraderKind kind) {
        return lines(kind, null);
    }

    public static List<TraderActionLine> lines(TraderKind kind, PlayerProfile profile) {
        return switch (kind) {
            case LOTTERY -> List.of(
                new TraderActionLine("lottery_daily", "action.nogeon_economy_land.lottery_daily", "action.nogeon_economy_land.lottery_daily.desc", 1000),
                new TraderActionLine("lottery_info", "action.nogeon_economy_land.lottery_info", "action.nogeon_economy_land.lottery_info.desc", 0),
                new TraderActionLine("luck_exchange_basic", "action.nogeon_economy_land.luck_exchange_basic", "action.nogeon_economy_land.luck_exchange_basic.desc", 0)
            );
            case GAMBLER -> List.of(
                new TraderActionLine("dice_duel", "action.nogeon_economy_land.dice_duel", "action.nogeon_economy_land.dice_duel.desc", 1000),
                new TraderActionLine("high_low", "action.nogeon_economy_land.blackjack", "action.nogeon_economy_land.blackjack.desc", 1000),
                new TraderActionLine("slot_machine", "action.nogeon_economy_land.slot_machine", "action.nogeon_economy_land.slot_machine.desc", 1000)
            );
            case GACHA -> List.of(
                new TraderActionLine("gacha_basic", "action.nogeon_economy_land.gacha_basic", "action.nogeon_economy_land.gacha_basic.desc", 1000),
                new TraderActionLine("gacha_middle", "action.nogeon_economy_land.gacha_middle", "action.nogeon_economy_land.gacha_middle.desc", 3000),
                new TraderActionLine("gacha_high", "action.nogeon_economy_land.gacha_high", "action.nogeon_economy_land.gacha_high.desc", 10000),
                new TraderActionLine("gacha_legend", "action.nogeon_economy_land.gacha_legend", "action.nogeon_economy_land.gacha_legend.desc", 100000)
            );
            case SMITH -> List.of(
                new TraderActionLine("smith_shop_open", "action.nogeon_economy_land.smith_shop_open", "action.nogeon_economy_land.smith_shop_open.desc", 0),
                new TraderActionLine("smith_open", "action.nogeon_economy_land.smith_open", "action.nogeon_economy_land.smith_open.desc", 0),
                new TraderActionLine("smith_deconstruct", "gui.nogeon_economy_land.smith_deconstruct_tab", "gui.nogeon_economy_land.smith_deconstruct_subtitle", 0),
                new TraderActionLine("smith_scrolls", "action.nogeon_economy_land.smith_scrolls", "action.nogeon_economy_land.smith_scrolls.desc", 0),
                new TraderActionLine("smith_reforge", "action.nogeon_economy_land.smith_reforge", "action.nogeon_economy_land.smith_reforge.desc", 0),
                new TraderActionLine("smith_socket", "action.nogeon_economy_land.smith_socket", "action.nogeon_economy_land.smith_socket.desc", 0)
            );
            case LAND -> List.of(
                new TraderActionLine("land_home", "action.nogeon_economy_land.land_home", "action.nogeon_economy_land.land_home.desc", 0),
                new TraderActionLine("buy_basic_deed", "item.nogeon_economy_land.basic_land_deed", "action.nogeon_economy_land.buy_basic_deed.desc", 0),
                new TraderActionLine("buy_normal_deed", "item.nogeon_economy_land.normal_land_deed", "action.nogeon_economy_land.buy_normal_deed.desc", 1000),
                new TraderActionLine("buy_industrial_deed", "item.nogeon_economy_land.industrial_land_deed", "action.nogeon_economy_land.buy_industrial_deed.desc", 3000),
                new TraderActionLine("class_middle", "social_class.nogeon_economy_land.middle", "action.nogeon_economy_land.class_middle.desc", 250000),
                new TraderActionLine("class_rich", "social_class.nogeon_economy_land.rich", "action.nogeon_economy_land.class_rich.desc", 1500000),
                new TraderActionLine("class_tycoon", "social_class.nogeon_economy_land.tycoon", "action.nogeon_economy_land.class_tycoon.desc", 7500000),
                new TraderActionLine("class_billionaire", "social_class.nogeon_economy_land.billionaire", "action.nogeon_economy_land.class_billionaire.desc", 40000000)
            );
            case CROP -> List.of(
                new TraderActionLine("job_farmer", "job.nogeon_economy_land.farmer", "command.nogeon_economy_land.job.select", 0)
            );
            case FISHER -> List.of(
                new TraderActionLine("job_fisher", "job.nogeon_economy_land.fisher", "command.nogeon_economy_land.job.select", 0)
            );
            case MINER -> List.of(
                new TraderActionLine("job_miner", "job.nogeon_economy_land.miner", "command.nogeon_economy_land.job.select", 0)
            );
            case CHEF -> List.of(
                new TraderActionLine("job_cook", "job.nogeon_economy_land.cook", "command.nogeon_economy_land.job.select", 0)
            );
            case ENGINEER -> List.of(
                new TraderActionLine("job_engineer", "job.nogeon_economy_land.engineer", "command.nogeon_economy_land.job.select", 0)
            );
            case HUNTER -> {
                long cost = profile != null ? profile.getInventoryUpgradeCost() : 20000L;
                yield List.of(
                    new TraderActionLine("job_hunter", "job.nogeon_economy_land.hunter", "command.nogeon_economy_land.job.select", 0),
                    new TraderActionLine("hunter_shop_open", "action.nogeon_economy_land.hunter_shop_open", "action.nogeon_economy_land.hunter_shop_open.desc", 0),
                    new TraderActionLine("hunter_inventory_open", "action.nogeon_economy_land.hunter_inventory_open", "action.nogeon_economy_land.hunter_inventory_open.desc", 0),
                    new TraderActionLine("hunter_inventory_upgrade", "action.nogeon_economy_land.hunter_inventory_upgrade", "action.nogeon_economy_land.hunter_inventory_upgrade.desc", cost)
                );
            }
            case AUCTION -> List.of(
                new TraderActionLine("auction_open", "action.nogeon_economy_land.auction_open", "action.nogeon_economy_land.auction_open.desc", 0),
                new TraderActionLine("auction_notice", "action.nogeon_economy_land.auction_notice", "action.nogeon_economy_land.auction_notice.desc", 0)
            );
            default -> List.of();
        };
    }
}
