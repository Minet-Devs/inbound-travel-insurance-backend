package com.travel.insurance.procedure;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ProcedureNameNormalizer {
    public record CleanedName(String display, String normalized) {
        public boolean isBlank() {
            return display == null || display.isBlank();
        }
    }

    public CleanedName clean(String raw) {
        if (raw == null) {
            return new CleanedName("", "");
        }
        String display = raw.replaceAll("\\u00A0", " ")
                .replaceAll("[\\p{Cntrl}&&[^\\s]]", "")
                .replaceAll("\\p{Cf}", "")
                .replaceAll("\\s+", " ")
                .trim();
        String normalized = display.toUpperCase(Locale.ROOT);
        return new CleanedName(display, normalized);
    }
}
