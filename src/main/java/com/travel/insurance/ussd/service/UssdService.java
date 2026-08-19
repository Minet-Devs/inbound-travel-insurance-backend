package com.travel.insurance.ussd.service;

import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdResponse;

public interface UssdService {

    UssdResponse processSessionStep(UssdSession session, String rawInput);

}
