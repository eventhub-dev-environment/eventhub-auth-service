package com.eventhub.auth_service.application.dto;

import com.eventhub.auth_service.domain.model.TenantRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class GrantTenantRolesRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-z0-9-]+$")
    private String tenantId;

    @NotEmpty
    private Set<@NotNull TenantRole> roles;
}
