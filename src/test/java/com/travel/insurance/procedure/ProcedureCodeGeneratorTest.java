package com.travel.insurance.procedure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcedureCodeGeneratorTest {

    @Mock
    private ProcedureRepository procedureRepository;

    @InjectMocks
    private ProcedureCodeGenerator codeGenerator;

    @Test
    void formatsSequenceValueAsZeroPaddedCode() {
        when(procedureRepository.nextProcedureCodeValue()).thenReturn(1L);
        assertThat(codeGenerator.next()).isEqualTo("PRC-0001");
    }

    @Test
    void formatsLargerSequenceValues() {
        assertThat(ProcedureCodeGenerator.format(42L)).isEqualTo("PRC-0042");
        assertThat(ProcedureCodeGenerator.format(12345L)).isEqualTo("PRC-12345");
    }
}
