package com.qiuhan.cardgame;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    /**
     * Deterministic regression test for the card-loss-on-game-over defect:
     * a player can successfully remove a card from its left deck, then
     * observe that another player has won before that card is ever added to
     * its hand (or returned anywhere), losing it entirely.
     *
     * Rather than racing real threads and hoping to hit the window, this
     * test uses a test-only CardDeck subclass (BlockingAfterDrawDeck, below)
     * that calls the real, unmodified super.drawCard() -- so the card is
     * genuinely removed from the deck exactly as in production -- and then
     * deterministically pauses on a CountDownLatch before returning it to
     * Player.run(). That gives the test a reliable window to declare a
     * winner while the drawn card is "in transit", with no reliance on
     * thread scheduling and no production code changes.
     *
     * EXPECTED: this test currently FAILS, because the drawn card ends up
     * in neither the player's hand nor either deck.
     */
    @Test
    public void drawnCardIsLostWhenGameEndsBeforeItIsAddedToHand() throws IOException, InterruptedException {
        CountDownLatch cardRemovedLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);

        BlockingAfterDrawDeck victimLeftDeck = new BlockingAfterDrawDeck(20, cardRemovedLatch, releaseLatch);
        CardDeck victimRightDeck = new CardDeck(21);
        GameState sharedGameState = new GameState();

        // Exactly one known extra card waiting in the victim's left deck.
        victimLeftDeck.addCard(new Card(9));

        Player victimPlayer = new Player(4, victimLeftDeck, victimRightDeck, sharedGameState);

        // A valid, non-winning four-card initial hand.
        victimPlayer.addCard(new Card(1));
        victimPlayer.addCard(new Card(2));
        victimPlayer.addCard(new Card(3));
        victimPlayer.addCard(new Card(4));

        // Right deck starts empty (default CardDeck state -- nothing to do).

        int initialTotal = victimPlayer.getHand().size() + victimLeftDeck.size() + victimRightDeck.size();

        Thread victimThread = new Thread(victimPlayer);

        try {
            victimThread.start();

            // Wait until the overridden drawCard() confirms that
            // super.drawCard() has already removed the extra card from the
            // deck, before declaring a winner.
            assertTrue(cardRemovedLatch.await(2, TimeUnit.SECONDS),
                "The victim player should have removed the extra card from its left deck by now");

            assertTrue(sharedGameState.declareWinner(99), "Another player should be able to declare the win");

            // Let the overridden drawCard() return the already-removed card
            // to Player.run().
            releaseLatch.countDown();

            victimThread.join(2000);
            assertFalse(victimThread.isAlive(), "Victim player thread should terminate after the game ends");

            int finalTotal = victimPlayer.getHand().size() + victimLeftDeck.size() + victimRightDeck.size();
            assertEquals(initialTotal, finalTotal,
                "Cards must be conserved when the game ends after a draw");
        } finally {
            if (victimThread.isAlive()) {
                victimThread.interrupt();
                victimThread.join(1000);
            }
            victimPlayer.closeOutput();
            new File("player4_output.txt").delete();
        }
    }

    /**
     * Test-only CardDeck subclass that genuinely removes a card via the
     * real, unmodified super.drawCard(), then deterministically pauses
     * (via a CountDownLatch, not a sleep or a poll) before returning that
     * card to the caller. This lets a test pin the exact moment between
     * "card removed from deck" and "card handed to Player.run()" without
     * any production code changes.
     */
    private static final class BlockingAfterDrawDeck extends CardDeck {
        private final CountDownLatch cardRemovedLatch;
        private final CountDownLatch releaseLatch;

        BlockingAfterDrawDeck(int deckNumber, CountDownLatch cardRemovedLatch, CountDownLatch releaseLatch) {
            super(deckNumber);
            this.cardRemovedLatch = cardRemovedLatch;
            this.releaseLatch = releaseLatch;
        }

        @Override
        public Card drawCard() throws InterruptedException {
            // Actually remove the card from the underlying deck first,
            // exactly as the real implementation does.
            Card card = super.drawCard();
            // Signal that the card has already been removed from the deck
            // -- before it has been returned to the caller and therefore
            // before it can possibly be added to the player's hand.
            cardRemovedLatch.countDown();
            // Deterministically pause here (after removal, before return)
            // so the test can declare a winner while this card is "in
            // transit" -- neither in the deck nor in any hand.
            releaseLatch.await();
            return card;
        }
    }
}
