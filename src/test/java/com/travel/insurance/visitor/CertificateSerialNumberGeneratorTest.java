package com.travel.insurance.visitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateSerialNumberGeneratorTest {

    @Mock
    private VisitorRepository visitorRepository;

    @InjectMocks
    private CertificateSerialNumberGenerator generator;

    @Test
    void formatsSequenceValueWithInsurerPrefixAndCurrentYear() {
        when(visitorRepository.nextCertificateSerialValue()).thenReturn(123L);

        String serial = generator.next("Acme Insurance");

        assertThat(serial).isEqualTo("ACME-" + Year.now().getValue() + "-000123");
    }

    @Test
    void formatUsesFirstWordOfMultiWordInsurerName() {
        assertThat(CertificateSerialNumberGenerator.format("Acme Insurance", 2026, 42L))
                .isEqualTo("ACME-2026-000042");
    }

    @Test
    void formatUppercasesAndStripsPunctuationFromPrefix() {
        assertThat(CertificateSerialNumberGenerator.prefix("aar Kenya")).isEqualTo("AAR");
        assertThat(CertificateSerialNumberGenerator.prefix("O'Brien & Co")).isEqualTo("OBRIEN");
    }

    @Test
    void formatPadsLargerSequenceValuesWithoutTruncating() {
        assertThat(CertificateSerialNumberGenerator.format("Acme", 2026, 1L))
                .isEqualTo("ACME-2026-000001");
        assertThat(CertificateSerialNumberGenerator.format("Acme", 2026, 123456L))
                .isEqualTo("ACME-2026-123456");
    }
}
