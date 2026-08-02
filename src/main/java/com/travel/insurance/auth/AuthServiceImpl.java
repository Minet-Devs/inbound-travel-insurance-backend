package com.travel.insurance.auth;

import com.travel.insurance.auth.dto.LoginRequest;
import com.travel.insurance.auth.dto.RefreshRequest;
import com.travel.insurance.auth.dto.RegisterRequest;
import com.travel.insurance.auth.dto.TokenResponse;
import com.travel.insurance.user.Role;
import com.travel.insurance.user.User;
import com.travel.insurance.user.UserService;
import com.travel.insurance.user.dto.UserRequest;
import com.travel.insurance.user.dto.UserResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public UserResponse register(RegisterRequest request) {
        UserRequest userRequest = new UserRequest(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password(),
                request.phoneNumber(),
                Set.of(Role.CUSTOMER),
                null);
        return userService.create(userRequest);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = userService.findEntityByEmail(request.email())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return issueTokens(user);
    }

    @Override
    public TokenResponse refresh(RefreshRequest request) {
        Claims claims = jwtTokenProvider.parse(request.refreshToken())
                .filter(parsed -> JwtTokenProvider.TOKEN_TYPE_REFRESH
                        .equals(parsed.get(JwtTokenProvider.CLAIM_TOKEN_TYPE)))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        User user = userService.getEntityById(UUID.fromString(claims.getSubject()));
        return issueTokens(user);
    }

    private TokenResponse issueTokens(User user) {
        return TokenResponse.bearer(
                jwtTokenProvider.createAccessToken(user),
                jwtTokenProvider.createRefreshToken(user),
                jwtTokenProvider.accessTokenTtlSeconds());
    }
}
