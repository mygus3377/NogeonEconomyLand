package com.nogeon.economyland.lottery;

import com.nogeon.economyland.item.ModItems;
import com.nogeon.economyland.network.SyncCreditsPacket;
import com.nogeon.economyland.player.ExtendedInventoryDelivery;
import com.nogeon.economyland.player.PlayerProfile;
import com.nogeon.economyland.state.EconomyState;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;

public final class LotteryEvents {
    private static final long MORNING_DRAW_TIME = 2000L;
    private static final int FIRST = 1;
    private static final int SECOND = 2;
    private static final int THIRD = 3;
    private static final int FOURTH = 4;
    private static final int FIFTH = 5;
    private static final int ENCOURAGEMENT = 6;
    private static final int FAIL = 7;
    private static final int BLANK = 8;

    private LotteryEvents() {
    }

    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        EconomyState state = EconomyState.get(level.getServer());
        long dayTime = level.getDayTime();
        long day = dayTime / 24000L;
        long timeOfDay = dayTime % 24000L;
        if (timeOfDay != MORNING_DRAW_TIME || state.lastLotteryDrawDay() >= day) {
            return;
        }

        level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.nogeon_economy_land.lottery.drumroll"), false);
        List<UUID> entries = state.consumeLotteryEntries(day);
        if (entries.isEmpty()) {
            level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.nogeon_economy_land.lottery.no_entry"), false);
            return;
        }

        // Increment jackpots based on ticket sales before drawing
        state.incrementJackpots(entries.size());

        RandomSource random = level.getRandom();
        boolean announcedWinner = false;
        for (UUID playerId : entries) {
            if (applyOutcome(level, state, playerId, roll(random), random)) {
                announcedWinner = true;
            }
        }
        if (!announcedWinner) {
            level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable("message.nogeon_economy_land.lottery.no_winner"), false);
        }
        state.setDirty();
    }

    private static int roll(RandomSource random) {
        int roll = random.nextInt(100000);
        if (roll < 10) {
            return FIRST; // 0.01% (1/10,000)
        }
        if (roll < 60) {
            return SECOND; // 0.05% (1/2,000)
        }
        if (roll < 260) {
            return THIRD; // 0.2% (1/500)
        }
        if (roll < 1260) {
            return FOURTH; // 1.0% (1/100)
        }
        if (roll < 4260) {
            return FIFTH; // 3.0%
        }
        if (roll < 9260) {
            return ENCOURAGEMENT; // 5.0%
        }
        return BLANK;
    }

    private static boolean applyOutcome(ServerLevel level, EconomyState state, UUID playerId, int outcome, RandomSource random) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        String playerName = player != null ? player.getGameProfile().getName() : state.knownPlayerName(playerId);
        state.rememberPlayer(playerId, playerName);
        PlayerProfile profile = state.profile(playerId);

        return switch (outcome) {
            case FIRST -> {
                long jackpot = state.lotteryJackpot1();
                giveCredits(player, profile, jackpot);
                broadcast(level, playerName, rankKey(outcome), Component.translatable("currency.nogeon_economy_land.credits"), jackpot);
                state.resetJackpot1();
                yield true;
            }
            case SECOND -> {
                long jackpot = state.lotteryJackpot2();
                giveCredits(player, profile, jackpot);
                broadcast(level, playerName, rankKey(outcome), Component.translatable("currency.nogeon_economy_land.credits"), jackpot);
                state.resetJackpot2();
                yield true;
            }
            case THIRD -> {
                giveCredits(player, profile, 1_000_000L);
                broadcast(level, playerName, rankKey(outcome), Component.translatable("currency.nogeon_economy_land.credits"), 1_000_000L);
                yield true;
            }
            case FOURTH -> {
                giveCredits(player, profile, 200_000L);
                broadcast(level, playerName, rankKey(outcome), Component.translatable("currency.nogeon_economy_land.credits"), 200_000L);
                yield true;
            }
            case FIFTH -> {
                giveCredits(player, profile, 50_000L);
                broadcast(level, playerName, rankKey(outcome), Component.translatable("currency.nogeon_economy_land.credits"), 50_000L);
                yield true;
            }
            case ENCOURAGEMENT -> {
                giveCredits(player, profile, 10_000L);
                broadcast(level, playerName, rankKey(outcome), Component.translatable("currency.nogeon_economy_land.credits"), 10_000L);
                yield true;
            }
            case FAIL, BLANK -> {
                profile.addUnluckyTokens(1);
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.nogeon_economy_land.lottery.blank_alt"), false);
                }
                yield false;
            }
            default -> false;
        };
    }

    private static String rankKey(int outcome) {
        return switch (outcome) {
            case FIRST -> "lottery.nogeon_economy_land.rank.first";
            case SECOND -> "lottery.nogeon_economy_land.rank.second";
            case THIRD -> "lottery.nogeon_economy_land.rank.third";
            case FOURTH -> "lottery.nogeon_economy_land.rank.fourth";
            case FIFTH -> "lottery.nogeon_economy_land.rank.fifth";
            case ENCOURAGEMENT -> "lottery.nogeon_economy_land.rank.encouragement";
            default -> "";
        };
    }

    private static void giveCredits(ServerPlayer player, PlayerProfile profile, long amount) {
        profile.addCredits(amount);
        if (player != null) {
            SyncCreditsPacket.send(player, profile.credits());
        }
    }

    private static void giveItem(EconomyState state, ServerPlayer player, UUID playerId, Item item, int count) {
        if (player != null) {
            ItemStack stack = new ItemStack(item, count);
            ExtendedInventoryDelivery.giveOrDrop(player, stack);
            return;
        }
        state.queueItemReward(playerId, item, count);
    }

    private static void broadcast(ServerLevel level, String playerName, String rankKey, Component rewardName, long rewardCount) {
        level.getServer().getPlayerList().broadcastSystemMessage(Component.translatable(
            "message.nogeon_economy_land.lottery.winner",
            playerName,
            Component.translatable(rankKey),
            rewardName,
            rewardCount
        ), false);
    }

}
