package com.travel.insurance.user;

import com.travel.insurance.common.exception.ResourceNotFoundException;
import com.travel.insurance.organization.OrganizationService;
import com.travel.insurance.user.dto.UserRequest;
import com.travel.insurance.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OrganizationService organizationService;

    private final UserMapper userMapper = new UserMapper();

    private UserServiceImpl userService;

    private UserRequest request;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userMapper, passwordEncoder, organizationService);
        request = new UserRequest("Jane", "Doe", "jane@acme.com", "password123", "0700000000",
                Role.INSURER_USER, UUID.randomUUID());
        lenient().when(organizationService.namesByIds(any())).thenReturn(Map.of());
    }

    @Test
    void createSavesAndReturnsUserWithSingleRole() {
        when(userRepository.existsByEmail("jane@acme.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(request);

        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.role()).isEqualTo(Role.INSURER_USER);
        assertThat(response.organizationId()).isEqualTo(request.organizationId());
        verify(userRepository).save(any(User.class));
        verify(organizationService).getEntityById(request.organizationId());
    }

    @Test
    void createResolvesOrganizationName() {
        when(userRepository.existsByEmail("jane@acme.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationService.namesByIds(List.of(request.organizationId())))
                .thenReturn(Map.of(request.organizationId(), "Minet HQ"));

        UserResponse response = userService.create(request);

        assertThat(response.organizationName()).isEqualTo("Minet HQ");
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("jane@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jane@acme.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void listReturnsAllUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        User existing = userMapper.toEntity(request);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(existing)));

        Page<UserResponse> result = userService.list(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).role()).isEqualTo(Role.INSURER_USER);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateReplacesRole() {
        UUID id = UUID.randomUUID();
        User existing = userMapper.toEntity(request);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserRequest updateRequest = new UserRequest("Jane", "Doe", "jane@acme.com", "password123",
                "0700000000", Role.PROVIDER_USER, UUID.randomUUID());

        UserResponse response = userService.update(id, updateRequest);

        assertThat(response.role()).isEqualTo(Role.PROVIDER_USER);
    }

    @Test
    void deleteRemovesEntity() {
        UUID id = UUID.randomUUID();
        User existing = userMapper.toEntity(request);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        userService.delete(id);

        verify(userRepository).delete(existing);
    }
}
