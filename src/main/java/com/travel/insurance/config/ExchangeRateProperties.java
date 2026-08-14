package com.travel.insurance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.exchangerate")
public class ExchangeRateProperties {

    private String baseUrl = "https://v6.exchangerate-api.com/v6";
    private String apiKey;
}
