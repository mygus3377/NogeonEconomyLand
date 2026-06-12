package com.nogeon.economyland.state;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerPlayer;
import com.nogeon.economyland.player.PlayerProfile;

public final class HighLowSession {
    private final UUID playerId;
    private long stake;
    private final List<Integer> playerCards = new ArrayList<>();
    private final List<Integer> dealerCards = new ArrayList<>();
    private long payout;
    private Result result = Result.READY;

    public HighLowSession(UUID playerId, long stake) {
        this.playerId = playerId;
        this.stake = stake;
    }

    public UUID playerId() {
        return playerId;
    }

    public long stake() {
        return stake;
    }

    public List<Integer> playerCards() {
        return playerCards;
    }

    public List<Integer> dealerCards() {
        return dealerCards;
    }

    public long payout() {
        return payout;
    }

    public Result result() {
        return result;
    }

    public boolean canHit() {
        return result == Result.READY && calculateVal(playerCards) < 21;
    }

    public boolean canStand() {
        return result == Result.READY;
    }

    public boolean canDoubleDown(ServerPlayer player) {
        if (playerCards.size() != 2 || result != Result.READY) {
            return false;
        }
        PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
        return profile.credits() >= stake;
    }

    public void start(ServerPlayer player) {
        playerCards.clear();
        dealerCards.clear();
        payout = 0L;
        result = Result.READY;

        // Draw initial 2 cards
        playerCards.add(drawCard(player));
        playerCards.add(drawCard(player));
        dealerCards.add(drawCard(player));
        dealerCards.add(drawCard(player));

        int pVal = calculateVal(playerCards);
        int dVal = calculateVal(dealerCards);

        // Check for natural Blackjack (21)
        if (pVal == 21) {
            if (dVal == 21) {
                result = Result.PUSH;
                payout = stake; // Tie returns the bet
            } else {
                result = Result.JACKPOT; // Blackjack pays 2.5x bet (1.5x net payout)
                PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
                double bonus = 1.0D + Math.min(10, profile.gambleStreak()) * 0.05D;
                payout = Math.round(stake * 2.5D * bonus);
                profile.incrementGambleStreak();
            }
        }
    }

    public void hit(ServerPlayer player) {
        if (!canHit()) {
            return;
        }
        playerCards.add(drawCard(player));
        int pVal = calculateVal(playerCards);
        if (pVal > 21) {
            result = Result.LOSE;
            payout = 0L;
            PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
            profile.resetGambleStreak();
        } else if (pVal == 21) {
            stand(player); // Auto-stand on 21
        }
    }

    public void stand(ServerPlayer player) {
        if (result != Result.READY) {
            return;
        }

        // Dealer plays until total is 17 or higher
        int dVal = calculateVal(dealerCards);
        while (dVal < 17) {
            dealerCards.add(drawCard(player));
            dVal = calculateVal(dealerCards);
        }

        int pVal = calculateVal(playerCards);
        PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());

        if (dVal > 21) {
            // Dealer bust
            result = Result.WIN;
            double bonus = 1.0D + Math.min(10, profile.gambleStreak()) * 0.05D;
            payout = Math.round(stake * 2L * bonus); // Normal win returns 2x bet (1.0x net payout)
            profile.incrementGambleStreak();
        } else if (pVal > dVal) {
            result = Result.WIN;
            double bonus = 1.0D + Math.min(10, profile.gambleStreak()) * 0.05D;
            payout = Math.round(stake * 2L * bonus);
            profile.incrementGambleStreak();
        } else if (pVal < dVal) {
            result = Result.LOSE;
            payout = 0L;
            profile.resetGambleStreak();
        } else {
            result = Result.PUSH;
            payout = stake;
        }
    }

    public void doubleDown(ServerPlayer player) {
        if (!canDoubleDown(player)) {
            return;
        }
        // Deduct double down stake
        PlayerProfile profile = EconomyState.get(player.server).profile(player.getUUID());
        profile.spendCredits(stake);
        stake *= 2;

        // Draw exactly one card
        playerCards.add(drawCard(player));
        int pVal = calculateVal(playerCards);
        if (pVal > 21) {
            result = Result.LOSE;
            payout = 0L;
            profile.resetGambleStreak();
        } else {
            stand(player);
        }
    }

    private int drawCard(ServerPlayer player) {
        return player.getRandom().nextInt(13) + 1;
    }

    public static int calculateVal(List<Integer> cards) {
        int sum = 0;
        int aces = 0;
        for (int card : cards) {
            if (card == 1) {
                aces++;
                sum += 11;
            } else if (card >= 11 && card <= 13) {
                sum += 10;
            } else {
                sum += card;
            }
        }
        while (sum > 21 && aces > 0) {
            sum -= 10;
            aces--;
        }
        return sum;
    }

    public String playerCardsString() {
        return playerCards.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    public String dealerCardsString() {
        return dealerCards.stream().map(Object::toString).collect(Collectors.joining(","));
    }

    public static List<Integer> parseCards(String str) {
        List<Integer> list = new ArrayList<>();
        if (str == null || str.isBlank()) {
            return list;
        }
        for (String s : str.split(",")) {
            try {
                list.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    public String statusKey() {
        return switch (result) {
            case WIN -> "gui.nogeon_economy_land.blackjack_status_win";
            case LOSE -> "gui.nogeon_economy_land.blackjack_status_lose";
            case PUSH -> "gui.nogeon_economy_land.blackjack_status_push";
            case READY -> "gui.nogeon_economy_land.blackjack_status_ready";
            case JACKPOT -> "gui.nogeon_economy_land.blackjack_status_jackpot";
        };
    }

    public enum Result {
        READY,
        WIN,
        LOSE,
        PUSH,
        JACKPOT
    }
}