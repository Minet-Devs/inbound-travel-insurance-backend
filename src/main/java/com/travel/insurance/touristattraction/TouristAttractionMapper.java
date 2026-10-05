package com.travel.insurance.touristattraction;

import com.travel.insurance.touristattraction.dto.TouristAttractionRequest;
import com.travel.insurance.touristattraction.dto.TouristAttractionResponse;
import org.springframework.stereotype.Component;

@Component
public class TouristAttractionMapper {

    public TouristAttraction toEntity(TouristAttractionRequest request) {
        TouristAttraction attraction = new TouristAttraction();
        updateEntity(attraction, request);
        return attraction;
    }

    public void updateEntity(TouristAttraction attraction, TouristAttractionRequest request) {
        attraction.setName(request.name().trim());
        attraction.setCounty(request.county().trim());
    }

    public TouristAttractionResponse toResponse(TouristAttraction attraction) {
        return new TouristAttractionResponse(
                attraction.getId(),
                attraction.getName(),
                attraction.getCounty(),
                attraction.getCreatedDate(),
                attraction.getUpdatedDate()
        );
    }
}
