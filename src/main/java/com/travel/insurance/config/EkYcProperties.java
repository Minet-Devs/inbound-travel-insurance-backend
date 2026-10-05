package com.travel.insurance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.ekyc")
public class EkYcProperties {

    private String clientId;
    private String clientSecret;
    private String accessTokenUrl;
    private String verificationUrl;
    private String callbackResendUrl;
    private String notificationCallbackUrl;
    private List<String> callbackAllowedIps = List.of();
}
