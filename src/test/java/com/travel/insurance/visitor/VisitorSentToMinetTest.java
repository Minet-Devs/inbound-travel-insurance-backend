package com.travel.insurance.visitor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorSentToMinetTest {

    @Test
    void newVisitorIsNotSentToMinet() {
        assertThat(new Visitor().isSentToMinet()).isFalse();
    }
}
