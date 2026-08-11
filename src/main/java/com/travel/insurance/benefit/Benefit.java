package com.travel.insurance.benefit;

import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "benefits")
@SQLDelete(sql = "update benefits set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Benefit extends BaseEntity {

    @Column(nullable = false)
    private String benefitName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal limitAmount;
}
