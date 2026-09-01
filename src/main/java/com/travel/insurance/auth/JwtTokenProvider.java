package com.travel.insurance.auth;

import com.travel.insurance.user.User;
import com.travel.insurance.visitor.Visitor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    public static final String CLAIM_FIRST_NAME = "firstName";
    public static final String CLAIM_LAST_NAME = "lastName";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_ORGANIZATION_ID = "organizationId";
    public static final String CLAIM_ORGANIZATION_NAME = "organizationName";
    public static final String CLAIM_SERVICE_PROVIDER_ID = "serviceProviderId";
    public static final String CLAIM_INSURER_ID = "insurerId";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_VISITOR_ID = "visitorId";
    public static final String CLAIM_PASSPORT_NUMBER = "passportNumber";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String ROLE_VISITOR = "VISITOR";

    private final SecretKey key;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                            @Value("${app.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds,
                            @Value("${app.jwt.refresh-token-ttl-seconds}") long refreshTokenTtlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public String createAccessToken(User user, String organizationName, UUID serviceProviderId, UUID insurerId) {
        return createToken(user, organizationName, serviceProviderId, insurerId, TOKEN_TYPE_ACCESS, accessTokenTtlSeconds);
    }

    public String createRefreshToken(User user, String organizationName, UUID serviceProviderId, UUID insurerId) {
        return createToken(user, organizationName, serviceProviderId, insurerId, TOKEN_TYPE_REFRESH, refreshTokenTtlSeconds);
    }

    public String createVisitorAccessToken(Visitor visitor) {
        return createVisitorToken(visitor, TOKEN_TYPE_ACCESS, accessTokenTtlSeconds);
    }

    public String createVisitorRefreshToken(Visitor visitor) {
        return createVisitorToken(visitor, TOKEN_TYPE_REFRESH, refreshTokenTtlSeconds);
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private String createToken(User user, String organizationName, UUID serviceProviderId, UUID insurerId,
                                String tokenType, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_FIRST_NAME, user.getFirstName())
                .claim(CLAIM_LAST_NAME, user.getLastName())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_ORGANIZATION_ID,
                        user.getOrganizationId() != null ? user.getOrganizationId().toString() : null)
                .claim(CLAIM_ORGANIZATION_NAME, organizationName)
                .claim(CLAIM_SERVICE_PROVIDER_ID, serviceProviderId != null ? serviceProviderId.toString() : null)
                .claim(CLAIM_INSURER_ID, insurerId != null ? insurerId.toString() : null)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    private String createVisitorToken(Visitor visitor, String tokenType, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(visitor.getId().toString())
                .claim(CLAIM_VISITOR_ID, visitor.getId().toString())
                .claim(CLAIM_PASSPORT_NUMBER, visitor.getPassportNumber())
                .claim(CLAIM_ROLE, ROLE_VISITOR)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }
}
