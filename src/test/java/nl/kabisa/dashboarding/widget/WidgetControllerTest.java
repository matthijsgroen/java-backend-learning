package nl.kabisa.dashboarding.widget;

import nl.kabisa.dashboarding.auth.JwtTestHelper;
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
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

import nl.kabisa.dashboarding.widget.orm.WidgetRepository;

import static nl.kabisa.dashboarding.widget.WidgetTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class WidgetControllerTest {

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

        private UUID extractIdFromResponse(MvcResult result) throws Exception {
                String content = result.getResponse().getContentAsString();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(content);
                String idString = root.get("id").asText();
                return UUID.fromString(idString);
        }

        @Test
        public void createMinimalWidgetWithoutConfiguration() throws Exception {
                MvcResult result = mvc
                                .perform(post("/widget")
                                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(MINIMAL_WIDGET_JSON)
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").isNotEmpty())
                                .andExpect(jsonPath("$.message").value("Widget created successfully"))
                                .andReturn();

                UUID widgetId = extractIdFromResponse(result);
                assertNotNull(widgetId);
                assertEquals(1, widgetRepository.count());
        }

        @Test
        public void createMinimalWidgetWithConfiguration() throws Exception {
                MvcResult result = mvc
                                .perform(post("/widget")
                                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(WIDGET_WITH_FRONTEND_CONFIG_JSON)
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").isNotEmpty())
                                .andExpect(jsonPath("$.message").value("Widget created successfully"))
                                .andReturn();

                UUID widgetId = extractIdFromResponse(result);
                assertNotNull(widgetId);
                assertEquals(1, widgetRepository.count());
                widgetRepository.findById(widgetId).ifPresent(widget -> {
                        assertEquals("google-calendar-widget", widget.getWidgetType());
                        assertEquals(2, ((java.util.Map<String, ?>) widget.getFrontendConfiguration()).size());
                        assertEquals("Lunch & Learn binnenkort",
                                        ((java.util.Map<String, ?>) widget.getFrontendConfiguration()).get("title"));
                        assertEquals(60,
                                        ((java.util.Map<String, ?>) widget.getFrontendConfiguration())
                                                        .get("lookAhead"));
                });
        }

        @Test
        public void createWidgetWithSecrets() throws Exception {
                MvcResult result = mvc
                                .perform(post("/widget")
                                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(FULL_WIDGET_JSON)
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").isNotEmpty())
                                .andExpect(jsonPath("$.message").value("Widget created successfully"))
                                .andReturn();

                UUID widgetId = extractIdFromResponse(result);
                assertNotNull(widgetId);
                assertEquals(1, widgetRepository.count());
                widgetRepository.findById(widgetId).ifPresent(widget -> {
                        assertEquals("google-calendar-widget", widget.getWidgetType());
                        assertEquals(3, ((java.util.Map<String, ?>) widget.getFrontendConfiguration()).size());
                        assertEquals("Lunch & Learn binnenkort",
                                        ((java.util.Map<String, ?>) widget.getFrontendConfiguration()).get("title"));
                        assertEquals(60,
                                        ((java.util.Map<String, ?>) widget.getFrontendConfiguration())
                                                        .get("lookAhead"));
                        assertEquals(0,
                                        ((java.util.Map<String, ?>) widget.getFrontendConfiguration()).get("lookBack"));
                        assertNull(((java.util.Map<String, ?>) widget.getFrontendConfiguration()).get("secretIcalUrl"));

                        assertEquals(1, ((java.util.Map<String, ?>) widget.getSecretsConfiguration()).size());
                        assertEquals("http://localhost:8089/ical/abcd1234",
                                        ((java.util.Map<String, ?>) widget.getSecretsConfiguration())
                                                        .get("secretIcalUrl"));

                });
        }

        @Test
        public void invalidCreateWidget() throws Exception {
                mvc.perform(post("/widget")
                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(INVALID_WIDGET_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnprocessableContent())
                                .andExpect(jsonPath("$.status").value(422))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors.widgetType[0]").value("Widget type cannot be blank"));
        }

        @Test
        public void getWidgetWithSecrets() throws Exception {
                MvcResult creationResult = mvc
                                .perform(post("/widget")
                                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(FULL_WIDGET_JSON)
                                                .accept(MediaType.APPLICATION_JSON))
                                .andReturn();

                UUID widgetId = extractIdFromResponse(creationResult);

                mvc.perform(get("/widget/" + widgetId.toString())
                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.widgetType").value("google-calendar-widget"))
                                .andExpect(jsonPath("$.version").value("1.0.0"))
                                .andExpect(jsonPath("$.configuration.title").value("Lunch & Learn binnenkort"))
                                .andExpect(jsonPath("$.configuration.lookAhead").value(60))
                                .andExpect(jsonPath("$.configuration.lookBack").value(0))
                .andExpect(jsonPath("$.configuration.secretIcalUrl").doesNotExist())
                .andExpect(jsonPath("$.parentId").doesNotExist())
                .andExpect(jsonPath("$.childIds").isArray())
                .andExpect(jsonPath("$.ownerId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.ownerName").value("testuser"))
                .andReturn();
        }

        @Test
        public void getNonExistingWidgetReturnsNotFound() throws Exception {
                mvc.perform(get("/widget/" + UUID.randomUUID().toString())
                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().is4xxClientError());
        }

        @Test
        public void getWidgetWithInvalidIdReturnsBadRequest() throws Exception {
                mvc.perform(get("/widget/invalid-uuid")
                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().is4xxClientError());
        }

        @Test
        public void createWidgetWithConfigDataTypeError() throws Exception {
                mvc.perform(post("/widget")
                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(WIDGET_WITH_WRONG_CONFIG_DATA_TYPE)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnprocessableContent())
                                .andExpect(jsonPath("$.status").value(422))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors.lookAhead[0]")
                                                .value("Configuration value does not match type INTEGER"));
        }

        @Test
        public void createWidgetWithMissingRequiredConfig() throws Exception {
                mvc.perform(post("/widget")
                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(WIDGET_WITH_MISSING_FIELD_IN_CONFIG)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnprocessableContent())
                                .andExpect(jsonPath("$.status").value(422))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors.lookAhead[0]").value("Missing configuration value"));
        }

        @Test
        public void createWidgetWithWrongConfigScope() throws Exception {
                mvc.perform(post("/widget")
                                .header(HttpHeaders.AUTHORIZATION, authHeader)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(WIDGET_WITH_WRONG_CONFIG_SCOPE)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Bad Request"))
                                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));

        }

}
