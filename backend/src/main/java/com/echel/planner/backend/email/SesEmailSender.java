package com.echel.planner.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/**
 * Sends transactional email through AWS SES (Simple Email Service) v2.
 *
 * <p>Active only when {@code app.email.provider=ses}. Requires:
 * <ul>
 *   <li>{@code app.email.from} — a verified SES sender identity (address or domain)</li>
 *   <li>AWS credentials on the default provider chain (instance role in prod,
 *       {@code AWS_*} env vars locally) and a region (see {@link SesEmailConfig})</li>
 *   <li>the SES account out of the sandbox so it can send to arbitrary recipients</li>
 * </ul>
 * These are operator-provisioned; see {@code docs/DEPLOYMENT.md}.
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "ses")
public class SesEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SesEmailSender.class);

    private final SesV2Client sesClient;
    private final String fromAddress;

    public SesEmailSender(SesV2Client sesClient,
                          @Value("${app.email.from}") String fromAddress) {
        this.sesClient = sesClient;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendEmailChangeVerification(String toEmail, String displayName, String verificationLink) {
        String subject = "Confirm your new Echel Planner email";
        String greeting = (displayName == null || displayName.isBlank()) ? "Hi there" : "Hi " + displayName;
        String text = greeting + ",\n\n"
                + "We received a request to change the email on your Echel Planner account to this address. "
                + "To confirm the change, follow this link:\n\n"
                + verificationLink + "\n\n"
                + "If you didn't request this, you can safely ignore this email — your account won't change.\n\n"
                + "— Echel Planner";

        SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(fromAddress)
                .destination(Destination.builder().toAddresses(toEmail).build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data(subject).build())
                                .body(Body.builder()
                                        .text(Content.builder().data(text).build())
                                        .build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
        log.info("Sent email-change verification to {} via SES", toEmail);
    }
}
