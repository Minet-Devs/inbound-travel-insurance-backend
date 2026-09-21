package com.travel.insurance.ussd.service;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.config.UssdProperties;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.touristattraction.TouristAttractionService;
import com.travel.insurance.touristattraction.dto.TouristAttractionResponse;
import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class UssdServiceImpl implements UssdService {

    private static final String MSG_INVALID_CHOICE =
            "Invalid choice. Please try again.\n";

    private static final String PROMPT_FEEDBACK_MESSAGE =
            "Provide your feedback:";

    private static final String PROMPT_HOSPITAL_SUB_MENU =
            "Select search area:\n"
                    + "1. County\n"
                    + "2. Town (Coming Soon)\n"
                    + "3. Border Point (Coming Soon)\n"
                    + "4. Nearest Tourist Attraction\n"
                    + "0. Main Menu";

    private static final String PROMPT_COUNTY_NAME =
            "Enter county name to search:";

    private static final String PROMPT_ATTRACTION_NAME =
            "Enter tourist attraction name:";

    private static final String MSG_COMING_SOON =
            "This option is coming soon.\n";

    private static final int USSD_MAX_RESULTS = 3;

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EmailService emailService;
    private final MailProperties mailProperties;
    private final UssdProperties ussdProperties;
    private final ServiceProviderService serviceProviderService;
    private final TouristAttractionService touristAttractionService;

    public UssdServiceImpl(EmailService emailService,
                           MailProperties mailProperties,
                           UssdProperties ussdProperties,
                           ServiceProviderService serviceProviderService,
                           TouristAttractionService touristAttractionService) {
        this.emailService = emailService;
        this.mailProperties = mailProperties;
        this.ussdProperties = ussdProperties;
        this.serviceProviderService = serviceProviderService;
        this.touristAttractionService = touristAttractionService;
    }

    @Override
    public UssdResponse processSessionStep(UssdSession session, String rawInput) {
        String step = session.getCurrentStep();

        return switch (step) {
            case "INIT" -> handleInit(session, rawInput);
            case "MAIN_MENU" -> handleMainMenuDynamic(session, rawInput);
            case "HOSPITAL_SUB_MENU" -> handleHospitalSubMenu(session, rawInput);
            case "PROMPT_COUNTY_NAME" -> handlePromptCountyName(session, rawInput);
            case "PROMPT_ATTRACTION_NAME" -> handlePromptAttractionName(session, rawInput);
            case "ATTRACTION_CHOICES" -> handleAttractionChoices(session, rawInput);
            case "COUNTY_RESULTS" -> handleCountyResults(session, rawInput);
            case "PROVIDER_DETAIL" -> handleProviderDetail(session, rawInput);
            case "FEEDBACK_MESSAGE" -> handleFeedbackMessage(session, rawInput);
            default -> {
                log.warn("Unknown step: {} for session {}", step, session.getSessionId());
                session.setCurrentStep("MAIN_MENU");
                session.setMenuMap(buildMenuMap());
                yield new UssdResponse(MSG_INVALID_CHOICE + buildMenuText(), "CON");
            }
        };
    }

    private UssdResponse handleInit(UssdSession session, String rawInput) {
        String msisdn = session.getMsisdn();
        String schemeName = getDefaultSchemeName();

        session.getCollectedData().put("schemeName", schemeName);
        session.setMenuMap(buildMenuMap());
        session.setCurrentStep("MAIN_MENU");

        if (msisdn == null || msisdn.isBlank()) {
            log.warn("MSISDN is null/blank in session {} -- showing default menu", session.getSessionId());
            return new UssdResponse(buildMenuText(), "CON");
        }

        if (rawInput == null || rawInput.isBlank()) {
            return new UssdResponse(buildMenuText(), "CON");
        }

        if (!session.getMenuMap().containsKey(rawInput.trim())) {
            return new UssdResponse(buildMenuText(), "CON");
        }

        return routeMenuChoice(session, rawInput.trim());
    }

    private UssdResponse handleMainMenuDynamic(UssdSession session, String rawInput) {
        String choice = rawInput != null ? rawInput.trim() : "";
        return routeMenuChoice(session, choice);
    }

    private UssdResponse routeMenuChoice(UssdSession session, String choice) {
        Map<String, String> menuMap = session.getMenuMap();
        String handlerName = menuMap != null ? menuMap.get(choice) : null;

        if (handlerName == null) {
            return new UssdResponse(MSG_INVALID_CHOICE + buildMenuText(), "CON");
        }

        return switch (handlerName) {
            case "FIND_HOSPITAL" -> {
                session.setCurrentStep("HOSPITAL_SUB_MENU");
                yield new UssdResponse(PROMPT_HOSPITAL_SUB_MENU, "CON");
            }
            case "FEEDBACK" -> {
                session.setCurrentStep("FEEDBACK_MESSAGE");
                yield new UssdResponse(PROMPT_FEEDBACK_MESSAGE, "CON");
            }
            default -> new UssdResponse(MSG_INVALID_CHOICE + buildMenuText(), "CON");
        };
    }

    private UssdResponse handleHospitalSubMenu(UssdSession session, String rawInput) {
        String choice = rawInput != null ? rawInput.trim() : "";

        return switch (choice) {
            case "1" -> {
                session.setCurrentStep("PROMPT_COUNTY_NAME");
                yield new UssdResponse(PROMPT_COUNTY_NAME, "CON");
            }
            case "4" -> {
                session.setCurrentStep("PROMPT_ATTRACTION_NAME");
                yield new UssdResponse(PROMPT_ATTRACTION_NAME, "CON");
            }
            case "2", "3" -> {
                session.setCurrentStep("MAIN_MENU");
                session.setMenuMap(buildMenuMap());
                yield new UssdResponse(MSG_COMING_SOON + buildMenuText(), "CON");
            }
            case "0" -> {
                session.setCurrentStep("MAIN_MENU");
                session.setMenuMap(buildMenuMap());
                yield new UssdResponse(buildMenuText(), "CON");
            }
            default -> new UssdResponse(MSG_INVALID_CHOICE + PROMPT_HOSPITAL_SUB_MENU, "CON");
        };
    }

    private UssdResponse handlePromptCountyName(UssdSession session, String rawInput) {
        String countyName = rawInput != null ? rawInput.trim() : "";
        if (countyName.isEmpty()) {
            return new UssdResponse("County name cannot be empty.\n" + PROMPT_COUNTY_NAME, "CON");
        }

        List<ServiceProviderResponse> results = serviceProviderService.searchByCounty(countyName);

        if (results.isEmpty()) {
            session.setCurrentStep("HOSPITAL_SUB_MENU");
            return new UssdResponse("No providers found for '" + countyName + "'.\n" + PROMPT_HOSPITAL_SUB_MENU, "CON");
        }

        session.getCollectedData().put("countyQuery", countyName);
        session.getCollectedData().put("countyResultPage", "0");
        return formatCountyResults(session, results, 0);
    }

    private UssdResponse handlePromptAttractionName(UssdSession session, String rawInput) {
        String attractionName = rawInput != null ? rawInput.trim() : "";
        if (attractionName.isEmpty()) {
            return new UssdResponse("Attraction name cannot be empty.\n" + PROMPT_ATTRACTION_NAME, "CON");
        }

        List<TouristAttractionResponse> matches =
                touristAttractionService.searchByName(attractionName, USSD_MAX_RESULTS);

        if (matches.isEmpty()) {
            session.setCurrentStep("HOSPITAL_SUB_MENU");
            return new UssdResponse("No tourist attraction found for '" + attractionName + "'.\n"
                    + PROMPT_HOSPITAL_SUB_MENU, "CON");
        }

        if (matches.size() == 1) {
            return showHospitalsNearAttraction(session, matches.get(0));
        }

        session.getCollectedData().put("attractionQuery", attractionName);
        return formatAttractionChoices(session, matches);
    }

    private UssdResponse handleAttractionChoices(UssdSession session, String rawInput) {
        String choice = rawInput != null ? rawInput.trim() : "";

        if ("0".equals(choice)) {
            session.setCurrentStep("HOSPITAL_SUB_MENU");
            return new UssdResponse(PROMPT_HOSPITAL_SUB_MENU, "CON");
        }

        String query = session.getCollectedData().getOrDefault("attractionQuery", "");
        List<TouristAttractionResponse> matches = touristAttractionService.searchByName(query, USSD_MAX_RESULTS);

        try {
            int selected = Integer.parseInt(choice);
            if (selected >= 1 && selected <= matches.size()) {
                return showHospitalsNearAttraction(session, matches.get(selected - 1));
            }
        } catch (NumberFormatException ignored) {
        }

        UssdResponse choices = formatAttractionChoices(session, matches);
        return new UssdResponse(MSG_INVALID_CHOICE + choices.getText(), "CON");
    }

    private UssdResponse formatAttractionChoices(UssdSession session, List<TouristAttractionResponse> matches) {
        session.setCurrentStep("ATTRACTION_CHOICES");

        StringBuilder sb = new StringBuilder("Select attraction:\n");
        for (int i = 0; i < matches.size(); i++) {
            sb.append(i + 1).append(". ").append(truncate(matches.get(i).name(), 30)).append("\n");
        }
        sb.append("0.Back");

        return new UssdResponse(sb.toString(), "CON");
    }

    /**
     * Lists the hospitals in the attraction's county. Hands off to the county results flow
     * (COUNTY_RESULTS / PROVIDER_DETAIL) by seeding its session state, so paging and
     * "back" behave exactly as they do for a plain county search.
     */
    private UssdResponse showHospitalsNearAttraction(UssdSession session, TouristAttractionResponse attraction) {
        List<ServiceProviderResponse> results = serviceProviderService.searchByCounty(attraction.county());

        if (results.isEmpty()) {
            session.setCurrentStep("HOSPITAL_SUB_MENU");
            return new UssdResponse("No providers found near " + attraction.name()
                    + " (" + attraction.county() + ").\n" + PROMPT_HOSPITAL_SUB_MENU, "CON");
        }

        session.getCollectedData().put("countyQuery", attraction.county());
        UssdResponse page = formatCountyResults(session, results, 0);
        return new UssdResponse(truncate(attraction.name(), 30) + " - " + attraction.county() + "\n"
                + page.getText(), "CON");
    }

    private UssdResponse handleCountyResults(UssdSession session, String rawInput) {
        String choice = rawInput != null ? rawInput.trim() : "";

        if ("0".equals(choice)) {
            session.setCurrentStep("HOSPITAL_SUB_MENU");
            return new UssdResponse(PROMPT_HOSPITAL_SUB_MENU, "CON");
        }

        String query = session.getCollectedData().getOrDefault("countyQuery", "");
        int page = Integer.parseInt(session.getCollectedData().getOrDefault("countyResultPage", "0"));
        List<ServiceProviderResponse> results = serviceProviderService.searchByCounty(query);

        if ("9".equals(choice)) {
            int nextPage = page + 1;
            int maxPage = (results.size() - 1) / USSD_MAX_RESULTS;
            if (nextPage > maxPage) {
                nextPage = 0;
            }
            return formatCountyResults(session, results, nextPage);
        }

        try {
            int selected = Integer.parseInt(choice);
            int index = (page * USSD_MAX_RESULTS) + (selected - 1);
            if (index >= 0 && index < results.size()) {
                return showProviderDetail(session, results.get(index));
            }
        } catch (NumberFormatException ignored) {
        }

        return new UssdResponse("Enter 1-5 to view details, 9 for Next, or 0 for Menu:\n" + PROMPT_HOSPITAL_SUB_MENU, "CON");
    }

    private UssdResponse showProviderDetail(UssdSession session, ServiceProviderResponse provider) {
        session.setCurrentStep("PROVIDER_DETAIL");

        StringBuilder sb = new StringBuilder();
        sb.append("--- ").append(provider.name()).append(" ---\n");
        if (provider.address() != null && !provider.address().isBlank()) {
            sb.append("Address: ").append(provider.address()).append("\n");
        }
        if (provider.contactPhone() != null && !provider.contactPhone().isBlank()) {
            sb.append("Phone: ").append(provider.contactPhone()).append("\n");
        }
        if (provider.county() != null && !provider.county().isBlank()) {
            sb.append("County: ").append(provider.county()).append("\n");
        }
        sb.append("0. Back to results");

        return new UssdResponse(sb.toString(), "CON");
    }

    private UssdResponse handleProviderDetail(UssdSession session, String rawInput) {
        String choice = rawInput != null ? rawInput.trim() : "";

        if ("0".equals(choice)) {
            String query = session.getCollectedData().getOrDefault("countyQuery", "");
            int page = Integer.parseInt(session.getCollectedData().getOrDefault("countyResultPage", "0"));
            List<ServiceProviderResponse> results = serviceProviderService.searchByCounty(query);
            return formatCountyResults(session, results, page);
        }

        return new UssdResponse("0. Back to results", "CON");
    }

    private UssdResponse formatCountyResults(UssdSession session, List<ServiceProviderResponse> results, int page) {
        session.setCurrentStep("COUNTY_RESULTS");
        session.getCollectedData().put("countyResultPage", String.valueOf(page));

        int start = page * USSD_MAX_RESULTS;
        int end = Math.min(start + USSD_MAX_RESULTS, results.size());

        StringBuilder sb = new StringBuilder();
        sb.append(start + 1).append("-").append(end)
                .append("/").append(results.size()).append(":\n");

        for (int i = start; i < end; i++) {
            sb.append(i + 1).append(". ").append(truncate(results.get(i).name(), 30)).append("\n");
        }

        if (end < results.size()) {
            sb.append("9.Next");
        }
        sb.append(" 0.Back");

        return new UssdResponse(sb.toString(), "CON");
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "~";
    }

    private Map<String, String> buildMenuMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("1", "FIND_HOSPITAL");
        map.put("2", "FEEDBACK");
        return map;
    }

    private String buildMenuText() {
        return String.format("Welcome to %s.\n1. Find Hospital\n2. Feedback", getDefaultSchemeName());
    }

    private UssdResponse handleFeedbackMessage(UssdSession session, String rawInput) {
        String message = rawInput != null ? rawInput.trim() : "";

        if (message.isEmpty()) {
            return new UssdResponse("Feedback cannot be empty.\n" + PROMPT_FEEDBACK_MESSAGE, "CON");
        }

        String msisdn = session.getMsisdn();

        try {
            String subject = "Inbound Travel Health Insurance - Feedback";
            String body = "<p>Feedback received via USSD:</p>"
                    + "<ul>"
                    + "<li><strong>MSISDN:</strong> " + (msisdn != null ? msisdn : "N/A") + "</li>"
                    + "<li><strong>Message:</strong> " + message + "</li>"
                    + "<li><strong>Timestamp:</strong> " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "</li>"
                    + "</ul>";

            emailService.send(mailProperties.getFrom(), getFeedbackRecipient(), subject, body);
        } catch (Exception e) {
            log.error("Failed to send feedback email for session {}: {}", session.getSessionId(), e.getMessage(), e);
        }

        session.setCurrentStep("MAIN_MENU");
        session.setMenuMap(buildMenuMap());
        return new UssdResponse("Feedback submitted. Thank you!", "END");
    }

    private String getDefaultSchemeName() {
        if (ussdProperties != null && ussdProperties.getFeedback() != null
                && ussdProperties.getFeedback().getDefaultSchemeName() != null) {
            return ussdProperties.getFeedback().getDefaultSchemeName();
        }
        return "Inbound Travel Health Insurance";
    }

    private String getFeedbackRecipient() {
        if (ussdProperties != null && ussdProperties.getFeedback() != null
                && ussdProperties.getFeedback().getEmail() != null
                && ussdProperties.getFeedback().getEmail().getTo() != null) {
            return ussdProperties.getFeedback().getEmail().getTo();
        }
        return "inbound.travel@minet.co.ke";
    }
}
