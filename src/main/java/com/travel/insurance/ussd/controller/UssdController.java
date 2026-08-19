package com.travel.insurance.ussd.controller;

import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdRequest;
import com.travel.insurance.ussd.dto.UssdResponse;
import com.travel.insurance.ussd.service.UssdService;
import com.travel.insurance.ussd.utils.UssdSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ussd")
public class UssdController {

    private final UssdSessionManager sessionManager;
    private final UssdService ussdService;

    public UssdController(UssdSessionManager sessionManager, UssdService ussdService) {
        this.sessionManager = sessionManager;
        this.ussdService = ussdService;
    }

    @PostMapping(value = "/handle", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String handleFormEncodedCallback(@RequestParam Map<String, String> params) {
        UssdRequest ussdRequest = buildRequest(params);
        return handleAndFormat(ussdRequest);
    }

    @PostMapping(value = "/handle", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public String handleJsonCallback(@RequestBody Map<String, Object> payload) {
        UssdRequest ussdRequest = buildRequest(payload);
        return handleAndFormat(ussdRequest);
    }

    private String handleAndFormat(UssdRequest request) {
        log.info("USSD Request -> session={}, msisdn={}, ussdString='{}', serviceCode='{}'",
                request.getSessionId(), request.getMsisdn(), request.getUssdString(), request.getServiceCode());

        String sessionKey = sessionManager.buildSessionKey(request);

        UssdSession ussdSession = sessionManager.findOrCreateSession(sessionKey, request);

        String rawInput = sessionManager.extractAndTrackInput(ussdSession, request.getUssdString());

        UssdResponse ussdResponse = ussdService.processSessionStep(ussdSession, rawInput);

        sessionManager.persistOrEvictSession(sessionKey, ussdSession, ussdResponse);

        return sessionManager.formatWireText(ussdResponse);
    }

    private UssdRequest buildRequest(Map<String, ?> map) {
        String sessionId = extractString(map, "sessionId", "SessionId", "session_id");
        String msisdn = extractString(map, "msisdn", "MSISDN", "Msisdn", "phoneNumber", "phone", "mobile", "callerNumber");
        String ussdString = extractString(map, "ussd_string", "ussdString", "text", "input", "content", "msg");
        String serviceCode = extractString(map, "serviceCode", "ServiceCode", "service_code");
        return new UssdRequest(sessionId, msisdn,
                ussdString != null ? ussdString : "",
                serviceCode != null ? serviceCode : "");
    }

    private String extractString(Map<String, ?> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return null;
    }
}
