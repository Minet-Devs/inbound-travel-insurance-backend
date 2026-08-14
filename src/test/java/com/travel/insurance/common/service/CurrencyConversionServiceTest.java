package com.travel.insurance.common.service;

import com.travel.insurance.common.exception.ExchangeRateUnavailableException;
import com.travel.insurance.config.ExchangeRateProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CurrencyConversionServiceTest {

    private MockRestServiceServer server;
    private CurrencyConversionService service;

    @BeforeEach
    void setUp() {
        ExchangeRateProperties properties = new ExchangeRateProperties();
        properties.setApiKey("test-api-key");
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new CurrencyConversionService(properties, builder);
    }

    @Test
    void returnsOneWhenCurrenciesMatch() {
        assertThat(service.getExchangeRate("USD", "USD")).isEqualByComparingTo("1");
        assertThat(service.getExchangeRate("KES", "kes")).isEqualByComparingTo("1");
    }

    @Test
    void returnsRateFromApi() {
        server.expect(requestTo("https://v6.exchangerate-api.com/v6/test-api-key/pair/KES/USD"))
                .andRespond(withSuccess("{\"result\":\"success\",\"conversion_rate\":0.0077}",
                        MediaType.APPLICATION_JSON));

        BigDecimal rate = service.getExchangeRate("KES", "USD");

        assertThat(rate).isEqualByComparingTo("0.0077");
    }

    @Test
    void throwsWhenApiReportsFailure() {
        server.expect(requestTo("https://v6.exchangerate-api.com/v6/test-api-key/pair/KES/USD"))
                .andRespond(withSuccess("{\"result\":\"error\",\"error_type\":\"unsupported-code\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.getExchangeRate("KES", "USD"))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void throwsWhenApiUnavailable() {
        server.expect(requestTo("https://v6.exchangerate-api.com/v6/test-api-key/pair/KES/USD"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.getExchangeRate("KES", "USD"))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    @Test
    void throwsWhenApiKeyMissing() {
        CurrencyConversionService unconfigured =
                new CurrencyConversionService(new ExchangeRateProperties(), RestClient.builder());

        assertThatThrownBy(() -> unconfigured.getExchangeRate("KES", "USD"))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }
}
