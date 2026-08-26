package com.restaurante.security;

import com.restaurante.config.JwtProperties;
import com.restaurante.domain.model.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final JwtProperties properties;
    private SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validateConfiguration() {
        if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()
            || properties.getSecretKey().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("SECRET_KEY_JWT must contain at least 32 characters");
        }
        if (properties.getExpirationTime() <= 0) {
            throw new IllegalStateException("EXPIRATION_TIME_JWT must be greater than zero");
        }
        signingKey = Keys.hmacShaKeyFor(properties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(UserAccount user) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(properties.getExpirationTime());
        return Jwts.builder()
            .subject(user.getEmail())
            .claim("uid", user.getId())
            .claim("role", user.getRole().getName().name())
            .claim("tv", user.getTokenVersion())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(signingKey)
            .compact();
    }

    public Optional<JwtData> parse(String token) {
        if (signingKey == null || token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            String email = claims.getSubject();
            Number userId = claims.get("uid", Number.class);
            String roleClaim = claims.get("role", String.class);
            Number tokenVersion = claims.get("tv", Number.class);
            if (email == null || userId == null || roleClaim == null || tokenVersion == null) {
                return Optional.empty();
            }
            return Optional.of(new JwtData(email, userId.longValue(),
                com.restaurante.domain.model.RoleName.valueOf(roleClaim), tokenVersion.intValue()));
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public long expirationTime() {
        return properties.getExpirationTime();
    }
}
