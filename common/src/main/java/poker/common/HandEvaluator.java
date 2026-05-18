package poker.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Evaluates poker hands and determines rankings.
 */
public class HandEvaluator {
    
    /**
     * Represents the result of evaluating a poker hand.
     */
    public static class HandResult implements Comparable<HandResult> {
        private final HandRank rank;
        private final List<Integer> tiebreakers; // Values for breaking ties
        private Long playerId; // Player ID for tracking winners
        
        public HandResult(HandRank rank, List<Integer> tiebreakers) {
            this.rank = rank;
            this.tiebreakers = tiebreakers;
        }
        
        public HandRank getRank() {
            return rank;
        }
        
        public List<Integer> getTiebreakers() {
            return tiebreakers;
        }
        
        public Long getPlayerId() {
            return playerId;
        }
        
        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }
        
        @Override
        public int compareTo(HandResult other) {
            // Compare by rank first
            int rankCompare = Integer.compare(this.rank.getValue(), other.rank.getValue());
            if (rankCompare != 0) {
                return rankCompare;
            }
            
            // If ranks are equal, compare tiebreakers
            for (int i = 0; i < Math.min(tiebreakers.size(), other.tiebreakers.size()); i++) {
                int compare = Integer.compare(tiebreakers.get(i), other.tiebreakers.get(i));
                if (compare != 0) {
                    return compare;
                }
            }
            
            return 0; // Hands are equal
        }
    }
    
    /**
     * Evaluates the best 5-card poker hand from the given cards.
     * @param cards List of cards (typically 5-7 cards for Texas Hold'em)
     * @return HandResult containing the rank and tiebreakers
     */
    public static HandResult evaluateHand(List<Card> cards) {
        if (cards.size() < 5) {
            throw new IllegalArgumentException("Need at least 5 cards to evaluate a hand");
        }

        // Texas Hold'em: if we have more than 5 cards, we must evaluate all 5-card combinations
        // and select the best.
        if (cards.size() == 5) {
            return evaluateFive(cards);
        }

        HandResult best = null;
        int n = cards.size();
        for (int a = 0; a < n - 4; a++) {
            for (int b = a + 1; b < n - 3; b++) {
                for (int c = b + 1; c < n - 2; c++) {
                    for (int d = c + 1; d < n - 1; d++) {
                        for (int e = d + 1; e < n; e++) {
                            List<Card> five = List.of(cards.get(a), cards.get(b), cards.get(c), cards.get(d), cards.get(e));
                            HandResult r = evaluateFive(five);
                            if (best == null || r.compareTo(best) > 0) {
                                best = r;
                            }
                        }
                    }
                }
            }
        }

        return best;
    }
    
    /**
     * Evaluates the best 5-card poker hand from a Hand object.
     * @param hand Hand object containing cards
     * @return HandResult containing the rank and tiebreakers
     */
    public static HandResult evaluateHand(Hand hand) {
        return evaluateHand(hand.getCards());
    }

    private static HandResult evaluateFive(List<Card> handCards) {
        if (handCards.size() != 5) {
            throw new IllegalArgumentException("evaluateFive expects exactly 5 cards");
        }

        if (isRoyalFlush(handCards)) {
            return new HandResult(HandRank.ROYAL_FLUSH, Collections.singletonList(14));
        }

        if (isStraightFlush(handCards)) {
            return new HandResult(HandRank.STRAIGHT_FLUSH,
                Collections.singletonList(getStraightHighValue(handCards)));
        }

        if (isFourOfAKind(handCards)) {
            return new HandResult(HandRank.FOUR_OF_A_KIND, getFourOfAKindTiebreakers(handCards));
        }

        if (isFullHouse(handCards)) {
            return new HandResult(HandRank.FULL_HOUSE, getFullHouseTiebreakers(handCards));
        }

        if (isFlush(handCards)) {
            return new HandResult(HandRank.FLUSH, getHighCardValues(handCards));
        }

        if (isStraight(handCards)) {
            return new HandResult(HandRank.STRAIGHT,
                Collections.singletonList(getStraightHighValue(handCards)));
        }

        if (isThreeOfAKind(handCards)) {
            return new HandResult(HandRank.THREE_OF_A_KIND, getThreeOfAKindTiebreakers(handCards));
        }

        if (isTwoPair(handCards)) {
            return new HandResult(HandRank.TWO_PAIR, getTwoPairTiebreakers(handCards));
        }

        if (isPair(handCards)) {
            return new HandResult(HandRank.PAIR, getPairTiebreakers(handCards));
        }

        return new HandResult(HandRank.HIGH_CARD, getHighCardValues(handCards));
    }
    
    public static boolean isRoyalFlush(List<Card> cards) {
        if (!isFlush(cards) || !isStraight(cards)) {
            return false;
        }
        // Must be exactly 10-J-Q-K-A of the same suit
        Set<Card.Rank> ranks = cards.stream().map(Card::getRank).collect(Collectors.toSet());
        return ranks.size() == 5
            && ranks.contains(Card.Rank.TEN)
            && ranks.contains(Card.Rank.JACK)
            && ranks.contains(Card.Rank.QUEEN)
            && ranks.contains(Card.Rank.KING)
            && ranks.contains(Card.Rank.ACE);
    }
    
    public static boolean isStraightFlush(List<Card> cards) {
        return isFlush(cards) && isStraight(cards);
    }
    
    public static boolean isFourOfAKind(List<Card> cards) {
        Map<Card.Rank, Long> rankCounts = getRankCounts(cards);
        return rankCounts.values().stream().anyMatch(count -> count == 4);
    }
    
    public static boolean isFullHouse(List<Card> cards) {
        Map<Card.Rank, Long> rankCounts = getRankCounts(cards);
        boolean hasThree = rankCounts.values().stream().anyMatch(count -> count == 3);
        boolean hasPair = rankCounts.values().stream().anyMatch(count -> count == 2);
        return hasThree && hasPair;
    }
    
    public static boolean isFlush(List<Card> cards) {
        Card.Suit firstSuit = cards.get(0).getSuit();
        return cards.stream().allMatch(card -> card.getSuit() == firstSuit);
    }
    
    public static boolean isStraight(List<Card> cards) {
        List<Card> sorted = new ArrayList<>(cards);
        Collections.sort(sorted);

        // Check normal straight
        boolean normal = true;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).getRank().getValue() != sorted.get(i - 1).getRank().getValue() + 1) {
                normal = false;
                break;
            }
        }

        if (normal) {
            return true;
        }

        // Ace-low straight (A-2-3-4-5)
        return sorted.get(4).getRank() == Card.Rank.ACE
            && sorted.get(0).getRank() == Card.Rank.TWO
            && sorted.get(1).getRank() == Card.Rank.THREE
            && sorted.get(2).getRank() == Card.Rank.FOUR
            && sorted.get(3).getRank() == Card.Rank.FIVE;
    }

    private static int getStraightHighValue(List<Card> cards) {
        // Assumes exactly 5 cards.
        List<Card> sorted = new ArrayList<>(cards);
        Collections.sort(sorted);
        // Wheel: A-2-3-4-5 => high is 5
        if (sorted.get(4).getRank() == Card.Rank.ACE
            && sorted.get(0).getRank() == Card.Rank.TWO
            && sorted.get(1).getRank() == Card.Rank.THREE
            && sorted.get(2).getRank() == Card.Rank.FOUR
            && sorted.get(3).getRank() == Card.Rank.FIVE) {
            return Card.Rank.FIVE.getValue();
        }
        return sorted.get(4).getRank().getValue();
    }
    
    public static boolean isThreeOfAKind(List<Card> cards) {
        Map<Card.Rank, Long> rankCounts = getRankCounts(cards);
        return rankCounts.values().stream().anyMatch(count -> count == 3);
    }
    
    public static boolean isTwoPair(List<Card> cards) {
        Map<Card.Rank, Long> rankCounts = getRankCounts(cards);
        return rankCounts.values().stream().filter(count -> count == 2).count() == 2;
    }
    
    public static boolean isPair(List<Card> cards) {
        Map<Card.Rank, Long> rankCounts = getRankCounts(cards);
        return rankCounts.values().stream().anyMatch(count -> count == 2);
    }
    
    public static boolean isHighCard(List<Card> cards) {
        return !isPair(cards) && !isTwoPair(cards) && !isThreeOfAKind(cards) &&
               !isStraight(cards) && !isFlush(cards) && !isFullHouse(cards) &&
               !isFourOfAKind(cards) && !isStraightFlush(cards);
    }
    
    /**
     * Compares two hands and returns the winner.
     * @return Positive if hand1 wins, negative if hand2 wins, 0 if tie
     */
    public static int compareHands(List<Card> hand1, List<Card> hand2) {
        HandResult result1 = evaluateHand(hand1);
        HandResult result2 = evaluateHand(hand2);
        return result1.compareTo(result2);
    }
    
    private static Map<Card.Rank, Long> getRankCounts(List<Card> cards) {
        return cards.stream()
            .collect(Collectors.groupingBy(Card::getRank, Collectors.counting()));
    }
    
    private static List<Integer> getHighCardValues(List<Card> cards) {
        return cards.stream()
            .sorted(Comparator.reverseOrder())
            .map(card -> card.getRank().getValue())
            .collect(Collectors.toList());
    }
    
    private static List<Integer> getFourOfAKindTiebreakers(List<Card> cards) {
        Map<Card.Rank, Long> rankCounts = getRankCounts(cards);
        List<Integer> tiebreakers = new ArrayList<>();
        
        // Add the four of a kind rank
        rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 4)
            .forEach(entry -> tiebreakers.add(entry.getKey().getValue()));
        
        // Add the kicker
        rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 1)
            .forEach(entry -> tiebreakers.add(entry.getKey().getValue()));
        
        return tiebreakers;
    }
    
    private static List<Integer> getFullHouseTiebreakers(List<Card> cards) {
        Map<Card.Rank, Long> rankCounts = getRankCounts(cards);
        List<Integer> tiebreakers = new ArrayList<>();
        
        // Add the three of a kind rank
        rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 3)
            .forEach(entry -> tiebreakers.add(entry.getKey().getValue()));
        
        // Add the pair rank
        rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 2)
            .forEach(entry -> tiebreakers.add(entry.getKey().getValue()));
        
        return tiebreakers;
    }
    
    private static List<Integer> getThreeOfAKindTiebreakers(List<Card> cards) {
        Map<Card.Rank, Long> rankCounts = getRankCounts(cards);
        List<Integer> tiebreakers = new ArrayList<>();
        
        // Add the three of a kind rank
        rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 3)
            .forEach(entry -> tiebreakers.add(entry.getKey().getValue()));
        
        // Add kickers in descending order
        rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 1)
            .sorted(Comparator.comparing(entry -> entry.getKey().getValue(), Comparator.reverseOrder()))
            .forEach(entry -> tiebreakers.add(entry.getKey().getValue()));
        
        return tiebreakers;
    }
    
    private static List<Integer> getTwoPairTiebreakers(List<Card> cards) {
        Map<Card.Rank, Long> rankCounts = getRankCounts(cards);
        List<Integer> tiebreakers = new ArrayList<>();
        
        // Add pair ranks in descending order
        rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 2)
            .sorted(Comparator.comparing(entry -> entry.getKey().getValue(), Comparator.reverseOrder()))
            .forEach(entry -> tiebreakers.add(entry.getKey().getValue()));
        
        // Add kicker
        rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 1)
            .forEach(entry -> tiebreakers.add(entry.getKey().getValue()));
        
        return tiebreakers;
    }
    
    private static List<Integer> getPairTiebreakers(List<Card> cards) {
        Map<Card.Rank, Long> rankCounts = getRankCounts(cards);
        List<Integer> tiebreakers = new ArrayList<>();
        
        // Add the pair rank
        rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 2)
            .forEach(entry -> tiebreakers.add(entry.getKey().getValue()));
        
        // Add kickers in descending order
        rankCounts.entrySet().stream()
            .filter(entry -> entry.getValue() == 1)
            .sorted(Comparator.comparing(entry -> entry.getKey().getValue(), Comparator.reverseOrder()))
            .forEach(entry -> tiebreakers.add(entry.getKey().getValue()));
        
        return tiebreakers;
    }
}
