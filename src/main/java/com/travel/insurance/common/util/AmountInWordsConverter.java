package com.travel.insurance.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Spells out a monetary amount, e.g. {@code 68044.00} becomes
 * "Sixty Eight Thousand and Forty Four Only" — matching the wording style
 * used on Minet's cheque/receipt vouchers. Whole shillings/dollars only;
 * cents are appended as "and NN/100" when non-zero.
 */
public final class AmountInWordsConverter {

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private AmountInWordsConverter() {
    }

    public static String toWords(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        long whole = rounded.longValue();
        int cents = rounded.subtract(BigDecimal.valueOf(whole)).movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP).intValue();

        String wholeWords = whole == 0 ? "Zero" : numberToWords(whole);
        if (cents == 0) {
            return wholeWords + " Only";
        }
        return wholeWords + " and " + cents + "/100 Only";
    }

    private static String numberToWords(long number) {
        StringBuilder result = new StringBuilder();
        long remaining = number;

        remaining = appendGroup(result, remaining, 1_000_000_000L, "Billion");
        remaining = appendGroup(result, remaining, 1_000_000L, "Million");
        remaining = appendGroup(result, remaining, 1_000L, "Thousand");

        if (remaining > 0) {
            if (result.length() > 0 && remaining < 100) {
                result.append("and ");
            }
            result.append(belowThousand(remaining));
        }
        return result.toString().trim();
    }

    private static long appendGroup(StringBuilder result, long remaining, long unit, String label) {
        if (remaining < unit) {
            return remaining;
        }
        result.append(belowThousand(remaining / unit)).append(' ').append(label).append(' ');
        return remaining % unit;
    }

    private static String belowThousand(long number) {
        if (number < 100) {
            return belowHundred(number);
        }
        long hundreds = number / 100;
        long rest = number % 100;
        String words = ONES[(int) hundreds] + " Hundred";
        if (rest > 0) {
            words += " and " + belowHundred(rest);
        }
        return words;
    }

    private static String belowHundred(long number) {
        if (number < 20) {
            return ONES[(int) number];
        }
        long tens = number / 10;
        long ones = number % 10;
        return ones == 0 ? TENS[(int) tens] : TENS[(int) tens] + " " + ONES[(int) ones];
    }
}
