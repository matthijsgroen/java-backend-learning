package nl.kabisa.dashboarding.widget;

import com.fasterxml.jackson.databind.JsonNode;
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
public class WidgetHierarchyTest {

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

    private User testUser;
    private String authHeader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        widgetRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@test.local");
        testUser.setPasswordHash(passwordEncoder.encode("testpassword"));
        testUser = userRepository.save(testUser);
        authHeader = jwtTestHelper.bearerHeader(testUser.getId(), testUser.getUsername());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private UUID createWidget(String json) throws Exception {
        MvcResult result = mvc.perform(post("/widget")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private UUID createMinimalWidget() throws Exception {
        return createWidget(MINIMAL_WIDGET_JSON);
    }

    // ── Create with parent ────────────────────────────────────────────────────

    @Test
    public void createWidgetWithNoParentSucceeds() throws Exception {
        mvc.perform(post("/widget")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MINIMAL_WIDGET_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.message").value("Widget created successfully"));
    }

    @Test
    public void createWidgetWithValidParentSucceeds() throws Exception {
        UUID parentId = createMinimalWidget();

        MvcResult result = mvc.perform(post("/widget")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalWidgetWithParentJson(parentId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        UUID childId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText());

        // Verify child has correct parentId in GET
        mvc.perform(get("/widget/" + childId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(parentId.toString()))
                .andExpect(jsonPath("$.ownerId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.ownerName").value("testuser"));
    }

    @Test
    public void createWidgetWithNonExistentParentReturns404() throws Exception {
        String randomId = UUID.randomUUID().toString();
        mvc.perform(post("/widget")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalWidgetWithParentJson(randomId))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void createWidgetWithInvalidParentIdReturnsBadRequest() throws Exception {
        mvc.perform(post("/widget")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalWidgetWithParentJson("not-a-uuid"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── GET with hierarchy info ───────────────────────────────────────────────

    @Test
    public void getRootWidgetHasNullParentAndEmptyChildIds() throws Exception {
        UUID id = createMinimalWidget();

        mvc.perform(get("/widget/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").doesNotExist())
                .andExpect(jsonPath("$.childIds").isArray())
                .andExpect(jsonPath("$.childIds").isEmpty());
    }

    @Test
    public void getWidgetIncludesParentId() throws Exception {
        UUID parentId = createMinimalWidget();
        UUID childId = createWidget(minimalWidgetWithParentJson(parentId.toString()));

        mvc.perform(get("/widget/" + childId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(parentId.toString()));
    }

    @Test
    public void getWidgetIncludesChildIds() throws Exception {
        UUID parentId = createMinimalWidget();
        UUID child1Id = createWidget(minimalWidgetWithParentJson(parentId.toString()));
        UUID child2Id = createWidget(minimalWidgetWithParentJson(parentId.toString()));

        MvcResult result = mvc.perform(get("/widget/" + parentId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.childIds").isArray())
                .andExpect(jsonPath("$.childIds.length()").value(2))
                .andReturn();

        JsonNode childIds = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("childIds");
        // Both child IDs are present (order may vary)
        boolean hasChild1 = false, hasChild2 = false;
        for (JsonNode node : childIds) {
            if (node.asText().equals(child1Id.toString())) hasChild1 = true;
            if (node.asText().equals(child2Id.toString())) hasChild2 = true;
        }
        org.junit.jupiter.api.Assertions.assertTrue(hasChild1, "child1 should be in childIds");
        org.junit.jupiter.api.Assertions.assertTrue(hasChild2, "child2 should be in childIds");
    }

    // ── List children ─────────────────────────────────────────────────────────

    @Test
    public void getChildrenReturnsDirectChildrenOnly() throws Exception {
        UUID parentId = createMinimalWidget();
        UUID childId = createWidget(minimalWidgetWithParentJson(parentId.toString()));
        // grandchild — should NOT appear in parent's children
        createWidget(minimalWidgetWithParentJson(childId.toString()));

        mvc.perform(get("/widget/" + parentId + "/children")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(childId.toString()))
                .andExpect(jsonPath("$[0].ownerId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$[0].ownerName").value("testuser"));
    }

    @Test
    public void getChildrenOfLeafReturnsEmptyList() throws Exception {
        UUID id = createMinimalWidget();

        mvc.perform(get("/widget/" + id + "/children")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    public void getChildrenOfNonExistentWidgetReturns404() throws Exception {
        mvc.perform(get("/widget/" + UUID.randomUUID() + "/children")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ── Update (re-parent) ───────────────────────────────────────────────────

    @Test
    public void updateWidgetToSetParent() throws Exception {
        UUID parentId = createMinimalWidget();
        UUID childId = createMinimalWidget();

        mvc.perform(put("/widget/" + childId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateParentJson(parentId.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(childId.toString()))
                .andExpect(jsonPath("$.message").value("Widget updated successfully"));

        mvc.perform(get("/widget/" + childId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.parentId").value(parentId.toString()));
    }

    @Test
    public void updateWidgetToChangeParent() throws Exception {
        UUID parentA = createMinimalWidget();
        UUID parentB = createMinimalWidget();
        UUID childId = createWidget(minimalWidgetWithParentJson(parentA.toString()));

        // Re-parent from A to B
        mvc.perform(put("/widget/" + childId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateParentJson(parentB.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get("/widget/" + childId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.parentId").value(parentB.toString()));
    }

    @Test
    public void updateWidgetToRemoveParent() throws Exception {
        UUID parentId = createMinimalWidget();
        UUID childId = createWidget(minimalWidgetWithParentJson(parentId.toString()));

        mvc.perform(put("/widget/" + childId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REMOVE_PARENT_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get("/widget/" + childId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.parentId").doesNotExist());
    }

    @Test
    public void updateNonExistentWidgetReturns404() throws Exception {
        mvc.perform(put("/widget/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REMOVE_PARENT_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateWidgetWithNonExistentParentReturns404() throws Exception {
        UUID childId = createMinimalWidget();

        mvc.perform(put("/widget/" + childId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateParentJson(UUID.randomUUID().toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ── Cycle detection ───────────────────────────────────────────────────────

    @Test
    public void cannotSetWidgetAsOwnParent() throws Exception {
        UUID id = createMinimalWidget();

        mvc.perform(put("/widget/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateParentJson(id.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    public void cannotCreateDirectCycle() throws Exception {
        // A → B, then try to set A's parent to B
        UUID widgetA = createMinimalWidget();
        UUID widgetB = createWidget(minimalWidgetWithParentJson(widgetA.toString()));

        mvc.perform(put("/widget/" + widgetA)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateParentJson(widgetB.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    public void cannotCreateTransitiveCycle() throws Exception {
        // A → B → C, then try to set A's parent to C
        UUID widgetA = createMinimalWidget();
        UUID widgetB = createWidget(minimalWidgetWithParentJson(widgetA.toString()));
        UUID widgetC = createWidget(minimalWidgetWithParentJson(widgetB.toString()));

        mvc.perform(put("/widget/" + widgetA)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateParentJson(widgetC.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    public void canReparentWithinSameTree() throws Exception {
        // A → B, A → C. Move B under C (no cycle)
        UUID widgetA = createMinimalWidget();
        UUID widgetB = createWidget(minimalWidgetWithParentJson(widgetA.toString()));
        UUID widgetC = createWidget(minimalWidgetWithParentJson(widgetA.toString()));

        mvc.perform(put("/widget/" + widgetB)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateParentJson(widgetC.toString()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get("/widget/" + widgetB)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.parentId").value(widgetC.toString()));
    }

    // ── Cascade delete ────────────────────────────────────────────────────────

    @Test
    public void deleteLeafWidget() throws Exception {
        UUID id = createMinimalWidget();

        mvc.perform(delete("/widget/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(1));

        mvc.perform(get("/widget/" + id)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteParentCascadesToChildren() throws Exception {
        // A → B, A → C  (3 total)
        UUID widgetA = createMinimalWidget();
        createWidget(minimalWidgetWithParentJson(widgetA.toString()));
        createWidget(minimalWidgetWithParentJson(widgetA.toString()));

        mvc.perform(delete("/widget/" + widgetA)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(3));

        org.junit.jupiter.api.Assertions.assertEquals(0, widgetRepository.count());
    }

    @Test
    public void deleteParentCascadesToGrandchildren() throws Exception {
        // A → B → C → D  (4 total)
        UUID widgetA = createMinimalWidget();
        UUID widgetB = createWidget(minimalWidgetWithParentJson(widgetA.toString()));
        UUID widgetC = createWidget(minimalWidgetWithParentJson(widgetB.toString()));
        createWidget(minimalWidgetWithParentJson(widgetC.toString()));

        mvc.perform(delete("/widget/" + widgetA)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(4));

        org.junit.jupiter.api.Assertions.assertEquals(0, widgetRepository.count());
    }

    @Test
    public void deleteChildDoesNotAffectParent() throws Exception {
        // A → B → C. Delete B → removes B + C, A remains
        UUID widgetA = createMinimalWidget();
        UUID widgetB = createWidget(minimalWidgetWithParentJson(widgetA.toString()));
        createWidget(minimalWidgetWithParentJson(widgetB.toString()));

        mvc.perform(delete("/widget/" + widgetB)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(2));

        // A still exists
        mvc.perform(get("/widget/" + widgetA)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(1, widgetRepository.count());
    }

    @Test
    public void deleteNonExistentWidgetReturns404() throws Exception {
        mvc.perform(delete("/widget/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
