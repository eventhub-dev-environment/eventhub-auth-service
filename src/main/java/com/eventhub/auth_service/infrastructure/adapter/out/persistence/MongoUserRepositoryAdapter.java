package com.eventhub.auth_service.infrastructure.adapter.out.persistence;

import com.eventhub.auth_service.domain.model.User;
import com.eventhub.auth_service.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MongoUserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataMongoUserRepository mongoRepository;

    @Override
    public User save(User user) {
        UserDocument doc = toDocument(user);
        UserDocument saved = mongoRepository.save(doc);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return mongoRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return mongoRepository.existsByEmail(email);
    }

    private UserDocument toDocument(User user) {
        return UserDocument.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .tenantId(user.getTenantId())
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .build();
    }

    private User toDomain(UserDocument doc) {
        return User.builder()
                .id(doc.getId())
                .email(doc.getEmail())
                .password(doc.getPassword())
                .firstName(doc.getFirstName())
                .lastName(doc.getLastName())
                .tenantId(doc.getTenantId())
                .roles(doc.getRoles())
                .enabled(doc.isEnabled())
                .build();
    }
}