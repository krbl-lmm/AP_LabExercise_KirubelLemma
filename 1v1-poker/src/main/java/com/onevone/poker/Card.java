package com.onevone.poker;

public class Card {

    public enum Suit { CLUBS, DIAMONDS, HEARTS, SPADES }

    public enum Rank {
        TWO(2,"2"), THREE(3,"3"), FOUR(4,"4"), FIVE(5,"5"), SIX(6,"6"),
        SEVEN(7,"7"), EIGHT(8,"8"), NINE(9,"9"), TEN(10,"10"),
        JACK(11,"J"), QUEEN(12,"Q"), KING(13,"K"), ACE(14,"1");

        public final int value;
        public final String label;
        Rank(int value, String label) { this.value = value; this.label = label; }
    }

    public final Suit suit;
    public final Rank rank;
    public boolean faceUp = false;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public boolean isRed() {
        return suit == Suit.HEARTS || suit == Suit.DIAMONDS;
    }

    public String suitSymbol() {
        return switch (suit) {
            case CLUBS    -> "♣";
            case DIAMONDS -> "♦";
            case HEARTS   -> "♥";
            case SPADES   -> "♠";
        };
    }

    @Override
    public String toString() {
        return rank.label + suitSymbol();
    }
}
