package com.travel.insurance.touristattraction;

import java.util.Locale;

/**
 * Small helpers for forgiving name lookups. Attractions are a short list, so matching runs
 * in memory rather than needing a database extension such as pg_trgm.
 */
final class NameMatcher {

    private NameMatcher() {
    }

    static String normalise(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /**
     * Smallest number of single-character edits needed to turn {@code pattern} into some
     * substring of {@code text} (Sellers' approximate substring matching). Zero means the
     * pattern occurs verbatim inside the text, so a misspelt partial name such as
     * "nakuru natonal" still lands within one edit of "lake nakuru national park".
     */
    static int substringDistance(String pattern, String text) {
        int m = pattern.length();
        int[] prev = new int[m + 1];
        for (int i = 0; i <= m; i++) {
            prev[i] = i;
        }
        int best = prev[m];
        for (int j = 0; j < text.length(); j++) {
            int[] cur = new int[m + 1];
            for (int i = 1; i <= m; i++) {
                int cost = pattern.charAt(i - 1) == text.charAt(j) ? 0 : 1;
                cur[i] = Math.min(Math.min(prev[i - 1] + cost, prev[i] + 1), cur[i - 1] + 1);
            }
            best = Math.min(best, cur[m]);
            prev = cur;
        }
        return best;
    }
}
