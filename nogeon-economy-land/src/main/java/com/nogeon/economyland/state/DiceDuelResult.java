package com.nogeon.economyland.state;

public record DiceDuelResult(long stake, int playerDieOne, int playerDieTwo, int dealerDieOne, int dealerDieTwo, long payout, String resultKey) {
}