package com.travel.insurance.auth;

import com.travel.insurance.user.Role;
import com.travel.insurance.user.User;
import com.travel.insurance.visitor.Visitor;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User user;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-test-secret-test-secret-test-secret", 900, 86400);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@acme.com");
        user.setRole(Role.INSURER_USER);
        user.setOrganizationId(UUID.randomUUID());
    }

    @Test
    void accessTokenCarriesUserProfileClaims() {
        UUID insurerId = UUID.randomUUID();
        String token = jwtTokenProvider.createAccessToken(user, "Minet Insurance", null, insurerId);

        Claims claims = jwtTokenProvider.parse(token).orElseThrow();

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get(JwtTokenProvider.CLAIM_FIRST_NAME)).isEqualTo("Jane");
        assertThat(claims.get(JwtTokenProvider.CLAIM_LAST_NAME)).isEqualTo("Doe");
        assertThat(claims.get(JwtTokenProvider.CLAIM_EMAIL)).isEqualTo("jane@acme.com");
        assertThat(claims.get(JwtTokenProvider.CLAIM_ROLE)).isEqualTo("INSURER_USER");
        assertThat(claims.get(JwtTokenProvider.CLAIM_ORGANIZATION_ID)).isEqualTo(user.getOrganizationId().toString());
        assertThat(claims.get(JwtTokenProvider.CLAIM_ORGANIZATION_NAME)).isEqualTo("Minet Insurance");
        assertThat(claims.get(JwtTokenProvider.CLAIM_SERVICE_PROVIDER_ID)).isNull();
        assertThat(claims.get(JwtTokenProvider.CLAIM_INSURER_ID)).isEqualTo(insurerId.toString());
        assertThat(claims.get(JwtTokenProvider.CLAIM_TOKEN_TYPE)).isEqualTo(JwtTokenProvider.TOKEN_TYPE_ACCESS);
    }

    @Test
    void refreshTokenCarriesRefreshTokenType() {
        String token = jwtTokenProvider.createRefreshToken(user, "Minet Insurance", null, UUID.randomUUID());

        Claims claims = jwtTokenProvider.parse(token).orElseThrow();

        assertThat(claims.get(JwtTokenProvider.CLAIM_TOKEN_TYPE)).isEqualTo(JwtTokenProvider.TOKEN_TYPE_REFRESH);
    }

    @Test
    void parseRejectsTamperedToken() {
        String token = jwtTokenProvider.createAccessToken(user, "Minet Insurance", null, UUID.randomUUID());

        assertThat(jwtTokenProvider.parse(token + "tampered")).isEmpty();
    }

    @Test
    void visitorAccessTokenCarriesVisitorIdAndRole() {
        Visitor visitor = new Visitor();
        visitor.setId(UUID.randomUUID());
        visitor.setPassportNumber("P1234567");

        String token = jwtTokenProvider.createVisitorAccessToken(visitor);
        Claims claims = jwtTokenProvider.parse(token).orElseThrow();

        assertThat(claims.getSubject()).isEqualTo(visitor.getId().toString());
        assertThat(claims.get(JwtTokenProvider.CLAIM_VISITOR_ID)).isEqualTo(visitor.getId().toString());
        assertThat(claims.get(JwtTokenProvider.CLAIM_PASSPORT_NUMBER)).isEqualTo("P1234567");
        assertThat(claims.get(JwtTokenProvider.CLAIM_ROLE)).isEqualTo(JwtTokenProvider.ROLE_VISITOR);
        assertThat(claims.get(JwtTokenProvider.CLAIM_TOKEN_TYPE)).isEqualTo(JwtTokenProvider.TOKEN_TYPE_ACCESS);
        assertThat(claims.get(JwtTokenProvider.CLAIM_ORGANIZATION_ID)).isNull();
    }

    @Test
    void visitorRefreshTokenCarriesRefreshTokenType() {
        Visitor visitor = new Visitor();
        visitor.setId(UUID.randomUUID());

        String token = jwtTokenProvider.createVisitorRefreshToken(visitor);
        Claims claims = jwtTokenProvider.parse(token).orElseThrow();

        assertThat(claims.get(JwtTokenProvider.CLAIM_TOKEN_TYPE)).isEqualTo(JwtTokenProvider.TOKEN_TYPE_REFRESH);
    }
}
