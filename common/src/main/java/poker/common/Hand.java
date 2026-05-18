package poker.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a player's hand of cards.
 */
public class Hand {
    
    private final List<Card> cards;
    
    public Hand() {
        this.cards = new ArrayList<>();
    }
    
    /**
     * Adds a card to the hand.
     */
    public void addCard(Card card) {
        cards.add(card);
    }
    
    /**
     * Removes all cards from the hand.
     */
    public void clear() {
        cards.clear();
    }
    
    /**
     * Returns all cards in the hand.
     */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }
    
    /**
     * Returns the number of cards in the hand.
     */
    public int size() {
        return cards.size();
    }
    
    /**
     * Returns a sorted copy of the cards in the hand.
     */
    public List<Card> getSortedCards() {
        List<Card> sorted = new ArrayList<>(cards);
        Collections.sort(sorted);
        return sorted;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(cards.get(i));
        }
        return sb.toString();
    }
}
