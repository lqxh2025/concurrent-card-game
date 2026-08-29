package com.qiuhan.cardgame;

/**
 * Represents a playing card with a denomination value.
 * This class is immutable and thread-safe.
 */
public class Card {
    private final int denomination;

    /**
     * Creates a new card with the specified denomination.
     * @param denomination the face value of the card (non-negative integer)
     */
    public Card(int denomination) {
        this.denomination = denomination;
    }

    /**
     * Gets the denomination of this card.
     * @return the card's denomination value
     */
    public int getDenomination() {
        return this.denomination;
    }

    /**
     * Returns a string representation of this card.
     * @return the denomination as a string
     */
    @Override
    public String toString() {
        return String.valueOf(this.denomination);
    }

    /**
     * Checks if this card equals another object.
     * Two cards are equal if they have the same denomination.
     * @param obj the object to compare with
     * @return true if the cards are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Card other = (Card) obj;
        return this.denomination == other.denomination;
    }

    /**
     * Returns a hash code for this card.
     * @return hash code based on denomination
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(this.denomination);
    }
}
