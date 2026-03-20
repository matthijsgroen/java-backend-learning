package nl.kabisa.dashboarding.user;

import nl.kabisa.dashboarding.auth.JwtTestHelper;
import nl.kabisa.dashboarding.user.orm.Role;
import nl.kabisa.dashboarding.user.orm.User;
import nl.kabisa.dashboarding.user.orm.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserAdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTestHelper jwtTestHelper;

    private User adminUser;
    private User regularUser;
    private String adminAuth;
    private String userAuth;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@test.local");
        adminUser.setPasswordHash(passwordEncoder.encode("adminpass"));
        adminUser.setRole(Role.ADMIN);
        adminUser.setEnabled(true);
        adminUser = userRepository.save(adminUser);
        adminAuth = jwtTestHelper.bearerHeader(adminUser.getId(), adminUser.getUsername());

        regularUser = new User();
        regularUser.setUsername("regular");
        regularUser.setEmail("regular@test.local");
        regularUser.setPasswordHash(passwordEncoder.encode("userpass"));
        regularUser.setRole(Role.USER);
        regularUser.setEnabled(true);
        regularUser = userRepository.save(regularUser);
        userAuth = jwtTestHelper.bearerHeader(regularUser.getId(), regularUser.getUsername());
    }

    // ── GET /users ─────────────────────────────────────────────────────────────

    @Test
    void adminCanListUsers() throws Exception {
        mvc.perform(get("/users")
                .header(HttpHeaders.AUTHORIZATION, adminAuth)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").isNotEmpty())
                .andExpect(jsonPath("$[0].enabled").isBoolean());
    }

    @Test
    void nonAdminCannotListUsers() throws Exception {
        mvc.perform(get("/users")
                .header(HttpHeaders.AUTHORIZATION, userAuth)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotListUsers() throws Exception {
        mvc.perform(get("/users")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /users/{id}/approve ────────────────────────────────────────────────

    @Test
    void adminCanApproveUser() throws Exception {
        User disabledUser = new User();
        disabledUser.setUsername("pending");
        disabledUser.setEmail("pending@test.local");
        disabledUser.setPasswordHash(passwordEncoder.encode("pass"));
        disabledUser.setRole(Role.USER);
        disabledUser.setEnabled(false);
        disabledUser = userRepository.save(disabledUser);

        mvc.perform(put("/users/" + disabledUser.getId() + "/approve")
                .header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.id").value(disabledUser.getId().toString()));
    }

    @Test
    void approveIsIdempotent() throws Exception {
        // regularUser is already enabled — approving again should return 200
        mvc.perform(put("/users/" + regularUser.getId() + "/approve")
                .header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void approveNonExistentUserReturns404() throws Exception {
        UUID nonExistent = UUID.randomUUID();

        mvc.perform(put("/users/" + nonExistent + "/approve")
                .header(HttpHeaders.AUTHORIZATION, adminAuth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void nonAdminCannotApproveUser() throws Exception {
        mvc.perform(put("/users/" + regularUser.getId() + "/approve")
                .header(HttpHeaders.AUTHORIZATION, userAuth))
                .andExpect(status().isForbidden());
    }

    // ── PUT /users/{id}/role ───────────────────────────────────────────────────

    @Test
    void adminCanPromoteUserToAdmin() throws Exception {
        mvc.perform(put("/users/" + regularUser.getId() + "/role")
                .header(HttpHeaders.AUTHORIZATION, adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.id").value(regularUser.getId().toString()));
    }

    @Test
    void adminCanDemoteAnotherAdmin() throws Exception {
        // Create a second admin to demote
        User secondAdmin = new User();
        secondAdmin.setUsername("admin2");
        secondAdmin.setEmail("admin2@test.local");
        secondAdmin.setPasswordHash(passwordEncoder.encode("pass"));
        secondAdmin.setRole(Role.ADMIN);
        secondAdmin.setEnabled(true);
        secondAdmin = userRepository.save(secondAdmin);

        mvc.perform(put("/users/" + secondAdmin.getId() + "/role")
                .header(HttpHeaders.AUTHORIZATION, adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void adminCannotChangeOwnRole() throws Exception {
        mvc.perform(put("/users/" + adminUser.getId() + "/role")
                .header(HttpHeaders.AUTHORIZATION, adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"USER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("An admin cannot change their own role"));
    }

    @Test
    void changeRoleNonExistentUserReturns404() throws Exception {
        UUID nonExistent = UUID.randomUUID();

        mvc.perform(put("/users/" + nonExistent + "/role")
                .header(HttpHeaders.AUTHORIZATION, adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void nonAdminCannotChangeRole() throws Exception {
        mvc.perform(put("/users/" + regularUser.getId() + "/role")
                .header(HttpHeaders.AUTHORIZATION, userAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidRoleValueReturnsBadRequest() throws Exception {
        mvc.perform(put("/users/" + regularUser.getId() + "/role")
                .header(HttpHeaders.AUTHORIZATION, adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"SUPERADMIN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingRoleValueReturnsValidationError() throws Exception {
        mvc.perform(put("/users/" + regularUser.getId() + "/role")
                .header(HttpHeaders.AUTHORIZATION, adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": null}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors.role[0]").value("Role cannot be null"));
    }
}
