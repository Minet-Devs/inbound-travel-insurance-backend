package com.travel.insurance.ussd.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPanelEntry {

    private String area;
    private String town;
    private String county;
    private String providerName;
    private String address;
    private String services;
}
