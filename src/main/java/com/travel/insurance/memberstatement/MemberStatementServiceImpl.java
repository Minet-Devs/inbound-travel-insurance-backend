package com.travel.insurance.memberstatement;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.claim.ClaimService;
import com.travel.insurance.claim.dto.ClaimResponse;
import com.travel.insurance.memberstatement.dto.MemberStatementResponse;
import com.travel.insurance.memberstatement.dto.MemberStatementTransaction;
import com.travel.insurance.policy.Policy;
import com.travel.insurance.policy.PolicyService;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import com.travel.insurance.visitorbenefit.VisitorBenefitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberStatementServiceImpl implements MemberStatementService {

    private final VisitorService visitorService;
    private final VisitorBenefitService visitorBenefitService;
    private final PolicyService policyService;
    private final ClaimService claimService;
    private final BenefitService benefitService;
    private final ServiceProviderService serviceProviderService;
    private final MemberStatementExcelWriter excelWriter;
    private final MemberStatementPdfRenderer pdfRenderer;

    @Override
    public MemberStatementResponse getStatement(String passportNumber) {
        return buildStatement(passportNumber, null, null);
    }

    @Override
    public byte[] export(String passportNumber, LocalDate fromDate, LocalDate toDate,
                          MemberStatementExportType exportType) {
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must not be after toDate");
        }
        MemberStatementResponse statement = buildStatement(passportNumber, fromDate, toDate);
        return switch (exportType) {
            case PDF -> pdfRenderer.render(statement);
            case EXCEL -> excelWriter.write(statement);
        };
    }

    private MemberStatementResponse buildStatement(String passportNumber, LocalDate fromDate, LocalDate toDate) {
        Visitor visitor = visitorService.getEntityByPassportNumber(passportNumber);
        Policy policy = policyService.getEntityById(visitor.getPolicyId());
        return new MemberStatementResponse(
                visitor.getId(),
                visitor.getFullName(),
                visitor.getPassportNumber(),
                policy.getId(),
                visitorBenefitService.listAllByVisitor(visitor.getId()),
                buildTransactions(visitor.getId(), fromDate, toDate));
    }

    private List<MemberStatementTransaction> buildTransactions(UUID visitorId, LocalDate fromDate, LocalDate toDate) {
        List<ClaimResponse> claims = claimService.listByVisitor(visitorId);
        Set<UUID> benefitIds = claims.stream().map(ClaimResponse::benefitId).collect(Collectors.toSet());
        Set<UUID> providerIds = claims.stream()
                .map(ClaimResponse::serviceProviderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> benefitNames = benefitService.namesByIds(benefitIds);
        Map<UUID, String> providerNames = serviceProviderService.namesByIds(providerIds);

        return claims.stream()
                .map(claim -> {
                    LocalDate transactionDate = LocalDate.ofInstant(claim.createdDate(), ZoneOffset.UTC);
                    return new MemberStatementTransaction(
                            claim.id(),
                            transactionDate,
                            claim.benefitId(),
                            benefitNames.get(claim.benefitId()),
                            claim.claimedAmount(),
                            claim.serviceProviderId(),
                            claim.serviceProviderId() != null ? providerNames.get(claim.serviceProviderId()) : null);
                })
                .filter(transaction -> isWithinRange(transaction.transactionDate(), fromDate, toDate))
                .sorted((a, b) -> a.transactionDate().compareTo(b.transactionDate()))
                .toList();
    }

    private boolean isWithinRange(LocalDate date, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            return true;
        }
        return date != null && !date.isBefore(fromDate) && !date.isAfter(toDate);
    }
}