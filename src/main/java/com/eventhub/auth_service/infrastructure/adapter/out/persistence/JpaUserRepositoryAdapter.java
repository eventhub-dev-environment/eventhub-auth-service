package com.eventhub.auth_service.infrastructure.adapter.out.persistence;

import com.eventhub.auth_service.domain.model.Role;
import com.eventhub.auth_service.domain.model.User;
import com.eventhub.auth_service.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
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
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    private UserEntity toEntity(User user) {
        String roleStr = "ROLE_USER";
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            // Si user.getRoles() retorna una colección de Role o un solo Role
            Object firstRole = user.getRoles().iterator().next();
            roleStr = firstRole.toString(); // Usa .name() o .toString() según si Role es Enum u Objeto
        }

        return UserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .role(roleStr)
                .enabled(user.isEnabled())
                .build();
    }

    private User toDomain(UserEntity entity) {
        // Convertimos la cadena almacenada en BD al tipo de Dominio Role
        Role role = Role.valueOf(entity.getRole());

        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .roles(Collections.singleton(role)) // O Set.of(role) / Role según el campo exacto en tu User.java
                .enabled(entity.isEnabled())
                .build();
    }
}