package com.echel.planner.backend.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;

/**
 * Fails application startup under the {@code prod} profile if neither an
 * upstream TLS-terminating proxy nor in-process TLS is configured. The
 * refresh-token cookie is issued with the {@code Secure} attribute; if the app
 * sees the request as plaintext HTTP the browser silently drops the cookie and
 * authentication breaks in a way that is hard to diagnose from server logs.
 *
 * <p>Valid configurations:
 * <ul>
 *   <li>{@code server.forward-headers-strategy} set (typically to
 *       {@code framework} or {@code native}) — a proxy like AWS App Runner,
 *       nginx, Caddy, or Traefik is terminating TLS and forwarding
 *       {@code X-Forwarded-Proto}.</li>
 *   <li>{@code server.ssl.enabled=true} — Spring Boot is terminating TLS
 *       itself (uncommon for this app but a legitimate setup).</li>
 * </ul>
 *
 * <p>Mirrors the production-guard pattern established by
 * {@code JwtSecretProductionGuard}.
 */
@Configuration
@Profile("prod")
public class HttpsTerminationGuard {

    private final String forwardHeadersStrategy;
    private final boolean sslEnabled;

    public HttpsTerminationGuard(
            @Value("${server.forward-headers-strategy:}") String forwardHeadersStrategy,
            @Value("${server.ssl.enabled:false}") boolean sslEnabled) {
        this.forwardHeadersStrategy = forwardHeadersStrategy;
        this.sslEnabled = sslEnabled;
    }

    @PostConstruct
    void requireHttpsTerminationConfigured() {
        if (hasForwardHeadersStrategy() || sslEnabled) {
            return;
        }
        throw new IllegalStateException(
                "Production profile is active but neither 'server.forward-headers-strategy' " +
                "nor 'server.ssl.enabled=true' is configured. The refresh-token cookie uses " +
                "the Secure attribute, so without HTTPS termination (either at an upstream " +
                "proxy that forwards X-Forwarded-Proto, or in-process TLS) browsers will " +
                "silently drop the cookie and authentication will break. " +
                "Set 'server.forward-headers-strategy=framework' when running behind a proxy " +
                "(App Runner, nginx, Caddy, Traefik), or 'server.ssl.enabled=true' for " +
                "in-process TLS. See docs/DEPLOYMENT.md.");
    }

    private boolean hasForwardHeadersStrategy() {
        return forwardHeadersStrategy != null && !forwardHeadersStrategy.isBlank();
    }
}
