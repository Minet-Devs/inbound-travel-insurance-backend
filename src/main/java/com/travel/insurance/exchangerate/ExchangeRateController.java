package com.travel.insurance.exchangerate;

import com.travel.insurance.common.service.CurrencyConversionService;
import com.travel.insurance.exchangerate.dto.ExchangeRateResponse;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/exchange-rate")
@RequiredArgsConstructor
public class ExchangeRateController {

    private static final String CURRENCY_CODE = "^[A-Za-z]{3}$";

    private final CurrencyConversionService currencyConversionService;

    @GetMapping
    public ResponseEntity<ExchangeRateResponse> get(
            @RequestParam(defaultValue = "USD") @Pattern(regexp = CURRENCY_CODE, message = "from must be a 3-letter currency code") String from,
            @RequestParam(defaultValue = "KES") @Pattern(regexp = CURRENCY_CODE, message = "to must be a 3-letter currency code") String to) {
        String base = from.toUpperCase(Locale.ROOT);
        String quote = to.toUpperCase(Locale.ROOT);
        BigDecimal rate = currencyConversionService.getExchangeRate(base, quote);
        return ResponseEntity.ok(new ExchangeRateResponse(base, quote, rate));
    }
}
