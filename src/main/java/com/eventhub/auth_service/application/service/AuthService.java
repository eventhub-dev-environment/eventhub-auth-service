package com.eventhub.auth_service.application.service;

import com.eventhub.auth_service.application.dto.AuthResponse;
import com.eventhub.auth_service.application.dto.LoginRequest;
import com.eventhub.auth_service.application.dto.RegisterRequest;
import com.eventhub.auth_service.domain.exception.InvalidCredentialsException;
import com.eventhub.auth_service.domain.model.User;
import com.eventhub.auth_service.domain.port.out.UserRepositoryPort;
import com.eventhub.auth_service.infrastructure.adapter.out.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
                .tenantId(request.getTenantId())
                .roles(request.getRoles())
                .enabled(true)
                .build();

        User savedUser = userRepositoryPort.save(user);
        String token = jwtTokenProvider.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .email(savedUser.getEmail())
                .tenantId(savedUser.getTenantId())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepositoryPort.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }

        String token = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .email(user.getEmail())
                .tenantId(user.getTenantId())
                .build();
    }
}