package com.travel.insurance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Getter
@Setter
@ConfigurationProperties(prefix = "ussd")
public class UssdProperties {

    @NestedConfigurationProperty
    private Feedback feedback = new Feedback();

    @Getter
    @Setter
    public static class Feedback {
        private String defaultSchemeName = "Inbound Travel Health Insurance";

        @NestedConfigurationProperty
        private Email email = new Email();

        @Getter
        @Setter
        public static class Email {
            private String to = "inbound.travel@minet.co.ke";
        }
    }
}
