package com.qiuhan.cardgame;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.File;
import java.io.IOException;

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
    public void testNotifyGameWon() {
        player.notifyGameWon(2);
        // After notification, player should know game is won
    }
}
