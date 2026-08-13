package com.travel.insurance.medicalservice;

import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "medical_services")
@SQLDelete(sql = "update medical_services set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class MedicalService extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private UUID departmentId;
}