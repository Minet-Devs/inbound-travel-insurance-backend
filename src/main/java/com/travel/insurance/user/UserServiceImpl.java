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
import java.util.Objects;
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
    private final OrganizationService organizationService;
    private final ServiceProviderService serviceProviderService;
    private final InsurerService insurerService;

    @Override
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already in use: " + request.email());
        }
        organizationService.getEntityById(request.organizationId());
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
        Set<UUID> organizationIds = page.getContent().stream()
                .map(User::getOrganizationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> organizationNames = organizationService.namesByIds(organizationIds);
        return page.map(user -> userMapper.toResponse(user, organizationNames.get(user.getOrganizationId()),
                serviceProviderId(user), insurerId(user)));
    }

    @Override
    public UserResponse update(UUID id, UserRequest request) {
        User user = getEntityById(id);
        organizationService.getEntityById(request.organizationId());
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
        return organizationService.namesByIds(List.of(organizationId)).get(organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID serviceProviderId(User user) {
        if (user.getRole() != Role.PROVIDER_USER || user.getOrganizationId() == null) {
            return null;
        }
        return serviceProviderService.findIdByOrganizationId(user.getOrganizationId()).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID insurerId(User user) {
        if (user.getRole() != Role.INSURER_USER || user.getOrganizationId() == null) {
            return null;
        }
        return insurerService.findIdByOrganizationId(user.getOrganizationId()).orElse(null);
    }

    private UserResponse toResponse(User user) {
        return userMapper.toResponse(user, organizationName(user), serviceProviderId(user), insurerId(user));
    }
}
