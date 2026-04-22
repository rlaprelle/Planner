package com.echel.planner.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Issues and validates JWTs. Tokens carry the user's email as the subject and
 * their role as a custom {@code role} claim so {@link JwtAuthFilter} can grant
 * authorities without an extra database hit.
 */
@Service
public class JwtService {

    static final String ROLE_CLAIM = "role";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @PostConstruct
    public void validateKey() {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 32 characters (256 bits). " +
                "Set the JWT_SECRET environment variable.");
        }
        getSigningKey();
    }

    public String generateAccessToken(String email, AppUser.Role role) {
        return buildToken(email, role, accessTokenExpiration);
    }

    public String generateRefreshToken(String email, AppUser.Role role) {
        return buildToken(email, role, refreshTokenExpiration);
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Returns the role embedded in the token, or {@link AppUser.Role#USER} for
     * tokens issued before the role claim was introduced.
     */
    public AppUser.Role extractRole(String token) {
        String raw = extractClaim(token, claims -> claims.get(ROLE_CLAIM, String.class));
        if (raw == null) {
            return AppUser.Role.USER;
        }
        try {
            return AppUser.Role.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return AppUser.Role.USER;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private String buildToken(String email, AppUser.Role role, long expiration) {
        return Jwts.builder()
                .claims(Map.of(ROLE_CLAIM, role.name()))
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
