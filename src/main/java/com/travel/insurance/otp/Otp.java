package com.travel.insurance.otp;

import com.travel.insurance.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otps")
@SQLDelete(sql = "update otps set deleted = true, deleted_date = now() where id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Otp extends BaseEntity {

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(nullable = false)
    private Instant expiryTime;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private UUID serviceProviderId;
}
