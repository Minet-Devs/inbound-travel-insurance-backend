package com.travel.insurance.ussd;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.config.UssdProperties;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.touristattraction.TouristAttractionService;
import com.travel.insurance.touristattraction.dto.TouristAttractionResponse;
import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdResponse;
import com.travel.insurance.ussd.service.UssdServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.time.Instant;
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

    @Mock
    private TouristAttractionService touristAttractionService;

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
        ussdService = new UssdServiceImpl(emailService, mailProperties, ussdProperties, serviceProviderService,
                touristAttractionService);
    }

    private ServiceProviderResponse provider(String name, String address, String phone, String county) {
        return new ServiceProviderResponse(UUID.randomUUID(), name, name.toLowerCase().replace(" ", "") + "@example.com",
                phone, address, county, null, Instant.now(), Instant.now());
    }

    private TouristAttractionResponse attraction(String name, String county) {
        return new TouristAttractionResponse(UUID.randomUUID(), name, county, Instant.now(), Instant.now());
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
        List<ServiceProviderResponse> results = List.of(
                provider("Karen Hospital", "Karen Road", "+254711000001", "Nairobi"),
                provider("Nairobi Hospital", "Westlands", "+254711000002", "Nairobi")
        );
        when(serviceProviderService.searchByCounty("Nairobi")).thenReturn(results);

        UssdResponse response = ussdService.processSessionStep(session, "Nairobi");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("1-2/2:");
        assertThat(response.getText()).contains("Karen Hospital");
        assertThat(response.getText()).contains("Nairobi Hospital");
        assertThat(session.getCurrentStep()).isEqualTo("COUNTY_RESULTS");
        assertThat(session.getCollectedData().get("countyQuery")).isEqualTo("Nairobi");
    }

    @Test
    void countySearchNoResults() {
        UssdSession session = createSession("PROMPT_COUNTY_NAME");
        when(serviceProviderService.searchByCounty("Invalid")).thenReturn(Collections.emptyList());

        UssdResponse response = ussdService.processSessionStep(session, "Invalid");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("No providers found for 'Invalid'");
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

        List<ServiceProviderResponse> results = List.of(
                provider("P1", null, null, "Nairobi"),
                provider("P2", null, null, "Nairobi"),
                provider("P3", null, null, "Nairobi"),
                provider("P4", null, null, "Nairobi"),
                provider("P5", null, null, "Nairobi"),
                provider("P6", null, null, "Nairobi"),
                provider("P7", null, null, "Nairobi")
        );
        when(serviceProviderService.searchByCounty("Nairobi")).thenReturn(results);

        UssdResponse response = ussdService.processSessionStep(session, "9");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("4-6/7:");
        assertThat(response.getText()).contains("P4");
        assertThat(response.getText()).contains("P5");
        assertThat(response.getText()).contains("P6");
        assertThat(session.getCollectedData().get("countyResultPage")).isEqualTo("1");
    }

    @Test
    void townPlaceholderReturnsToMainMenu() {
        UssdSession session = createSession("HOSPITAL_SUB_MENU");

        UssdResponse response = ussdService.processSessionStep(session, "2");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("This option is coming soon.");
        assertThat(response.getText()).contains("1. Find Hospital");
        assertThat(session.getCurrentStep()).isEqualTo("MAIN_MENU");
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
    void hospitalSubMenuOffersTouristAttractionSearch() {
        UssdSession session = createSession("HOSPITAL_SUB_MENU");

        UssdResponse response = ussdService.processSessionStep(session, "4");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Enter tourist attraction name:");
        assertThat(session.getCurrentStep()).isEqualTo("PROMPT_ATTRACTION_NAME");
    }

    @Test
    void attractionPromptEmptyRePrompts() {
        UssdSession session = createSession("PROMPT_ATTRACTION_NAME");

        UssdResponse response = ussdService.processSessionStep(session, "  ");

        assertThat(response.getText()).contains("Attraction name cannot be empty");
        assertThat(session.getCurrentStep()).isEqualTo("PROMPT_ATTRACTION_NAME");
    }

    @Test
    void attractionNotFoundReturnsToSubMenu() {
        UssdSession session = createSession("PROMPT_ATTRACTION_NAME");
        when(touristAttractionService.searchByName("Nowhere", 3)).thenReturn(Collections.emptyList());

        UssdResponse response = ussdService.processSessionStep(session, "Nowhere");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("No tourist attraction found for 'Nowhere'");
        assertThat(response.getText()).contains("4. Nearest Tourist Attraction");
        assertThat(session.getCurrentStep()).isEqualTo("HOSPITAL_SUB_MENU");
    }

    @Test
    void singleAttractionMatchListsHospitalsInItsCounty() {
        UssdSession session = createSession("PROMPT_ATTRACTION_NAME");
        when(touristAttractionService.searchByName("nakuru natonal", 3))
                .thenReturn(List.of(attraction("Lake Nakuru National Park", "Nakuru")));
        when(serviceProviderService.searchByCounty("Nakuru")).thenReturn(List.of(
                provider("Nakuru Level 5 Hospital", "Nakuru Town", "+254711000003", "Nakuru"),
                provider("War Memorial Hospital", "Nakuru Town", "+254711000004", "Nakuru")));

        UssdResponse response = ussdService.processSessionStep(session, "nakuru natonal");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Lake Nakuru National Park - Nakuru");
        assertThat(response.getText()).contains("1-2/2:");
        assertThat(response.getText()).contains("Nakuru Level 5 Hospital");
        assertThat(response.getText()).contains("War Memorial Hospital");
        assertThat(session.getCurrentStep()).isEqualTo("COUNTY_RESULTS");
        assertThat(session.getCollectedData().get("countyQuery")).isEqualTo("Nakuru");
        assertThat(session.getCollectedData().get("countyResultPage")).isEqualTo("0");
    }

    @Test
    void attractionWithNoHospitalsInCountyReturnsToSubMenu() {
        UssdSession session = createSession("PROMPT_ATTRACTION_NAME");
        when(touristAttractionService.searchByName("Mara", 3))
                .thenReturn(List.of(attraction("Maasai Mara National Reserve", "Narok")));
        when(serviceProviderService.searchByCounty("Narok")).thenReturn(Collections.emptyList());

        UssdResponse response = ussdService.processSessionStep(session, "Mara");

        assertThat(response.getText()).contains("No providers found near Maasai Mara National Reserve (Narok)");
        assertThat(session.getCurrentStep()).isEqualTo("HOSPITAL_SUB_MENU");
    }

    @Test
    void multipleAttractionMatchesLetTheUserChoose() {
        UssdSession session = createSession("PROMPT_ATTRACTION_NAME");
        when(touristAttractionService.searchByName("park", 3)).thenReturn(List.of(
                attraction("Amboseli National Park", "Kajiado"),
                attraction("Lake Nakuru National Park", "Nakuru")));

        UssdResponse response = ussdService.processSessionStep(session, "park");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Select attraction:");
        assertThat(response.getText()).contains("1. Amboseli National Park");
        assertThat(response.getText()).contains("2. Lake Nakuru National Park");
        assertThat(session.getCurrentStep()).isEqualTo("ATTRACTION_CHOICES");
        assertThat(session.getCollectedData().get("attractionQuery")).isEqualTo("park");
    }

    @Test
    void choosingAnAttractionListsHospitalsInItsCounty() {
        UssdSession session = createSession("ATTRACTION_CHOICES");
        session.getCollectedData().put("attractionQuery", "park");
        when(touristAttractionService.searchByName("park", 3)).thenReturn(List.of(
                attraction("Amboseli National Park", "Kajiado"),
                attraction("Lake Nakuru National Park", "Nakuru")));
        when(serviceProviderService.searchByCounty("Nakuru")).thenReturn(List.of(
                provider("Nakuru Level 5 Hospital", "Nakuru Town", "+254711000003", "Nakuru")));

        UssdResponse response = ussdService.processSessionStep(session, "2");

        assertThat(response.getText()).contains("Lake Nakuru National Park - Nakuru");
        assertThat(response.getText()).contains("Nakuru Level 5 Hospital");
        assertThat(session.getCurrentStep()).isEqualTo("COUNTY_RESULTS");
        assertThat(session.getCollectedData().get("countyQuery")).isEqualTo("Nakuru");
    }

    @Test
    void attractionChoiceOutOfRangeRePrompts() {
        UssdSession session = createSession("ATTRACTION_CHOICES");
        session.getCollectedData().put("attractionQuery", "park");
        when(touristAttractionService.searchByName("park", 3)).thenReturn(List.of(
                attraction("Amboseli National Park", "Kajiado"),
                attraction("Lake Nakuru National Park", "Nakuru")));

        UssdResponse response = ussdService.processSessionStep(session, "7");

        assertThat(response.getText()).contains("Invalid choice");
        assertThat(response.getText()).contains("Select attraction:");
        assertThat(session.getCurrentStep()).isEqualTo("ATTRACTION_CHOICES");
    }

    @Test
    void attractionChoicesBackReturnsToSubMenu() {
        UssdSession session = createSession("ATTRACTION_CHOICES");
        session.getCollectedData().put("attractionQuery", "park");

        UssdResponse response = ussdService.processSessionStep(session, "0");

        assertThat(response.getText()).contains("1. County");
        assertThat(session.getCurrentStep()).isEqualTo("HOSPITAL_SUB_MENU");
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

        List<ServiceProviderResponse> results = List.of(
                provider("Karen Hospital", "Karen Road", "+254711000001", "Nairobi"),
                provider("Nairobi Hospital", "Westlands", "+254711000002", "Nairobi")
        );
        when(serviceProviderService.searchByCounty("Nairobi")).thenReturn(results);

        UssdResponse response = ussdService.processSessionStep(session, "1");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("Karen Hospital");
        assertThat(response.getText()).contains("Address: Karen Road");
        assertThat(response.getText()).contains("Phone: +254711000001");
        assertThat(response.getText()).contains("County: Nairobi");
        assertThat(response.getText()).contains("0. Back to results");
        assertThat(session.getCurrentStep()).isEqualTo("PROVIDER_DETAIL");
    }

    @Test
    void providerDetailBackReturnsToResults() {
        UssdSession session = createSession("PROVIDER_DETAIL");
        session.getCollectedData().put("countyQuery", "Nairobi");
        session.getCollectedData().put("countyResultPage", "0");

        List<ServiceProviderResponse> results = List.of(
                provider("Karen Hospital", "Karen Road", "+254711000001", "Nairobi"),
                provider("Nairobi Hospital", "Westlands", "+254711000002", "Nairobi")
        );
        when(serviceProviderService.searchByCounty("Nairobi")).thenReturn(results);

        UssdResponse response = ussdService.processSessionStep(session, "0");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("1-2/2:");
        assertThat(session.getCurrentStep()).isEqualTo("COUNTY_RESULTS");
    }

    @Test
    void providerDetailInvalidInputRePrompts() {
        UssdSession session = createSession("PROVIDER_DETAIL");

        UssdResponse response = ussdService.processSessionStep(session, "abc");

        assertThat(response.getType()).isEqualTo("CON");
        assertThat(response.getText()).contains("0. Back to results");
        assertThat(session.getCurrentStep()).isEqualTo("PROVIDER_DETAIL");
    }
}
