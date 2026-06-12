package com.nogeon.economyland.player;

import java.util.Locale;

public enum SocialClass {
    COMMONER("commoner", 1, false, false, 0, 10, 5, false),
    MIDDLE("middle", 3, false, false, 0, 10, 4, false),
    RICH("rich", 5, true, false, 5, 8, 3, true),
    TYCOON("tycoon", 10, true, false, 10, 6, 2, true),
    BILLIONAIRE("billionaire", Integer.MAX_VALUE, true, true, 15, 4, 1, true);

    private final String id;
    private final int homeLimit;
    private final boolean canBuyIndustrialLand;
    private final boolean unlimitedHomes;
    private final int landDiscountPercent;
    private final int auctionFeePercent;
    private final int homeTeleportDelaySeconds;
    private final boolean homeAllowsMovement;

    SocialClass(String id, int homeLimit, boolean canBuyIndustrialLand, boolean unlimitedHomes, int landDiscountPercent,
        int auctionFeePercent, int homeTeleportDelaySeconds, boolean homeAllowsMovement) {
        this.id = id;
        this.homeLimit = homeLimit;
        this.canBuyIndustrialLand = canBuyIndustrialLand;
        this.unlimitedHomes = unlimitedHomes;
        this.landDiscountPercent = landDiscountPercent;
        this.auctionFeePercent = auctionFeePercent;
        this.homeTeleportDelaySeconds = homeTeleportDelaySeconds;
        this.homeAllowsMovement = homeAllowsMovement;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "social_class.nogeon_economy_land." + id;
    }

    public int homeLimit() {
        return homeLimit;
    }

    public boolean canBuyIndustrialLand() {
        return canBuyIndustrialLand;
    }

    public boolean unlimitedHomes() {
        return unlimitedHomes;
    }

    public int landDiscountPercent() {
        return landDiscountPercent;
    }

    public long discountedLandPrice(long basePrice) {
        return basePrice * (100 - landDiscountPercent) / 100;
    }

    public int auctionFeePercent() {
        return auctionFeePercent;
    }

    public int homeTeleportDelaySeconds() {
        return homeTeleportDelaySeconds;
    }

    public boolean homeAllowsMovement() {
        return homeAllowsMovement;
    }

    public int spawnReturnDelaySeconds() {
        return Math.max(7, 10 - Math.min(3, ordinal()));
    }

    public long maxBetCap() {
        return switch (this) {
            case COMMONER -> 10000L;
            case MIDDLE -> 50000L;
            case RICH -> 200000L;
            case TYCOON -> 500000L;
            case BILLIONAIRE -> 1000000L;
        };
    }

    public static SocialClass byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (SocialClass socialClass : values()) {
            if (socialClass.id.equals(normalized)) {
                return socialClass;
            }
        }
        throw new IllegalArgumentException("Unknown social class: " + id);
    }
}
