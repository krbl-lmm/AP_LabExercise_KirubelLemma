package com.onevone.poker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PokerGame {

    public enum Phase { WAITING, PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN }

    private final Deck deck = new Deck();
    private final List<Card> playerCards   = new ArrayList<>();
    private final List<Card> computerCards = new ArrayList<>();
    private final List<Card> community     = new ArrayList<>();

    private int playerChips   = 1000;
    private int computerChips = 1000;
    private int pot           = 0;
    private int playerBet     = 0;
    private int computerBet   = 0;
    private int currentBet    = 0;

    private Phase phase = Phase.WAITING;
    private boolean playerTurn    = true;
    private boolean playerIsDealer = false;

    private boolean playerActed   = false;
    private boolean computerActed = false;

    private String lastMessage = "Press 'New Hand' to start.";
    private String winner = "";
    private HandEvaluator.HandResult playerResult;
    private HandEvaluator.HandResult computerResult;

    private final Random rng = new Random();

    static final int BIG_BLIND   = 20;
    static final int SMALL_BLIND = 10;

    public void startHand() {
        playerCards.clear();
        computerCards.clear();
        community.clear();
        pot = 0; playerBet = 0; computerBet = 0; currentBet = 0;
        winner = ""; playerResult = null; computerResult = null;
        playerActed = false; computerActed = false;

        playerIsDealer = !playerIsDealer;

        deck.reset();
        deck.shuffle();

        dealCard(playerCards,   true);
        dealCard(computerCards, false);
        dealCard(playerCards,   true);
        dealCard(computerCards, false);

        if (playerIsDealer) {
            chargeBlind(true,  SMALL_BLIND);
            chargeBlind(false, BIG_BLIND);
            playerTurn = true;
        } else {
            chargeBlind(false, SMALL_BLIND);
            chargeBlind(true,  BIG_BLIND);
            playerTurn = false;
        }
        currentBet = BIG_BLIND;

        phase = Phase.PRE_FLOP;
        lastMessage = "Cards dealt. Blinds posted.";
    }

    private void dealCard(List<Card> hand, boolean faceUp) {
        Card c = deck.deal();
        c.faceUp = faceUp;
        hand.add(c);
    }

    private void chargeBlind(boolean isPlayer, int amount) {
        if (isPlayer) {
            int paid = Math.min(amount, playerChips);
            playerChips -= paid; playerBet += paid; pot += paid;
        } else {
            int paid = Math.min(amount, computerChips);
            computerChips -= paid; computerBet += paid; pot += paid;
        }
    }

    public void playerCheck() {
        if (!playerTurn) return;
        lastMessage = "You check.";
        playerActed = true;
        playerTurn = false;
        tryAdvanceOrComputerAct();
    }

    public void playerCall() {
        if (!playerTurn) return;
        int toCall = Math.min(currentBet - playerBet, playerChips);
        playerChips -= toCall; playerBet += toCall; pot += toCall;
        lastMessage = "You call $" + toCall + ".";
        playerActed = true;
        playerTurn = false;
        tryAdvanceOrComputerAct();
    }

    public void playerRaise(int raiseBy) {
        if (!playerTurn) return;
        int toCall = currentBet - playerBet;
        int total  = Math.min(toCall + raiseBy, playerChips);
        playerChips -= total; playerBet += total; pot += total;
        currentBet = playerBet;
        lastMessage = "You raise to $" + currentBet + ".";
        playerActed = true;
        computerActed = false; // computer must respond to the raise
        playerTurn = false;
        tryAdvanceOrComputerAct();
    }

    public void playerFold() {
        if (!playerTurn) return;
        computerChips += pot;
        winner = "Computer wins (you folded).";
        phase = Phase.SHOWDOWN;
        lastMessage = "You folded.";
        revealComputerCards();
    }

    private void tryAdvanceOrComputerAct() {
        if (isBettingRoundOver()) {
            advancePhase();
        }
    }

    private boolean isBettingRoundOver() {
        return playerActed && computerActed && playerBet == computerBet;
    }

    public boolean isComputerTurn() {
        return !playerTurn && phase != Phase.SHOWDOWN && phase != Phase.WAITING;
    }

    public void doComputerAction() {
        if (playerTurn) return;

        double strength = estimateComputerStrength();
        int toCall = currentBet - computerBet;
        boolean canCheck = toCall <= 0;

        if (canCheck) {
            if (strength > 0.65 && rng.nextDouble() > 0.3) {
                // Bet
                int raise = BIG_BLIND * (1 + rng.nextInt(3));
                raise = Math.min(raise, computerChips);
                computerChips -= raise; computerBet += raise; pot += raise;
                currentBet = computerBet;
                lastMessage = "Computer bets $" + raise + ".";
                playerActed = false; // player must respond
            } else {
                lastMessage = "Computer checks.";
            }
        } else {
            if (strength > 0.7) {
                int raise = BIG_BLIND * rng.nextInt(3);
                int total = Math.min(toCall + raise, computerChips);
                computerChips -= total; computerBet += total; pot += total;
                if (raise > 0) {
                    currentBet = computerBet;
                    lastMessage = "Computer raises to $" + currentBet + ".";
                    playerActed = false; // player must respond
                } else {
                    lastMessage = "Computer calls $" + toCall + ".";
                }
            } else if (strength > 0.35 || toCall <= BIG_BLIND) {
                int paid = Math.min(toCall, computerChips);
                computerChips -= paid; computerBet += paid; pot += paid;
                lastMessage = "Computer calls $" + paid + ".";
            } else {
                playerChips += pot;
                winner = "You win! (Computer folded)";
                phase = Phase.SHOWDOWN;
                lastMessage = "Computer folds. " + winner;
                playerTurn = true;
                return;
            }
        }

        computerActed = true;
        playerTurn = true;

        if (isBettingRoundOver()) {
            advancePhase();
        }
    }

    private double estimateComputerStrength() {
        List<Card> all = new ArrayList<>(computerCards);
        all.addAll(community);
        if (all.size() < 5) {
            int v1 = computerCards.get(0).rank.value;
            int v2 = computerCards.get(1).rank.value;
            double s = (v1 + v2) / 28.0;
            if (v1 == v2) s += 0.2;
            if (computerCards.get(0).suit == computerCards.get(1).suit) s += 0.1;
            return Math.min(s, 0.95);
        }
        return HandEvaluator.evaluate(all).rank.ordinal() / 9.0;
    }

    private void advancePhase() {
        playerBet = 0; computerBet = 0; currentBet = 0;
        playerActed = false; computerActed = false;

        playerTurn = !playerIsDealer;

        switch (phase) {
            case PRE_FLOP -> dealFlop();
            case FLOP     -> dealTurn();
            case TURN     -> dealRiver();
            case RIVER    -> doShowdown();
            default       -> {}
        }
    }

    private void dealFlop() {
        deck.deal();
        for (int i = 0; i < 3; i++) { Card c = deck.deal(); c.faceUp = true; community.add(c); }
        phase = Phase.FLOP;
        lastMessage += " — Flop dealt.";
    }

    private void dealTurn() {
        deck.deal();
        Card c = deck.deal(); c.faceUp = true; community.add(c);
        phase = Phase.TURN;
        lastMessage += " — Turn dealt.";
    }

    private void dealRiver() {
        deck.deal();
        Card c = deck.deal(); c.faceUp = true; community.add(c);
        phase = Phase.RIVER;
        lastMessage += " — River dealt.";
    }

    private void doShowdown() {
        phase = Phase.SHOWDOWN;
        revealComputerCards();

        List<Card> pAll = new ArrayList<>(playerCards);   pAll.addAll(community);
        List<Card> cAll = new ArrayList<>(computerCards); cAll.addAll(community);

        playerResult   = HandEvaluator.evaluate(pAll);
        computerResult = HandEvaluator.evaluate(cAll);

        int cmp = playerResult.compareTo(computerResult);
        if (cmp > 0) {
            playerChips += pot;
            winner = "You win with " + playerResult.describe() + "!";
        } else if (cmp < 0) {
            computerChips += pot;
            winner = "Computer wins with " + computerResult.describe() + ".";
        } else {
            playerChips += pot / 2; computerChips += pot / 2;
            winner = "Split pot — both have " + playerResult.describe() + ".";
        }
        lastMessage = winner;
    }

    private void revealComputerCards() {
        computerCards.forEach(c -> c.faceUp = true);
    }

    public boolean canCheck()   { return playerBet >= currentBet; }
    public int callAmount()     { return Math.max(0, currentBet - playerBet); }


    public List<Card> getPlayerCards()   { return playerCards; }
    public List<Card> getComputerCards() { return computerCards; }
    public List<Card> getCommunity()     { return community; }
    public int  getPlayerChips()         { return playerChips; }
    public int  getComputerChips()       { return computerChips; }
    public int  getPot()                 { return pot; }
    public Phase getPhase()              { return phase; }
    public boolean isPlayerTurn()        { return playerTurn; }
    public String getLastMessage()       { return lastMessage; }
    public String getWinner()            { return winner; }
    public boolean isGameOver()          { return playerChips <= 0 || computerChips <= 0; }
    public HandEvaluator.HandResult getPlayerResult()   { return playerResult; }
    public HandEvaluator.HandResult getComputerResult() { return computerResult; }
}
