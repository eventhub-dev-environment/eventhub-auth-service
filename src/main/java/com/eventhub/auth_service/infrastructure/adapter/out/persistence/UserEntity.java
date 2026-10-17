package com.eventhub.auth_service.infrastructure.adapter.out.persistence;

import com.eventhub.auth_service.domain.model.Role;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private boolean enabled;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_global_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "global_role", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<Role> globalRoles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_tenant_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<TenantRoleEntity> tenantRoles = new HashSet<>();
}