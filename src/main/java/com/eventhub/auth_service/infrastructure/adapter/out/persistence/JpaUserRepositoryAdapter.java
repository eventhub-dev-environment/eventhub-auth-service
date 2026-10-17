package com.eventhub.auth_service.infrastructure.adapter.out.persistence;

import com.eventhub.auth_service.domain.model.Role;
import com.eventhub.auth_service.domain.model.TenantRoleAssignment;
import com.eventhub.auth_service.domain.model.User;
import com.eventhub.auth_service.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataJpaUserRepository jpaRepository;

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    private UserEntity toEntity(User user) {
        Set<Role> roles = user.getRoles() == null || user.getRoles().isEmpty()
                ? Set.of(Role.CUSTOMER)
                : user.getRoles();

        return UserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(roles.iterator().next().name())
                .enabled(user.isEnabled())
                .globalRoles(new HashSet<>(roles))
                .tenantRoles(user.getTenantRoles() == null
                        ? new HashSet<>()
                        : user.getTenantRoles().stream()
                                .map(tenantRole -> new TenantRoleEntity(
                                        tenantRole.getTenantId(),
                                        tenantRole.getRole()))
                                .collect(Collectors.toCollection(HashSet::new)))
                .build();
    }

    private User toDomain(UserEntity entity) {
        Set<Role> roles = entity.getGlobalRoles() == null || entity.getGlobalRoles().isEmpty()
                ? Set.of(Role.CUSTOMER)
                : new HashSet<>(entity.getGlobalRoles());

        Set<TenantRoleAssignment> tenantRoles = entity.getTenantRoles() == null
                ? Set.of()
                : entity.getTenantRoles().stream()
                        .map(tenantRole -> TenantRoleAssignment.builder()
                                .tenantId(tenantRole.getTenantId())
                                .role(tenantRole.getRole())
                                .build())
                        .collect(Collectors.toSet());

        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .roles(roles)
                .tenantRoles(tenantRoles)
                .enabled(entity.isEnabled())
                .build();
    }
}