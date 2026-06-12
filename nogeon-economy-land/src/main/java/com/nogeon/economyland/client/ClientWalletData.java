package com.nogeon.economyland.client;

public final class ClientWalletData {
    private static long credits;

    private ClientWalletData() {
    }

    public static long credits() {
        return credits;
    }

    public static void setCredits(long value) {
        credits = Math.max(0, value);
    }
}
