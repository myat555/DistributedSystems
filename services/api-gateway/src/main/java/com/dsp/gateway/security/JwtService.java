package com.dsp.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and validates the HS256 JWTs used at the gateway edge.
 * Shared by {@code AuthController} (issuing) and {@code JwtAuthenticationFilter} (validating).
 */
@Service
public class JwtService {

    private static final Duration TOKEN_VALIDITY = Duration.ofHours(1);

    private final SecretKey secretKey;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_VALIDITY)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validates the token's signature and expiry and returns its subject.
     * Throws {@link io.jsonwebtoken.JwtException} or {@link IllegalArgumentException}
     * if the token is missing, malformed, expired, or has a bad signature.
     */
    public String parseSubject(String token) {
        Jws<Claims> claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
        return claims.getPayload().getSubject();
    }
}
