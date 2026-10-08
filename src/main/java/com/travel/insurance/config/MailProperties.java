package com.travel.insurance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;
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
    private Resend resend = new Resend();

    @NestedConfigurationProperty
    private EmergencyAssistance emergencyAssistance = new EmergencyAssistance();

    /** Scheduled re-send of activation emails that were never delivered. */
    @Getter
    @Setter
    public static class Resend {
        private boolean enabled = true;
        /** Fixed delay between runs. */
        private Duration interval = Duration.ofMinutes(10);
        /** Skip visitors newer than this, so the AFTER_COMMIT listener gets first go. */
        private Duration minAge = Duration.ofMinutes(5);
        /** Stop retrying visitors older than this. */
        private Duration maxAge = Duration.ofDays(7);
        /** Max visitors attempted per run. */
        private int batchSize = 50;
    }

    @Getter
    @Setter
    public static class EmergencyAssistance {
        private String phone;
        private String email;
    }
}
