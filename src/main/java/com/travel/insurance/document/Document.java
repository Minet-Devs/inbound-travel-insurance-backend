package com.travel.insurance.document;

import com.travel.insurance.common.domain.BaseEntity;
import com.travel.insurance.document.dto.SourceFileInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One uploaded document: the merged PDF held in blob storage under {@code storageKey}. */
@Entity
@Table(name = "documents")
@SQLDelete(sql = "update documents set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Document extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentOwnerType ownerType;

    /** Id of the owning claim or pre-authorization; no FK, per the cross-feature rule. */
    @Column(nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private int pageCount;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false)
    private String storageKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private List<SourceFileInfo> sourceFiles = new ArrayList<>();

    private UUID uploadedBy;

    private UUID uploadedByOrganizationId;
}
