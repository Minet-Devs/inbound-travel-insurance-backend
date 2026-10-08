package com.travel.insurance.visitor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class VisitorStatusTest {

    private static final Set<String> ALLOWED = Set.of(
            "PENDING->ACTIVE",
            "ACTIVE->SUSPENDED",
            "ACTIVE->DEACTIVATED",
            "SUSPENDED->ACTIVE",
            "SUSPENDED->DEACTIVATED");

    static Stream<Object[]> allPairs() {
        return Arrays.stream(VisitorStatus.values())
                .flatMap(from -> Arrays.stream(VisitorStatus.values())
                        .map(to -> arguments(from, to).get()));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allPairs")
    void transitionTableCoversEveryPair(VisitorStatus from, VisitorStatus to) {
        assertThat(from.canTransitionTo(to))
                .isEqualTo(ALLOWED.contains(from + "->" + to));
    }
}
