package com.travel.insurance.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AmountInWordsConverterTest {

    @Test
    void spellsOutWholeAmountWithOnlySuffix() {
        String result = AmountInWordsConverter.toWords(new BigDecimal("44.00"));

        assertThat(result).isEqualTo("Forty Four Only");
    }

    @Test
    void spellsOutThousandsMatchingVoucherWording() {
        String result = AmountInWordsConverter.toWords(new BigDecimal("68044.00"));

        assertThat(result).isEqualTo("Sixty Eight Thousand and Forty Four Only");
    }

    @Test
    void appendsCentsAsFractionWhenNonZero() {
        String result = AmountInWordsConverter.toWords(new BigDecimal("44.50"));

        assertThat(result).isEqualTo("Forty Four and 50/100 Only");
    }

    @Test
    void spellsOutZero() {
        String result = AmountInWordsConverter.toWords(BigDecimal.ZERO);

        assertThat(result).isEqualTo("Zero Only");
    }

    @Test
    void spellsOutMillions() {
        String result = AmountInWordsConverter.toWords(new BigDecimal("1250000.00"));

        assertThat(result).isEqualTo("One Million Two Hundred and Fifty Thousand Only");
    }
}
