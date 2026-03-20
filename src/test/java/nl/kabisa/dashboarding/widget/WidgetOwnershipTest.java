package nl.kabisa.dashboarding.widget;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.kabisa.dashboarding.auth.JwtTestHelper;
import nl.kabisa.dashboarding.user.orm.User;
import nl.kabisa.dashboarding.user.orm.UserRepository;
import nl.kabisa.dashboarding.widget.orm.WidgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static nl.kabisa.dashboarding.widget.WidgetTestFixtures.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class WidgetOwnershipTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private WidgetRepository widgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTestHelper jwtTestHelper;

    private User alice;
    private User bob;
    private String aliceAuthHeader;
    private String bobAuthHeader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        widgetRepository.deleteAll();
        userRepository.deleteAll();

        alice = new User();
        alice.setUsername("alice");
        alice.setEmail("alice@test.local");
        alice.setPasswordHash(passwordEncoder.encode("alicepass"));
        alice = userRepository.save(alice);
        aliceAuthHeader = jwtTestHelper.bearerHeader(alice.getId(), alice.getUsername());

        bob = new User();
        bob.setUsername("bob");
        bob.setEmail("bob@test.local");
        bob.setPasswordHash(passwordEncoder.encode("bobpass"));
        bob = userRepository.save(bob);
        bobAuthHeader = jwtTestHelper.bearerHeader(bob.getId(), bob.getUsername());
    }

    private UUID createWidgetAs(String authHeader) throws Exception {
        MvcResult result = mvc.perform(post("/widget")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MINIMAL_WIDGET_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(
                result.getResponse().getContentAsString()).get("id").asText());
    }

    // ── Read: owner can read own widget ──────────────────────────────────────

    @Test
    public void getWidgetIncludesOwnerIdAndOwnerName() throws Exception {
        UUID widgetId = createWidgetAs(aliceAuthHeader);

        mvc.perform(get("/widget/" + widgetId)
                        .header(HttpHeaders.AUTHORIZATION, aliceAuthHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(widgetId.toString()))
                .andExpect(jsonPath("$.ownerId").value(alice.getId().toString()))
                .andExpect(jsonPath("$.ownerName").value("alice"));
    }

    @Test
    public void getWidgetChildrenIncludesOwnerFields() throws Exception {
        UUID parentId = createWidgetAs(aliceAuthHeader);
        UUID childId = createWidgetAs(aliceAuthHeader);

        // Assign child under parent
        mvc.perform(put("/widget/" + childId)
                        .header(HttpHeaders.AUTHORIZATION, aliceAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateParentJson(parentId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get("/widget/" + parentId + "/children")
                        .header(HttpHeaders.AUTHORIZATION, aliceAuthHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ownerId").value(alice.getId().toString()))
                .andExpect(jsonPath("$[0].ownerName").value("alice"));
    }

    // ── Read: non-owner is forbidden ─────────────────────────────────────────

    @Test
    public void nonOwnerCannotReadWidgetOwnedByOther() throws Exception {
        UUID aliceWidgetId = createWidgetAs(aliceAuthHeader);

        mvc.perform(get("/widget/" + aliceWidgetId)
                        .header(HttpHeaders.AUTHORIZATION, bobAuthHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    public void nonOwnerCannotReadChildrenOfWidgetOwnedByOther() throws Exception {
        UUID parentId = createWidgetAs(aliceAuthHeader);
        UUID childId = createWidgetAs(aliceAuthHeader);
        mvc.perform(put("/widget/" + childId)
                        .header(HttpHeaders.AUTHORIZATION, aliceAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateParentJson(parentId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get("/widget/" + parentId + "/children")
                        .header(HttpHeaders.AUTHORIZATION, bobAuthHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // ── Write: owner can mutate own widget ───────────────────────────────────

    @Test
    public void ownerCanUpdateOwnWidget() throws Exception {
        UUID aliceWidgetId = createWidgetAs(aliceAuthHeader);
        UUID bobWidgetId = createWidgetAs(bobAuthHeader);

        mvc.perform(put("/widget/" + aliceWidgetId)
                        .header(HttpHeaders.AUTHORIZATION, aliceAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateParentJson(bobWidgetId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceWidgetId.toString()))
                .andExpect(jsonPath("$.message").value("Widget updated successfully"));
    }

    @Test
    public void ownerCanDeleteOwnWidget() throws Exception {
        UUID aliceWidgetId = createWidgetAs(aliceAuthHeader);

        mvc.perform(delete("/widget/" + aliceWidgetId)
                        .header(HttpHeaders.AUTHORIZATION, aliceAuthHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(1));
    }

    // ── Write: non-owner is forbidden ────────────────────────────────────────

    @Test
    public void nonOwnerCannotUpdateWidget() throws Exception {
        UUID aliceWidgetId = createWidgetAs(aliceAuthHeader);

        mvc.perform(put("/widget/" + aliceWidgetId)
                        .header(HttpHeaders.AUTHORIZATION, bobAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REMOVE_PARENT_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    public void nonOwnerCannotDeleteWidget() throws Exception {
        UUID aliceWidgetId = createWidgetAs(aliceAuthHeader);

        mvc.perform(delete("/widget/" + aliceWidgetId)
                        .header(HttpHeaders.AUTHORIZATION, bobAuthHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // ── 404 precedes 403 (ordering invariant) ────────────────────────────────

    @Test
    public void getNonExistentWidgetReturns404NotForbidden() throws Exception {
        mvc.perform(get("/widget/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, aliceAuthHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    public void updateNonExistentWidgetReturns404NotForbidden() throws Exception {
        mvc.perform(put("/widget/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, aliceAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REMOVE_PARENT_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    public void deleteNonExistentWidgetReturns404NotForbidden() throws Exception {
        mvc.perform(delete("/widget/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, aliceAuthHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ── Mixed-ownership tree ─────────────────────────────────────────────────

    @Test
    public void ownerIdAndOwnerNameReflectActualOwnerOnChildWidget() throws Exception {
        // Alice creates parent; Bob creates child under Alice's parent
        UUID aliceParentId = createWidgetAs(aliceAuthHeader);
        UUID bobChildId = createWidgetAs(bobAuthHeader);

        mvc.perform(put("/widget/" + bobChildId)
                        .header(HttpHeaders.AUTHORIZATION, bobAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateParentJson(aliceParentId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Bob owns the child — Bob can read it
        mvc.perform(get("/widget/" + bobChildId)
                        .header(HttpHeaders.AUTHORIZATION, bobAuthHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(bob.getId().toString()))
                .andExpect(jsonPath("$.ownerName").value("bob"));

        // Alice does NOT own the child — forbidden
        mvc.perform(get("/widget/" + bobChildId)
                        .header(HttpHeaders.AUTHORIZATION, aliceAuthHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
