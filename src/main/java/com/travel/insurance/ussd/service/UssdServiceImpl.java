package com.travel.insurance.ussd.service;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.config.UssdProperties;
import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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
                    + "2. Town\n"
                    + "3. Border Point (Coming Soon)\n"
                    + "4. Nearest Tourist Attraction (Coming Soon)\n"
                    + "0. Main Menu";

    private static final String PROMPT_COUNTY_NAME =
            "Enter county name to search:";

    private static final String PROMPT_TOWN_NAME =
            "Enter town name to search:";

    private static final String MSG_COMING_SOON =
            "This option is coming soon.\n";

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EmailService emailService;
    private final MailProperties mailProperties;
    private final UssdProperties ussdProperties;

    public UssdServiceImpl(EmailService emailService,
                           MailProperties mailProperties,
                           UssdProperties ussdProperties) {
        this.emailService = emailService;
        this.mailProperties = mailProperties;
        this.ussdProperties = ussdProperties;
    }

    @Override
    public UssdResponse processSessionStep(UssdSession session, String rawInput) {
        String step = session.getCurrentStep();

        return switch (step) {
            case "INIT" -> handleInit(session, rawInput);
            case "MAIN_MENU" -> handleMainMenuDynamic(session, rawInput);
            case "HOSPITAL_SUB_MENU" -> handleHospitalSubMenu(session, rawInput);
            case "PROMPT_COUNTY_NAME" -> handlePromptCountyName(session, rawInput);
            case "PROMPT_TOWN_NAME" -> handlePromptTownName(session, rawInput);
            case "COUNTY_PLACEHOLDER" -> handleCountyPlaceholder(session);
            case "TOWN_PLACEHOLDER" -> handleTownPlaceholder(session);
            case "BORDER_POINT_PLACEHOLDER" -> handleBorderPointPlaceholder(session);
            case "TOURIST_ATTRACTION_PLACEHOLDER" -> handleTouristAttractionPlaceholder(session);
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
            case "2" -> {
                session.setCurrentStep("PROMPT_TOWN_NAME");
                yield new UssdResponse(PROMPT_TOWN_NAME, "CON");
            }
            case "3" -> {
                session.setCurrentStep("MAIN_MENU");
                session.setMenuMap(buildMenuMap());
                yield new UssdResponse(MSG_COMING_SOON + buildMenuText(), "CON");
            }
            case "4" -> {
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

        session.getCollectedData().put("pendingLocationType", "COUNTY");
        session.getCollectedData().put("pendingLocationValue", countyName);
        session.setCurrentStep("MAIN_MENU");
        session.setMenuMap(buildMenuMap());
        return new UssdResponse("County search is not live yet for '" + countyName + "'.\n" + buildMenuText(), "CON");
    }

    private UssdResponse handlePromptTownName(UssdSession session, String rawInput) {
        String townName = rawInput != null ? rawInput.trim() : "";
        if (townName.isEmpty()) {
            return new UssdResponse("Town name cannot be empty.\n" + PROMPT_TOWN_NAME, "CON");
        }

        session.getCollectedData().put("pendingLocationType", "TOWN");
        session.getCollectedData().put("pendingLocationValue", townName);
        session.setCurrentStep("MAIN_MENU");
        session.setMenuMap(buildMenuMap());
        return new UssdResponse("Town search is not live yet for '" + townName + "'.\n" + buildMenuText(), "CON");
    }

    private UssdResponse handleCountyPlaceholder(UssdSession session) {
        session.setCurrentStep("MAIN_MENU");
        session.setMenuMap(buildMenuMap());
        return new UssdResponse(buildPlaceholderText("County", session) + buildMenuText(), "CON");
    }

    private UssdResponse handleTownPlaceholder(UssdSession session) {
        session.setCurrentStep("MAIN_MENU");
        session.setMenuMap(buildMenuMap());
        return new UssdResponse(buildPlaceholderText("Town", session) + buildMenuText(), "CON");
    }

    private UssdResponse handleBorderPointPlaceholder(UssdSession session) {
        session.setCurrentStep("MAIN_MENU");
        session.setMenuMap(buildMenuMap());
        return new UssdResponse(MSG_COMING_SOON + buildMenuText(), "CON");
    }

    private UssdResponse handleTouristAttractionPlaceholder(UssdSession session) {
        session.setCurrentStep("MAIN_MENU");
        session.setMenuMap(buildMenuMap());
        return new UssdResponse(MSG_COMING_SOON + buildMenuText(), "CON");
    }

    private String buildPlaceholderText(String locationType, UssdSession session) {
        String value = session.getCollectedData().getOrDefault("pendingLocationValue", "");
        if (value.isBlank()) {
            return locationType + " search is not live yet.\n";
        }
        return locationType + " search is not live yet for '" + value + "'.\n";
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
            String subject = "Inbound Travel Medical Insurance - Feedback";
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
        return "Inbound Travel Medical Insurance";
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
