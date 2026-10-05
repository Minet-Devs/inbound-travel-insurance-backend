package com.travel.insurance.mobileauth;

import com.travel.insurance.mobileauth.dto.RequestVisitorOtpRequest;
import com.travel.insurance.mobileauth.dto.VerifyVisitorOtpRequest;
import com.travel.insurance.mobileauth.dto.VisitorTokenResponse;

public interface MobileAuthService {

    void requestOtp(RequestVisitorOtpRequest request);

    VisitorTokenResponse verifyOtp(VerifyVisitorOtpRequest request);
}
