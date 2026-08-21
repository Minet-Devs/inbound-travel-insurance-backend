package com.travel.insurance.user;

import com.travel.insurance.user.dto.UserRequest;
import com.travel.insurance.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

    UserResponse create(UserRequest request);

    UserResponse getById(UUID id);

    Page<UserResponse> list(Pageable pageable);

    UserResponse update(UUID id, UserRequest request);

    void delete(UUID id);

    Optional<User> findEntityByEmail(String email);

    User getEntityById(UUID id);

    String organizationName(User user);
}
