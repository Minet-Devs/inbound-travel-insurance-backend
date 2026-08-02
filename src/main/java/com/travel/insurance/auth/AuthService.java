package com.travel.insurance.auth;

import com.travel.insurance.auth.dto.LoginRequest;
import com.travel.insurance.auth.dto.RefreshRequest;
import com.travel.insurance.auth.dto.TokenResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshRequest request);
}