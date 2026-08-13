package com.travel.insurance.procedure;

import com.travel.insurance.procedure.ProcedureNameNormalizer.CleanedName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProcedureNameNormalizerTest {

    private final ProcedureNameNormalizer normalizer = new ProcedureNameNormalizer();

    @Test
    void trimsCollapsesAndUppercasesForNormalization() {
        CleanedName cleaned = normalizer.clean("  CHEST   TUBE INSERTION  ");

        assertThat(cleaned.display()).isEqualTo("CHEST TUBE INSERTION");
        assertThat(cleaned.normalized()).isEqualTo("CHEST TUBE INSERTION");
    }

    @Test
    void normalizesCaseVariantsToTheSameNormalizedName() {
        assertThat(normalizer.clean("Nebulization").normalized()).isEqualTo("NEBULIZATION");
        assertThat(normalizer.clean("NEBULIZATION").normalized()).isEqualTo("NEBULIZATION");
        assertThat(normalizer.clean(" nebulization ").normalized()).isEqualTo("NEBULIZATION");
    }

    @Test
    void replacesNonBreakingSpacesWithNormalSpaces() {
        // "Lumbar" + non-breaking space (U+00A0) + "Puncture"
        CleanedName cleaned = normalizer.clean("Lumbar Puncture");

        assertThat(cleaned.display()).isEqualTo("Lumbar Puncture");
        assertThat(cleaned.normalized()).isEqualTo("LUMBAR PUNCTURE");
    }

    @Test
    void stripsControlAndFormatCharacters() {
        // embedded bell (U+0007 control) and zero-width space (U+200B format)
        CleanedName cleaned = normalizer.clean("Nebuliz​ation");

        assertThat(cleaned.display()).isEqualTo("Nebulization");
    }

    @Test
    void preservesMedicalTerminologyAndSpelling() {
        assertThat(normalizer.clean("Nebulisation").display()).isEqualTo("Nebulisation");
    }

    @Test
    void treatsNullAndBlankAsBlank() {
        assertThat(normalizer.clean(null).isBlank()).isTrue();
        assertThat(normalizer.clean("   ").isBlank()).isTrue();
    }
}
