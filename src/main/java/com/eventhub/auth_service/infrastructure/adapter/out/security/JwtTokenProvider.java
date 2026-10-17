package com.eventhub.auth_service.infrastructure.adapter.out.security;

import com.eventhub.auth_service.domain.model.User;
import com.eventhub.auth_service.domain.model.TenantRoleAssignment;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean matchesCurrentAuthorization(Claims claims, User user) {
        if (!user.isEnabled()
                || !Objects.equals(user.getId(), claims.get("userId", String.class))
                || !Objects.equals(user.getEmail(), claims.getSubject())) {
            return false;
        }

        Set<String> tokenRoles = readStringSet(claims.get("roles"));
        Map<String, Set<String>> tokenTenantRoles = readTenantRoles(claims.get("tenantMemberships"));
        return tokenRoles != null && tokenRoles.equals(roleNames(user))
                && tokenTenantRoles != null && tokenTenantRoles.equals(tenantRoleNames(user));
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("roles", roleNames(user).stream().sorted().toList());
        Map<String, Set<String>> rolesByTenant = tenantRoleNames(user);
        claims.put("tenantMemberships", rolesByTenant.entrySet().stream()
                .map(entry -> Map.of("tenantId", entry.getKey(), "roles", entry.getValue().stream().sorted().toList()))
                .toList());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private Set<String> roleNames(User user) {
        if (user.getRoles() == null) {
            return Set.of();
        }
        return user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Map<String, Set<String>> tenantRoleNames(User user) {
        if (user.getTenantRoles() == null) {
            return Map.of();
        }
        return user.getTenantRoles().stream()
                .collect(Collectors.groupingBy(
                        TenantRoleAssignment::getTenantId,
                        TreeMap::new,
                        Collectors.mapping(
                                assignment -> assignment.getRole().name(),
                                Collectors.toCollection(TreeSet::new))));
    }

    private Set<String> readStringSet(Object claim) {
        if (!(claim instanceof List<?> values)) {
            return null;
        }
        Set<String> result = new TreeSet<>();
        for (Object value : values) {
            if (!(value instanceof String role) || role.isBlank()) {
                return null;
            }
            result.add(role);
        }
        return result;
    }

    private Map<String, Set<String>> readTenantRoles(Object claim) {
        if (!(claim instanceof List<?> memberships)) {
            return null;
        }
        Map<String, Set<String>> result = new TreeMap<>();
        for (Object value : memberships) {
            if (!(value instanceof Map<?, ?> membership)
                    || !(membership.get("tenantId") instanceof String tenantId)
                    || tenantId.isBlank()
                    || result.containsKey(tenantId)) {
                return null;
            }
            Set<String> roles = readStringSet(membership.get("roles"));
            if (roles == null || roles.isEmpty()) {
                return null;
            }
            result.put(tenantId, roles);
        }
        return result;
    }
}