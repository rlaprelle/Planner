package com.echel.planner.backend.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class HttpsTerminationGuardTest {

    /**
     * Context-wiring tests. These prove that Spring instantiates the guard
     * under the prod profile and applies the validation, and that the guard
     * is silent under other profiles.
     */

    @Test
    void contextStarts_underProdProfile_whenForwardHeadersStrategyIsSet() {
        new ApplicationContextRunner()
                .withUserConfiguration(HttpsTerminationGuard.class)
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "server.forward-headers-strategy=framework")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(HttpsTerminationGuard.class));
    }

    @Test
    void contextStarts_underProdProfile_whenSslEnabledIsTrue() {
        new ApplicationContextRunner()
                .withUserConfiguration(HttpsTerminationGuard.class)
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "server.ssl.enabled=true")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(HttpsTerminationGuard.class));
    }

    @Test
    void contextFailsToStart_underProdProfile_whenNeitherTerminationOptionIsSet() {
        new ApplicationContextRunner()
                .withUserConfiguration(HttpsTerminationGuard.class)
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .isInstanceOf(BeanCreationException.class)
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("forward-headers-strategy")
                        .hasMessageContaining("Secure"));
    }

    @Test
    void contextFailsToStart_underProdProfile_whenForwardHeadersStrategyIsBlank() {
        new ApplicationContextRunner()
                .withUserConfiguration(HttpsTerminationGuard.class)
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "server.forward-headers-strategy=",
                        "server.ssl.enabled=false")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .isInstanceOf(BeanCreationException.class)
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class));
    }

    @Test
    void guardIsNotLoaded_underDefaultProfile_evenWithNoTerminationConfigured() {
        new ApplicationContextRunner()
                .withUserConfiguration(HttpsTerminationGuard.class)
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(HttpsTerminationGuard.class));
    }
}
