package com.travel.insurance.common.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.travel.insurance.common.exception.ExchangeRateUnavailableException;
import com.travel.insurance.config.ExchangeRateProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Service
public class CurrencyConversionService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionService.class);

    private static final String RESULT_SUCCESS = "success";
    private static final String BASE_CURRENCY = "USD";

    private final ExchangeRateProperties properties;
    private final RestClient restClient;

    public CurrencyConversionService(ExchangeRateProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Cacheable(value = "fxRates", key = "#fromCurrency + '-' + #toCurrency")
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null
                || fromCurrency.equalsIgnoreCase(toCurrency)) {
            return BigDecimal.ONE;
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.error("Exchange-rate API key is not configured");
            throw new ExchangeRateUnavailableException("Exchange-rate service is not configured");
        }
        String uri = properties.getBaseUrl() + "/" + properties.getApiKey() + "/pair/{from}/{to}";
        try {
            FxRateResponse response = restClient.get()
                    .uri(uri, fromCurrency.toUpperCase(), toCurrency.toUpperCase())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new ExchangeRateUnavailableException(
                                "Exchange-rate service returned " + res.getStatusCode());
                    })
                    .body(FxRateResponse.class);
            if (response == null || !RESULT_SUCCESS.equals(response.result())
                    || response.conversionRate() == null) {
                log.error("Invalid exchange-rate response for {} -> {}", fromCurrency, toCurrency);
                throw new ExchangeRateUnavailableException("Exchange-rate service returned an invalid response");
            }
            return response.conversionRate();
        } catch (RestClientException ex) {
            log.error("Failed to fetch exchange rate {} -> {}", fromCurrency, toCurrency, ex);
            throw new ExchangeRateUnavailableException("Exchange-rate service is unavailable", ex);
        }
    }

    public String baseCurrency() {
        return BASE_CURRENCY;
    }

    record FxRateResponse(@JsonProperty("result") String result,
                          @JsonProperty("conversion_rate") BigDecimal conversionRate) {
    }
}
