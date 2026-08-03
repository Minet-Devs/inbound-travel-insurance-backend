package com.travel.insurance.claim;

import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.Column;
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
@Table(name = "claims")
@SQLDelete(sql = "update claims set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Claim extends BaseEntity {

    @Column(nullable = false)
    private UUID policyId;

    @Column(nullable = false)
    private UUID benefitId;

    private UUID serviceProviderId;

    private UUID preauthorizationId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal claimedAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String decisionReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.SUBMITTED;
}
