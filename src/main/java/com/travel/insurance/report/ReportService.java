package com.travel.insurance.report;

import com.travel.insurance.report.dto.ClaimReceiptResponse;
import com.travel.insurance.report.dto.ProviderClaimReportResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface ReportService {

    ClaimReceiptResponse getClaimReceipt(UUID claimId);

    byte[] generateClaimReceiptPdf(UUID claimId);

    ProviderClaimReportResponse getProviderReport(UUID providerId, String status,
                                                   LocalDate dateFrom, LocalDate dateTo,
                                                   Pageable pageable);

    byte[] generateProviderReportPdf(UUID providerId, String status,
                                      LocalDate dateFrom, LocalDate dateTo);

    byte[] generateProviderReportExcel(UUID providerId, String status,
                                        LocalDate dateFrom, LocalDate dateTo);
}
