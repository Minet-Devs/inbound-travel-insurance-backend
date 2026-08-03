package com.travel.insurance.insurer;

import com.travel.insurance.insurer.dto.InsurerRequest;
import com.travel.insurance.insurer.dto.InsurerResponse;
import org.springframework.stereotype.Component;

@Component
public class InsurerMapper {

    public Insurer toEntity(InsurerRequest request) {
        Insurer insurer = new Insurer();
        updateEntity(insurer, request);
        return insurer;
    }

    public void updateEntity(Insurer insurer, InsurerRequest request) {
        insurer.setName(request.name());
        insurer.setContactEmail(request.contactEmail());
        insurer.setContactPhone(request.contactPhone());
        insurer.setAddress(request.address());
    }

    public InsurerResponse toResponse(Insurer insurer) {
        return new InsurerResponse(
                insurer.getId(),
                insurer.getName(),
                insurer.getContactEmail(),
                insurer.getContactPhone(),
                insurer.getAddress(),
                insurer.getCreatedDate(),
                insurer.getUpdatedDate()
        );
    }
}
