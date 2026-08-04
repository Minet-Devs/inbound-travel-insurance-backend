package com.travel.insurance.visitorbenefit;

import com.travel.insurance.common.domain.BaseEntity;
import com.travel.insurance.visitor.VisitorStatus;
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
@Table(name = "visitor_benefits")
@SQLDelete(sql = "update visitor_benefits set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class VisitorBenefit extends BaseEntity {

    @Column(nullable = false)
    private UUID visitorId;

    @Column(nullable = false)
    private UUID benefitId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal limitAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitorStatus status = VisitorStatus.PENDING;
}
