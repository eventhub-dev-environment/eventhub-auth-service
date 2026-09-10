package com.eventhub.auth_service.domain.port.out;

import com.eventhub.auth_service.domain.model.User;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}