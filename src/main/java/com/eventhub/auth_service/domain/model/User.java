package com.eventhub.auth_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String tenantId;
    private Set<Role> roles;
    private boolean enabled;
}