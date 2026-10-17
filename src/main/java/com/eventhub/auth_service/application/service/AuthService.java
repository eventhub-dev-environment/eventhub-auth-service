package com.eventhub.auth_service.application.service;

import com.eventhub.auth_service.application.dto.AuthResponse;
import com.eventhub.auth_service.application.dto.LoginRequest;
import com.eventhub.auth_service.application.dto.RegisterRequest;
import com.eventhub.auth_service.domain.model.Role;
import com.eventhub.auth_service.domain.model.TenantRole;
import com.eventhub.auth_service.domain.model.TenantRoleAssignment;
import com.eventhub.auth_service.domain.exception.InvalidCredentialsException;
import com.eventhub.auth_service.domain.model.User;
import com.eventhub.auth_service.domain.port.out.UserRepositoryPort;
import com.eventhub.auth_service.infrastructure.adapter.out.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse register(RegisterRequest request) {
        if (userRepositoryPort.existsByEmail(request.getEmail())) {
            throw new InvalidCredentialsException("El email ya se encuentra registrado");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .roles(Set.of(Role.CUSTOMER))
                .tenantRoles(Set.of())
                .enabled(true)
                .build();

        User savedUser = userRepositoryPort.save(user);
        return toAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepositoryPort.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        return toAuthResponse(user);
    }

    public void assignTenantRoles(String userId, String tenantId, Set<TenantRole> roles) {
        if (tenantId == null || tenantId.isBlank() || roles == null || roles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tenant y al menos un rol son obligatorios");
        }

        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Set<TenantRoleAssignment> tenantRoles = user.getTenantRoles() == null
                ? new HashSet<>()
                : new HashSet<>(user.getTenantRoles());
        roles.stream()
                .map(role -> TenantRoleAssignment.builder().tenantId(tenantId).role(role).build())
                .forEach(tenantRoles::add);

        user.setTenantRoles(tenantRoles);
        userRepositoryPort.save(user);
    }

    public void removeTenantRoles(String userId, String tenantId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Set<TenantRoleAssignment> tenantRoles = user.getTenantRoles() == null
                ? new HashSet<>()
                : new HashSet<>(user.getTenantRoles());
        if (tenantRoles.removeIf(assignment -> tenantId.equals(assignment.getTenantId()))) {
            user.setTenantRoles(tenantRoles);
            userRepositoryPort.save(user);
        }
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.builder()
                .token(jwtTokenProvider.generateToken(user))
                .type("Bearer")
                .email(user.getEmail())
                .roles(user.getRoles())
                .tenantRoles(user.getTenantRoles())
                .build();
    }
}