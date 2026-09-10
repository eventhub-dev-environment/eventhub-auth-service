package com.eventhub.auth_service.application.dto;

import com.eventhub.auth_service.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class RegisterRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Schema(example = "admin@eventhub.com")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Schema(example = "Password123!")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    @Schema(example = "Juan Pablo")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Schema(example = "Ospina")
    private String lastName;

    @NotBlank(message = "El tenantId es obligatorio")
    @Schema(example = "carrera-tulua-2026")
    private String tenantId;

    @NotEmpty(message = "Debe asignar al menos un rol")
    private Set<Role> roles;
}