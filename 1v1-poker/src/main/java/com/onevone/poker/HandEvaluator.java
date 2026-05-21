package com.onevone.poker;

import java.util.*;

public class HandEvaluator {

    public enum HandRank {
        HIGH_CARD, ONE_PAIR, TWO_PAIR, THREE_OF_A_KIND,
        STRAIGHT, FLUSH, FULL_HOUSE, FOUR_OF_A_KIND,
        STRAIGHT_FLUSH, ROYAL_FLUSH;

        public String display() {
            return name().replace("_", " ").toLowerCase();
        }
    }

    public static class HandResult implements Comparable<HandResult> {
        public final HandRank rank;
        public final int[] tiebreakers; // higher is better

        HandResult(HandRank rank, int[] tiebreakers) {
            this.rank = rank;
            this.tiebreakers = tiebreakers;
        }

        @Override
        public int compareTo(HandResult other) {
            int cmp = rank.ordinal() - other.rank.ordinal();
            if (cmp != 0) return cmp;
            for (int i = 0; i < tiebreakers.length && i < other.tiebreakers.length; i++) {
                cmp = tiebreakers[i] - other.tiebreakers[i];
                if (cmp != 0) return cmp;
            }
            return 0;
        }

        public String describe() { return rank.display(); }
    }

    public static HandResult evaluate(List<Card> cards) {
        // Try every combination of 5 cards and keep the best
        List<List<Card>> combos = choose5(cards);
        return combos.stream()
                .map(HandEvaluator::score5)
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    private static HandResult score5(List<Card> five) {
        // Sort descending by rank value
        int[] vals = five.stream()
                .mapToInt(c -> c.rank.value)
                .boxed().sorted(Comparator.reverseOrder())
                .mapToInt(i -> i).toArray();

        boolean flush    = isFlush(five);
        boolean straight = isStraight(vals);
        boolean wheel    = isWheel(vals); // A-2-3-4-5

        if (flush && straight) return new HandResult(
                vals[0] == 14 ? HandRank.ROYAL_FLUSH : HandRank.STRAIGHT_FLUSH, vals);
        if (flush && wheel)    return new HandResult(HandRank.STRAIGHT_FLUSH, new int[]{5});

        Map<Integer, Integer> freq = frequency(five);
        List<Integer> counts = new ArrayList<>(freq.values());
        counts.sort(Comparator.reverseOrder());

        if (counts.get(0) == 4) return new HandResult(HandRank.FOUR_OF_A_KIND,  tiebreak(freq, 4));
        if (counts.get(0) == 3 && counts.get(1) == 2)
                                 return new HandResult(HandRank.FULL_HOUSE,       tiebreak(freq, 3));
        if (flush)               return new HandResult(HandRank.FLUSH,            vals);
        if (straight)            return new HandResult(HandRank.STRAIGHT,         vals);
        if (wheel)               return new HandResult(HandRank.STRAIGHT,         new int[]{5,4,3,2,1});
        if (counts.get(0) == 3) return new HandResult(HandRank.THREE_OF_A_KIND,  tiebreak(freq, 3));
        if (counts.get(0) == 2 && counts.get(1) == 2)
                                 return new HandResult(HandRank.TWO_PAIR,         tiebreak(freq, 2));
        if (counts.get(0) == 2) return new HandResult(HandRank.ONE_PAIR,         tiebreak(freq, 2));
        return                           new HandResult(HandRank.HIGH_CARD,       vals);
    }

    private static boolean isFlush(List<Card> five) {
        Card.Suit s = five.get(0).suit;
        return five.stream().allMatch(c -> c.suit == s);
    }

    private static boolean isStraight(int[] sortedVals) {
        for (int i = 1; i < sortedVals.length; i++)
            if (sortedVals[i] != sortedVals[i - 1] - 1) return false;
        return true;
    }

    private static boolean isWheel(int[] sortedVals) {
        return sortedVals[0] == 14 && sortedVals[1] == 5
            && sortedVals[2] == 4  && sortedVals[3] == 3 && sortedVals[4] == 2;
    }

    private static Map<Integer, Integer> frequency(List<Card> five) {
        Map<Integer, Integer> map = new HashMap<>();
        for (Card c : five) map.merge(c.rank.value, 1, Integer::sum);
        return map;
    }

    private static int[] tiebreak(Map<Integer, Integer> freq, int primaryCount) {
        List<Integer> primary  = new ArrayList<>();
        List<Integer> kickers  = new ArrayList<>();
        freq.entrySet().stream()
            .sorted(Map.Entry.<Integer,Integer>comparingByValue().reversed()
                    .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
            .forEach(e -> {
                if (e.getValue() == primaryCount) primary.add(e.getKey());
                else kickers.add(e.getKey());
            });
        Collections.sort(primary,  Comparator.reverseOrder());
        Collections.sort(kickers,  Comparator.reverseOrder());
        int[] result = new int[primary.size() + kickers.size()];
        int i = 0;
        for (int v : primary) result[i++] = v;
        for (int v : kickers) result[i++] = v;
        return result;
    }

    private static List<List<Card>> choose5(List<Card> cards) {
        List<List<Card>> result = new ArrayList<>();
        combine(cards, 5, 0, new ArrayList<>(), result);
        return result;
    }

    private static void combine(List<Card> cards, int k, int start,
                                List<Card> current, List<List<Card>> result) {
        if (current.size() == k) { result.add(new ArrayList<>(current)); return; }
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            combine(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
