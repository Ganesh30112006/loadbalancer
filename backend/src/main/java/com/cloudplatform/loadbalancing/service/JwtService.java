package com.cloudplatform.loadbalancing.service;

import com.cloudplatform.loadbalancing.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationHours;

    public JwtService(
            @Value("${jwt.secret:your-256-bit-secret-key-for-jwt-signing-replace-in-production}") String secret,
            @Value("${jwt.expiration-hours:24}") long expirationHours) {
        // Ensure the secret is at least 256 bits (32 bytes) for HS256
        String paddedSecret = secret;
        while (paddedSecret.length() < 32) {
            paddedSecret += paddedSecret;
        }
        this.secretKey = Keys.hmacShaKeyFor(paddedSecret.substring(0, 64).getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    /**
     * Generate a JWT token for the given user
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getUsername())
                .claim("userId", user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validate and parse a JWT token
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return validateToken(token).getSubject();
    }

    /**
     * Extract user ID from token
     */
    public UUID extractUserId(String token) {
        String userId = validateToken(token).get("userId", String.class);
        return UUID.fromString(userId);
    }

    /**
     * Extract role from token
     */
    public User.Role extractRole(String token) {
        String role = validateToken(token).get("role", String.class);
        return User.Role.valueOf(role);
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * Refresh a token if it's still valid
     */
    public String refreshToken(String token, User user) {
        if (!isTokenExpired(token)) {
            return generateToken(user);
        }
        throw new JwtException("Cannot refresh expired token");
    }
}
