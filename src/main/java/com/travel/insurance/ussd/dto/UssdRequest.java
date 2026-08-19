package com.travel.insurance.ussd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UssdRequest {

    private String sessionId;
    private String msisdn;
    @JsonProperty("ussd_string")
    private String ussdString;
    private String serviceCode;

}
