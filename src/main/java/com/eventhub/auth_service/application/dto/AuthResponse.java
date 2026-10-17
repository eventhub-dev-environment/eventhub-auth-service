package com.eventhub.auth_service.application.dto;

import com.eventhub.auth_service.domain.model.Role;
import com.eventhub.auth_service.domain.model.TenantRoleAssignment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private String email;
    private Set<Role> roles;
    private Set<TenantRoleAssignment> tenantRoles;
}