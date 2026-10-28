package com.travel.insurance.preauthorization.upload;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.common.domain.VisitType;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.preauthorization.PreauthorizationService;
import com.travel.insurance.preauthorization.dto.OfflineApproval;
import com.travel.insurance.preauthorization.dto.PreauthorizationUploadResponse;
import com.travel.insurance.preauthorization.dto.PreauthorizationUploadRowResult;
import com.travel.insurance.preauthorization.dto.PreauthorizationUploadRowStatus;
import com.travel.insurance.preauthorization.upload.PreauthorizationExcelParser.PreauthorizationExcelRow;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;

/**
 * Deliberately not {@code @Transactional}: every row is created through
 * {@link PreauthorizationService#createOfflineApproval}, which runs in its own transaction, so one
 * bad row never rolls back the valid ones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreauthorizationUploadServiceImpl implements PreauthorizationUploadService {

    private static final DateTimeFormatter DAY_FIRST = DateTimeFormatter.ofPattern("d/M/uuuu");
    private static final DateTimeFormatter MONTH_FIRST = DateTimeFormatter.ofPattern("M/d/uuuu");
    private static final int MIN_FUZZY_LENGTH = 5;

    private final PreauthorizationExcelParser parser;
    private final PreauthorizationService preauthorizationService;
    private final VisitorService visitorService;
    private final ServiceProviderService serviceProviderService;
    private final BenefitService benefitService;

    @Override
    public PreauthorizationUploadResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }
        List<PreauthorizationExcelRow> excelRows;
        try (InputStream in = file.getInputStream()) {
            excelRows = parser.parse(in);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read the uploaded workbook", e);
        }

        List<ServiceProviderResponse> providers = serviceProviderService.listAll();
        List<BenefitResponse> benefits = benefitService.listAll();

        List<PreauthorizationUploadRowResult> results = new ArrayList<>();
        int created = 0;
        for (PreauthorizationExcelRow row : excelRows) {
            PreauthorizationUploadRowResult result = importRow(row, providers, benefits);
            if (result.status() == PreauthorizationUploadRowStatus.CREATED) {
                created++;
            }
            results.add(result);
        }
        return new PreauthorizationUploadResponse(results.size(), created, results.size() - created, results);
    }

    private PreauthorizationUploadRowResult importRow(PreauthorizationExcelRow row,
                                                      List<ServiceProviderResponse> providers,
                                                      List<BenefitResponse> benefits) {
        try {
            UUID id = preauthorizationService.createOfflineApproval(toApproval(row, providers, benefits));
            return new PreauthorizationUploadRowResult(row.excelRowNumber(), row.passportNumber(),
                    PreauthorizationUploadRowStatus.CREATED, id, null);
        } catch (RuntimeException e) {
            if (!(e instanceof IllegalArgumentException || e instanceof IllegalStateException
                    || e instanceof ResourceNotFoundException)) {
                log.error("Unexpected error importing pre-authorization upload row {}", row.excelRowNumber(), e);
            }
            return new PreauthorizationUploadRowResult(row.excelRowNumber(), row.passportNumber(),
                    PreauthorizationUploadRowStatus.FAILED, null, e.getMessage());
        }
    }

    private OfflineApproval toApproval(PreauthorizationExcelRow row, List<ServiceProviderResponse> providers,
                                       List<BenefitResponse> benefits) {
        if (row.passportNumber().isBlank()) {
            throw new IllegalArgumentException("Passport number is missing");
        }
        Visitor visitor;
        try {
            visitor = visitorService.getEntityByPassportNumber(row.passportNumber());
        } catch (ResourceNotFoundException e) {
            throw new IllegalArgumentException("No visitor found for passport " + row.passportNumber());
        }
        UUID providerId = resolve(providers, ServiceProviderResponse::name, ServiceProviderResponse::id,
                row.providerName(), "service provider");
        UUID benefitId = resolve(benefits, BenefitResponse::benefitName, BenefitResponse::id,
                row.benefit(), "benefit");
        return new OfflineApproval(visitor.getId(), benefitId, providerId, parseAmount(row.amount()),
                parseVisitType(row.visitType()), parseDate(row.treatmentDate(), "date of treatment"),
                parseDate(row.dischargeDate(), "date of discharge"));
    }

    private BigDecimal parseAmount(String raw) {
        if (raw.isBlank() || !raw.matches("[0-9,]*\\.?[0-9]+")) {
            throw new IllegalArgumentException("Amount is missing or not a number: '" + raw + "'");
        }
        BigDecimal amount = new BigDecimal(raw.replace(",", ""));
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive: '" + raw + "'");
        }
        return amount;
    }

    private VisitType parseVisitType(String raw) {
        if (raw.isBlank()) {
            return null;
        }
        try {
            return VisitType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown visit type '" + raw + "' (expected OUTPATIENT or INPATIENT)");
        }
    }

    /**
     * Blank and placeholder values ("TBA", "TBD") mean "not known yet" and become null. Text dates are
     * read as ISO, then day-first, then month-first (the sheet mixes {@code 19/10/2026} and
     * {@code 10/19/2026}; an ambiguous date such as {@code 10/05/2026} is read day-first).
     */
    private LocalDate parseDate(String raw, String field) {
        String value = raw.trim();
        if (value.isEmpty() || value.equalsIgnoreCase("TBA") || value.equalsIgnoreCase("TBD")) {
            return null;
        }
        for (DateTimeFormatter format : List.of(DateTimeFormatter.ISO_LOCAL_DATE, DAY_FIRST, MONTH_FIRST)) {
            try {
                return LocalDate.parse(value, format);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        throw new IllegalArgumentException("Unrecognised " + field + ": '" + raw + "'");
    }

    /** Exact match on the normalised name, else a single unambiguous containment match. */
    private <T> UUID resolve(List<T> candidates, Function<T, String> nameOf, Function<T, UUID> idOf,
                             String rawName, String what) {
        String wanted = normalise(rawName);
        if (wanted.isEmpty()) {
            throw new IllegalArgumentException("The " + what + " is missing");
        }
        List<T> exact = candidates.stream().filter(c -> normalise(nameOf.apply(c)).equals(wanted)).toList();
        if (exact.size() == 1) {
            return idOf.apply(exact.get(0));
        }
        boolean ambiguous = exact.size() > 1;
        if (exact.isEmpty() && wanted.length() >= MIN_FUZZY_LENGTH) {
            List<T> partial = candidates.stream().filter(c -> {
                String name = normalise(nameOf.apply(c));
                return !name.isEmpty() && (name.contains(wanted) || wanted.contains(name));
            }).toList();
            if (partial.size() == 1) {
                return idOf.apply(partial.get(0));
            }
            ambiguous = partial.size() > 1;
        }
        throw new IllegalArgumentException(ambiguous
                ? "Ambiguous " + what + ": '" + rawName + "'"
                : "No matching " + what + " found for '" + rawName + "'");
    }

    private static String normalise(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return lower.startsWith("the") && lower.length() > 3 ? lower.substring(3) : lower;
    }
}
