package com.travel.insurance.auth;

import com.travel.insurance.auth.dto.LoginRequest;
import com.travel.insurance.auth.dto.RefreshRequest;
import com.travel.insurance.auth.dto.RegisterRequest;
import com.travel.insurance.auth.dto.TokenResponse;
import com.travel.insurance.user.dto.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshRequest request);
}
