package nl.kabisa.dashboarding.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Test utility that mints real JWT tokens by delegating to the application's JwtService.
 * This ensures tests exercise the full JwtAuthenticationFilter chain with the same
 * signing key and claims format as production.
 *
 * Usage in tests:
 *   mvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, jwtTestHelper.bearerHeader(userId, username)))
 */
@Component
public class JwtTestHelper {

    private final JwtService jwtService;

    @Autowired
    public JwtTestHelper(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String mintToken(UUID userId, String username) {
        return jwtService.generateToken(userId, username);
    }

    public String bearerHeader(UUID userId, String username) {
        return "Bearer " + mintToken(userId, username);
    }
}
