package com.travel.insurance.procedure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcedureCodeGenerator {

    private final ProcedureRepository procedureRepository;

    public String next() {
        return format(procedureRepository.nextProcedureCodeValue());
    }

    static String format(long value) {
        return "PRC-%04d".formatted(value);
    }
}
