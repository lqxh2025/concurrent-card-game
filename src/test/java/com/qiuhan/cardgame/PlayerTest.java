package com.qiuhan.cardgame;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Unit tests for the Player class.
 */
public class PlayerTest {

    private Player player;
    private CardDeck leftDeck;
    private CardDeck rightDeck;
    private GameState gameState;

    @BeforeEach
    public void setUp() throws IOException {
        leftDeck = new CardDeck(1);
        rightDeck = new CardDeck(2);
        gameState = new GameState();
        player = new Player(1, leftDeck, rightDeck, gameState);
    }

    @AfterEach
    public void tearDown() {
        player.closeOutput();
        // Clean up output file
        File outputFile = new File("player1_output.txt");
        if (outputFile.exists()) {
            outputFile.delete();
        }
    }

    @Test
    public void testPlayerCreation() {
        assertEquals(1, player.getPlayerNumber());
        assertEquals(0, player.getHand().size());
    }

    @Test
    public void testAddCard() {
        Card card = new Card(5);
        player.addCard(card);
        assertEquals(1, player.getHand().size());
        assertEquals(5, player.getHand().get(0).getDenomination());
    }

    @Test
    public void testHasWinningHand() {
        // Not winning - different cards
        player.addCard(new Card(1));
        player.addCard(new Card(2));
        player.addCard(new Card(3));
        player.addCard(new Card(4));
        assertFalse(player.hasWinningHand());

        // Clear and test winning hand
        Player player2;
        try {
            GameState gameState2 = new GameState();
            player2 = new Player(2, leftDeck, rightDeck, gameState2);
            player2.addCard(new Card(5));
            player2.addCard(new Card(5));
            player2.addCard(new Card(5));
            player2.addCard(new Card(5));
            assertTrue(player2.hasWinningHand());
            player2.closeOutput();
            new File("player2_output.txt").delete();
        } catch (IOException e) {
            fail("IOException: " + e.getMessage());
        }
    }

    @Test
    public void testInitialHandLogging() {
        player.addCard(new Card(1));
        player.addCard(new Card(2));
        player.addCard(new Card(3));
        player.addCard(new Card(4));

        try {
            player.logInitialHand();
            player.closeOutput();

            // Check that file was created
            File outputFile = new File("player1_output.txt");
            assertTrue(outputFile.exists());
        } catch (IOException e) {
            fail("IOException: " + e.getMessage());
        }
    }

    @Test
    public void notifyGameWonWritesExpectedOutput() throws IOException {
        int winnerNumber = 2;
        player.addCard(new Card(1));
        player.addCard(new Card(2));
        player.addCard(new Card(3));
        player.addCard(new Card(4));

        player.notifyGameWon(winnerNumber);
        player.closeOutput();

        File outputFile = new File("player1_output.txt");
        assertTrue(outputFile.exists(), "Output file should exist");

        String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("player " + winnerNumber + " has informed player 1 that player " + winnerNumber + " has won"),
            "Output should contain the win notification line");
        assertTrue(content.contains("player 1 exits"), "Output should contain the exit line");
        assertTrue(content.contains("player 1 hand: 1 2 3 4"), "Output should contain the final hand information");
    }

    /**
     * Characterization/regression test for the known shutdown bug: a player
     * thread blocked in CardDeck.drawCard() on an empty deck has no way to be
     * woken when another player wins, because GameState holds no reference to
     * any CardDeck and cannot broadcast a wakeup. This test is EXPECTED TO
     * FAIL on the current implementation -- it documents the defect rather
     * than exercising correct behavior. It always cleans up its own thread so
     * a failure here cannot hang the rest of the suite.
     */
    @Test
    public void blockedPlayerTerminatesWhenGameEnds() throws IOException, InterruptedException {
        CardDeck blockedLeftDeck = new CardDeck(10);
        CardDeck blockedRightDeck = new CardDeck(11);
        GameState sharedGameState = new GameState();
        Player blockedPlayer = new Player(3, blockedLeftDeck, blockedRightDeck, sharedGameState);

        // Valid, non-winning 4-card hand.
        blockedPlayer.addCard(new Card(1));
        blockedPlayer.addCard(new Card(2));
        blockedPlayer.addCard(new Card(3));
        blockedPlayer.addCard(new Card(4));

        Thread playerThread = new Thread(blockedPlayer);
        playerThread.start();

        try {
            waitUntilWaiting(playerThread, 2000);
            assertEquals(Thread.State.WAITING, playerThread.getState(),
                "Player thread should be blocked drawing from its empty left deck");

            assertTrue(sharedGameState.declareWinner(99), "Another player should be able to declare the win");

            playerThread.join(1000);
            assertFalse(playerThread.isAlive(),
                "Blocked player thread should terminate once the game has been won (currently fails: known shutdown bug)");
        } finally {
            if (playerThread.isAlive()) {
                playerThread.interrupt();
                playerThread.join(1000);
            }
            blockedPlayer.closeOutput();
            new File("player3_output.txt").delete();
        }
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
