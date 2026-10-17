package com.eventhub.auth_service.infrastructure.adapter.out.persistence;

import com.eventhub.auth_service.domain.model.TenantRole;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantRoleEntity {

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_role", nullable = false)
    private TenantRole role;
}
