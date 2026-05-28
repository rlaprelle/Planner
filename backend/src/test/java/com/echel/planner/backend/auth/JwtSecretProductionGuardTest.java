package com.echel.planner.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretProductionGuardTest {

    /**
     * Direct unit tests of the guard's validation logic. The guard is annotated
     * {@code @Profile("prod")}, so instantiating it here simulates "prod is active".
     */

    @Test
    void rejectDevDefaultInProduction_throws_whenSecretMatchesDevDefault() {
        JwtSecretProductionGuard guard =
                new JwtSecretProductionGuard(JwtSecretProductionGuard.DEV_DEFAULT_SECRET);

        assertThatThrownBy(guard::rejectDevDefaultInProduction)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET")
                .hasMessageContaining("prod");
    }

    @Test
    void rejectDevDefaultInProduction_allows_whenSecretIsProductionStrengthOverride() {
        String prodSecret = "an-actual-production-secret-with-plenty-of-entropy-1234567890";
        JwtSecretProductionGuard guard = new JwtSecretProductionGuard(prodSecret);

        // Asserting that the method returns normally is the assertion.
        guard.rejectDevDefaultInProduction();
        assertThat(prodSecret).isNotEqualTo(JwtSecretProductionGuard.DEV_DEFAULT_SECRET);
    }

    /**
     * Context-wiring tests. These cover what the unit tests above cannot:
     * that Spring actually instantiates the guard under the prod profile and
     * does NOT instantiate it under other profiles.
     */

    @Test
    void contextFailsToStart_underProdProfile_whenSecretIsDevDefault() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtSecretProductionGuard.class)
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "app.jwt.secret=" + JwtSecretProductionGuard.DEV_DEFAULT_SECRET)
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .isInstanceOf(BeanCreationException.class)
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("JWT_SECRET")
                        .hasMessageContaining("prod"));
    }

    @Test
    void contextStarts_underProdProfile_whenSecretIsProductionStrength() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtSecretProductionGuard.class)
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "app.jwt.secret=an-actual-production-secret-with-plenty-of-entropy-1234567890")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(JwtSecretProductionGuard.class));
    }

    @Test
    void guardIsNotLoaded_underDefaultProfile_evenWithDevDefaultSecret() {
        new ApplicationContextRunner()
                .withUserConfiguration(JwtSecretProductionGuard.class)
                .withPropertyValues(
                        "app.jwt.secret=" + JwtSecretProductionGuard.DEV_DEFAULT_SECRET)
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(JwtSecretProductionGuard.class));
    }
}
