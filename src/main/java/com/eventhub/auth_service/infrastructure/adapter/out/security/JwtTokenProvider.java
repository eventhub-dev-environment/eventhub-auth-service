package com.eventhub.auth_service.infrastructure.adapter.out.security;

import com.eventhub.auth_service.domain.model.User;
import com.eventhub.auth_service.domain.model.TenantRoleAssignment;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("roles", user.getRoles().stream().map(Enum::name).sorted().toList());
        Map<String, TreeSet<String>> rolesByTenant = user.getTenantRoles().stream()
                .collect(Collectors.groupingBy(
                        TenantRoleAssignment::getTenantId,
                        TreeMap::new,
                        Collectors.mapping(
                                assignment -> assignment.getRole().name(),
                                Collectors.toCollection(TreeSet::new))));
        claims.put("tenantMemberships", rolesByTenant.entrySet().stream()
                .map(entry -> Map.of("tenantId", entry.getKey(), "roles", entry.getValue()))
                .toList());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }
}