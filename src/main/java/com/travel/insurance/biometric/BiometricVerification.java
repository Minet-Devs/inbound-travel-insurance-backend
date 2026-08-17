package com.travel.insurance.biometric;

import com.travel.insurance.common.crypto.EncryptedStringConverter;
import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "biometric_verifications")
@SQLDelete(sql = "update biometric_verifications set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class BiometricVerification extends BaseEntity {

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String subjectIdNumber;

    @Column(nullable = false, length = 50)
    private String subjectIdType;

    @Column(nullable = false, length = 100)
    private String policyNumber;

    @Column(nullable = false, length = 255)
    private String workstationId;

    @Column(length = 100)
    private String ekycRequestId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String embededToken;

    @Column(length = 100)
    private String embededExpiry;

    @Column(length = 2000)
    private String requestUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BiometricVerificationStatus status = BiometricVerificationStatus.PENDING;

    @Column(length = 50)
    private String result;

    @Column(length = 100)
    private String statusCode;

    private Integer remainingAttempts;
}
