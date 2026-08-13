package com.travel.insurance.preauthorization;

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
import java.util.UUID;

@Entity
@Table(name = "preauthorization_enhancements")
@SQLDelete(sql = "update preauthorization_enhancements set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class PreauthorizationEnhancement extends BaseEntity {

    @Column(name = "preauthorization_id", nullable = false, unique = true)
    private UUID preauthorizationId;

    @Column(name = "medical_service_id")
    private UUID medicalServiceId;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;
}