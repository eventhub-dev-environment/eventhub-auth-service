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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Objects;
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

    public boolean isAccessTokenCurrent(String token) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parseClaims(token);
        } catch (JwtException exception) {
            return false;
        }
        String userId;
        try {
            userId = claims.get("userId", String.class);
        } catch (JwtException exception) {
            return false;
        }
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return userRepositoryPort.findById(userId)
                .map(user -> {
                    try {
                        return jwtTokenProvider.matchesCurrentAuthorization(claims, user);
                    } catch (JwtException exception) {
                        return false;
                    }
                })
                .orElse(false);
    }

    public void assignTenantRoles(String userId, String tenantId, Set<TenantRole> roles) {
        if (!isValidTenantId(tenantId) || roles == null || roles.isEmpty()
                || roles.stream().anyMatch(Objects::isNull)) {
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
        if (!isValidTenantId(tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tenantId no es válido");
        }
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

    private boolean isValidTenantId(String tenantId) {
        return tenantId != null && tenantId.matches("^[a-z0-9-]{3,50}$");
    }
}