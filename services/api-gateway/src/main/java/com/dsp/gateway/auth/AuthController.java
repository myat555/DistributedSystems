package com.dsp.gateway.auth;

import com.dsp.common.web.ApiError;
import com.dsp.gateway.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Issues demo JWTs. This is the only endpoint the {@code JwtAuthenticationFilter}
 * lets through unauthenticated (along with /actuator/**).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    // DEMO ONLY — not a real user store. A real service would check a
    // user table/identity provider with hashed passwords, not a hardcoded map.
    private static final Map<String, String> DEMO_USERS = Map.of(
            "alice", "password123",
            "bob", "password123"
    );

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, ServerHttpRequest httpRequest) {
        String expectedPassword = request.username() == null ? null : DEMO_USERS.get(request.username());
        if (expectedPassword == null || !expectedPassword.equals(request.password())) {
            ApiError error = ApiError.of(
                    HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    "Invalid username or password",
                    httpRequest.getPath().value());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        String token = jwtService.generateToken(request.username());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
