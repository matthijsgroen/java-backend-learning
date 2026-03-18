package nl.kabisa.dashboarding.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.kabisa.dashboarding.auth.JwtTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import nl.kabisa.dashboarding.user.orm.UserRepository;

import java.util.UUID;

import static nl.kabisa.dashboarding.user.UserTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTestHelper jwtTestHelper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private String extractIdFromResponse(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content);
        return root.get("id").asText();
    }

    @Test
    public void registerSucceeds() throws Exception {
        MvcResult result = mvc
                .perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_USER_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        String id = extractIdFromResponse(result);
        assertDoesNotThrow(() -> UUID.fromString(id), "id should be a valid UUID");
        assertEquals(1, userRepository.count());
    }

    @Test
    public void registerDuplicateUsernameReturnsConflict() throws Exception {
        mvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_USER_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        mvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_DUPLICATE_USERNAME_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already taken: johndoe"));
    }

    @Test
    public void registerDuplicateEmailReturnsConflict() throws Exception {
        mvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_USER_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        mvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_DUPLICATE_EMAIL_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already registered: john@example.com"));
    }

    @Test
    public void getProfileReturnsUserData() throws Exception {
        MvcResult registerResult = mvc
                .perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_USER_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = extractIdFromResponse(registerResult);
        String token = jwtTestHelper.mintToken(UUID.fromString(userId), "johndoe");

        mvc.perform(get("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    public void getProfileWithNonExistentUserReturnsNotFound() throws Exception {
        UUID randomId = UUID.randomUUID();
        String token = jwtTestHelper.mintToken(randomId, "nobody");

        mvc.perform(get("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    public void getProfileWithNoTokenReturnsUnauthorized() throws Exception {
        mvc.perform(get("/users/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    public void getProfileWithInvalidTokenReturnsUnauthorized() throws Exception {
        mvc.perform(get("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalidtoken")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    public void registerWithBlankUsernameReturnsValidationError() throws Exception {
        mvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_INVALID_BLANK_USERNAME_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.username[0]").value("Username cannot be blank"));
    }

    @Test
    public void registerWithInvalidEmailReturnsValidationError() throws Exception {
        mvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_INVALID_EMAIL_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email[0]").value("Email must be a valid email address"));
    }

    @Test
    public void registerWithBlankPasswordReturnsValidationError() throws Exception {
        mvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REGISTER_BLANK_PASSWORD_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.password[0]").value("Password cannot be blank"));
    }
}
