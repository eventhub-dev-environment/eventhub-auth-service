package com.eventhub.auth_service.infrastructure.adapter.out.persistence;

import com.eventhub.auth_service.domain.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class UserDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;
    private String firstName;
    private String lastName;
    private String tenantId;
    private Set<Role> roles;
    private boolean enabled;
}