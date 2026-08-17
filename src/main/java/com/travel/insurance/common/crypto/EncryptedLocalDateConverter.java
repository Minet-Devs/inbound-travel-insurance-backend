package com.travel.insurance.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Converter
@Component
@RequiredArgsConstructor
public class EncryptedLocalDateConverter implements AttributeConverter<LocalDate, String> {

    private final FieldEncryptionService fieldEncryptionService;

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute == null ? null : fieldEncryptionService.encrypt(attribute.toString());
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        String decrypted = fieldEncryptionService.decrypt(dbData);
        return decrypted == null || decrypted.isEmpty() ? null : LocalDate.parse(decrypted);
    }
}
