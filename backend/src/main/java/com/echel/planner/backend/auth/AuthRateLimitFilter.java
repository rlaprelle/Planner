package com.echel.planner.backend.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-IP rate limiter for the public auth endpoints. Defends {@code /auth/login}
 * and {@code /auth/register} from brute force and credential stuffing.
 *
 * <p>Buckets are kept in-memory ({@link ConcurrentHashMap}). This is sufficient
 * for the planned single-instance deployment but won't survive a process
 * restart and won't share state across replicas. Move to a shared store
 * (Redis, etc.) before scaling horizontally.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    static final int CAPACITY = 5;
    static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register"
    );

    private final ConcurrentMap<String, Bucket> bucketsByClientKey = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isRateLimited(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = bucketsByClientKey.computeIfAbsent(clientKey(request), key -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        writeTooManyRequests(response, probe);
    }

    private boolean isRateLimited(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && RATE_LIMITED_PATHS.contains(request.getRequestURI());
    }

    private String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(CAPACITY)
                .refillIntervally(CAPACITY, REFILL_PERIOD)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private void writeTooManyRequests(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        long retryAfterSeconds = Math.max(1L, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"error\":\"Too many requests. Try again in " + retryAfterSeconds + " seconds.\"}"
        );
    }
}
