package com.eventhub.auth_service.application.dto;

import com.eventhub.auth_service.domain.model.TenantRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class GrantTenantRolesRequest {

    @NotBlank
    private String tenantId;

    @NotEmpty
    private Set<TenantRole> roles;
}
