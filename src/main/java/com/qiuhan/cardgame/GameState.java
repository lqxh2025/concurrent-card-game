package com.qiuhan.cardgame;

/**
 * Shared game state to coordinate multiple player threads.
 * Ensures only one player can win.
 */
public class GameState {
    private volatile boolean gameWon = false;
    private volatile int winnerNumber = -1;

    /**
     * Attempts to declare this player as the winner.
     * @param playerNumber the player attempting to win
     * @return true if this player successfully claimed the win, false if another player already won
     */
    public synchronized boolean declareWinner(int playerNumber) {
        if (!gameWon) {
            gameWon = true;
            winnerNumber = playerNumber;
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
