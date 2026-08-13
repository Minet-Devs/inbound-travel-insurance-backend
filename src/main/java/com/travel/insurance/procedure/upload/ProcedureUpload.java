package com.travel.insurance.procedure.upload;

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

import java.time.Instant;
import java.util.UUID;

/**
 * Batch record for one procedure Excel upload. The upload time is the inherited
 * {@link BaseEntity#getCreatedDate() createdDate}; {@code uploadedBy} is the
 * acting user captured at validation time.
 */
@Entity
@Table(name = "procedure_uploads")
@SQLDelete(sql = "update procedure_uploads set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class ProcedureUpload extends BaseEntity {

    private String originalFilename;

    @Column(name = "department_public_id", nullable = false)
    private UUID departmentPublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcedureUploadStatus status;

    private int totalRows;
    private int validRows;
    private int createdRows;
    private int skippedRows;
    private int failedRows;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    private Instant processingStartTime;
    private Instant completionTime;
    private String failureReason;
}
