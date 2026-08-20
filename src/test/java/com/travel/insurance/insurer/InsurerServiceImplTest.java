package com.travel.insurance.insurer;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.insurer.dto.InsurerResponse;
import com.travel.insurance.organization.Organization;
import com.travel.insurance.organization.OrganizationService;
import com.travel.insurance.policy.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsurerServiceImplTest {

    @Mock
    private InsurerRepository insurerRepository;

    @Mock
    private PolicyService policyService;

    @Mock
    private OrganizationService organizationService;

    private final InsurerMapper insurerMapper = new InsurerMapper();

    private InsurerServiceImpl insurerService;

    private InsurerRequest request;

    @BeforeEach
    void setUp() {
        insurerService = new InsurerServiceImpl(insurerRepository, insurerMapper, policyService, organizationService);
        lenient().when(policyService.findPolicyIdByInsurerId(any())).thenReturn(Optional.empty());
        request = new InsurerRequest("Acme Insurance", "contact@acme.example", "+254700000000", "Nairobi",
                "https://cdn.example/acme.png", 42L, "notify@acme.example", "s3cr3t", "smtp.acme.example", 587,
                "signature-data", null);
    }

    @Test
    void createSavesAndReturnsInsurer() {
        when(insurerRepository.existsByName("Acme Insurance")).thenReturn(false);
        when(insurerRepository.save(any(Insurer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InsurerResponse response = insurerService.create(request);

        assertThat(response.name()).isEqualTo("Acme Insurance");
        assertThat(response.contactEmail()).isEqualTo("contact@acme.example");
        assertThat(response.logoUrl()).isEqualTo("https://cdn.example/acme.png");
        assertThat(response.policyToken()).isEqualTo(42L);
        assertThat(response.notificationEmail()).isEqualTo("notify@acme.example");
        assertThat(response.host()).isEqualTo("smtp.acme.example");
        assertThat(response.port()).isEqualTo(587);
        assertThat(response.esignature()).isEqualTo("signature-data");
        verify(insurerRepository).save(any(Insurer.class));
    }

    @Test
    void createRejectsUnknownOrganizationId() {
        UUID organizationId = UUID.randomUUID();
        InsurerRequest withOrganization = new InsurerRequest("Acme Insurance", "contact@acme.example", null, null,
                null, null, null, null, null, null, null, organizationId);
        when(insurerRepository.existsByName("Acme Insurance")).thenReturn(false);
        when(organizationService.getEntityById(organizationId))
                .thenThrow(new ResourceNotFoundException("Organization", organizationId));

        assertThatThrownBy(() -> insurerService.create(withOrganization))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(insurerRepository, never()).save(any());
    }

    @Test
    void createAcceptsExistingOrganizationId() {
        UUID organizationId = UUID.randomUUID();
        InsurerRequest withOrganization = new InsurerRequest("Acme Insurance", "contact@acme.example", null, null,
                null, null, null, null, null, null, null, organizationId);
        when(insurerRepository.existsByName("Acme Insurance")).thenReturn(false);
        when(organizationService.getEntityById(organizationId)).thenReturn(new Organization());
        when(insurerRepository.save(any(Insurer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InsurerResponse response = insurerService.create(withOrganization);

        assertThat(response.organizationId()).isEqualTo(organizationId);
    }

    @Test
    void createNormalizesDropboxLogoUrlToDirectDownload() {
        InsurerRequest dropboxLogo = new InsurerRequest("Guardian Assurance", "ga@example.com", null, null,
                "https://www.dropbox.com/scl/fi/abc/ga-logo.png?rlkey=key&dl=0", null, null, null, null, null, null,
                null);
        when(insurerRepository.existsByName("Guardian Assurance")).thenReturn(false);
        when(insurerRepository.save(any(Insurer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InsurerResponse response = insurerService.create(dropboxLogo);

        assertThat(response.logoUrl())
                .isEqualTo("https://dl.dropboxusercontent.com/scl/fi/abc/ga-logo.png?rlkey=key&dl=1");
    }

    @Test
    void createRejectsDuplicateName() {
        when(insurerRepository.existsByName("Acme Insurance")).thenReturn(true);

        assertThatThrownBy(() -> insurerService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Acme Insurance");
        verify(insurerRepository, never()).save(any());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(insurerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> insurerService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getEntityByIdReturnsEntity() {
        UUID id = UUID.randomUUID();
        Insurer insurer = insurerMapper.toEntity(request);
        insurer.setId(id);
        when(insurerRepository.findById(id)).thenReturn(Optional.of(insurer));

        Insurer result = insurerService.getEntityById(id);

        assertThat(result.getName()).isEqualTo("Acme Insurance");
    }

    @Test
    void getEntityByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(insurerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> insurerService.getEntityById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdIncludesPolicyIdBackedByInsurer() {
        UUID id = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();
        Insurer insurer = insurerMapper.toEntity(request);
        insurer.setId(id);
        when(insurerRepository.findById(id)).thenReturn(Optional.of(insurer));
        when(policyService.findPolicyIdByInsurerId(id)).thenReturn(Optional.of(policyId));

        InsurerResponse response = insurerService.getById(id);

        assertThat(response.policyId()).isEqualTo(policyId);
    }

    @Test
    void listAllReturnsEveryInsurer() {
        Insurer insurer = insurerMapper.toEntity(request);
        insurer.setId(UUID.randomUUID());
        when(insurerRepository.findAll()).thenReturn(List.of(insurer));

        List<InsurerResponse> responses = insurerService.listAll();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().name()).isEqualTo("Acme Insurance");
    }

    @Test
    void updateAppliesChanges() {
        UUID id = UUID.randomUUID();
        Insurer existing = insurerMapper.toEntity(request);
        when(insurerRepository.findById(id)).thenReturn(Optional.of(existing));
        when(insurerRepository.save(any(Insurer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InsurerRequest update = new InsurerRequest("Acme Re", "hello@acme.example", null, null, null, null, null,
                null, null, null, null, null);
        InsurerResponse response = insurerService.update(id, update);

        assertThat(response.name()).isEqualTo("Acme Re");
        assertThat(response.contactEmail()).isEqualTo("hello@acme.example");
    }
}
