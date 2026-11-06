package com.travel.insurance.exchangerate;

import com.travel.insurance.auth.JwtTokenProvider;
import com.travel.insurance.common.exception.ExchangeRateUnavailableException;
import com.travel.insurance.common.service.CurrencyConversionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeRateController.class)
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyConversionService currencyConversionService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser
    void defaultsToUsdKes() throws Exception {
        when(currencyConversionService.getExchangeRate("USD", "KES")).thenReturn(new BigDecimal("129.2500"));

        mockMvc.perform(get("/api/v1/exchange-rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("KES"))
                .andExpect(jsonPath("$.rate").value(129.25));
    }

    @Test
    @WithMockUser
    void upperCasesExplicitCurrencies() throws Exception {
        when(currencyConversionService.getExchangeRate("KES", "USD")).thenReturn(new BigDecimal("0.0077"));

        mockMvc.perform(get("/api/v1/exchange-rate").param("from", "kes").param("to", "usd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("KES"))
                .andExpect(jsonPath("$.to").value("USD"))
                .andExpect(jsonPath("$.rate").value(0.0077));
        verify(currencyConversionService).getExchangeRate("KES", "USD");
    }

    @Test
    @WithMockUser
    void rejectsInvalidCurrencyCode() throws Exception {
        mockMvc.perform(get("/api/v1/exchange-rate").param("from", "US1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void returns503WhenRateUnavailable() throws Exception {
        when(currencyConversionService.getExchangeRate("USD", "KES"))
                .thenThrow(new ExchangeRateUnavailableException("down"));

        mockMvc.perform(get("/api/v1/exchange-rate"))
                .andExpect(status().isServiceUnavailable());
    }
}
