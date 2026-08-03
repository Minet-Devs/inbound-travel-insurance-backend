package com.travel.insurance.insurer;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.insurer.dto.InsurerResponse;
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
class InsurerServiceImplTest {

    @Mock
    private InsurerRepository insurerRepository;

    private final InsurerMapper insurerMapper = new InsurerMapper();

    private InsurerServiceImpl insurerService;

    private InsurerRequest request;

    @BeforeEach
    void setUp() {
        insurerService = new InsurerServiceImpl(insurerRepository, insurerMapper);
        request = new InsurerRequest("Acme Insurance", "contact@acme.example", "+254700000000", "Nairobi");
    }

    @Test
    void createSavesAndReturnsInsurer() {
        when(insurerRepository.existsByName("Acme Insurance")).thenReturn(false);
        when(insurerRepository.save(any(Insurer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InsurerResponse response = insurerService.create(request);

        assertThat(response.name()).isEqualTo("Acme Insurance");
        assertThat(response.contactEmail()).isEqualTo("contact@acme.example");
        verify(insurerRepository).save(any(Insurer.class));
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
    void updateAppliesChanges() {
        UUID id = UUID.randomUUID();
        Insurer existing = insurerMapper.toEntity(request);
        when(insurerRepository.findById(id)).thenReturn(Optional.of(existing));
        when(insurerRepository.save(any(Insurer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InsurerRequest update = new InsurerRequest("Acme Re", "hello@acme.example", null, null);
        InsurerResponse response = insurerService.update(id, update);

        assertThat(response.name()).isEqualTo("Acme Re");
        assertThat(response.contactEmail()).isEqualTo("hello@acme.example");
    }
}
