package com.nogeon.economyland.network;

import com.nogeon.economyland.entity.TraderKind;
import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.item.SmithingService;
import com.nogeon.economyland.menu.AuctionOpener;
import com.nogeon.economyland.menu.DiceDuelOpener;
import com.nogeon.economyland.menu.EnhancementScrollOpener;
import com.nogeon.economyland.menu.ExtendedInventoryOpener;
import com.nogeon.economyland.menu.GachaCategory;
import com.nogeon.economyland.menu.GachaOpener;
import com.nogeon.economyland.menu.HighLowOpener;
import com.nogeon.economyland.menu.LandHomeOpener;
import com.nogeon.economyland.menu.JobChangeOpener;
import com.nogeon.economyland.menu.LuckExchangeOpener;
import com.nogeon.economyland.menu.DeconstructOpener;
import com.nogeon.economyland.menu.ReforgeOpener;
import com.nogeon.economyland.menu.SmithOpener;
import com.nogeon.economyland.menu.SlotMachineOpener;
import com.nogeon.economyland.menu.SocketUpgradeOpener;
import com.nogeon.economyland.menu.TraderActionLine;
import com.nogeon.economyland.menu.TraderActionOpener;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import com.nogeon.economyland.player.SocialClass;
import com.nogeon.economyland.state.DiceDuelResult;
import com.nogeon.economyland.state.EconomyState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

public final class TraderActionPacket {
    private final String kindId;
    private final String traderDatabaseId;
    private final String actionId;
    private final long stake;

    public TraderActionPacket(String kindId, String actionId) {
        this(kindId, "", actionId, -1L);
    }

    public TraderActionPacket(String kindId, String actionId, long stake) {
        this(kindId, "", actionId, stake);
    }

    public TraderActionPacket(String kindId, String traderDatabaseId, String actionId, long stake) {
        this.kindId = kindId;
        this.traderDatabaseId = traderDatabaseId == null ? "" : traderDatabaseId;
        this.actionId = actionId;
        this.stake = stake;
    }

    public static void encode(TraderActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.kindId);
        buffer.writeUtf(packet.traderDatabaseId);
        buffer.writeUtf(packet.actionId);
        buffer.writeLong(packet.stake);
    }

    public static TraderActionPacket decode(FriendlyByteBuf buffer) {
        return new TraderActionPacket(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readLong());
    }

    public static void handle(TraderActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            TraderKind kind = TraderKind.byId(packet.kindId);
            EconomyState state = EconomyState.get(player.server);
            PlayerProfile profile = state.profile(player.getUUID());
            TraderActionLine line = findLine(kind, packet.actionId);
            if (line == null) {
                return;
            }

            runAction(player, state, profile, kind, packet.traderDatabaseId, line, packet.stake);
            if (!opensDedicatedScreen(kind, line.actionId())) {
                TraderActionOpener.open(player, kind);
            }
        });
        context.setPacketHandled(true);
    }

    private static boolean opensDedicatedScreen(TraderKind kind, String actionId) {
        return (kind == TraderKind.LAND && "land_home".equals(actionId))
            || (kind == TraderKind.AUCTION && "auction_open".equals(actionId))
            || (kind == TraderKind.LOTTERY && "luck_exchange_basic".equals(actionId))
            || (kind == TraderKind.GAMBLER && "dice_duel".equals(actionId))
            || (kind == TraderKind.GAMBLER && "high_low".equals(actionId))
            || (kind == TraderKind.GAMBLER && "slot_machine".equals(actionId))
            || (kind == TraderKind.GACHA && actionId.startsWith("gacha_"))
            || (kind == TraderKind.SMITH && ("smith_open".equals(actionId) || "smith_shop_open".equals(actionId) || "smith_deconstruct".equals(actionId) || "smith_scrolls".equals(actionId) || "smith_reforge".equals(actionId) || "smith_socket".equals(actionId)))
            || (kind == TraderKind.HUNTER && ("hunter_inventory_open".equals(actionId) || "hunter_shop_open".equals(actionId)))
            || actionId.startsWith("job_");
    }

    private static TraderActionLine findLine(TraderKind kind, String actionId) {
        for (TraderActionLine line : TraderActionOpener.lines(kind)) {
            if (line.actionId().equals(actionId)) {
                return line;
            }
        }
        return null;
    }

    private static void runAction(ServerPlayer player, EconomyState state, PlayerProfile profile, TraderKind kind, String traderDatabaseId, TraderActionLine line, long requestedStake) {
        switch (line.actionId()) {
            case "lottery_daily" -> {
                if (!buyLottery(player, state, profile, line.price())) {
                    return;
                }
            }
            case "luck_exchange_basic" -> LuckExchangeOpener.open(player);
            case "dice_duel" -> {
                long stake = resolvedStake(requestedStake, line.price());
                if (requestedStake <= 0L) {
                    DiceDuelOpener.openSetup(player);
                    return;
                }
                long baseCap = profile.socialClass().maxBetCap();
                long maxCap = Math.min(1000000L, Math.round(baseCap * (1.0D + Math.min(10, profile.gambleStreak()) * 0.1D)));
                if (stake > maxCap) {
                    player.displayClientMessage(Component.literal("배팅 한도를 초과했습니다. (최대 " + maxCap + " C)"), false);
                    return;
                }
                if (!spendCredits(player, profile, stake)) {
                    return;
                }
                DiceDuelOpener.open(player, playDice(player, profile, stake));
            }
            case "high_low" -> {
                long stake = resolvedStake(requestedStake, line.price());
                if (requestedStake <= 0L) {
                    HighLowOpener.openSetup(player);
                    return;
                }
                if (!spendCredits(player, profile, stake)) {
                    return;
                }
                HighLowOpener.open(player, state.startHighLow(player, stake));
            }
            case "slot_machine" -> SlotMachineOpener.openSetup(player);
            case "gacha_basic", "gacha_middle", "gacha_high", "gacha_legend" -> {
                GachaOpener.open(player, traderDatabaseId, line.actionId(), GachaCategory.ITEM.id(), line.price(), 1);
            }
            case "smith_shop_open" -> {
                java.util.List<com.nogeon.economyland.shop.ShopEntry> entries = com.nogeon.economyland.menu.ShopOpener.entriesFor(player, TraderKind.SMITH, traderDatabaseId);
                com.nogeon.economyland.menu.ShopOpener.openShop(player, TraderKind.SMITH, traderDatabaseId, entries);
            }
            case "smith_open" -> SmithOpener.open(player, -1, null);
            case "smith_deconstruct" -> DeconstructOpener.open(player, -1, null, true);
            case "smith_scrolls" -> EnhancementScrollOpener.open(player, -1, null);
            case "smith_reforge" -> ReforgeOpener.open(player, -1, null);
            case "smith_socket" -> SocketUpgradeOpener.open(player, -1, null);
            case "land_home" -> LandHomeOpener.open(player);
            case "auction_open" -> AuctionOpener.open(player);
            case "auction_notice" -> player.displayClientMessage(Component.translatable("action.nogeon_economy_land.auction_notice.desc"), false);
            case "job_farmer" -> JobChangeOpener.open(player, "farmer");
            case "job_fisher" -> JobChangeOpener.open(player, "fisher");
            case "job_miner" -> JobChangeOpener.open(player, "miner");
            case "job_cook" -> JobChangeOpener.open(player, "cook");
            case "job_hunter" -> JobChangeOpener.open(player, "hunter");
            case "job_engineer" -> JobChangeOpener.open(player, "engineer");
            case "buy_basic_deed" -> {
                if (!spendCredits(player, profile, line.price())) {
                    return;
                }
                give(player, ModItems.BASIC_LAND_DEED.get(), 1);
            }
            case "buy_normal_deed" -> {
                if (!spendCredits(player, profile, line.price())) {
                    return;
                }
                give(player, ModItems.NORMAL_LAND_DEED.get(), 1);
            }
            case "buy_industrial_deed" -> {
                if (!spendCredits(player, profile, line.price())) {
                    return;
                }
                give(player, ModItems.INDUSTRIAL_LAND_DEED.get(), 1);
            }
            case "class_middle" -> {
                if (!upgradeClass(player, profile, line.price(), SocialClass.MIDDLE)) {
                    return;
                }
            }
            case "class_rich" -> {
                if (!upgradeClass(player, profile, line.price(), SocialClass.RICH)) {
                    return;
                }
            }
            case "class_tycoon" -> {
                if (!upgradeClass(player, profile, line.price(), SocialClass.TYCOON)) {
                    return;
                }
            }
            case "class_billionaire" -> {
                if (!upgradeClass(player, profile, line.price(), SocialClass.BILLIONAIRE)) {
                    return;
                }
            }
            case "hunter_shop_open" -> {
                java.util.List<com.nogeon.economyland.shop.ShopEntry> entries = com.nogeon.economyland.menu.ShopOpener.entriesFor(player, TraderKind.HUNTER, traderDatabaseId);
                com.nogeon.economyland.menu.ShopOpener.openShop(player, TraderKind.HUNTER, traderDatabaseId, entries);
            }
            case "hunter_inventory_open" -> {
                if (profile.inventoryExtLevel() <= 0) {
                    player.displayClientMessage(Component.literal("보관함을 먼저 업그레이드해야 사용할 수 있습니다."), false);
                    TraderActionOpener.open(player, kind, traderDatabaseId);
                    return;
                }
                ExtendedInventoryOpener.open(player);
            }
            case "hunter_inventory_upgrade" -> {
                long price = profile.getInventoryUpgradeCost();
                if (!spendCredits(player, profile, price)) {
                    return;
                }
                profile.setInventoryExtLevel(profile.inventoryExtLevel() + 1);
                player.displayClientMessage(Component.translatable("message.nogeon_economy_land.inventory_upgraded", profile.inventoryExtLevel()), false);
            }
            default -> player.displayClientMessage(Component.translatable("message.nogeon_economy_land.action_soon"), false);
        }

        SyncCreditsPacket.send(player, profile.credits());
        state.setDirty();
    }

    private static boolean spendCredits(ServerPlayer player, PlayerProfile profile, long amount) {
        if (amount <= 0 || profile.spendCredits(amount)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("message.nogeon_economy_land.shop.no_money"), false);
        return false;
    }

    private static long resolvedStake(long requestedStake, long defaultStake) {
        return requestedStake > 0L ? requestedStake : defaultStake;
    }

    private static boolean buyLottery(ServerPlayer player, EconomyState state, PlayerProfile profile, long price) {
        if (state.lotteryTicketsRemaining(player.getUUID()) <= 0) {
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.lottery.sold_out"), false);
            return false;
        }
        if (!spendCredits(player, profile, price)) {
            return false;
        }
        if (!state.recordLotteryEntry(player)) {
            profile.addCredits(price);
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.lottery.sold_out"), false);
            return false;
        }
        player.displayClientMessage(Component.translatable("message.nogeon_economy_land.lottery.bought", state.lotteryTicketsRemaining(player.getUUID())), false);
        return true;
    }

    private static DiceDuelResult playDice(ServerPlayer player, PlayerProfile profile, long stake) {
        RandomSource random = player.getRandom();
        int playerDieOne = random.nextInt(6) + 1;
        int playerDieTwo = random.nextInt(6) + 1;
        int dealerDieOne = random.nextInt(6) + 1;
        int dealerDieTwo = random.nextInt(6) + 1;
        int playerRoll = playerDieOne + playerDieTwo;
        int dealerRoll = dealerDieOne + dealerDieTwo;
        long payout = 0L;
        String resultKey;
        if (playerRoll > dealerRoll) {
            double bonus = 1.0D + Math.min(10, profile.gambleStreak()) * 0.05D;
            long basePayout = playerRoll == 12 ? stake * 3L : stake * 18L / 10L;
            payout = Math.round(basePayout * bonus);
            profile.addCredits(payout);
            profile.incrementGambleStreak();
            resultKey = "gui.nogeon_economy_land.gamble_result_win";
        } else if (playerRoll == dealerRoll) {
            payout = stake;
            profile.addCredits(payout);
            resultKey = "gui.nogeon_economy_land.gamble_result_draw";
        } else {
            profile.resetGambleStreak();
            resultKey = "gui.nogeon_economy_land.gamble_result_lose";
        }
        return new DiceDuelResult(stake, playerDieOne, playerDieTwo, dealerDieOne, dealerDieTwo, payout, resultKey);
    }

    private static void setClass(ServerPlayer player, PlayerProfile profile, SocialClass socialClass) {
        profile.setSocialClass(socialClass);
        com.nogeon.economyland.player.PlayerDisplayNameManager.refresh(player, profile);
        player.displayClientMessage(Component.translatable("message.nogeon_economy_land.class.changed", Component.translatable(socialClass.translationKey())), false);
    }

    private static boolean upgradeClass(ServerPlayer player, PlayerProfile profile, long price, SocialClass socialClass) {
        if (profile.socialClass().ordinal() >= socialClass.ordinal()) {
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.class.upgrade_only"), false);
            return false;
        }
        if (profile.socialClass().ordinal() + 1 != socialClass.ordinal()) {
            player.displayClientMessage(Component.translatable("message.nogeon_economy_land.class.previous_required",
                Component.translatable(SocialClass.values()[socialClass.ordinal() - 1].translationKey())), false);
            return false;
        }
        if (!spendCredits(player, profile, price)) {
            return false;
        }
        setClass(player, profile, socialClass);
        return true;
    }

    private static void give(ServerPlayer player, Item item, int count) {
        ExtendedInventoryDelivery.giveOrDrop(player, new ItemStack(item, count));
    }
}
