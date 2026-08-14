package com.travel.insurance.claim;

import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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

    private UUID visitorId;

    private UUID insurerId;

    @Column(precision = 15, scale = 2)
    private BigDecimal claimedAmount;

    @Column(nullable = false, length = 3)
    private String currency = "KES";

    @Column(nullable = false, length = 3)
    private String baseCurrency = "USD";

    @Column(precision = 15, scale = 2)
    private BigDecimal claimedAmountBase;

    @Column(precision = 12, scale = 6)
    private BigDecimal exchangeRate;

    private LocalDateTime fxRateDate;

    @Column(precision = 15, scale = 2)
    private BigDecimal approvedAmount;

    @Column(length = 1000)
    private String description;

    @Column(length = 2000)
    private String prescription;

    @ElementCollection
    @CollectionTable(name = "claim_diagnoses", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "diagnosis_id")
    private Set<UUID> diagnosisIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "claim_procedures", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "procedure_id")
    private Set<UUID> procedureIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "claim_invoices", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "invoice_id")
    private Set<UUID> invoiceIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "claim_documents", joinColumns = @JoinColumn(name = "claim_id"))
    @Column(name = "document_id")
    private Set<UUID> documentIds = new HashSet<>();

    @Column(length = 1000)
    private String decisionReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status = ClaimStatus.SUBMITTED;
}
