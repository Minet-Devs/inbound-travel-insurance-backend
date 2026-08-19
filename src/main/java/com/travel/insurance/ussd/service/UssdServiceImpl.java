package com.travel.insurance.ussd.service;

import com.travel.insurance.common.email.EmailService;
import com.travel.insurance.config.MailProperties;
import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EmailService emailService;
    private final MailProperties mailProperties;
    private final String defaultSchemeName;
    private final String feedbackRecipient;

    public UssdServiceImpl(EmailService emailService,
                           MailProperties mailProperties,
                           @Value("${ussd.feedback.default-scheme-name:Minet Healthcare}") String defaultSchemeName,
                           @Value("${ussd.feedback.email.to:feedback-recipient@travelinsurance.example}") String feedbackRecipient) {
        this.emailService = emailService;
        this.mailProperties = mailProperties;
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
                return new UssdResponse("Feature coming soon. You will be able to search for hospitals near you.", "END");

            case "FEEDBACK":
                session.setCurrentStep("FEEDBACK_MESSAGE");
                return new UssdResponse(PROMPT_FEEDBACK_MESSAGE, "CON");

            default:
                return new UssdResponse(MSG_INVALID_CHOICE + buildMenuText(), "CON");
        }
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
            String subject = "Traveller Insurance Feedback";
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
