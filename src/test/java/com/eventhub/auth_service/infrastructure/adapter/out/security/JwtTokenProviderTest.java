package com.eventhub.auth_service.infrastructure.adapter.out.security;

import com.eventhub.auth_service.domain.model.Role;
import com.eventhub.auth_service.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
                .tenantId("tenant-123")
                .roles(Set.of(Role.CUSTOMER))
                .build();

        String token = tokenProvider.generateToken(user);
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("userId", String.class)).isEqualTo("user-123");
    }
}
