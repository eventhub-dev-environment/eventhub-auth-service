package com.eventhub.auth_service.infrastructure.adapter.in.web;

import com.eventhub.auth_service.application.dto.AuthResponse;
import com.eventhub.auth_service.application.dto.GrantTenantRolesRequest;
import com.eventhub.auth_service.application.dto.LoginRequest;
import com.eventhub.auth_service.application.dto.RegisterRequest;
import com.eventhub.auth_service.application.service.AuthService;
import com.eventhub.auth_service.domain.model.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints para registro e inicio de sesión de usuarios")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Registrar un nuevo usuario")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o email ya existente")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener Bearer JWT Token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PutMapping("/admin/users/{userId}/tenant-roles")
    public ResponseEntity<Void> grantTenantRoles(
            @PathVariable String userId,
            @Valid @RequestBody GrantTenantRolesRequest request,
            @RequestHeader("X-User-Roles") String userRoles) {
        requirePlatformAdmin(userRoles);
        authService.assignTenantRoles(userId, request.getTenantId(), request.getRoles());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/users/{userId}/tenant-roles/{tenantId}")
    public ResponseEntity<Void> removeTenantRoles(
            @PathVariable String userId,
            @PathVariable String tenantId,
            @RequestHeader("X-User-Roles") String userRoles) {
        requirePlatformAdmin(userRoles);

        authService.removeTenantRoles(userId, tenantId);
        return ResponseEntity.noContent().build();
    }

    private void requirePlatformAdmin(String userRoles) {
        Set<String> roles = Arrays.stream(userRoles.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        if (!roles.contains(Role.PLATFORM_ADMIN.name())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere PLATFORM_ADMIN");
        }
    }
}