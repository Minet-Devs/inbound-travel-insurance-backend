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
    private String accessTokenUrl = "https://dev.hakika.ecs.africa/api/v2/auth/access-token";
    private String verificationUrl = "https://dev.hakika.ecs.africa/api/v3/requests/embeded";
    private String callbackResendUrl = "https://dev.hakika.ecs.africa/api/v3/requests/callback/resend";
    private String notificationCallbackUrl;
    private List<String> callbackAllowedIps = List.of("167.71.142.137", "167.71.128.93");
}
