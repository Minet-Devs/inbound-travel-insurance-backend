package com.travel.insurance.ussd.service;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
            "1. Check Hospital by Name";

    private static final String PROMPT_FACILITY_NAME =
            "Enter facility name to search:";

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EmailService emailService;
    private final MailProperties mailProperties;
    private final ServiceProviderService serviceProviderService;
    private final String defaultSchemeName;
    private final String feedbackRecipient;

    public UssdServiceImpl(EmailService emailService,
                           MailProperties mailProperties,
                           ServiceProviderService serviceProviderService,
                           @Value("${ussd.feedback.default-scheme-name:Inbound Travel Medical Insurance}") String defaultSchemeName,
                           @Value("${ussd.feedback.email.to:inbound.travel@minet.co.ke}") String feedbackRecipient) {
        this.emailService = emailService;
        this.mailProperties = mailProperties;
        this.serviceProviderService = serviceProviderService;
        this.defaultSchemeName = defaultSchemeName;
        this.feedbackRecipient = feedbackRecipient;
    }

    @Override
    public UssdResponse processSessionStep(UssdSession session, String rawInput) {
        String step = session.getCurrentStep();

        switch (step) {
            case "INIT":
                return handleInit(session, rawInput);

            case "MAIN_MENU":
                return handleMainMenuDynamic(session, rawInput);

            case "HOSPITAL_SUB_MENU":
                return handleHospitalSubMenu(session, rawInput);

            case "PROMPT_HOSPITAL_NAME":
                return handlePromptHospitalName(session, rawInput);

            case "NO_HOSPITAL_MATCH":
                return handleNoHospitalMatch(session, rawInput);

            case "SELECT_HOSPITAL_RESULT":
                return handleSelectHospitalResult(session, rawInput);

            case "FEEDBACK_MESSAGE":
                return handleFeedbackMessage(session, rawInput);

            default:
                log.warn("Unknown step: {} for session {}", step, session.getSessionId());
                session.setCurrentStep("MAIN_MENU");
                Map<String, String> menuMap = buildMenuMap();
                session.setMenuMap(menuMap);
                return new UssdResponse(MSG_INVALID_CHOICE + buildMenuText(), "CON");
        }
    }

    private UssdResponse handleInit(UssdSession session, String rawInput) {
        String msisdn = session.getMsisdn();
        String schemeName = defaultSchemeName;

        if (msisdn == null || msisdn.isBlank()) {
            log.warn("MSISDN is null/blank in session {} -- showing default menu", session.getSessionId());
            Map<String, String> defaultMenu = buildMenuMap();
            session.setMenuMap(defaultMenu);
            session.getCollectedData().put("schemeName", schemeName);
            session.setCurrentStep("MAIN_MENU");
            return new UssdResponse(buildMenuText(), "CON");
        }

        try {
            session.getCollectedData().put("schemeName", schemeName);

            Map<String, String> menuMap = buildMenuMap();
            session.setMenuMap(menuMap);
            session.setCurrentStep("MAIN_MENU");

            if (rawInput == null || rawInput.isEmpty()) {
                return new UssdResponse(buildMenuText(), "CON");
            }

            if (!menuMap.containsKey(rawInput.trim())) {
                return new UssdResponse(buildMenuText(), "CON");
            }

            return routeMenuChoice(session, rawInput.trim());

        } catch (Exception e) {
            log.error("Fatal system error during USSD INIT sequence for msisdn {}: ", msisdn, e);
            session.setCurrentStep("MAIN_MENU");
            Map<String, String> fallbackMenuMap = buildMenuMap();
            session.setMenuMap(fallbackMenuMap);
            return new UssdResponse(buildMenuText(), "CON");
        }
    }

    private UssdResponse handleMainMenuDynamic(UssdSession session, String rawInput) {
        String choice = rawInput.trim();
        return routeMenuChoice(session, choice);
    }

    private UssdResponse routeMenuChoice(UssdSession session, String choice) {
        Map<String, String> menuMap = session.getMenuMap();
        String handlerName = menuMap != null ? menuMap.get(choice) : null;

        if (handlerName == null) {
            return new UssdResponse(MSG_INVALID_CHOICE + buildMenuText(), "CON");
        }

        switch (handlerName) {
            case "FIND_HOSPITAL":
                session.setCurrentStep("HOSPITAL_SUB_MENU");
                return new UssdResponse(PROMPT_HOSPITAL_SUB_MENU, "CON");

            case "FEEDBACK":
                session.setCurrentStep("FEEDBACK_MESSAGE");
                return new UssdResponse(PROMPT_FEEDBACK_MESSAGE, "CON");

            default:
                return new UssdResponse(MSG_INVALID_CHOICE + buildMenuText(), "CON");
        }
    }

    private UssdResponse handleHospitalSubMenu(UssdSession session, String rawInput) {
        String choice = rawInput != null ? rawInput.trim() : "";
        if ("1".equals(choice)) {
            session.setCurrentStep("PROMPT_HOSPITAL_NAME");
            return new UssdResponse(PROMPT_FACILITY_NAME, "CON");
        } else if ("0".equals(choice)) {
            session.setCurrentStep("MAIN_MENU");
            session.setMenuMap(buildMenuMap());
            return new UssdResponse(buildMenuText(), "CON");
        } else {
            return new UssdResponse(MSG_INVALID_CHOICE + PROMPT_HOSPITAL_SUB_MENU, "CON");
        }
    }

    private UssdResponse handlePromptHospitalName(UssdSession session, String rawInput) {
        String searchTerm = rawInput != null ? rawInput.trim() : "";
        if (searchTerm.isEmpty()) {
            return new UssdResponse("Facility name cannot be empty.\n" + PROMPT_FACILITY_NAME, "CON");
        }

        List<ServiceProviderResponse> matches = serviceProviderService.searchByName(searchTerm, 5);

        if (matches.isEmpty()) {
            session.getCollectedData().put("lastSearchQuery", searchTerm);
            session.setCurrentStep("NO_HOSPITAL_MATCH");
            return new UssdResponse("No facility found matching '" + searchTerm + "'.\n1. Try again\n0. Main Menu", "CON");
        }

        session.getCollectedData().put("facility_count", String.valueOf(matches.size()));
        for (int i = 0; i < matches.size(); i++) {
            int index = i + 1;
            ServiceProviderResponse p = matches.get(i);
            session.getCollectedData().put("facility_id_" + index, p.id().toString());
            session.getCollectedData().put("facility_name_" + index, p.name() != null ? p.name() : "");
            session.getCollectedData().put("facility_addr_" + index, p.address() != null ? p.address() : "");
            session.getCollectedData().put("facility_phone_" + index, p.contactPhone() != null ? p.contactPhone() : "");
        }

        session.setCurrentStep("SELECT_HOSPITAL_RESULT");
        return new UssdResponse(buildFacilityListText(session), "CON");
    }

    private UssdResponse handleNoHospitalMatch(UssdSession session, String rawInput) {
        String choice = rawInput != null ? rawInput.trim() : "";
        if ("1".equals(choice)) {
            session.setCurrentStep("PROMPT_HOSPITAL_NAME");
            return new UssdResponse(PROMPT_FACILITY_NAME, "CON");
        } else if ("0".equals(choice)) {
            session.setCurrentStep("MAIN_MENU");
            session.setMenuMap(buildMenuMap());
            return new UssdResponse(buildMenuText(), "CON");
        } else {
            String lastQuery = session.getCollectedData().getOrDefault("lastSearchQuery", "");
            return new UssdResponse(MSG_INVALID_CHOICE + "No facility found matching '" + lastQuery + "'.\n1. Try again\n0. Main Menu", "CON");
        }
    }

    private UssdResponse handleSelectHospitalResult(UssdSession session, String rawInput) {
        String choice = rawInput != null ? rawInput.trim() : "";
        if ("0".equals(choice)) {
            session.setCurrentStep("MAIN_MENU");
            session.setMenuMap(buildMenuMap());
            return new UssdResponse(buildMenuText(), "CON");
        }

        int count = 0;
        try {
            count = Integer.parseInt(session.getCollectedData().getOrDefault("facility_count", "0"));
        } catch (NumberFormatException ignored) {
        }

        int selectedIndex = -1;
        try {
            selectedIndex = Integer.parseInt(choice);
        } catch (NumberFormatException ignored) {
        }

        if (selectedIndex >= 1 && selectedIndex <= count) {
            String name = session.getCollectedData().get("facility_name_" + selectedIndex);
            String addr = session.getCollectedData().get("facility_addr_" + selectedIndex);
            String phone = session.getCollectedData().get("facility_phone_" + selectedIndex);

            StringBuilder sb = new StringBuilder();
            sb.append(name != null ? name : "Facility");
            if (addr != null && !addr.isBlank()) {
                String compactAddr = addr.length() > 60 ? addr.substring(0, 57) + "..." : addr;
                sb.append("\nLoc: ").append(compactAddr);
            }
            if (phone != null && !phone.isBlank()) {
                sb.append("\nTel: ").append(phone);
            }

            return new UssdResponse(sb.toString().trim(), "END");
        } else {
            return new UssdResponse(MSG_INVALID_CHOICE + buildFacilityListText(session), "CON");
        }
    }

    private String buildFacilityListText(UssdSession session) {
        int count = 0;
        try {
            count = Integer.parseInt(session.getCollectedData().getOrDefault("facility_count", "0"));
        } catch (NumberFormatException ignored) {
        }

        StringBuilder sb = new StringBuilder("Select Facility:\n");
        for (int i = 1; i <= count; i++) {
            String name = session.getCollectedData().get("facility_name_" + i);
            if (name != null) {
                String displayName = name.length() > 30 ? name.substring(0, 27) + "..." : name;
                sb.append(i).append(". ").append(displayName);
                if (i < count) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    private Map<String, String> buildMenuMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("1", "FIND_HOSPITAL");
        map.put("2", "FEEDBACK");
        return map;
    }

    private String buildMenuText() {
        return String.format("Welcome to %s.\n1. Find Hospital\n2. Feedback", defaultSchemeName);
    }

    private UssdResponse handleFeedbackMessage(UssdSession session, String rawInput) {
        String message = rawInput.trim();

        if (message.isEmpty()) {
            return new UssdResponse("Feedback cannot be empty.\n" + PROMPT_FEEDBACK_MESSAGE, "CON");
        }

        String msisdn = session.getMsisdn();

        try {
            String subject = "Inbound Travel Medical Insurance - Feedback";
            String body = "<p>Feedback received via USSD:</p>"
                    + "<ul>"
                    + "<li><strong>MSISDN:</strong> " + (msisdn != null ? msisdn : "N/A") + "</li>"
                    + "<li><strong>Message:</strong> " + message + "</li>"
                    + "<li><strong>Timestamp:</strong> " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "</li>"
                    + "</ul>";

            emailService.send(mailProperties.getFrom(), feedbackRecipient, subject, body);
        } catch (Exception e) {
            log.error("Failed to send feedback email for session {}: {}", session.getSessionId(), e.getMessage(), e);
        }

        session.setCurrentStep("MAIN_MENU");
        Map<String, String> menuMap = buildMenuMap();
        session.setMenuMap(menuMap);
        return new UssdResponse("Feedback submitted. Thank you!", "END");
    }

}
