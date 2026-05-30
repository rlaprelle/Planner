package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.ChangeEmailRequest;
import com.echel.planner.backend.auth.dto.ChangePasswordRequest;
import com.echel.planner.backend.common.ValidationException;
import com.echel.planner.backend.email.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Self-service account management for the authenticated user: changing their
 * password and changing the email they log in with.
 *
 * <p>Both operations re-authenticate with the current password. The password
 * change rotates the session ({@link #changePassword} revokes all sessions then
 * reissues one for the active client). The email change is verify-before-switch:
 * a link goes to the <em>new</em> address and the email only changes once that
 * link is redeemed via {@link #confirmEmailChange}.
 */
@Service
@Transactional
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final int RAW_TOKEN_BYTES = 32; // 256 bits of entropy

    private final AppUserRepository userRepository;
    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final EmailSender emailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    private final String frontendBaseUrl;
    private final Duration emailChangeTokenTtl;

    public AccountService(AppUserRepository userRepository,
                          EmailChangeTokenRepository emailChangeTokenRepository,
                          PasswordEncoder passwordEncoder,
                          AuthService authService,
                          EmailSender emailSender,
                          @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
                          @Value("${app.account.email-change-token-ttl-minutes:60}") long emailChangeTokenTtlMinutes) {
        this.userRepository = userRepository;
        this.emailChangeTokenRepository = emailChangeTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.emailSender = emailSender;
        // Trailing slash would double up against the "/verify-email" path we append.
        this.frontendBaseUrl = frontendBaseUrl.endsWith("/")
                ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
                : frontendBaseUrl;
        this.emailChangeTokenTtl = Duration.ofMinutes(emailChangeTokenTtlMinutes);
    }

    /**
     * Re-authenticate, set a new password, and rotate the session: every existing
     * refresh token is revoked (signing the user out everywhere) and a fresh
     * session is issued for the calling client so it stays logged in.
     */
    public AuthService.AuthResult changePassword(AppUser user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IncorrectPasswordException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ValidationException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        authService.revokeAllSessions(user.getId());
        log.info("Password changed for user {}; all sessions revoked and reissued", user.getId());
        return authService.issueSession(user);
    }

    /**
     * Re-authenticate and begin an email change. Mints a single-use token, stores
     * its hash, and emails a verification link to the new address. The user's
     * email is NOT changed here — only {@link #confirmEmailChange} does that.
     */
    public void requestEmailChange(AppUser user, ChangeEmailRequest request) {
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IncorrectPasswordException("Current password is incorrect");
        }

        String newEmail = request.newEmail().trim();
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new ValidationException("That is already your email address");
        }
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new AuthService.EmailAlreadyTakenException("Email already registered: " + newEmail);
        }

        Instant now = Instant.now();
        // A fresh request supersedes any older pending ones — only the newest link works.
        emailChangeTokenRepository.consumeAllPendingForUser(user.getId(), now);

        String rawToken = generateRawToken();
        EmailChangeToken token = new EmailChangeToken(
                user.getId(), newEmail, AuthService.sha256(rawToken), now.plus(emailChangeTokenTtl));
        emailChangeTokenRepository.save(token);

        String link = frontendBaseUrl + "/verify-email?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        emailSender.sendEmailChangeVerification(newEmail, user.getDisplayName(), link);
        log.info("Email-change verification requested for user {} -> {}", user.getId(), newEmail);
    }

    /**
     * Redeem an email-change verification token: switch the user's login email to
     * the verified address, consume the token, and revoke all sessions so every
     * device re-authenticates under the new email.
     *
     * @return the new email address now in effect
     */
    public String confirmEmailChange(String rawToken) {
        Instant now = Instant.now();
        EmailChangeToken token = emailChangeTokenRepository.findByTokenHash(AuthService.sha256(rawToken))
                .orElseThrow(() -> new InvalidEmailChangeTokenException("This verification link is not valid"));

        if (token.isConsumed()) {
            throw new InvalidEmailChangeTokenException("This verification link has already been used");
        }
        if (token.isExpired(now)) {
            throw new InvalidEmailChangeTokenException("This verification link has expired");
        }

        AppUser user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new InvalidEmailChangeTokenException("This verification link is not valid"));

        // Guard against the address being claimed by someone else between request and confirm.
        boolean takenByOther = userRepository.findByEmail(token.getNewEmail())
                .filter(other -> !other.getId().equals(user.getId()))
                .isPresent();
        if (takenByOther) {
            throw new AuthService.EmailAlreadyTakenException("Email already registered: " + token.getNewEmail());
        }

        user.setEmail(token.getNewEmail());
        userRepository.save(user);
        token.consume(now);
        authService.revokeAllSessions(user.getId());
        log.info("Email changed for user {} -> {}; all sessions revoked", user.getId(), token.getNewEmail());
        return token.getNewEmail();
    }

    private String generateRawToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static class IncorrectPasswordException extends RuntimeException {
        public IncorrectPasswordException(String message) {
            super(message);
        }
    }

    public static class InvalidEmailChangeTokenException extends RuntimeException {
        public InvalidEmailChangeTokenException(String message) {
            super(message);
        }
    }
}
