package com.echel.planner.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the CORS bean exposes the configured origins, methods, and
 * credential support. Cross-origin behavior in a real browser is out of scope
 * for unit tests.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, AuthService.class, AuthExceptionHandler.class, AuthRateLimitFilter.class})
class SecurityConfigCorsTest {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @MockBean
    private AppUserRepository userRepository;

    @Test
    void corsBean_exposesDefaultDevOriginsAndCredentialedMethods() {
        CorsConfiguration config = configFor("/api/v1/tasks");

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOriginPatterns())
                .containsExactlyInAnyOrder("http://localhost:5173", "http://localhost:52*");
        assertThat(config.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    private CorsConfiguration configFor(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        return corsConfigurationSource.getCorsConfiguration(request);
    }
}
