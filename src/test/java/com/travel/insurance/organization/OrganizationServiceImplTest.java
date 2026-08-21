package com.travel.insurance.organization;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.organization.dto.OrganizationRequest;
import com.travel.insurance.organization.dto.OrganizationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceImplTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private final OrganizationMapper organizationMapper = new OrganizationMapper();

    private OrganizationServiceImpl organizationService;

    private OrganizationRequest request;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationServiceImpl(organizationRepository, organizationMapper, eventPublisher);
        request = new OrganizationRequest("Acme Ltd", OrganizationType.INSURER, "contact@acme.com",
                "0700000000", "123 Main St", "Nairobi", null, null, null, null, null, null, null);
    }

    @Test
    void createSavesAndReturnsOrganization() {
        when(organizationRepository.existsByName("Acme Ltd")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response = organizationService.create(request);

        assertThat(response.name()).isEqualTo("Acme Ltd");
        assertThat(response.organizationType()).isEqualTo(OrganizationType.INSURER);
        assertThat(response.email()).isEqualTo("contact@acme.com");
        assertThat(response.phoneNumber()).isEqualTo("0700000000");
        assertThat(response.address()).isEqualTo("123 Main St");
        assertThat(response.city()).isEqualTo("Nairobi");
        verify(organizationRepository).save(any(Organization.class));
        verify(eventPublisher).publishEvent(any(OrganizationCreatedEvent.class));
    }

    @Test
    void createRejectsDuplicateName() {
        when(organizationRepository.existsByName("Acme Ltd")).thenReturn(true);

        assertThatThrownBy(() -> organizationService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Acme Ltd");
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void listWithoutFilterReturnsAllOrganizations() {
        Pageable pageable = PageRequest.of(0, 10);
        Organization existing = organizationMapper.toEntity(request);
        when(organizationRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(existing)));

        Page<OrganizationResponse> result = organizationService.list(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(organizationRepository, never()).findByOrganizationType(any(), any());
    }

    @Test
    void listFiltersByOrganizationType() {
        Pageable pageable = PageRequest.of(0, 10);
        Organization existing = organizationMapper.toEntity(request);
        when(organizationRepository.findByOrganizationType(OrganizationType.INSURER, pageable))
                .thenReturn(new PageImpl<>(List.of(existing)));

        Page<OrganizationResponse> result = organizationService.list(OrganizationType.INSURER, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).organizationType()).isEqualTo(OrganizationType.INSURER);
        verify(organizationRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesChanges() {
        UUID id = UUID.randomUUID();
        Organization existing = organizationMapper.toEntity(request);
        when(organizationRepository.findById(id)).thenReturn(Optional.of(existing));
        OrganizationRequest updateRequest =
                new OrganizationRequest("Beta Ltd", OrganizationType.SERVICE_PROVIDER, "info@beta.com",
                        "0711111111", "456 Side St", "Mombasa", null, null, null, null, null, null, null);
        when(organizationRepository.existsByNameAndIdNot("Beta Ltd", id)).thenReturn(false);
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response = organizationService.update(id, updateRequest);

        assertThat(response.name()).isEqualTo("Beta Ltd");
        assertThat(response.city()).isEqualTo("Mombasa");
    }

    @Test
    void updateRejectsNameAlreadyUsedByAnotherOrganization() {
        UUID id = UUID.randomUUID();
        Organization existing = organizationMapper.toEntity(request);
        when(organizationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(organizationRepository.existsByNameAndIdNot("Beta Ltd", id)).thenReturn(true);

        assertThatThrownBy(() -> organizationService.update(id,
                new OrganizationRequest("Beta Ltd", OrganizationType.SERVICE_PROVIDER, "info@beta.com",
                        null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(IllegalStateException.class);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void deleteRemovesEntity() {
        UUID id = UUID.randomUUID();
        Organization existing = organizationMapper.toEntity(request);
        when(organizationRepository.findById(id)).thenReturn(Optional.of(existing));

        organizationService.delete(id);

        verify(organizationRepository).delete(existing);
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(organizationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(organizationRepository, never()).delete(any());
    }
}
