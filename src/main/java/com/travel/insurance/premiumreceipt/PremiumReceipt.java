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

    /**
     * Rate for visitors aged 18 and above.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPremium;

    /**
     * Rate for visitors aged 3 to 17.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minorPremium;

    /**
     * Rate for visitors aged 2 and below.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal infantPremium;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal pcfLevy;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal insurancePremiumLevy;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal stampDuty;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal trainingLevy;
}
