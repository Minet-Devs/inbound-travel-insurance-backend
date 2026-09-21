package com.travel.insurance.visitor;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class VisitorAgeAtTravelTest {

    @Test
    void computesAgeAsOfDateInNotBirthday() {
        Visitor visitor = new Visitor();
        visitor.setDateOfBirth(LocalDate.of(2000, 6, 15));
        visitor.setDateIn(LocalDate.of(2026, 6, 14));

        assertThat(visitor.getAgeAtTravel()).isEqualTo(25);
    }

    @Test
    void computesAgeAsOfDateInOnBirthday() {
        Visitor visitor = new Visitor();
        visitor.setDateOfBirth(LocalDate.of(2000, 6, 15));
        visitor.setDateIn(LocalDate.of(2026, 6, 15));

        assertThat(visitor.getAgeAtTravel()).isEqualTo(26);
    }

    @Test
    void computesInfantAge() {
        Visitor visitor = new Visitor();
        visitor.setDateOfBirth(LocalDate.of(2025, 1, 1));
        visitor.setDateIn(LocalDate.of(2026, 6, 1));

        assertThat(visitor.getAgeAtTravel()).isEqualTo(1);
    }
}
