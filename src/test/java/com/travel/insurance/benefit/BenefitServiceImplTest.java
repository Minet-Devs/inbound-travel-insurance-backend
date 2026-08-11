package com.travel.insurance.benefit;

import com.travel.insurance.benefit.dto.BenefitRequest;
import com.travel.insurance.benefit.dto.BenefitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenefitServiceImplTest {

    @Mock
    private BenefitRepository benefitRepository;

    private final BenefitMapper benefitMapper = new BenefitMapper();

    private BenefitServiceImpl benefitService;

    @BeforeEach
    void setUp() {
        benefitService = new BenefitServiceImpl(benefitRepository, benefitMapper);
    }

    @Test
    void createSavesBenefitWithNameAndLimit() {
        when(benefitRepository.save(any(Benefit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BenefitResponse response = benefitService.create(
                new BenefitRequest("Medical Expenses", new BigDecimal("20000.00")));

        assertThat(response.benefitName()).isEqualTo("Medical Expenses");
        assertThat(response.limitAmount()).isEqualByComparingTo("20000.00");
        verify(benefitRepository).save(any(Benefit.class));
    }

    @Test
    void updateAppliesNameAndLimit() {
        UUID id = UUID.randomUUID();
        Benefit existing = new Benefit();
        existing.setBenefitName("Old");
        existing.setLimitAmount(new BigDecimal("100.00"));
        when(benefitRepository.findById(id)).thenReturn(Optional.of(existing));
        when(benefitRepository.save(any(Benefit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BenefitResponse response = benefitService.update(
                id, new BenefitRequest("Mental Illness", new BigDecimal("1000.00")));

        assertThat(response.benefitName()).isEqualTo("Mental Illness");
        assertThat(response.limitAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void namesByIdsResolvesNames() {
        UUID id = UUID.randomUUID();
        Benefit benefit = new Benefit();
        benefit.setId(id);
        benefit.setBenefitName("Prescribed Medicines");
        when(benefitRepository.findAllById(Set.of(id))).thenReturn(List.of(benefit));

        Map<UUID, String> names = benefitService.namesByIds(Set.of(id));

        assertThat(names).containsEntry(id, "Prescribed Medicines");
    }

    @Test
    void namesByIdsReturnsEmptyForEmptyInput() {
        assertThat(benefitService.namesByIds(Set.of())).isEmpty();
    }
}
