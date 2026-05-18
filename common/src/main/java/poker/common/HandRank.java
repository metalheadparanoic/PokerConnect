package poker.common;

/**
 * Represents the ranking of poker hands from lowest to highest.
 */
public enum HandRank {
    HIGH_CARD(1, "High Card"),
    PAIR(2, "Pair"),
    TWO_PAIR(3, "Two Pair"),
    THREE_OF_A_KIND(4, "Three of a Kind"),
    STRAIGHT(5, "Straight"),
    FLUSH(6, "Flush"),
    FULL_HOUSE(7, "Full House"),
    FOUR_OF_A_KIND(8, "Four of a Kind"),
    STRAIGHT_FLUSH(9, "Straight Flush"),
    ROYAL_FLUSH(10, "Royal Flush");
    
    private final int value;
    private final String displayName;
    
    HandRank(int value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }
    
    public int getValue() {
        return value;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
