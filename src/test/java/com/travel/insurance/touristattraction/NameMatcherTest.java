package com.travel.insurance.touristattraction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameMatcherTest {

    @Test
    void normaliseTrimsLowercasesAndCollapsesWhitespace() {
        assertThat(NameMatcher.normalise("  Lake   NAKURU\tPark ")).isEqualTo("lake nakuru park");
        assertThat(NameMatcher.normalise(null)).isEmpty();
    }

    @Test
    void substringDistanceIsZeroWhenPatternOccursInText() {
        assertThat(NameMatcher.substringDistance("nakuru", "lake nakuru national park")).isZero();
    }

    @Test
    void substringDistanceCountsSingleCharacterEdits() {
        assertThat(NameMatcher.substringDistance("masai mara", "maasai mara national reserve")).isEqualTo(1);
        assertThat(NameMatcher.substringDistance("nakuru natonal", "lake nakuru national park")).isEqualTo(1);
    }

    @Test
    void substringDistanceIsLargeForUnrelatedText() {
        assertThat(NameMatcher.substringDistance("zzzzzzzz", "amboseli national park")).isGreaterThan(4);
    }
}
