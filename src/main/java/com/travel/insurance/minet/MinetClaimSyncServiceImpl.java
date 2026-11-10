package com.travel.insurance.minet;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.claim.Claim;
import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.icd11.Icd11CodeService;
import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import com.travel.insurance.invoice.InvoiceService;
import com.travel.insurance.invoice.dto.InvoiceItemResponse;
import com.travel.insurance.invoice.dto.InvoiceResponse;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitor.dto.VisitorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class MinetClaimSyncServiceImpl implements MinetClaimSyncService {

    /** Minet expects these two values verbatim for every claim. */
    static final String GROUPED_SERVICE = "Grouped Service";
    static final String GROUPED_DEPARTMENT = "Grouped Department";
    private static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");

    private final ClaimService claimService;
    private final VisitorService visitorService;
    private final ServiceProviderService serviceProviderService;
    private final BenefitService benefitService;
    private final Icd11CodeService icd11CodeService;
    private final InvoiceService invoiceService;
    private final MinetClient minetClient;

    /**
     * Read-only transaction keeps the Hibernate session open so the claim's lazy
     * {@code diagnosisIds} collection can be read. {@code markSentToMinet} runs in its own
     * ({@code REQUIRES_NEW}) transaction, so the flag is still committed.
     */
    @Override
    @Transactional(readOnly = true)
    public MinetClaimSyncResponse syncClaim(UUID claimId) {
        Claim claim = claimService.getEntityById(claimId);
        if (claim.getServiceProviderId() == null) {
            throw new IllegalStateException("Claim has no service provider; cannot send to Minet");
        }
        ServiceProviderResponse provider = serviceProviderService.getById(claim.getServiceProviderId());
        if (provider.minetServiceProviderId() == null || provider.minetServiceProviderId().isBlank()) {
            throw new IllegalStateException(
                    "Service provider has no Minet service provider id; cannot send claim to Minet");
        }
        minetClient.sendClaim(toRequest(claim, provider));
        claimService.markSentToMinet(claimId);
        return new MinetClaimSyncResponse(claimId, true);
    }

    private MinetClaimRequest toRequest(Claim claim, ServiceProviderResponse provider) {
        VisitorResponse visitor = claim.getVisitorId() != null ? visitorService.getById(claim.getVisitorId()) : null;
        String benefit = claim.getBenefitId() != null ? benefitService.getById(claim.getBenefitId()).benefitName() : null;
        List<Icd11CodeResponse> diagnoses = claim.getDiagnosisIds().stream()
                .map(this::diagnosis).filter(Objects::nonNull).toList();
        List<InvoiceResponse> invoices = invoiceService.listByClaimId(claim.getId());
        LocalDate claimDate = claim.getCreatedDate() != null
                ? claim.getCreatedDate().atZone(NAIROBI).toLocalDate()
                : LocalDate.now(NAIROBI);
        LocalDate visitDate = invoices.stream().flatMap(i -> i.invoiceItems().stream())
                .map(InvoiceItemResponse::serviceDate).filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElse(claimDate);
        return new MinetClaimRequest(
                provider.name(),
                provider.minetServiceProviderId(),
                benefit,
                visitor != null ? visitor.passportNumber() : null,
                claimDate.toString(),
                claim.getVisitType() != null ? titleCase(claim.getVisitType().name()) : null,
                joined(diagnoses.stream().map(Icd11CodeResponse::code)),
                joined(diagnoses.stream().map(Icd11CodeResponse::title)),
                visitDate.toString(),
                claim.getClaimedAmount(),
                GROUPED_SERVICE,
                GROUPED_DEPARTMENT,
                claim.getClaimedAmountBase(),
                claim.getExchangeRate(),
                claim.getId().toString(),
                invoices.stream().map(i -> new MinetClaimRequest.Invoice(
                        i.invoiceNumber(),
                        i.issueDate() != null ? i.issueDate().toString() : claimDate.toString(),
                        i.totalAmount(),
                        i.currency())).toList());
    }

    private Icd11CodeResponse diagnosis(UUID id) {
        try {
            return icd11CodeService.getById(id);
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    private static String joined(Stream<String> values) {
        String joined = values.filter(v -> v != null && !v.isBlank()).distinct().collect(Collectors.joining("; "));
        return joined.isEmpty() ? null : joined;
    }

    /** {@code OUTPATIENT} to {@code Outpatient}. */
    private static String titleCase(String enumName) {
        return enumName.charAt(0) + enumName.substring(1).toLowerCase();
    }
}
