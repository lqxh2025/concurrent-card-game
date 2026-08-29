package com.qiuhan.cardgame;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Card class.
 */
public class CardTest {

    @Test
    public void testCardCreation() {
        Card card = new Card(5);
        assertEquals(5, card.getDenomination());
    }

    @Test
    public void testCardWithZeroDenomination() {
        Card card = new Card(0);
        assertEquals(0, card.getDenomination());
    }

    @Test
    public void testCardWithLargeDenomination() {
        Card card = new Card(100);
        assertEquals(100, card.getDenomination());
    }

    @Test
    public void testCardToString() {
        Card card = new Card(7);
        assertEquals("7", card.toString());
    }

    @Test
    public void testCardEquality() {
        Card card1 = new Card(3);
        Card card2 = new Card(3);
        Card card3 = new Card(5);

        assertEquals(card1, card2);
        assertNotEquals(card1, card3);
    }

    @Test
    public void testCardHashCode() {
        Card card1 = new Card(4);
        Card card2 = new Card(4);

        assertEquals(card1.hashCode(), card2.hashCode());
    }
}
