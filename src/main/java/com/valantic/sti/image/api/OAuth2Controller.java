package com.valantic.sti.image.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/oauth2")
@Tag(name = "OAuth2", description = "🔐 OAuth2 authentication endpoints")
public class OAuth2Controller {

    @Operation(summary = "Get User Info", description = "Get authenticated user information")
    @GetMapping("/user")
    public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
        return Map.of(
            "name", principal.getAttribute("name"),
            "email", principal.getAttribute("email"),
            "picture", principal.getAttribute("picture"),
            "authorities", principal.getAuthorities()
        );
    }

    @Operation(summary = "Get JWT Claims", description = "Get JWT token claims")
    @GetMapping("/jwt")
    public Map<String, Object> jwt(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "subject", jwt.getSubject(),
            "claims", jwt.getClaims(),
            "headers", jwt.getHeaders()
        );
    }

    @Operation(summary = "Login", description = "Redirect to OAuth2 provider")
    @GetMapping("/login")
    public Map<String, String> login() {
        return Map.of(
            "google", "/oauth2/authorization/google",
            "github", "/oauth2/authorization/github",
            "message", "Choose your OAuth2 provider"
        );
    }
}