package com.echel.planner.backend.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * Builds the {@link SesV2Client} used by {@link SesEmailSender}. Only wired when
 * {@code app.email.provider=ses}, so dev/test runs never touch the AWS SDK's
 * credential or region resolution.
 *
 * <p>If {@code app.email.ses.region} is set it is used explicitly; otherwise the
 * SDK falls back to its default region provider chain ({@code AWS_REGION} env
 * var, profile config, or instance metadata).
 */
@Configuration
@ConditionalOnProperty(name = "app.email.provider", havingValue = "ses")
public class SesEmailConfig {

    @Bean
    public SesV2Client sesV2Client(@Value("${app.email.ses.region:}") String region) {
        var builder = SesV2Client.builder();
        if (region != null && !region.isBlank()) {
            builder.region(Region.of(region));
        }
        return builder.build();
    }
}
