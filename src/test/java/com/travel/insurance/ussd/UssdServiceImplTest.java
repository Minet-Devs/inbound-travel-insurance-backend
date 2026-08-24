package com.travel.insurance.ussd;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.config.UssdProperties;
import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdResponse;
import com.travel.insurance.ussd.service.UssdServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UssdServiceImplTest {

    @Mock
    private EmailService emailService;

    private MailProperties mailProperties;
    private UssdServiceImpl ussdService;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setFrom("no-reply@travel.example");
        MailProperties.EmergencyAssistance ea = new MailProperties.EmergencyAssistance();
        ea.setPhone("+254700000000");
        mailProperties.setEmergencyAssistance(ea);

        UssdProperties ussdProperties = new UssdProperties();
        ussdService = new UssdServiceImpl(emailService, mailProperties, ussdProperties);
    }

    private UssdSession createSession(String step) {
        UssdSession session = new UssdSession();
        session.setSessionId("sess-123");
        session.setMsisdn("254712345678");
        session.setCurrentStep(step);
        session.setCollectedData(new HashMap<>());
        session.setMenuMap(new HashMap<>());
        return session;
    }

    @Test
    void initShowsMainMenu() {
        UssdSession session = createSession("INIT");

        UssdResponse response = ussdService.processSessionStep(session, "");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Welcome to Inbound Travel Medical Insurance");
        assertThat(response.getText()).contains("1. Find Hospital");
        assertThat(response.getText()).contains("2. Feedback");
        assertThat(session.getCurrentStep()).isEqualTo("MAIN_MENU");
    }

    @Test
    void hospitalSubMenuShowsLocationOptions() {
        UssdSession session = createSession("HOSPITAL_SUB_MENU");

        UssdResponse response = ussdService.processSessionStep(session, "1");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Enter county name to search:");
        assertThat(session.getCurrentStep()).isEqualTo("PROMPT_COUNTY_NAME");
    }

    @Test
    void countyPromptEmptyRePrompts() {
        UssdSession session = createSession("PROMPT_COUNTY_NAME");

        UssdResponse response = ussdService.processSessionStep(session, "   ");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("County name cannot be empty");
        assertThat(session.getCurrentStep()).isEqualTo("PROMPT_COUNTY_NAME");
    }

    @Test
    void countyPromptCapturesValueAndReturnsPlaceholder() {
        UssdSession session = createSession("PROMPT_COUNTY_NAME");

        UssdResponse response = ussdService.processSessionStep(session, "Nairobi");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("County search is not live yet for 'Nairobi'");
        assertThat(response.getText()).contains("1. Find Hospital");
        assertThat(session.getCurrentStep()).isEqualTo("MAIN_MENU");
        assertThat(session.getCollectedData().get("pendingLocationType")).isEqualTo("COUNTY");
        assertThat(session.getCollectedData().get("pendingLocationValue")).isEqualTo("Nairobi");
    }

    @Test
    void townPromptCapturesValueAndReturnsPlaceholder() {
        UssdSession session = createSession("PROMPT_TOWN_NAME");

        UssdResponse response = ussdService.processSessionStep(session, "Mombasa");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Town search is not live yet for 'Mombasa'");
        assertThat(response.getText()).contains("1. Find Hospital");
        assertThat(session.getCurrentStep()).isEqualTo("MAIN_MENU");
        assertThat(session.getCollectedData().get("pendingLocationType")).isEqualTo("TOWN");
        assertThat(session.getCollectedData().get("pendingLocationValue")).isEqualTo("Mombasa");
    }

    @Test
    void borderPointPlaceholderReturnsToMainMenu() {
        UssdSession session = createSession("HOSPITAL_SUB_MENU");

        UssdResponse response = ussdService.processSessionStep(session, "3");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("This option is coming soon.");
        assertThat(response.getText()).contains("1. Find Hospital");
        assertThat(session.getCurrentStep()).isEqualTo("MAIN_MENU");
    }

    @Test
    void feedbackMessageSendsEmailAndEnds() {
        UssdSession session = createSession("FEEDBACK_MESSAGE");

        UssdResponse response = ussdService.processSessionStep(session, "Great service overall!");

        assertThat(response.getType()).isEqualTo("END");
        assertThat(response.getText()).contains("Feedback submitted. Thank you!");
        verify(emailService).send(eq("no-reply@travel.example"), eq("inbound.travel@minet.co.ke"), anyString(), anyString());
    }

    @Test
    void feedbackMessageEmptyRePrompts() {
        UssdSession session = createSession("FEEDBACK_MESSAGE");

        UssdResponse response = ussdService.processSessionStep(session, "  ");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Feedback cannot be empty");
        assertThat(session.getCurrentStep()).isEqualTo("FEEDBACK_MESSAGE");
    }
}
