package com.travel.insurance.ussd.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UssdResponse {

    private String text;
    private String type;

}
