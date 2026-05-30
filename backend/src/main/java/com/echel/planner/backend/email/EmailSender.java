package com.echel.planner.backend.email;

/**
 * Sends the (few) transactional emails the app needs. Implementations are
 * selected by the {@code app.email.provider} property: {@code log} (default,
 * dev/test — writes the message to the log) or {@code ses} (AWS SES).
 */
public interface EmailSender {

    /**
     * Send the "confirm your new email address" verification message.
     *
     * @param toEmail          the new address being verified (the link is sent here,
     *                         not to the current address, to prove control of it)
     * @param displayName      the recipient's preferred name, for the greeting
     * @param verificationLink absolute URL the user follows to complete the change
     */
    void sendEmailChangeVerification(String toEmail, String displayName, String verificationLink);
}
