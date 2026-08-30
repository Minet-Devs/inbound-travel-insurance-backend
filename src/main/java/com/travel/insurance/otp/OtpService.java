package com.travel.insurance.otp;

import com.travel.insurance.otp.dto.SendOtpRequest;
import com.travel.insurance.otp.dto.VerifyOtpRequest;

public interface OtpService {

    void send(SendOtpRequest request);

    void verify(VerifyOtpRequest request);
}
