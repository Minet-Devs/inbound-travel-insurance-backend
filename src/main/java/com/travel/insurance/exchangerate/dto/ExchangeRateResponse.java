package com.travel.insurance.exchangerate.dto;

import java.math.BigDecimal;

public record ExchangeRateResponse(String from, String to, BigDecimal rate) {
}
