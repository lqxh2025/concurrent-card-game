package com.qiuhan.cardgame;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for the CardDeck class.
 */
public class CardDeckTest {

    private CardDeck deck;

    @BeforeEach
    public void setUp() {
        deck = new CardDeck(1);
    }

    @Test
    public void testDeckCreation() {
        assertEquals(1, deck.getDeckNumber());
        assertEquals(0, deck.size());
    }

    @Test
    public void testAddCard() {
        Card card = new Card(5);
        deck.addCard(card);
        assertEquals(1, deck.size());
    }

    @Test
    public void testDrawCard() throws InterruptedException {
        Card card1 = new Card(3);
        deck.addCard(card1);

        Card drawn = deck.drawCard();
        assertEquals(3, drawn.getDenomination());
        assertEquals(0, deck.size());
    }

    @Test
    public void testFIFOOrder() throws InterruptedException {
        deck.addCard(new Card(1));
        deck.addCard(new Card(2));
        deck.addCard(new Card(3));

        assertEquals(1, deck.drawCard().getDenomination());
        assertEquals(2, deck.drawCard().getDenomination());
        assertEquals(3, deck.drawCard().getDenomination());
    }

    @Test
    public void testToString() {
        deck.addCard(new Card(1));
        deck.addCard(new Card(2));
        deck.addCard(new Card(3));

        assertEquals("1 2 3", deck.toString());
    }

    /**
     * Test thread safety: multiple threads adding cards concurrently.
     */
    @Test
    public void testConcurrentAdd() throws InterruptedException {
        final int numThreads = 10;
        final int cardsPerThread = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                for (int j = 0; j < cardsPerThread; j++) {
                    deck.addCard(new Card(j));
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(numThreads * cardsPerThread, deck.size());
    }

    /**
     * Test thread safety: concurrent draws and adds.
     */
    @Test
    public void testConcurrentDrawAndAdd() throws InterruptedException {
        // Pre-fill deck
        for (int i = 0; i < 50; i++) {
            deck.addCard(new Card(i));
        }

        final int numThreads = 5;
        CountDownLatch latch = new CountDownLatch(numThreads * 2);
        AtomicInteger drawCount = new AtomicInteger(0);

        // Drawing threads
        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        deck.drawCard();
                        drawCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                latch.countDown();
            }).start();
        }

        // Adding threads
        for (int i = 0; i < numThreads; i++) {
            new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    deck.addCard(new Card(j));
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(25, drawCount.get());
        assertEquals(50, deck.size()); // 50 initial - 25 drawn + 25 added
    }
}
