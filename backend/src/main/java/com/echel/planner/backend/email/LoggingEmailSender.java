package com.echel.planner.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default {@link EmailSender} for local development and tests: it doesn't send
 * anything, it logs the message (including the verification link) so a developer
 * can copy the link out of the console and complete the flow without any mail
 * infrastructure. Active unless {@code app.email.provider=ses}.
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "log", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void sendEmailChangeVerification(String toEmail, String displayName, String verificationLink) {
        log.info("[email:log] Email-change verification for {} ({}). Link: {}",
                toEmail, displayName, verificationLink);
    }
}
