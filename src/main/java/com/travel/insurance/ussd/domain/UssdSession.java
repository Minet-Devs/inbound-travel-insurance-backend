package com.travel.insurance.ussd.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UssdSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String msisdn;
    private String currentStep;
    private Map<String, String> collectedData = new HashMap<>();
    private Map<String, String> tempDependant = new HashMap<>();
    private Map<String, String> menuMap = new HashMap<>();
    private String memberState;

}
