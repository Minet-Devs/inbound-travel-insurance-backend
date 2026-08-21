package com.travel.insurance.user;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.insurer.InsurerService;
import com.travel.insurance.organization.OrganizationService;
import com.travel.insurance.serviceprovider.ServiceProviderService;
import com.travel.insurance.user.dto.UserRequest;
import com.travel.insurance.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final InsurerService insurerService;
    private final ServiceProviderService serviceProviderService;
    private final OrganizationService organizationService;

    @Override
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already in use: " + request.email());
        }
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return toResponse(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        Page<User> page = userRepository.findAll(pageable);
        List<User> users = page.getContent();
        Map<UUID, String> insurerNames = insurerService.namesByIds(organizationIdsForRole(users, Role.INSURER_USER));
        Map<UUID, String> providerNames =
                serviceProviderService.namesByIds(organizationIdsForRole(users, Role.PROVIDER_USER));
        Map<UUID, String> adminOrgNames = organizationService.namesByIds(organizationIdsForRole(users, Role.ADMIN));
        return page.map(user -> userMapper.toResponse(user,
                organizationNameFor(user, insurerNames, providerNames, adminOrgNames)));
    }

    @Override
    public UserResponse update(UUID id, UserRequest request) {
        User user = getEntityById(id);
        userMapper.updateEntity(user, request);
        user.setPassword(passwordEncoder.encode(request.password()));
        return toResponse(userRepository.save(user));
    }

    @Override
    public void delete(UUID id) {
        userRepository.delete(getEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findEntityByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public User getEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional(readOnly = true)
    public String organizationName(User user) {
        if (user.getOrganizationId() == null) {
            return null;
        }
        UUID organizationId = user.getOrganizationId();
        return switch (user.getRole()) {
            case INSURER_USER -> insurerService.namesByIds(List.of(organizationId)).get(organizationId);
            case PROVIDER_USER -> serviceProviderService.namesByIds(List.of(organizationId)).get(organizationId);
            case ADMIN -> organizationService.namesByIds(List.of(organizationId)).get(organizationId);
        };
    }

    private UserResponse toResponse(User user) {
        return userMapper.toResponse(user, organizationName(user));
    }

    private String organizationNameFor(User user, Map<UUID, String> insurerNames, Map<UUID, String> providerNames,
                                        Map<UUID, String> adminOrgNames) {
        if (user.getOrganizationId() == null) {
            return null;
        }
        return switch (user.getRole()) {
            case INSURER_USER -> insurerNames.get(user.getOrganizationId());
            case PROVIDER_USER -> providerNames.get(user.getOrganizationId());
            case ADMIN -> adminOrgNames.get(user.getOrganizationId());
        };
    }

    private Set<UUID> organizationIdsForRole(List<User> users, Role role) {
        return users.stream()
                .filter(user -> user.getRole() == role && user.getOrganizationId() != null)
                .map(User::getOrganizationId)
                .collect(Collectors.toSet());
    }
}
