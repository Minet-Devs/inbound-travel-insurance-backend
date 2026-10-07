package com.travel.insurance.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void allowsConfiguredOriginsAndMethodsForAnyPath() {
        CorsConfigurationSource source = new CorsConfig().corsConfigurationSource(
                List.of("https://app.inboundtravelhealthinsurance.ke", "http://localhost:3000"));

        CorsConfiguration config = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login"));

        assertThat(config).isNotNull();
        assertThat(config.checkOrigin("https://app.inboundtravelhealthinsurance.ke"))
                .isEqualTo("https://app.inboundtravelhealthinsurance.ke");
        assertThat(config.checkOrigin("https://evil.example.com")).isNull();
        assertThat(config.getAllowedMethods()).contains("POST", "OPTIONS");
        assertThat(config.checkHeaders(List.of("Authorization", "Content-Type")))
                .containsExactly("Authorization", "Content-Type");
    }
}
