package com.qiuhan.cardgame;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Represents a deck of cards that supports thread-safe draw and discard operations.
 * Multiple player threads can safely draw from and discard to this deck.
 */
public class CardDeck {
    private final Queue<Card> cards;
    private final int deckNumber;

    /**
     * Creates a new empty card deck with the specified deck number.
     * @param deckNumber the identifier for this deck (1 to n)
     */
    public CardDeck(int deckNumber) {
        this.cards = new LinkedList<>();
        this.deckNumber = deckNumber;
    }

    /**
     * Draws a card from the top of this deck.
     * This method is synchronized to ensure thread safety.
     * If the deck is empty, the calling thread will wait until a card is available.
     * @return the card drawn from the deck
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public synchronized Card drawCard() throws InterruptedException {
        // Wait while deck is empty
        while (cards.isEmpty()) {
            wait();
        }
        Card card = cards.remove();
        // Notify waiting threads that deck state has changed
        notifyAll();
        return card;
    }

    /**
     * Adds a card to the bottom of this deck.
     * This method is synchronized to ensure thread safety.
     * @param card the card to add to the deck
     */
    public synchronized void addCard(Card card) {
        cards.add(card);
        // Notify waiting threads that a card is now available
        notifyAll();
    }

    /**
     * Gets the number of cards currently in this deck.
     * This method is synchronized to ensure thread safety.
     * @return the number of cards in the deck
     */
    public synchronized int size() {
        return cards.size();
    }

    /**
     * Gets the deck number identifier.
     * @return the deck number
     */
    public int getDeckNumber() {
        return this.deckNumber;
    }

    /**
     * Writes the contents of this deck to an output file.
     * The output file is named "deck{deckNumber}_output.txt".
     * @throws IOException if there is an error writing to the file
     */
    public synchronized void writeOutput() throws IOException {
        String filename = "deck" + deckNumber + "_output.txt";
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("deck" + deckNumber + " contents:");
            for (Card card : cards) {
                writer.write(" " + card.getDenomination());
            }
            writer.write("\n");
        }
    }

    /**
     * Returns a string representation of the cards in this deck.
     * @return a space-separated list of card denominations
     */
    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        for (Card card : cards) {
            sb.append(card.getDenomination()).append(" ");
        }
        return sb.toString().trim();
    }
}
