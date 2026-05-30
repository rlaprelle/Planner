package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.AuthResponse;
import com.echel.planner.backend.auth.dto.LoginRequest;
import com.echel.planner.backend.auth.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);
    private static final int REFRESH_COOKIE_MAX_AGE_SECONDS = (int) REFRESH_TOKEN_TTL.toSeconds();
    private static final int RAW_TOKEN_BYTES = 32; // 256 bits of entropy

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AppUserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyTakenException("Email already registered: " + request.email());
        }

        String timezone = (request.timezone() != null && !request.timezone().isBlank())
                ? request.timezone() : "UTC";

        AppUser user = new AppUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.displayName(),
                timezone
        );
        userRepository.save(user);

        return issueNewSession(user);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        AppUser user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user vanished from repository: " + request.email()));
        return issueNewSession(user);
    }

    /**
     * Validate the presented refresh token, rotate it (revoke the old, issue a new),
     * and return a fresh access token + cookie. Reuse of an already-revoked token
     * is treated as a theft signal and revokes every active token for the user.
     */
    @Transactional
    public AuthResult refresh(HttpServletRequest httpRequest) {
        String rawToken = readRefreshCookie(httpRequest);
        String hash = sha256(rawToken);
        Instant now = Instant.now();

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new MissingRefreshTokenException(
                        "Refresh token not recognized"));

        if (stored.isRevoked()) {
            // Reuse of a revoked token. Either the legitimate client is replaying
            // the old cookie (rare — they always overwrite it) or someone stole it
            // before the legitimate rotation. Either way, the safe response is to
            // invalidate every active token for this user and force a fresh login.
            int revoked = refreshTokenRepository.revokeAllActiveForUser(stored.getUserId(), now);
            log.warn("Refresh-token reuse detected for user {}: replayed jti={}, revoked {} active tokens in chain",
                    stored.getUserId(), stored.getJti(), revoked);
            throw new MissingRefreshTokenException("Refresh token has been revoked");
        }

        if (stored.isExpired(now)) {
            throw new MissingRefreshTokenException("Refresh token has expired");
        }

        AppUser user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new MissingRefreshTokenException("User not found for refresh token"));

        stored.revoke(now);
        return issueRotatedSession(user, stored.getId());
    }

    /**
     * Revoke the refresh token presented in the cookie (if any) and return a
     * cookie-clearing instruction for the response. Missing/unknown cookies are
     * silently no-ops — logout should always succeed from the client's view.
     */
    @Transactional
    public ResponseCookie logout(HttpServletRequest httpRequest) {
        Cookie cookie = WebUtils.getCookie(httpRequest, REFRESH_COOKIE_NAME);
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            String hash = sha256(cookie.getValue());
            refreshTokenRepository.findByTokenHash(hash)
                    .filter(rt -> !rt.isRevoked())
                    .ifPresent(rt -> rt.revoke(Instant.now()));
        }
        return buildClearRefreshCookie();
    }

    /**
     * Issue a brand-new session (access token + rotated refresh cookie) for a user.
     * Used after a credential change so the active client stays signed in even
     * though {@link #revokeAllSessions} has just invalidated its old token.
     */
    @Transactional
    public AuthResult issueSession(AppUser user) {
        return issueNewSession(user);
    }

    /**
     * Revoke every active refresh token for a user — "sign out everywhere".
     * Used after a password or email change so other devices must re-authenticate.
     *
     * @return the number of tokens revoked
     */
    @Transactional
    public int revokeAllSessions(UUID userId) {
        return refreshTokenRepository.revokeAllActiveForUser(userId, Instant.now());
    }

    private String readRefreshCookie(HttpServletRequest httpRequest) {
        Cookie cookie = WebUtils.getCookie(httpRequest, REFRESH_COOKIE_NAME);
        if (cookie == null || cookie.getValue() == null || cookie.getValue().isBlank()) {
            throw new MissingRefreshTokenException("Refresh token cookie not found");
        }
        return cookie.getValue();
    }

    private AuthResult issueNewSession(AppUser user) {
        return issueRotatedSession(user, null);
    }

    private AuthResult issueRotatedSession(AppUser user, UUID parentTokenId) {
        String rawToken = generateRawToken();
        UUID jti = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(REFRESH_TOKEN_TTL);

        RefreshToken record = new RefreshToken(
                user.getId(),
                sha256(rawToken),
                jti,
                parentTokenId,
                expiresAt
        );
        refreshTokenRepository.save(record);

        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole());
        ResponseCookie refreshCookie = buildRefreshCookie(rawToken);
        return new AuthResult(new AuthResponse(accessToken), refreshCookie);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        // URL-safe Base64 (no padding) keeps the cookie value compact and free of
        // characters that would need encoding in a Set-Cookie header.
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available in JRE", ex);
        }
    }

    // Cookie path is scoped to /api/v1/auth (not the broader /api or /) so the
    // refresh cookie is only ever attached to auth-flow requests. It needs to
    // cover both /auth/refresh AND /auth/logout — logout has to read the
    // presented cookie to revoke the matching DB row.
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    public ResponseCookie buildClearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path(REFRESH_COOKIE_PATH)
                .sameSite("Strict")
                .maxAge(0)
                .build();
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path(REFRESH_COOKIE_PATH)
                .sameSite("Strict")
                .maxAge(REFRESH_COOKIE_MAX_AGE_SECONDS)
                .build();
    }

    public record AuthResult(AuthResponse authResponse, ResponseCookie refreshCookie) {}

    public static class EmailAlreadyTakenException extends RuntimeException {
        public EmailAlreadyTakenException(String message) {
            super(message);
        }
    }

    public static class MissingRefreshTokenException extends RuntimeException {
        public MissingRefreshTokenException(String message) {
            super(message);
        }
    }
}
