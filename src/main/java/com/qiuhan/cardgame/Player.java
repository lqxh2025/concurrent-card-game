package com.qiuhan.cardgame;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a player in the card game.
 */
public class Player implements Runnable {
    private final int playerNumber;
    private final List<Card> hand;
    private final CardDeck leftDeck;
    private final CardDeck rightDeck;
    private final int preferredDenomination;
    private final FileWriter outputWriter;
    private final Random random;
    private final GameState gameState;

    public Player(int playerNumber, CardDeck leftDeck, CardDeck rightDeck, GameState gameState) throws IOException {
        this.playerNumber = playerNumber;
        this.hand = new ArrayList<>();
        this.leftDeck = leftDeck;
        this.rightDeck = rightDeck;
        this.preferredDenomination = playerNumber;
        this.random = new Random();
        this.gameState = gameState;

        // Register both decks so that if the game ends while this player
        // (or whichever player shares one of these decks) is blocked inside
        // drawCard(), that thread can be woken up instead of waiting forever.
        gameState.registerDeck(leftDeck);
        gameState.registerDeck(rightDeck);

        String filename = "player" + playerNumber + "_output.txt";
        this.outputWriter = new FileWriter(filename);
    }

    public synchronized void addCard(Card card) {
        hand.add(card);
    }

    public synchronized List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    public synchronized boolean hasWinningHand() {
        if (hand.size() != 4) {
            return false;
        }
        int firstDenomination = hand.get(0).getDenomination();
        for (Card card : hand) {
            if (card.getDenomination() != firstDenomination) {
                return false;
            }
        }
        return true;
    }

    public synchronized void logInitialHand() throws IOException {
        outputWriter.write("player " + playerNumber + " initial hand");
        for (Card card : hand) {
            outputWriter.write(" " + card.getDenomination());
        }
        outputWriter.write("\n");
        outputWriter.flush();
    }

    private synchronized Card selectCardToDiscard() {
        List<Card> nonPreferred = new ArrayList<>();
        for (Card card : hand) {
            if (card.getDenomination() != preferredDenomination) {
                nonPreferred.add(card);
            }
        }

        if (nonPreferred.isEmpty()) {
            return hand.get(0);
        }

        return nonPreferred.get(random.nextInt(nonPreferred.size()));
    }

    private void logDraw(Card card, int deckNumber) throws IOException {
        outputWriter.write("player " + playerNumber + " draws a " +
                          card.getDenomination() + " from deck " + deckNumber + "\n");
        outputWriter.flush();
    }

    private void logDiscard(Card card, int deckNumber) throws IOException {
        outputWriter.write("player " + playerNumber + " discards a " +
                          card.getDenomination() + " to deck " + deckNumber + "\n");
        outputWriter.flush();
    }

    private synchronized void logCurrentHand() throws IOException {
        outputWriter.write("player " + playerNumber + " current hand is");
        for (Card card : hand) {
            outputWriter.write(" " + card.getDenomination());
        }
        outputWriter.write("\n");
        outputWriter.flush();
    }

    private void logWin() throws IOException {
        outputWriter.write("player " + playerNumber + " wins\n");
        outputWriter.write("player " + playerNumber + " exits\n");
        outputWriter.write("player " + playerNumber + " final hand:");
        for (Card card : hand) {
            outputWriter.write(" " + card.getDenomination());
        }
        outputWriter.write("\n");
        outputWriter.flush();
    }

    private void logOtherPlayerWon(int winnerNumber) throws IOException {
        outputWriter.write("player " + winnerNumber + " has informed player " +
                          playerNumber + " that player " + winnerNumber + " has won\n");
        outputWriter.write("player " + playerNumber + " exits\n");
        outputWriter.write("player " + playerNumber + " hand:");
        for (Card card : hand) {
            outputWriter.write(" " + card.getDenomination());
        }
        outputWriter.write("\n");
        outputWriter.flush();
    }

    public void notifyGameWon(int winnerNumber) {
        try {
            if (winnerNumber != this.playerNumber) {
                logOtherPlayerWon(winnerNumber);
            }
        } catch (IOException e) {
            System.err.println("Error writing to player " + playerNumber + " output: " + e.getMessage());
        }
    }

    public void closeOutput() {
        try {
            outputWriter.close();
        } catch (IOException e) {
            System.err.println("Error closing player " + playerNumber + " output: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // Main game loop
            while (!gameState.isGameWon()) {
                // Draw a card from left deck
                Card drawnCard = leftDeck.drawCard();

                if (drawnCard == null) {
                    // The deck stayed empty because the game ended while
                    // this player was blocked waiting for a card; nothing
                    // was drawn, so there is nothing to log or add to the
                    // hand. Exit the loop cleanly.
                    break;
                }

                // Check if game ended while waiting
                if (gameState.isGameWon()) {
                    // The card was already removed from the deck but the
                    // game ended before it could be added to this player's
                    // hand. Return it to the deck it was drawn from so it
                    // is not lost.
                    leftDeck.addCard(drawnCard);
                    break;
                }

                logDraw(drawnCard, leftDeck.getDeckNumber());

                // Add to hand
                synchronized (this) {
                    hand.add(drawnCard);
                }

                // Select and discard
                Card cardToDiscard = selectCardToDiscard();
                synchronized (this) {
                    hand.remove(cardToDiscard);
                }

                rightDeck.addCard(cardToDiscard);
                logDiscard(cardToDiscard, rightDeck.getDeckNumber());

                logCurrentHand();

                // Check for win
                if (hasWinningHand()) {
                    if (gameState.declareWinner(playerNumber)) {
                        // This player successfully claimed the win
                        logWin();
                        System.out.println("player " + playerNumber + " wins");
                        return;
                    }
                    // Another player won first, exit gracefully
                    break;
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error in player " + playerNumber + ": " + e.getMessage());
        }
    }

    public int getPlayerNumber() {
        return playerNumber;
    }
}
