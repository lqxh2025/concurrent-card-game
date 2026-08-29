package com.qiuhan.cardgame;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Shared game state to coordinate multiple player threads.
 * Ensures only one player can win.
 */
public class GameState {
    private volatile boolean gameWon = false;
    private volatile int winnerNumber = -1;
    private final Set<CardDeck> registeredDecks = new LinkedHashSet<>();

    /**
     * Registers a deck so that, if the game ends while a thread is blocked
     * inside that deck's drawCard(), it can be woken up instead of waiting
     * forever for a card that will never arrive. Safe to call more than
     * once with the same deck (e.g. because it is shared between two
     * players as one player's left deck and another's right deck).
     * @param deck the deck to register for game-over wakeups
     */
    public synchronized void registerDeck(CardDeck deck) {
        registeredDecks.add(deck);
    }

    /**
     * Attempts to declare this player as the winner.
     * @param playerNumber the player attempting to win
     * @return true if this player successfully claimed the win, false if another player already won
     */
    public synchronized boolean declareWinner(int playerNumber) {
        if (!gameWon) {
            gameWon = true;
            winnerNumber = playerNumber;
            // Wake any thread currently blocked waiting for a card in any
            // registered deck, so it can observe that the game has ended
            // and exit instead of waiting indefinitely.
            for (CardDeck deck : registeredDecks) {
                deck.signalGameEnded();
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if the game has been won.
     * @return true if the game is over
     */
    public boolean isGameWon() {
        return gameWon;
    }

    /**
     * Gets the winner's number.
     * @return the winner's player number, or -1 if no winner yet
     */
    public int getWinnerNumber() {
        return winnerNumber;
    }
}
