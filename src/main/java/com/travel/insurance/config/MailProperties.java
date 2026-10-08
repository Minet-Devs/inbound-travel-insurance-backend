package com.travel.insurance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private String from;

    /** Hidden recipients copied on every visitor activation email. */
    private List<String> activationBcc = new ArrayList<>();

    @NestedConfigurationProperty
    private EmergencyAssistance emergencyAssistance = new EmergencyAssistance();

    @Getter
    @Setter
    public static class EmergencyAssistance {
        private String phone;
        private String email;
    }
}
