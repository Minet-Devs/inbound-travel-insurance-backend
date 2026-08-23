package com.travel.insurance.ussd;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.config.UssdProperties;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdResponse;
import com.travel.insurance.ussd.service.UssdServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UssdServiceImplTest {

    @Mock
    private EmailService emailService;

    @Mock
    private ServiceProviderService serviceProviderService;

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
        ussdService = new UssdServiceImpl(
                emailService,
                mailProperties,
                serviceProviderService,
                ussdProperties
        );
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
    void initShowsMainMenuWhenMsisdnBlank() {
        UssdSession session = createSession("INIT");
        session.setMsisdn("");

        UssdResponse response = ussdService.processSessionStep(session, "");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Welcome to Inbound Travel Medical Insurance");
        assertThat(session.getCurrentStep()).isEqualTo("MAIN_MENU");
    }

    @Test
    void mainMenuOption1TransitionsToHospitalSubMenu() {
        UssdSession session = createSession("INIT");
        ussdService.processSessionStep(session, ""); // move to MAIN_MENU

        UssdResponse response = ussdService.processSessionStep(session, "1");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).isEqualTo("1. Check Hospital by Name");
        assertThat(session.getCurrentStep()).isEqualTo("HOSPITAL_SUB_MENU");
    }

    @Test
    void mainMenuOption2TransitionsToFeedbackPrompt() {
        UssdSession session = createSession("INIT");
        ussdService.processSessionStep(session, ""); // move to MAIN_MENU

        UssdResponse response = ussdService.processSessionStep(session, "2");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Provide your feedback:");
        assertThat(session.getCurrentStep()).isEqualTo("FEEDBACK_MESSAGE");
    }

    @Test
    void mainMenuInvalidChoiceRePrompts() {
        UssdSession session = createSession("INIT");
        ussdService.processSessionStep(session, "");

        UssdResponse response = ussdService.processSessionStep(session, "9");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Invalid choice");
        assertThat(response.getText()).contains("1. Find Hospital");
    }

    @Test
    void hospitalSubMenuOption1PromptsFacilityName() {
        UssdSession session = createSession("HOSPITAL_SUB_MENU");

        UssdResponse response = ussdService.processSessionStep(session, "1");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).isEqualTo("Enter facility name to search:");
        assertThat(session.getCurrentStep()).isEqualTo("PROMPT_HOSPITAL_NAME");
    }

    @Test
    void hospitalSubMenuOption0ReturnsToMainMenu() {
        UssdSession session = createSession("HOSPITAL_SUB_MENU");

        UssdResponse response = ussdService.processSessionStep(session, "0");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Welcome to Inbound Travel Medical Insurance");
        assertThat(session.getCurrentStep()).isEqualTo("MAIN_MENU");
    }

    @Test
    void hospitalSubMenuInvalidChoiceRePrompts() {
        UssdSession session = createSession("HOSPITAL_SUB_MENU");

        UssdResponse response = ussdService.processSessionStep(session, "5");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Invalid choice");
        assertThat(response.getText()).contains("1. Check Hospital by Name");
    }

    @Test
    void promptHospitalNameEmptyRePrompts() {
        UssdSession session = createSession("PROMPT_HOSPITAL_NAME");

        UssdResponse response = ussdService.processSessionStep(session, "   ");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Facility name cannot be empty");
        assertThat(session.getCurrentStep()).isEqualTo("PROMPT_HOSPITAL_NAME");
    }

    @Test
    void promptHospitalNameNoMatchOffersTryAgainOrMainMenu() {
        UssdSession session = createSession("PROMPT_HOSPITAL_NAME");
        when(serviceProviderService.searchByName("UnknownHosp", 5)).thenReturn(List.of());

        UssdResponse response = ussdService.processSessionStep(session, "UnknownHosp");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("No facility found matching 'UnknownHosp'");
        assertThat(response.getText()).contains("1. Try again");
        assertThat(response.getText()).contains("0. Main Menu");
        assertThat(session.getCurrentStep()).isEqualTo("NO_HOSPITAL_MATCH");
    }

    @Test
    void noHospitalMatchOption1ReturnsToPrompt() {
        UssdSession session = createSession("NO_HOSPITAL_MATCH");
        session.getCollectedData().put("lastSearchQuery", "UnknownHosp");

        UssdResponse response = ussdService.processSessionStep(session, "1");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).isEqualTo("Enter facility name to search:");
        assertThat(session.getCurrentStep()).isEqualTo("PROMPT_HOSPITAL_NAME");
    }

    @Test
    void noHospitalMatchOption0ReturnsToMainMenu() {
        UssdSession session = createSession("NO_HOSPITAL_MATCH");
        session.getCollectedData().put("lastSearchQuery", "UnknownHosp");

        UssdResponse response = ussdService.processSessionStep(session, "0");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Welcome to Inbound Travel Medical Insurance");
        assertThat(session.getCurrentStep()).isEqualTo("MAIN_MENU");
    }

    @Test
    void noHospitalMatchInvalidChoiceRePrompts() {
        UssdSession session = createSession("NO_HOSPITAL_MATCH");
        session.getCollectedData().put("lastSearchQuery", "UnknownHosp");

        UssdResponse response = ussdService.processSessionStep(session, "9");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Invalid choice");
        assertThat(response.getText()).contains("No facility found matching 'UnknownHosp'");
    }

    @Test
    void promptHospitalNameWithMatchesDisplaysNumberedList() {
        UssdSession session = createSession("PROMPT_HOSPITAL_NAME");
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ServiceProviderResponse p1 = new ServiceProviderResponse(id1, "Avenue Healthcare Clinic", "info@avenue.example", "+254711111111", "Buruburu, Nairobi", null, Instant.now(), Instant.now());
        ServiceProviderResponse p2 = new ServiceProviderResponse(id2, "Avenue Healthcare Rescue", "rescue@avenue.example", "+254722222222", "Parklands, Nairobi", null, Instant.now(), Instant.now());
        when(serviceProviderService.searchByName("Avenue", 5)).thenReturn(List.of(p1, p2));

        UssdResponse response = ussdService.processSessionStep(session, "Avenue");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Select Facility:");
        assertThat(response.getText()).contains("1. Avenue Healthcare Clinic");
        assertThat(response.getText()).contains("2. Avenue Healthcare Rescue");
        assertThat(session.getCurrentStep()).isEqualTo("SELECT_HOSPITAL_RESULT");
        assertThat(session.getCollectedData().get("facility_count")).isEqualTo("2");
    }

    @Test
    void selectHospitalResultValidChoiceReturnsEndWithCompactDetails() {
        UssdSession session = createSession("SELECT_HOSPITAL_RESULT");
        session.getCollectedData().put("facility_count", "2");
        session.getCollectedData().put("facility_name_1", "Avenue Hospital");
        session.getCollectedData().put("facility_addr_1", "Parklands, Nairobi");
        session.getCollectedData().put("facility_phone_1", "+254 700 000 000");

        UssdResponse response = ussdService.processSessionStep(session, "1");

        assertThat(response.getType()).isEqualTo("END");
        assertThat(response.getText()).contains("Avenue Hospital");
        assertThat(response.getText()).contains("Loc: Parklands, Nairobi");
        assertThat(response.getText()).contains("Tel: +254 700 000 000");
    }

    @Test
    void selectHospitalResultChoice0ReturnsToMainMenu() {
        UssdSession session = createSession("SELECT_HOSPITAL_RESULT");
        session.getCollectedData().put("facility_count", "2");
        session.getCollectedData().put("facility_name_1", "Avenue Hospital");

        UssdResponse response = ussdService.processSessionStep(session, "0");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Welcome to Inbound Travel Medical Insurance");
        assertThat(session.getCurrentStep()).isEqualTo("MAIN_MENU");
    }

    @Test
    void selectHospitalResultOutOfRangeRePrompts() {
        UssdSession session = createSession("SELECT_HOSPITAL_RESULT");
        session.getCollectedData().put("facility_count", "2");
        session.getCollectedData().put("facility_name_1", "Avenue Hospital");
        session.getCollectedData().put("facility_name_2", "Nairobi Hospital");

        UssdResponse response = ussdService.processSessionStep(session, "5");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Invalid choice");
        assertThat(response.getText()).contains("Select Facility:");
        assertThat(response.getText()).contains("1. Avenue Hospital");
        assertThat(response.getText()).contains("2. Nairobi Hospital");
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
