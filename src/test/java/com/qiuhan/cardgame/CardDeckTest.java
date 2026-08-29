package com.qiuhan.cardgame;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

    /**
     * Test that a thread blocked in drawCard() on an empty deck is released
     * once a card becomes available, and receives exactly that card. Uses
     * bounded polling of Thread.State (not an arbitrary sleep) to confirm the
     * drawer has actually parked in wait() before the card is added.
     */
    @Test
    public void blockedDrawReturnsAfterCardAdded() throws InterruptedException {
        CardDeck emptyDeck = new CardDeck(99);
        Card expectedCard = new Card(42);
        AtomicReference<Card> drawnCard = new AtomicReference<>();

        Thread drawerThread = new Thread(() -> {
            try {
                drawnCard.set(emptyDeck.drawCard());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        drawerThread.start();

        waitUntilWaiting(drawerThread, 2000);
        assertEquals(Thread.State.WAITING, drawerThread.getState(),
            "Drawer thread should be parked in wait() before any card is added");

        emptyDeck.addCard(expectedCard);

        drawerThread.join(2000);
        assertFalse(drawerThread.isAlive(), "Drawer thread should terminate once a card is added");
        assertEquals(expectedCard, drawnCard.get(), "Drawer should receive the exact card that was added");
    }

    /**
     * Polls the given thread's state in short bounded increments until it
     * reaches WAITING (or TIMED_WAITING), failing the test if the timeout
     * elapses first. This avoids a single arbitrary sleep in favor of a
     * bounded, deterministic wait for the actual condition we care about.
     */
    private static void waitUntilWaiting(Thread thread, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            Thread.State state = thread.getState();
            if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
                return;
            }
            Thread.sleep(5);
        }
        fail("Thread did not reach a WAITING state within " + timeoutMillis + "ms");
    }
}
