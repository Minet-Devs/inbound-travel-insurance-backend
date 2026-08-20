package com.travel.insurance.insurer;

import com.travel.insurance.common.crypto.EncryptedStringConverter;
import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "insurers")
@SQLDelete(sql = "update insurers set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Insurer extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String contactEmail;

    private String contactPhone;

    private String address;

    private String logoUrl;

    private Long policyToken;

    private String notificationEmail;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String notificationEmailPassword;

    private String host;

    private Integer port;

    private String esignature;
}
