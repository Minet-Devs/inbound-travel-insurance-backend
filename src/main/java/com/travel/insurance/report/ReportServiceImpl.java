package com.travel.insurance.report;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.claim.Claim;
import com.travel.insurance.claim.ClaimRepository;
import com.travel.insurance.claim.ClaimStatus;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.icd11.Icd11CodeService;
import com.travel.insurance.invoice.InvoiceService;
import com.travel.insurance.invoice.dto.InvoiceItemResponse;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.medicalservice.MedicalServiceService;
import com.travel.insurance.procedure.ProcedureService;
import com.travel.insurance.report.dto.*;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter LONG_DATE =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    private final ClaimRepository claimRepository;
    private final VisitorService visitorService;
    private final BenefitService benefitService;
    private final InvoiceService invoiceService;
    private final Icd11CodeService icd11CodeService;
    private final ProcedureService procedureService;
    private final ServiceProviderService serviceProviderService;
    private final MedicalServiceService medicalServiceService;
    private final SpringTemplateEngine templateEngine;

    @Override
    public ClaimReceiptResponse getClaimReceipt(UUID claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", claimId));
        return assembleClaimReceipt(claim);
    }

    @Override
    public byte[] generateClaimReceiptPdf(UUID claimId) {
        ClaimReceiptResponse receipt = getClaimReceipt(claimId);
        String html = renderClaimReceiptHtml(receipt);
        return renderPdf(html, "Failed to render claim receipt PDF");
    }

    @Override
    public ProviderClaimReportResponse getProviderReport(UUID providerId, String status,
                                                          LocalDate dateFrom, LocalDate dateTo,
                                                          Pageable pageable) {
        serviceProviderService.getById(providerId);
        ClaimStatus claimStatus = parseStatus(status);
        Instant from = dateFrom != null ? dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant to = dateTo != null ? dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;

        Page<Claim> page = claimRepository.findProviderClaims(providerId, claimStatus, from, to, pageable);
        List<ProviderClaimReportRow> rows = page.getContent().stream()
                .map(this::toReportRow)
                .toList();

        ProviderClaimReportSummary summary = buildSummary(providerId, from, to);
        return new ProviderClaimReportResponse(summary, rows,
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public byte[] generateProviderReportPdf(UUID providerId, String status,
                                             LocalDate dateFrom, LocalDate dateTo) {
        ProviderClaimReportResponse report = fetchFullProviderReport(providerId, status, dateFrom, dateTo);
        String html = renderProviderReportHtml(report);
        return renderPdf(html, "Failed to render provider claims report PDF");
    }

    @Override
    public byte[] generateProviderReportExcel(UUID providerId, String status,
                                               LocalDate dateFrom, LocalDate dateTo) {
        ProviderClaimReportResponse report = fetchFullProviderReport(providerId, status, dateFrom, dateTo);
        return renderExcel(report);
    }

    private ProviderClaimReportResponse fetchFullProviderReport(UUID providerId, String status,
                                                                 LocalDate dateFrom, LocalDate dateTo) {
        serviceProviderService.getById(providerId);
        ClaimStatus claimStatus = parseStatus(status);
        Instant from = dateFrom != null ? dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        Instant to = dateTo != null ? dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC) : null;

        List<Claim> claims = claimRepository.findProviderClaimsAll(providerId, claimStatus, from, to);
        List<ProviderClaimReportRow> rows = claims.stream().map(this::toReportRow).toList();
        ProviderClaimReportSummary summary = buildSummary(providerId, from, to);

        return new ProviderClaimReportResponse(summary, rows, 0, rows.size(), rows.size());
    }

    private ClaimReceiptResponse assembleClaimReceipt(Claim claim) {
        Visitor visitor = claim.getVisitorId() != null
                ? safeVisitLookup(claim.getVisitorId()) : null;
        String benefitName = benefitService.namesByIds(Set.of(claim.getBenefitId()))
                .getOrDefault(claim.getBenefitId(), "Unknown");

        List<InvoiceResponse> invoices = invoiceService.getByIds(claim.getInvoiceIds());

        Map<UUID, String> deptNamesByServiceId = resolveDepartmentNames(invoices);

        List<ClaimInvoiceGroup> invoiceGroups = invoices.stream()
                .map(inv -> toInvoiceGroup(inv, deptNamesByServiceId))
                .toList();

        List<String> diagnoses = resolveNames(claim.getDiagnosisIds(),
                id -> icd11CodeService.getById(id).title());
        List<String> procedures = resolveNames(claim.getProcedureIds(),
                id -> procedureService.getById(id).name());

        BigDecimal totalAmount = invoiceGroups.stream()
                .map(ClaimInvoiceGroup::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.equals(BigDecimal.ZERO) && claim.getClaimedAmount() != null) {
            totalAmount = claim.getClaimedAmount();
        }

        String providerName = claim.getServiceProviderId() != null
                ? serviceProviderService.getById(claim.getServiceProviderId()).name() : "N/A";

        return new ClaimReceiptResponse(
                claim.getId(),
                claim.getId().toString(),
                claim.getStatus().name(),
                claim.getCreatedDate(),
                visitor != null ? visitor.getFullName() : "Unknown",
                visitor != null ? visitor.getPassportNumber() : "N/A",
                visitor != null ? visitor.getDateOfBirth() : null,
                visitor != null ? visitor.getNationality() : "N/A",
                visitor != null ? visitor.getDateIn() : null,
                visitor != null ? visitor.getDateOut() : null,
                benefitName,
                providerName,
                invoiceGroups,
                diagnoses,
                procedures,
                totalAmount
        );
    }

    private ClaimInvoiceGroup toInvoiceGroup(InvoiceResponse invoice, Map<UUID, String> deptNamesByServiceId) {
        List<ClaimLineItem> items = invoice.invoiceItems().stream()
                .map(item -> new ClaimLineItem(
                        item.medicalServiceName() != null ? item.medicalServiceName() : item.description(),
                        resolveDeptName(item.medicalServiceId(), deptNamesByServiceId),
                        item.quantity(),
                        item.unitPrice(),
                        item.amount()))
                .toList();
        return new ClaimInvoiceGroup(
                invoice.id(),
                invoice.invoiceNumber(),
                invoice.issueDate(),
                invoice.currency(),
                invoice.totalAmount(),
                items);
    }

    private ProviderClaimReportRow toReportRow(Claim claim) {
        Visitor visitor = claim.getVisitorId() != null
                ? safeVisitLookup(claim.getVisitorId()) : null;
        String benefitName = benefitService.namesByIds(Set.of(claim.getBenefitId()))
                .getOrDefault(claim.getBenefitId(), "Unknown");

        return new ProviderClaimReportRow(
                claim.getId(),
                claim.getId().toString(),
                claim.getStatus().name(),
                claim.getCreatedDate(),
                visitor != null ? visitor.getFullName() : "Unknown",
                benefitName,
                claim.getClaimedAmount(),
                claim.getApprovedAmount());
    }

    private Visitor safeVisitLookup(UUID visitorId) {
        try {
            return visitorService.getEntityById(visitorId);
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    private ProviderClaimReportSummary buildSummary(UUID providerId, Instant from, Instant to) {
        BigDecimal totalClaimed = claimRepository.sumClaimedAmountByProvider(providerId, null, from, to);
        BigDecimal totalApproved = claimRepository.sumApprovedAmountByProvider(providerId, from, to);

        List<Object[]> statusCounts = claimRepository.countByStatusForProvider(providerId, from, to);
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        int totalClaims = 0;
        for (Object[] row : statusCounts) {
            String statusName = ((ClaimStatus) row[0]).name();
            int count = ((Long) row[1]).intValue();
            byStatus.put(statusName, count);
            totalClaims += count;
        }

        return new ProviderClaimReportSummary(totalClaims,
                totalClaimed != null ? totalClaimed : BigDecimal.ZERO,
                totalApproved != null ? totalApproved : BigDecimal.ZERO,
                byStatus);
    }

    private List<String> resolveNames(Set<UUID> ids, java.util.function.Function<UUID, String> resolver) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(id -> safeResolve(id, resolver))
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<UUID, String> resolveDepartmentNames(List<InvoiceResponse> invoices) {
        Set<UUID> serviceIds = invoices.stream()
                .flatMap(inv -> inv.invoiceItems().stream())
                .map(InvoiceItemResponse::medicalServiceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (serviceIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> result = new HashMap<>();
        for (UUID serviceId : serviceIds) {
            try {
                String deptName = medicalServiceService.getById(serviceId).departmentName();
                if (deptName != null) {
                    result.put(serviceId, deptName);
                }
            } catch (ResourceNotFoundException ignored) {
            }
        }
        return result;
    }

    private String resolveDeptName(UUID medicalServiceId, Map<UUID, String> deptNamesByServiceId) {
        if (medicalServiceId == null) {
            return "";
        }
        return deptNamesByServiceId.getOrDefault(medicalServiceId, "");
    }

    private String safeResolve(UUID id, java.util.function.Function<UUID, String> resolver) {
        try {
            return resolver.apply(id);
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    private ClaimStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return ClaimStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    }

    // ── PDF rendering ────────────────────────────────────────────────────

    private String renderClaimReceiptHtml(ClaimReceiptResponse receipt) {
        Context ctx = new Context();
        ctx.setVariable("receipt", receipt);
        ctx.setVariable("generatedDate", LocalDate.now().format(LONG_DATE));
        return templateEngine.process("report/claim-receipt", ctx);
    }

    private String renderProviderReportHtml(ProviderClaimReportResponse report) {
        Context ctx = new Context();
        ctx.setVariable("report", report);
        ctx.setVariable("generatedDate", LocalDate.now().format(LONG_DATE));
        return templateEngine.process("report/provider-claims", ctx);
    }

    private byte[] renderPdf(String html, String errorMessage) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
        } catch (IOException ex) {
            throw new IllegalStateException(errorMessage, ex);
        }
        return out.toByteArray();
    }

    // ── Excel rendering ──────────────────────────────────────────────────

    private byte[] renderExcel(ProviderClaimReportResponse report) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet claimsSheet = workbook.createSheet("Claims");
            renderClaimsSheet(claimsSheet, report.claims());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render provider claims Excel", ex);
        }
    }

    private void renderClaimsSheet(Sheet sheet, List<ProviderClaimReportRow> claims) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle currencyStyle = createCurrencyStyle(sheet.getWorkbook());

        String[] headers = {"Status", "Created Date", "Visitor", "Benefit",
                "Claimed Amount (KES)", "Approved Amount (KES)"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            setCell(headerRow, i, headers[i], headerStyle);
        }

        int rowIdx = 1;
        for (ProviderClaimReportRow claim : claims) {
            Row row = sheet.createRow(rowIdx++);
            setCell(row, 0, claim.status(), null);
            setCell(row, 1, claim.createdDate() != null
                    ? claim.createdDate().atOffset(ZoneOffset.UTC).format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "", null);
            setCell(row, 2, claim.visitorName(), null);
            setCell(row, 3, claim.benefitName(), null);
            setCell(row, 4, claim.claimedAmount(), currencyStyle);
            setCell(row, 5, claim.approvedAmount(), currencyStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private void setCell(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (style != null) {
            cell.setCellStyle(style);
        }
        if (value instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
        } else if (value instanceof Number num) {
            cell.setCellValue(num.intValue());
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }
    }
}
