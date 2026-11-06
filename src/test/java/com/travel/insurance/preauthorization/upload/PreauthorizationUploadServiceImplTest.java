package com.travel.insurance.preauthorization.upload;

import com.travel.insurance.benefit.BenefitService;
import com.travel.insurance.benefit.dto.BenefitResponse;
import com.travel.insurance.common.domain.VisitType;
import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.preauthorization.PreauthorizationService;
import com.travel.insurance.preauthorization.dto.OfflineApproval;
import com.travel.insurance.preauthorization.dto.PreauthorizationUploadResponse;
import com.travel.insurance.preauthorization.dto.PreauthorizationUploadRowStatus;
import com.travel.insurance.preauthorization.upload.PreauthorizationExcelParser.PreauthorizationExcelRow;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.serviceprovider.dto.ServiceProviderResponse;
import com.travel.insurance.visitor.Visitor;
import com.travel.insurance.visitor.VisitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreauthorizationUploadServiceImplTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Mock
    private PreauthorizationExcelParser parser;
    @Mock
    private PreauthorizationService preauthorizationService;
    @Mock
    private VisitorService visitorService;
    @Mock
    private ServiceProviderService serviceProviderService;
    @Mock
    private BenefitService benefitService;

    private PreauthorizationUploadServiceImpl uploadService;

    private final UUID visitorId = UUID.randomUUID();
    private final UUID nairobiHospitalId = UUID.randomUUID();
    private final UUID agaKhanId = UUID.randomUUID();
    private final UUID benefitId = UUID.randomUUID();
    private final MockMultipartFile file = new MockMultipartFile("file", "approvals.xlsx", XLSX, new byte[]{1});

    @BeforeEach
    void setUp() {
        uploadService = new PreauthorizationUploadServiceImpl(parser, preauthorizationService, visitorService,
                serviceProviderService, benefitService);
        lenient().when(serviceProviderService.listAll()).thenReturn(List.of(
                provider(nairobiHospitalId, "The Nairobi Hospital"),
                provider(agaKhanId, "Aga Khan University Hospital"),
                provider(UUID.randomUUID(), "Aga Khan Hospital Kisumu")));
        lenient().when(benefitService.listAll()).thenReturn(List.of(
                new BenefitResponse(benefitId, "Medical Expenses", new BigDecimal("100000"),
                        Instant.now(), Instant.now())));
    }

    private ServiceProviderResponse provider(UUID id, String name) {
        return new ServiceProviderResponse(id, name, null, null, null, null, null, null, null,
                Instant.now(), Instant.now());
    }

    private PreauthorizationExcelRow row(int excelRow, String passport, String provider, String visitType,
                                         String treatment, String discharge, String amount) {
        return new PreauthorizationExcelRow(excelRow, passport, provider, "Medical Expenses", visitType,
                treatment, discharge, amount);
    }

    private void knownVisitor() {
        Visitor visitor = new Visitor();
        visitor.setId(visitorId);
        when(visitorService.getEntityByPassportNumber("A1")).thenReturn(visitor);
    }

    @Test
    void createsApprovedPreauthorizationFromValidRow() {
        knownVisitor();
        UUID createdId = UUID.randomUUID();
        when(parser.parse(any())).thenReturn(List.of(
                row(2, "A1", "Nairobi Hospital", "outpatient", "10/10/2026", "TBA", "1,234.50")));
        when(preauthorizationService.createOfflineApproval(any())).thenReturn(createdId);

        PreauthorizationUploadResponse response = uploadService.upload(file);

        assertThat(response.totalRows()).isEqualTo(1);
        assertThat(response.createdRows()).isEqualTo(1);
        assertThat(response.rows().get(0).preauthorizationId()).isEqualTo(createdId);
        ArgumentCaptor<OfflineApproval> captor = ArgumentCaptor.forClass(OfflineApproval.class);
        verify(preauthorizationService).createOfflineApproval(captor.capture());
        OfflineApproval approval = captor.getValue();
        assertThat(approval.visitorId()).isEqualTo(visitorId);
        assertThat(approval.serviceProviderId()).isEqualTo(nairobiHospitalId);
        assertThat(approval.benefitId()).isEqualTo(benefitId);
        assertThat(approval.amount()).isEqualByComparingTo("1234.50");
        assertThat(approval.visitType()).isEqualTo(VisitType.OUTPATIENT);
        assertThat(approval.treatmentDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(approval.dischargeDate()).isNull();
    }

    @Test
    void readsBothDayFirstAndMonthFirstTextDates() {
        knownVisitor();
        when(parser.parse(any())).thenReturn(List.of(
                row(2, "A1", "Nairobi Hospital", "", "19/10/2026", "10/21/2026", "100")));
        when(preauthorizationService.createOfflineApproval(any())).thenReturn(UUID.randomUUID());

        uploadService.upload(file);

        ArgumentCaptor<OfflineApproval> captor = ArgumentCaptor.forClass(OfflineApproval.class);
        verify(preauthorizationService).createOfflineApproval(captor.capture());
        assertThat(captor.getValue().treatmentDate()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(captor.getValue().dischargeDate()).isEqualTo(LocalDate.of(2026, 10, 21));
        assertThat(captor.getValue().visitType()).isNull();
    }

    @Test
    void failsRowsWithUnresolvableDataWithoutStoppingTheRest() {
        knownVisitor();
        when(visitorService.getEntityByPassportNumber("ZZ")).thenThrow(new ResourceNotFoundException("Visitor", "ZZ"));
        when(parser.parse(any())).thenReturn(List.of(
                row(2, "ZZ", "Nairobi Hospital", "", "", "", "100"),
                row(3, "A1", "Unknown Clinic", "", "", "", "100"),
                row(4, "A1", "Nairobi Hospital", "", "", "", "TBD"),
                row(5, "A1", "Nairobi Hospital", "DAYCASE", "", "", "100"),
                row(6, "A1", "Nairobi Hospital", "", "31/31/2026", "", "100"),
                row(7, "A1", "Aga Khan", "", "", "", "100"),
                row(8, "A1", "Nairobi Hospital", "INPATIENT", "", "", "200")));
        UUID createdId = UUID.randomUUID();
        when(preauthorizationService.createOfflineApproval(any())).thenReturn(createdId);

        PreauthorizationUploadResponse response = uploadService.upload(file);

        assertThat(response.totalRows()).isEqualTo(7);
        assertThat(response.createdRows()).isEqualTo(1);
        assertThat(response.failedRows()).isEqualTo(6);
        assertThat(response.rows()).extracting(r -> r.status()).containsExactly(
                PreauthorizationUploadRowStatus.FAILED, PreauthorizationUploadRowStatus.FAILED,
                PreauthorizationUploadRowStatus.FAILED, PreauthorizationUploadRowStatus.FAILED,
                PreauthorizationUploadRowStatus.FAILED, PreauthorizationUploadRowStatus.FAILED,
                PreauthorizationUploadRowStatus.CREATED);
        assertThat(response.rows().get(0).errorMessage()).contains("No visitor found for passport ZZ");
        assertThat(response.rows().get(1).errorMessage()).contains("No matching service provider");
        assertThat(response.rows().get(2).errorMessage()).contains("Amount");
        assertThat(response.rows().get(3).errorMessage()).contains("visit type");
        assertThat(response.rows().get(4).errorMessage()).contains("date of treatment");
        assertThat(response.rows().get(5).errorMessage()).contains("Ambiguous service provider");
    }

    @Test
    void reportsServiceFailureOnTheRowOnly() {
        knownVisitor();
        when(parser.parse(any())).thenReturn(List.of(row(2, "A1", "Nairobi Hospital", "", "", "", "100")));
        when(preauthorizationService.createOfflineApproval(any()))
                .thenThrow(new IllegalStateException("A matching pre-authorization already exists"));

        PreauthorizationUploadResponse response = uploadService.upload(file);

        assertThat(response.failedRows()).isEqualTo(1);
        assertThat(response.rows().get(0).errorMessage()).isEqualTo("A matching pre-authorization already exists");
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "approvals.xlsx", XLSX, new byte[0]);

        assertThatThrownBy(() -> uploadService.upload(empty)).isInstanceOf(IllegalArgumentException.class);
        verify(preauthorizationService, never()).createOfflineApproval(any());
    }
}
