package com.eventhub.auth_service.application.service;

import com.eventhub.auth_service.application.dto.RegisterRequest;
import com.eventhub.auth_service.domain.model.Role;
import com.eventhub.auth_service.domain.model.TenantRole;
import com.eventhub.auth_service.domain.model.TenantRoleAssignment;
import com.eventhub.auth_service.domain.model.User;
import com.eventhub.auth_service.domain.port.out.UserRepositoryPort;
import com.eventhub.auth_service.infrastructure.adapter.out.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @InjectMocks
    private AuthService authService;

    @Test
    void publicRegistrationAlwaysCreatesCustomerWithoutTenantRoles() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("customer@example.com");
        request.setPassword("password123");
        request.setFirstName("First");
        request.setLastName("Last");
        when(userRepositoryPort.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn("signed-token");

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRoles()).containsExactly(Role.CUSTOMER);
        assertThat(userCaptor.getValue().getTenantRoles()).isEmpty();
    }

    @Test
    void platformAdminGrantAddsRolesOnlyToTheSelectedTenant() {
        User user = User.builder()
                .id("user-123")
                .email("organizer@example.com")
                .roles(Set.of(Role.CUSTOMER))
                .tenantRoles(Set.of())
                .enabled(true)
                .build();
        when(userRepositoryPort.findById("user-123")).thenReturn(Optional.of(user));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.assignTenantRoles("user-123", "tenant-a", Set.of(TenantRole.EVENT_ORGANIZER));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getTenantRoles())
                .containsExactly(TenantRoleAssignment.builder()
                        .tenantId("tenant-a")
                        .role(TenantRole.EVENT_ORGANIZER)
                        .build());
        assertThat(userCaptor.getValue().getRoles()).containsExactly(Role.CUSTOMER);
    }

    @Test
    void tenantRoleRemovalLeavesOtherTenantMembershipsIntact() {
        User user = User.builder()
                .id("user-123")
                .email("organizer@example.com")
                .roles(Set.of(Role.CUSTOMER))
                .tenantRoles(Set.of(
                        TenantRoleAssignment.builder()
                                .tenantId("tenant-a")
                                .role(TenantRole.EVENT_ORGANIZER)
                                .build(),
                        TenantRoleAssignment.builder()
                                .tenantId("tenant-b")
                                .role(TenantRole.TENANT_ADMIN)
                                .build()))
                .enabled(true)
                .build();
        when(userRepositoryPort.findById("user-123")).thenReturn(Optional.of(user));
        when(userRepositoryPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.removeTenantRoles("user-123", "tenant-a");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepositoryPort).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getTenantRoles())
                .containsExactly(TenantRoleAssignment.builder()
                        .tenantId("tenant-b")
                        .role(TenantRole.TENANT_ADMIN)
                        .build());
    }
}
