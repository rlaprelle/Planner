package com.echel.planner.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a local development admin account when the {@code dev} Spring profile
 * is active. Idempotent: re-running against an existing seeded user is a no-op;
 * pre-existing accounts with the seed email are promoted to {@code ADMIN} but
 * their credentials and profile data are left untouched.
 *
 * <p>The seed credentials are intentionally simple and are documented in
 * {@code CONTRIBUTING.md}. Do not enable the {@code dev} profile in production.
 */
@Component
@Profile("dev")
public class DevAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminSeeder.class);

    static final String ADMIN_EMAIL = "admin@echel.dev";
    static final String ADMIN_PASSWORD = "adminadmin";
    static final String ADMIN_DISPLAY_NAME = "Dev Admin";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevAdminSeeder(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        userRepository.findByEmail(ADMIN_EMAIL).ifPresentOrElse(
                this::promoteIfNeeded,
                this::createAdmin
        );
    }

    private void promoteIfNeeded(AppUser user) {
        if (user.getRole() == AppUser.Role.ADMIN) {
            return;
        }
        user.setRole(AppUser.Role.ADMIN);
        userRepository.save(user);
        log.info("Promoted existing user {} to ADMIN (dev profile).", ADMIN_EMAIL);
    }

    private void createAdmin() {
        AppUser admin = new AppUser(
                ADMIN_EMAIL,
                passwordEncoder.encode(ADMIN_PASSWORD),
                ADMIN_DISPLAY_NAME,
                "UTC"
        );
        admin.setRole(AppUser.Role.ADMIN);
        userRepository.save(admin);
        log.info("Seeded dev admin user {} (dev profile).", ADMIN_EMAIL);
    }
}
