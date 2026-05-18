package poker.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HandEvaluatorTest {

    @Test
    void evaluateHand_picksBestFiveFromSeven() {
        // Community: A K Q J 2, Hole: T 9 => best is Broadway straight (A-K-Q-J-T)
        List<Card> cards = List.of(
                new Card(Card.Rank.ACE, Card.Suit.SPADES),
                new Card(Card.Rank.KING, Card.Suit.HEARTS),
                new Card(Card.Rank.QUEEN, Card.Suit.DIAMONDS),
                new Card(Card.Rank.JACK, Card.Suit.CLUBS),
                new Card(Card.Rank.TEN, Card.Suit.SPADES),
                new Card(Card.Rank.NINE, Card.Suit.HEARTS),
                new Card(Card.Rank.TWO, Card.Suit.CLUBS)
        );

        HandEvaluator.HandResult r = HandEvaluator.evaluateHand(cards);
        assertEquals(HandRank.STRAIGHT, r.getRank());
        assertEquals(List.of(14), r.getTiebreakers());
    }

    @Test
    void royalFlush_notTriggeredByWheelStraightFlush() {
        // A-2-3-4-5 straight flush is NOT royal flush
        List<Card> cards = List.of(
                new Card(Card.Rank.ACE, Card.Suit.HEARTS),
                new Card(Card.Rank.TWO, Card.Suit.HEARTS),
                new Card(Card.Rank.THREE, Card.Suit.HEARTS),
                new Card(Card.Rank.FOUR, Card.Suit.HEARTS),
                new Card(Card.Rank.FIVE, Card.Suit.HEARTS)
        );

        HandEvaluator.HandResult r = HandEvaluator.evaluateHand(cards);
        assertEquals(HandRank.STRAIGHT_FLUSH, r.getRank());
        assertEquals(List.of(5), r.getTiebreakers());
    }

    @Test
    void straight_wheelHasHighCardFive() {
        List<Card> cards = List.of(
                new Card(Card.Rank.ACE, Card.Suit.SPADES),
                new Card(Card.Rank.TWO, Card.Suit.HEARTS),
                new Card(Card.Rank.THREE, Card.Suit.DIAMONDS),
                new Card(Card.Rank.FOUR, Card.Suit.CLUBS),
                new Card(Card.Rank.FIVE, Card.Suit.SPADES)
        );

        HandEvaluator.HandResult r = HandEvaluator.evaluateHand(cards);
        assertEquals(HandRank.STRAIGHT, r.getRank());
        assertEquals(List.of(5), r.getTiebreakers());
    }

    @Test
    void compareHands_usesKickersForPair() {
        // Pair of Aces with K kicker beats pair of Aces with Q kicker
        List<Card> h1 = List.of(
                new Card(Card.Rank.ACE, Card.Suit.SPADES),
                new Card(Card.Rank.ACE, Card.Suit.HEARTS),
                new Card(Card.Rank.KING, Card.Suit.DIAMONDS),
                new Card(Card.Rank.TWO, Card.Suit.CLUBS),
                new Card(Card.Rank.THREE, Card.Suit.SPADES)
        );
        List<Card> h2 = List.of(
                new Card(Card.Rank.ACE, Card.Suit.DIAMONDS),
                new Card(Card.Rank.ACE, Card.Suit.CLUBS),
                new Card(Card.Rank.QUEEN, Card.Suit.HEARTS),
                new Card(Card.Rank.TWO, Card.Suit.CLUBS),
                new Card(Card.Rank.THREE, Card.Suit.SPADES)
        );

        assertTrue(HandEvaluator.compareHands(h1, h2) > 0);
    }
}
