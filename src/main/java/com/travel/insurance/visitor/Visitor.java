package com.travel.insurance.visitor;

import com.travel.insurance.common.crypto.EncryptedLocalDateConverter;
import com.travel.insurance.common.crypto.EncryptedStringConverter;
import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "visitors")
@SQLDelete(sql = "update visitors set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Visitor extends BaseEntity {

    @Column(nullable = false)
    private UUID policyId;

    @Column(nullable = false)
    private UUID insurerId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String fullName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String passportNumber;

    @Column(name = "passport_number_hash", nullable = false, length = 64, unique = true)
    private String passportNumberHash;

    @Convert(converter = EncryptedLocalDateConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String nationality;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String address;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String email;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String phoneNumber;

    @Column(nullable = false)
    private LocalDate dateIn;

    @Column(nullable = false)
    private LocalDate dateOut;

    @Transient
    public LocalDate getPolicyExpiryDate() {
        return dateIn != null ? dateIn.plusDays(365) : null;
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaritalStatus maritalStatus;

    @Column(nullable = false)
    private String reasonForTravel;

    @Column(nullable = false)
    private String facePhotoUrl;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String underlyingConditions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitorStatus visitorStatus = VisitorStatus.ACTIVE;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String nextOfKinName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String nextOfKinPhone;

    private String paymentReference;

    private String etaReference;

    private Instant entryTimestamp;

    private Instant exitTimestamp;

    private String portOfEntry;

    private String certificateSerialNumber;
}
