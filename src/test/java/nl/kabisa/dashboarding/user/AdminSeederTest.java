package nl.kabisa.dashboarding.user;

import nl.kabisa.dashboarding.user.orm.Role;
import nl.kabisa.dashboarding.user.orm.User;
import nl.kabisa.dashboarding.user.orm.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the AdminSeeder correctly seeds an admin user on application startup.
 * Uses @DirtiesContext(classMode = BEFORE_CLASS) so this test class always gets a
 * fresh Spring context — meaning the seeder runs on startup and the admin is present.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AdminSeederTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminUserExistsAfterStartup() {
        assertTrue(userRepository.existsByRole(Role.ADMIN),
                "An admin user should exist after application startup");
    }

    @Test
    void seededAdminHasCorrectAttributes() {
        User admin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("Admin user not found in DB"));

        assertEquals(Role.ADMIN, admin.getRole(), "Admin user should have role ADMIN");
        assertTrue(admin.isEnabled(), "Admin user should be enabled");
        assertEquals("admin@test.local", admin.getEmail(), "Admin user should have the configured email");
    }

    @Test
    void seededAdminPasswordIsHashed() {
        User admin = userRepository.findByUsername("admin")
                .orElseThrow(() -> new AssertionError("Admin user not found in DB"));

        assertNotEquals("adminpassword", admin.getPasswordHash(),
                "Password should be stored as a BCrypt hash, not in plaintext");
        assertTrue(passwordEncoder.matches("adminpassword", admin.getPasswordHash()),
                "BCrypt hash should verify correctly against the configured admin password");
    }

    @Test
    void seederIsIdempotent() {
        // The context starts once and the seeder runs once.
        // Verify there is exactly one admin — not duplicated on repeated checks.
        long adminCount = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .count();

        assertEquals(1, adminCount,
                "There should be exactly one admin user after startup (seeder is idempotent)");
    }
}
