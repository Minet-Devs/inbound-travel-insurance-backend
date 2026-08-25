package com.travel.insurance.user;

import com.travel.insurance.user.dto.UserRequest;
import com.travel.insurance.user.dto.UserResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserMapper {

    public User toEntity(UserRequest request) {
        User user = new User();
        applyRequest(user, request);
        return user;
    }

    public void updateEntity(User user, UserRequest request) {
        applyRequest(user, request);
    }

    public UserResponse toResponse(User user, String organizationName, UUID serviceProviderId, UUID insurerId) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getOrganizationId(),
                organizationName,
                serviceProviderId,
                insurerId,
                user.getCreatedDate(),
                user.getUpdatedDate()
        );
    }

    private void applyRequest(User user, UserRequest request) {
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setRole(request.role());
        user.setOrganizationId(request.organizationId());
    }
}
