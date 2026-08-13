package com.travel.insurance.department;

import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "departments")
@SQLDelete(sql = "update departments set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;
}