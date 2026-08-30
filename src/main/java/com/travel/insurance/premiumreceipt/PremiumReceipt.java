package com.travel.insurance.premiumreceipt;

import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "premium_receipts")
@Getter
@Setter
@NoArgsConstructor
public class PremiumReceipt extends BaseEntity {

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPremium;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal pcfLevy;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal insurancePremiumLevy;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal stampDuty;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal trainingLevy;
}
