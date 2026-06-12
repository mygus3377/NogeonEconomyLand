package com.nogeon.economyland.state;

public record SlotMachineResult(long stake, int leftSymbol, int middleSymbol, int rightSymbol, long payout, String resultKey) {
}
