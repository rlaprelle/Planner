package com.planner.backend.auth;

import com.planner.backend.auth.dto.AuthResponse;
import com.planner.backend.auth.dto.LoginRequest;
import com.planner.backend.auth.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;

@Service
public class AuthService {

    static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final int REFRESH_COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60; // 7 days

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

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

        return buildAuthResult(user.getEmail());
    }

    public AuthResult login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        return buildAuthResult(request.email());
    }

    public AuthResult refresh(HttpServletRequest httpRequest) {
        Cookie cookie = WebUtils.getCookie(httpRequest, REFRESH_COOKIE_NAME);
        if (cookie == null) {
            throw new MissingRefreshTokenException("Refresh token cookie not found");
        }

        String refreshToken = cookie.getValue();
        String email = jwtService.extractEmail(refreshToken);

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MissingRefreshTokenException("User not found for refresh token"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new MissingRefreshTokenException("Refresh token is invalid or expired");
        }

        return buildAuthResult(email);
    }

    private AuthResult buildAuthResult(String email) {
        String accessToken = jwtService.generateAccessToken(email);
        String refreshToken = jwtService.generateRefreshToken(email);
        ResponseCookie refreshCookie = buildRefreshCookie(refreshToken);
        return new AuthResult(new AuthResponse(accessToken), refreshCookie);
    }

    public ResponseCookie buildClearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth/refresh")
                .sameSite("Strict")
                .maxAge(0)
                .build();
    }

    private ResponseCookie buildRefreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth/refresh")
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
