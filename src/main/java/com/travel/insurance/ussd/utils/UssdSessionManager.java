package com.travel.insurance.ussd.utils;

import com.travel.insurance.ussd.domain.UssdSession;
import com.travel.insurance.ussd.dto.UssdRequest;
import com.travel.insurance.ussd.dto.UssdResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class UssdSessionManager {

    private static final String SESSION_KEY_PREFIX = "ussd:session:";
    private static final long SESSION_TTL_SECONDS = 180;
    private static final String LAST_RAW_TEXT_LEN_KEY = "_lastRawTextLen";

    private final RedisTemplate<String, UssdSession> redisTemplate;

    public UssdSessionManager(RedisTemplate<String, UssdSession> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String buildSessionKey(UssdRequest request) {
        String msisdn = request.getMsisdn() != null ? request.getMsisdn() : "unknown";
        String sessionId = request.getSessionId() != null ? request.getSessionId() : String.valueOf(System.currentTimeMillis());
        return SESSION_KEY_PREFIX + msisdn + ":" + sessionId;
    }

    public UssdSession findOrCreateSession(String sessionKey, UssdRequest request) {
        try {
            UssdSession session = redisTemplate.opsForValue().get(sessionKey);
            if (session == null) {
                session = createNewSession(request);
            }
            return session;
        } catch (Exception e) {
            log.error("Redis unavailable while fetching session for {}: {}", request.getMsisdn(), e.getMessage());
            return createNewSession(request);
        }
    }

    public void persistOrEvictSession(String sessionKey, UssdSession session, UssdResponse response) {
        try {
            if ("END".equals(response.getType())) {
                redisTemplate.delete(sessionKey);
                log.info("USSD session destroyed: key={}", sessionKey);
            } else {
                redisTemplate.opsForValue().set(sessionKey, session, SESSION_TTL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Redis unavailable - could not persist/evict session for key {}: {}", sessionKey, e.getMessage());
        }
    }

    public String extractAndTrackInput(UssdSession session, String incomingText) {
        String cleanedText = incomingText != null ? decodeUrl(incomingText.trim()) : "";
        if (cleanedText.isEmpty()) return "";

        if (!cleanedText.contains("*")) return cleanedText;

        String lenStr = session.getCollectedData().getOrDefault(LAST_RAW_TEXT_LEN_KEY, "0");
        int lastLen = Integer.parseInt(lenStr);

        String rawInput;
        if (cleanedText.length() > lastLen && lastLen > 0) {
            String delta = cleanedText.substring(lastLen);
            if (delta.startsWith("*")) delta = delta.substring(1);
            rawInput = delta.trim();
        } else {
            String[] parts = cleanedText.split("\\*");
            rawInput = parts[parts.length - 1].trim();
        }

        session.getCollectedData().put(LAST_RAW_TEXT_LEN_KEY, String.valueOf(cleanedText.length()));
        return rawInput;
    }

    private String decodeUrl(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            log.warn("Failed to URL-decode value: {}", value);
            return value;
        }
    }

    public String formatWireText(UssdResponse response) {
        String text = response.getText();
        if (text == null || text.isEmpty()) return "";

        String type = response.getType();
        if ("CON".equals(type) && !text.startsWith("CON ")) return "CON " + text;
        if ("END".equals(type) && !text.startsWith("END ")) return "END " + text;
        return text;
    }

    private UssdSession createNewSession(UssdRequest request) {
        UssdSession session = new UssdSession();
        session.setSessionId(request.getSessionId());
        session.setMsisdn(request.getMsisdn());
        session.setCurrentStep("INIT");
        session.setCollectedData(new HashMap<>());
        session.setTempDependant(new HashMap<>());
        return session;
    }
}
