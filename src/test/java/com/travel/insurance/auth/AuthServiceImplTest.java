package com.travel.insurance.auth;

import com.travel.insurance.auth.dto.LoginRequest;
import com.travel.insurance.auth.dto.RefreshRequest;
import com.travel.insurance.auth.dto.TokenResponse;
import com.travel.insurance.user.Role;
import com.travel.insurance.user.User;
import com.travel.insurance.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userService, passwordEncoder, jwtTokenProvider);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("jane@acme.com");
        user.setPassword("hashed");
        user.setRole(Role.INSURER_USER);
        user.setOrganizationId(UUID.randomUUID());
    }

    @Test
    void loginIssuesTokensWithOrganizationName() {
        UUID insurerId = UUID.randomUUID();
        when(userService.findEntityByEmail("jane@acme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(userService.organizationName(user)).thenReturn("Minet Insurance");
        when(userService.serviceProviderId(user)).thenReturn(null);
        when(userService.insurerId(user)).thenReturn(insurerId);
        when(jwtTokenProvider.createAccessToken(user, "Minet Insurance", null, insurerId)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(user, "Minet Insurance", null, insurerId)).thenReturn("refresh-token");
        when(jwtTokenProvider.accessTokenTtlSeconds()).thenReturn(900L);

        TokenResponse response = authService.login(new LoginRequest("jane@acme.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.expiresInSeconds()).isEqualTo(900L);
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userService.findEntityByEmail("jane@acme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@acme.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshRejectsInvalidToken() {
        when(jwtTokenProvider.parse("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("bad-token")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
