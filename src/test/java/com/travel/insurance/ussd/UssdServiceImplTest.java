package com.travel.insurance.ussd;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.config.UssdProperties;
import com.travel.insurance.ussd.domain.ProviderPanelEntry;
import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdResponse;
import com.travel.insurance.ussd.service.ProviderPanelService;
import com.travel.insurance.ussd.service.UssdServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
    private ProviderPanelService providerPanelService;

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
        ussdService = new UssdServiceImpl(emailService, mailProperties, ussdProperties, providerPanelService);
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
        assertThat(response.getText()).contains("Welcome to Inbound Travel Health Insurance");
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
    void countySearchReturnsResults() {
        UssdSession session = createSession("PROMPT_COUNTY_NAME");
        List<ProviderPanelEntry> results = List.of(
                new ProviderPanelEntry("KAREN", "KAREN", "NAIROBI", "Karen Hospital", "Karen Road", "Outpatient, Inpatient"),
                new ProviderPanelEntry("WESTLANDS", "WESTLANDS", "NAIROBI", "Nairobi Hospital", "Westlands", "Inpatient, Pharmacy")
        );
        when(providerPanelService.searchByCounty("Nairobi")).thenReturn(results);

        UssdResponse response = ussdService.processSessionStep(session, "Nairobi");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Providers (1-2 of 2)");
        assertThat(response.getText()).contains("Karen Hospital");
        assertThat(response.getText()).contains("Nairobi Hospital");
        assertThat(session.getCurrentStep()).isEqualTo("COUNTY_RESULTS");
        assertThat(session.getCollectedData().get("countyQuery")).isEqualTo("Nairobi");
    }

    @Test
    void countySearchNoResults() {
        UssdSession session = createSession("PROMPT_COUNTY_NAME");
        when(providerPanelService.searchByCounty("Invalid")).thenReturn(Collections.emptyList());

        UssdResponse response = ussdService.processSessionStep(session, "Invalid");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("No providers found for 'Invalid'");
        assertThat(session.getCurrentStep()).isEqualTo("HOSPITAL_SUB_MENU");
    }

    @Test
    void townPromptEmptyRePrompts() {
        UssdSession session = createSession("PROMPT_TOWN_NAME");

        UssdResponse response = ussdService.processSessionStep(session, "   ");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Town name cannot be empty");
        assertThat(session.getCurrentStep()).isEqualTo("PROMPT_TOWN_NAME");
    }

    @Test
    void townSearchReturnsResults() {
        UssdSession session = createSession("PROMPT_TOWN_NAME");
        List<ProviderPanelEntry> results = List.of(
                new ProviderPanelEntry("KAREN", "KAREN", "NAIROBI", "Karen Hospital", "Karen Road", "Outpatient, Inpatient")
        );
        when(providerPanelService.searchByTown("Karen")).thenReturn(results);

        UssdResponse response = ussdService.processSessionStep(session, "Karen");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Providers (1-1 of 1)");
        assertThat(response.getText()).contains("Karen Hospital");
        assertThat(session.getCurrentStep()).isEqualTo("TOWN_RESULTS");
        assertThat(session.getCollectedData().get("townQuery")).isEqualTo("Karen");
    }

    @Test
    void townSearchNoResults() {
        UssdSession session = createSession("PROMPT_TOWN_NAME");
        when(providerPanelService.searchByTown("Nowhere")).thenReturn(Collections.emptyList());

        UssdResponse response = ussdService.processSessionStep(session, "Nowhere");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("No providers found for 'Nowhere'");
        assertThat(session.getCurrentStep()).isEqualTo("HOSPITAL_SUB_MENU");
    }

    @Test
    void countyResultsBackToSubMenu() {
        UssdSession session = createSession("COUNTY_RESULTS");
        session.getCollectedData().put("countyQuery", "Nairobi");
        session.getCollectedData().put("countyResultPage", "0");

        UssdResponse response = ussdService.processSessionStep(session, "0");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("1. County");
        assertThat(session.getCurrentStep()).isEqualTo("HOSPITAL_SUB_MENU");
    }

    @Test
    void countyResultsNextPage() {
        UssdSession session = createSession("COUNTY_RESULTS");
        session.getCollectedData().put("countyQuery", "Nairobi");
        session.getCollectedData().put("countyResultPage", "0");

        List<ProviderPanelEntry> results = List.of(
                new ProviderPanelEntry("A", "A", "NAIROBI", "P1", "", ""),
                new ProviderPanelEntry("B", "B", "NAIROBI", "P2", "", ""),
                new ProviderPanelEntry("C", "C", "NAIROBI", "P3", "", ""),
                new ProviderPanelEntry("D", "D", "NAIROBI", "P4", "", ""),
                new ProviderPanelEntry("E", "E", "NAIROBI", "P5", "", ""),
                new ProviderPanelEntry("F", "F", "NAIROBI", "P6", "", ""),
                new ProviderPanelEntry("G", "G", "NAIROBI", "P7", "", "")
        );
        when(providerPanelService.searchByCounty("Nairobi")).thenReturn(results);

        UssdResponse response = ussdService.processSessionStep(session, "9");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Providers (6-7 of 7)");
        assertThat(response.getText()).contains("P6");
        assertThat(response.getText()).contains("P7");
        assertThat(session.getCollectedData().get("countyResultPage")).isEqualTo("1");
    }

    @Test
    void townResultsBackToSubMenu() {
        UssdSession session = createSession("TOWN_RESULTS");
        session.getCollectedData().put("townQuery", "Karen");
        session.getCollectedData().put("townResultPage", "0");

        UssdResponse response = ussdService.processSessionStep(session, "0");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("1. County");
        assertThat(session.getCurrentStep()).isEqualTo("HOSPITAL_SUB_MENU");
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

    @Test
    void countyResultSelectShowsProviderDetail() {
        UssdSession session = createSession("COUNTY_RESULTS");
        session.getCollectedData().put("countyQuery", "Nairobi");
        session.getCollectedData().put("countyResultPage", "0");

        List<ProviderPanelEntry> results = List.of(
                new ProviderPanelEntry("KAREN", "KAREN", "NAIROBI", "Karen Hospital", "Karen Road", "Outpatient, Inpatient"),
                new ProviderPanelEntry("WESTLANDS", "WESTLANDS", "NAIROBI", "Nairobi Hospital", "Westlands", "Inpatient, Pharmacy")
        );
        when(providerPanelService.searchByCounty("Nairobi")).thenReturn(results);

        UssdResponse response = ussdService.processSessionStep(session, "1");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Karen Hospital");
        assertThat(response.getText()).contains("Address: Karen Road");
        assertThat(response.getText()).contains("Services: Outpatient, Inpatient");
        assertThat(response.getText()).contains("0. Back to results");
        assertThat(session.getCurrentStep()).isEqualTo("PROVIDER_DETAIL");
    }

    @Test
    void providerDetailBackReturnsToResults() {
        UssdSession session = createSession("PROVIDER_DETAIL");
        session.getCollectedData().put("detailReturnStep", "COUNTY_RESULTS");
        session.getCollectedData().put("countyQuery", "Nairobi");
        session.getCollectedData().put("countyResultPage", "0");

        List<ProviderPanelEntry> results = List.of(
                new ProviderPanelEntry("KAREN", "KAREN", "NAIROBI", "Karen Hospital", "Karen Road", "Outpatient"),
                new ProviderPanelEntry("WESTLANDS", "WESTLANDS", "NAIROBI", "Nairobi Hospital", "Westlands", "Inpatient")
        );
        when(providerPanelService.searchByCounty("Nairobi")).thenReturn(results);

        UssdResponse response = ussdService.processSessionStep(session, "0");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Providers (1-2 of 2)");
        assertThat(session.getCurrentStep()).isEqualTo("COUNTY_RESULTS");
    }

    @Test
    void townResultSelectShowsProviderDetail() {
        UssdSession session = createSession("TOWN_RESULTS");
        session.getCollectedData().put("townQuery", "Karen");
        session.getCollectedData().put("townResultPage", "0");

        List<ProviderPanelEntry> results = List.of(
                new ProviderPanelEntry("KAREN", "KAREN", "NAIROBI", "Karen Hospital", "Karen Road", "Outpatient")
        );
        when(providerPanelService.searchByTown("Karen")).thenReturn(results);

        UssdResponse response = ussdService.processSessionStep(session, "1");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Karen Hospital");
        assertThat(response.getText()).contains("Address: Karen Road");
        assertThat(session.getCurrentStep()).isEqualTo("PROVIDER_DETAIL");
    }

    @Test
    void providerDetailInvalidInputRePrompts() {
        UssdSession session = createSession("PROVIDER_DETAIL");
        session.getCollectedData().put("detailReturnStep", "COUNTY_RESULTS");

        UssdResponse response = ussdService.processSessionStep(session, "abc");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("0. Back to results");
        assertThat(session.getCurrentStep()).isEqualTo("PROVIDER_DETAIL");
    }
}
