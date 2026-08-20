package com.travel.insurance.organization;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.organization.dto.OrganizationRequest;
import com.travel.insurance.organization.dto.OrganizationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private final OrganizationMapper organizationMapper = new OrganizationMapper();

    private OrganizationServiceImpl organizationService;

    private OrganizationRequest request;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationServiceImpl(organizationRepository, organizationMapper);
        request = new OrganizationRequest("Acme Ltd", "contact@acme.com", "0700000000", "123 Main St", "Nairobi");
    }

    @Test
    void createSavesAndReturnsOrganization() {
        when(organizationRepository.existsByName("Acme Ltd")).thenReturn(false);
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response = organizationService.create(request);

        assertThat(response.name()).isEqualTo("Acme Ltd");
        assertThat(response.email()).isEqualTo("contact@acme.com");
        assertThat(response.phoneNumber()).isEqualTo("0700000000");
        assertThat(response.address()).isEqualTo("123 Main St");
        assertThat(response.city()).isEqualTo("Nairobi");
        verify(organizationRepository).save(any(Organization.class));
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
                new OrganizationRequest("Beta Ltd", "info@beta.com", "0711111111", "456 Side St", "Mombasa");
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
                new OrganizationRequest("Beta Ltd", "info@beta.com", null, null, null)))
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
