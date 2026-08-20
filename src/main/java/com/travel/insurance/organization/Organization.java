package com.travel.insurance.organization;

import com.travel.insurance.common.crypto.EncryptedStringConverter;
import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "organizations")
@SQLDelete(sql = "update organizations set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Organization extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationType organizationType;

    @Column(nullable = false)
    private String email;

    private String phoneNumber;

    private String address;

    private String city;

    private String notificationEmail;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(columnDefinition = "text")
    private String notificationEmailPassword;

    private String host;

    private Integer port;

    private String esignature;
}
