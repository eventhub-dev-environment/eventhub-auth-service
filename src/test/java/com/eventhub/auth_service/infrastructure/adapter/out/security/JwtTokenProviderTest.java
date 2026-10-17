package com.eventhub.auth_service.infrastructure.adapter.out.security;

import com.eventhub.auth_service.domain.model.Role;
import com.eventhub.auth_service.domain.model.TenantRole;
import com.eventhub.auth_service.domain.model.TenantRoleAssignment;
import com.eventhub.auth_service.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    @Test
    void generateTokenIncludesUserIdClaim() {
        String secret = Base64.getEncoder().encodeToString(
                "test-secret-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
        JwtTokenProvider tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "secret", secret);
        ReflectionTestUtils.setField(tokenProvider, "expirationMs", 60_000L);

        User user = User.builder()
                .id("user-123")
                .email("user@example.com")
                .roles(Set.of(Role.CUSTOMER))
                .tenantRoles(Set.of(TenantRoleAssignment.builder()
                        .tenantId("tenant-123")
                        .role(TenantRole.EVENT_ORGANIZER)
                        .build(), TenantRoleAssignment.builder()
                        .tenantId("tenant-123")
                        .role(TenantRole.TENANT_ADMIN)
                        .build(), TenantRoleAssignment.builder()
                        .tenantId("tenant-456")
                        .role(TenantRole.TENANT_ADMIN)
                        .build()))
                .enabled(true)
                .build();

        String token = tokenProvider.generateToken(user);
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("userId", String.class)).isEqualTo("user-123");
        assertThat(claims.get("tenantId")).isNull();
        assertThat((Object) claims.get("roles")).asList().containsExactly("CUSTOMER");
        assertThat((Object) claims.get("tenantMemberships"))
                .asList()
                .containsExactly(
                        Map.of("tenantId", "tenant-123", "roles", List.of("EVENT_ORGANIZER", "TENANT_ADMIN")),
                        Map.of("tenantId", "tenant-456", "roles", List.of("TENANT_ADMIN")));
        assertThat(tokenProvider.matchesCurrentAuthorization(claims, user)).isTrue();
    }

    @Test
    void rejectsTokenWithTenantRolesThatWereRevoked() {
        String secret = Base64.getEncoder().encodeToString(
                "test-secret-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
        JwtTokenProvider tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "secret", secret);
        ReflectionTestUtils.setField(tokenProvider, "expirationMs", 60_000L);

        User user = User.builder()
                .id("user-123")
                .email("user@example.com")
                .roles(Set.of(Role.CUSTOMER))
                .tenantRoles(Set.of(TenantRoleAssignment.builder()
                        .tenantId("tenant-123")
                        .role(TenantRole.EVENT_ORGANIZER)
                        .build()))
                .enabled(true)
                .build();
        Claims claims = tokenProvider.parseClaims(tokenProvider.generateToken(user));
        user.setTenantRoles(Set.of());

        assertThat(tokenProvider.matchesCurrentAuthorization(claims, user)).isFalse();
    }

    @Test
    void rejectsTokenWhenUserHasBeenDisabled() {
        String secret = Base64.getEncoder().encodeToString(
                "test-secret-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
        JwtTokenProvider tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "secret", secret);
        ReflectionTestUtils.setField(tokenProvider, "expirationMs", 60_000L);

        User user = User.builder()
                .id("user-123")
                .email("user@example.com")
                .roles(Set.of(Role.CUSTOMER))
                .tenantRoles(Set.of())
                .enabled(true)
                .build();
        Claims claims = tokenProvider.parseClaims(tokenProvider.generateToken(user));
        user.setEnabled(false);

        assertThat(tokenProvider.matchesCurrentAuthorization(claims, user)).isFalse();
    }
}
