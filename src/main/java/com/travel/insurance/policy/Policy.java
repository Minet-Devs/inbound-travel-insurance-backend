package com.travel.insurance.policy;

import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "policies")
@SQLDelete(sql = "update policies set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Policy extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String policyNumber;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "policy_insurers", joinColumns = @JoinColumn(name = "policy_id"))
    @Column(name = "insurer_id", nullable = false)
    private Set<UUID> insurerIds = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyStatus status = PolicyStatus.DRAFT;
}
