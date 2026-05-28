package com.echel.planner.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * Fails application startup if the {@code prod} profile is active but the
 * configured JWT secret is the dev default published in
 * {@code application.properties}. Anyone with source access knows that default,
 * so accepting it in production would let outsiders forge tokens.
 *
 * <p>{@link JwtService#validateKey()} enforces the unrelated 256-bit length
 * requirement; this guard layers on top to catch the specific case of the
 * env-var being unset and silently falling back to the committed default.
 */
@Configuration
@Profile("prod")
public class JwtSecretProductionGuard {

    /**
     * The literal value in {@code application.properties}. Kept in sync with
     * the dev default; if that default changes, change this constant too.
     */
    static final String DEV_DEFAULT_SECRET =
            "your-very-long-secret-key-that-is-at-least-256-bits-long-for-hs256";

    private final String configuredSecret;

    public JwtSecretProductionGuard(@Value("${app.jwt.secret}") String configuredSecret) {
        this.configuredSecret = configuredSecret;
    }

    @PostConstruct
    void rejectDevDefaultInProduction() {
        if (DEV_DEFAULT_SECRET.equals(configuredSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is set to the committed dev default while the 'prod' profile " +
                    "is active. Set the JWT_SECRET environment variable to a unique, " +
                    "production-only value before starting the server.");
        }
    }
}
