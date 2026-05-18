package poker.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a deck of 52 playing cards.
 * Provides methods to shuffle and deal cards.
 */
public class Deck {
    
    private List<Card> cards;
    private int nextCardIndex;
    
    public Deck() {
        reset();
    }
    
    /**
     * Resets the deck to a full set of 52 cards.
     */
    public void reset() {
        cards = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        nextCardIndex = 0;
    }
    
    /**
     * Shuffles the deck randomly.
     */
    public void shuffle() {
        Collections.shuffle(cards);
        nextCardIndex = 0;
    }
    
    /**
     * Deals the next card from the deck.
     * @return The next card, or null if deck is empty
     */
    public Card deal() {
        if (nextCardIndex >= cards.size()) {
            return null;
        }
        return cards.get(nextCardIndex++);
    }
    
    /**
     * Returns the number of cards remaining in the deck.
     */
    public int remainingCards() {
        return cards.size() - nextCardIndex;
    }
    
    /**
     * Returns all cards in the deck (for testing purposes).
     */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }
}
