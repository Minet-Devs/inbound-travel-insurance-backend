package com.travel.insurance.icd11;

import com.travel.insurance.icd11.dto.Icd11CodeResponse;
import org.springframework.stereotype.Component;

@Component
public class Icd11CodeMapper {

    public Icd11CodeResponse toResponse(Icd11Code code) {
        return new Icd11CodeResponse(
                code.getId(),
                code.getCode(),
                code.getTitle(),
                code.getCreatedDate(),
                code.getUpdatedDate()
        );
    }
}
