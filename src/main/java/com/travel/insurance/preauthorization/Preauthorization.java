package com.travel.insurance.preauthorization;

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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "preauthorizations")
@SQLDelete(sql = "update preauthorizations set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Preauthorization extends BaseEntity {

    @Column(nullable = false)
    private UUID policyId;

    @Column(nullable = false)
    private UUID visitorId;

    @Column(name = "icd11_code_id", nullable = false)
    private UUID icd11CodeId;

    @Column(nullable = false)
    private UUID benefitId;

    @Column(nullable = false)
    private UUID serviceProviderId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String serviceDescription;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String decisionReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreauthorizationStatus status = PreauthorizationStatus.PENDING;

    @Column()
    private Boolean convertedToClaim = false;
}
