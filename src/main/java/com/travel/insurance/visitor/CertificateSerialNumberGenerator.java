package com.travel.insurance.visitor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CertificateSerialNumberGenerator {

    private final VisitorRepository visitorRepository;

    public String next(String insurerName) {
        return format(insurerName, Year.now().getValue(), visitorRepository.nextCertificateSerialValue());
    }

    static String format(String insurerName, int year, long value) {
        return "%s-%d-%06d".formatted(prefix(insurerName), year, value);
    }

    static String prefix(String insurerName) {
        String firstWord = insurerName.trim().split("\\s+")[0];
        String cleaned = firstWord.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return cleaned.isEmpty() ? "CERT" : cleaned;
    }
}
