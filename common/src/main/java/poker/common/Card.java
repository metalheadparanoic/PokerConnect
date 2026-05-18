package poker.common;

/**
 * Represents a playing card with a suit and rank.
 */
public class Card implements Comparable<Card> {
    
    public enum Suit {
        HEARTS("H"), DIAMONDS("D"), CLUBS("C"), SPADES("S");
        
        private final String symbol;
        
        Suit(String symbol) {
            this.symbol = symbol;
        }
        
        public String getSymbol() {
            return symbol;
        }
    }
    
    public enum Rank {
        TWO("2", 2), THREE("3", 3), FOUR("4", 4), FIVE("5", 5), 
        SIX("6", 6), SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9), 
        TEN("T", 10), JACK("J", 11), QUEEN("Q", 12), KING("K", 13), ACE("A", 14);
        
        private final String symbol;
        private final int value;
        
        Rank(String symbol, int value) {
            this.symbol = symbol;
            this.value = value;
        }
        
        public String getSymbol() {
            return symbol;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    private final Rank rank;
    private final Suit suit;
    
    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }
    
    public Rank getRank() {
        return rank;
    }
    
    public Suit getSuit() {
        return suit;
    }
    
    @Override
    public String toString() {
        return rank.getSymbol() + suit.getSymbol();
    }
    
    @Override
    public int compareTo(Card other) {
        return Integer.compare(this.rank.getValue(), other.rank.getValue());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return rank == card.rank && suit == card.suit;
    }
    
    @Override
    public int hashCode() {
        return 31 * rank.hashCode() + suit.hashCode();
    }
}
