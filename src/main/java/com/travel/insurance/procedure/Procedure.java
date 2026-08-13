package com.travel.insurance.procedure;

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

import java.util.UUID;

@Entity
@Table(name = "procedures")
@SQLDelete(sql = "update procedures set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Procedure extends BaseEntity {

    @Column(name = "procedure_code", nullable = false, unique = true, updatable = false)
    private String procedureCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    private String description;

    @Column(name = "department_public_id", nullable = false)
    private UUID departmentPublicId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "upload_batch_public_id")
    private UUID uploadBatchPublicId;
}
