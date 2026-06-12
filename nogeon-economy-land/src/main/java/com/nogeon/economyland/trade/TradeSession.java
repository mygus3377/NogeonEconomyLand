package com.nogeon.economyland.trade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TradeSession {
    private static final int MAX_CHAT_MESSAGES = 12;

    private final UUID firstPlayer;
    private final UUID secondPlayer;
    private final LinkedHashMap<String, Integer> firstOffers = new LinkedHashMap<>();
    private final LinkedHashMap<String, Integer> secondOffers = new LinkedHashMap<>();
    private final LinkedHashSet<Integer> firstLandOffers = new LinkedHashSet<>();
    private final LinkedHashSet<Integer> secondLandOffers = new LinkedHashSet<>();
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private long firstCredits;
    private long secondCredits;
    private boolean firstReady;
    private boolean secondReady;
    private boolean firstConfirmed;
    private boolean secondConfirmed;

    public TradeSession(UUID firstPlayer, UUID secondPlayer) {
        this.firstPlayer = firstPlayer;
        this.secondPlayer = secondPlayer;
    }

    public UUID firstPlayer() {
        return firstPlayer;
    }

    public UUID secondPlayer() {
        return secondPlayer;
    }

    public UUID partner(UUID playerId) {
        return firstPlayer.equals(playerId) ? secondPlayer : firstPlayer;
    }

    public Map<String, Integer> offers(UUID playerId) {
        return firstPlayer.equals(playerId) ? firstOffers : secondOffers;
    }

    public Map<String, Integer> partnerOffers(UUID playerId) {
        return firstPlayer.equals(playerId) ? secondOffers : firstOffers;
    }

    public Set<Integer> landOffers(UUID playerId) {
        return firstPlayer.equals(playerId) ? firstLandOffers : secondLandOffers;
    }

    public Set<Integer> partnerLandOffers(UUID playerId) {
        return firstPlayer.equals(playerId) ? secondLandOffers : firstLandOffers;
    }

    public long credits(UUID playerId) {
        return firstPlayer.equals(playerId) ? firstCredits : secondCredits;
    }

    public long partnerCredits(UUID playerId) {
        return firstPlayer.equals(playerId) ? secondCredits : firstCredits;
    }

    public void setCredits(UUID playerId, long credits) {
        if (firstPlayer.equals(playerId)) {
            firstCredits = credits;
        } else {
            secondCredits = credits;
        }
        resetAgreement();
    }

    public void addOffer(UUID playerId, String itemId, int count) {
        offers(playerId).merge(itemId, count, Integer::sum);
        resetAgreement();
    }

    public void addLandOffer(UUID playerId, int landId) {
        landOffers(playerId).add(landId);
        resetAgreement();
    }

    public void clearOffers(UUID playerId) {
        offers(playerId).clear();
        landOffers(playerId).clear();
        if (firstPlayer.equals(playerId)) {
            firstCredits = 0L;
        } else {
            secondCredits = 0L;
        }
        resetAgreement();
    }

    public List<ChatMessage> chatMessages() {
        return List.copyOf(chatMessages);
    }

    public void addChat(UUID senderId, String senderName, String message) {
        chatMessages.add(new ChatMessage(senderId, senderName, message));
        if (chatMessages.size() > MAX_CHAT_MESSAGES) {
            chatMessages.remove(0);
        }
    }

    public boolean ready(UUID playerId) {
        return firstPlayer.equals(playerId) ? firstReady : secondReady;
    }

    public boolean partnerReady(UUID playerId) {
        return firstPlayer.equals(playerId) ? secondReady : firstReady;
    }

    public boolean confirmed(UUID playerId) {
        return firstPlayer.equals(playerId) ? firstConfirmed : secondConfirmed;
    }

    public boolean partnerConfirmed(UUID playerId) {
        return firstPlayer.equals(playerId) ? secondConfirmed : firstConfirmed;
    }

    public void toggleReady(UUID playerId) {
        if (firstPlayer.equals(playerId)) {
            firstReady = !firstReady;
        } else {
            secondReady = !secondReady;
        }
        firstConfirmed = false;
        secondConfirmed = false;
    }

    public boolean canConfirm(UUID playerId) {
        return ready(playerId) && partnerReady(playerId);
    }

    public void confirm(UUID playerId) {
        if (firstPlayer.equals(playerId)) {
            firstConfirmed = true;
        } else {
            secondConfirmed = true;
        }
    }

    public boolean fullyConfirmed() {
        return firstReady && secondReady && firstConfirmed && secondConfirmed;
    }

    public record ChatMessage(UUID senderId, String senderName, String message) {
    }

    private void resetAgreement() {
        firstReady = false;
        secondReady = false;
        firstConfirmed = false;
        secondConfirmed = false;
    }
}