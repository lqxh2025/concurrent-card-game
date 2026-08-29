package com.qiuhan.cardgame;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for the GameState class.
 */
public class GameStateTest {

    /**
     * Many threads race to declare themselves the winner at the same instant
     * (synchronized via a CyclicBarrier so they all call declareWinner()
     * concurrently rather than in some arbitrary sequential order). Exactly
     * one should succeed, regardless of scheduling, because declareWinner()
     * performs its check-and-set under a single lock.
     */
    @Test
    public void concurrentDeclareWinnerAllowsExactlyOneWinner() throws InterruptedException {
        final int numThreads = 50;
        GameState gameState = new GameState();
        CyclicBarrier startBarrier = new CyclicBarrier(numThreads);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger successfulPlayer = new AtomicInteger(-1);

        for (int i = 1; i <= numThreads; i++) {
            final int playerNumber = i;
            new Thread(() -> {
                try {
                    startBarrier.await();
                    if (gameState.declareWinner(playerNumber)) {
                        successCount.incrementAndGet();
                        successfulPlayer.set(playerNumber);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (BrokenBarrierException e) {
                    // A thread failing to reach the barrier fails the test below via the latch/count checks.
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should complete within the timeout");
        assertEquals(1, successCount.get(), "Exactly one thread should succeed in declaring a winner");
        assertEquals(successfulPlayer.get(), gameState.getWinnerNumber(),
            "getWinnerNumber() should report the player that actually won the race");
    }
}
