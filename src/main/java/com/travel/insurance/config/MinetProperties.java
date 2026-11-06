package com.travel.insurance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.minet")
public class MinetProperties {

    private String tokenUrl;
    private String visitorsUrl;
    private String claimsUrl;
    /** Minet scheme the inbound claims belong to; sent as the {@code schemePublicId} query parameter. */
    private String inboundSchemeId;
    private String basicAuthorization;
    private String clientId;
    private String username;
    private String password;
}
