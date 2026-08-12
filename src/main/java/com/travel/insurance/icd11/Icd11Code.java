package com.travel.insurance.icd11;

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
@Table(name = "icd11_codes")
@SQLDelete(sql = "update icd11_codes set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Icd11Code extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false, length = 1000)
    private String title;
}
