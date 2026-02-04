package nl.kabisa.dashboarding.widget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

import nl.kabisa.dashboarding.widget.orm.WidgetRepository;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class WidgetControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private WidgetRepository widgetRepository;

    @BeforeEach
    void setUp() {
        widgetRepository.deleteAll();
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
        final String FULL_WIDGET_JSON = """
                {
                    "widgetType": "google-calendar-widget",
                    "version": "1.0.0",
                    "configuration": {},
                    "configurationModel": [],
                    "dataEndpoints": []
                }
                """;

        MvcResult result = mvc.perform(post("/widget").contentType(MediaType.APPLICATION_JSON).content(FULL_WIDGET_JSON)
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
        final String FULL_WIDGET_JSON = """
                {
                    "widgetType": "google-calendar-widget",
                    "version": "1.0.0",
                    "configuration": {
                        "title": "Lunch & Learn binnenkort",
                        "lookAhead": 60
                    },
                    "configurationModel": [{
                        "id": "title",
                        "type": "string",
                        "scope": "frontend"
                    }, {
                        "id": "lookAhead",
                        "type": "integer",
                        "scope": "frontend"
                    }],
                    "dataEndpoints": []
                }
                """;

        MvcResult result = mvc.perform(post("/widget").contentType(MediaType.APPLICATION_JSON).content(FULL_WIDGET_JSON)
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
                    ((java.util.Map<String, ?>) widget.getFrontendConfiguration()).get("lookAhead"));
        });
    }

    @Test
    public void createWidget() throws Exception {
        final String FULL_WIDGET_JSON = """
                {
                    "widgetType": "google-calendar-widget",
                    "version": "1.0.0",
                    "configuration": {
                        "title": "Lunch & Learn binnenkort",
                        "secretIcalUrl": "https://.....",
                        "lookAhead": 60,
                        "lookBack": 0
                    },
                    "configurationModel": [{
                        "id": "title",
                        "type": "string",
                        "scope": "frontend"
                    }, {
                        "id": "secretIcalUrl",
                        "type": "string",
                        "scope": "backend"
                    }, {
                        "id": "lookAhead",
                        "type": "integer",
                        "scope": "frontend"
                    }, {
                        "id": "lookBack",
                        "type": "integer",
                        "scope": "frontend"
                    }],
                    "dataEndpoints": [{
                        "path": "calendar",
                        "cache": 600000,
                        "steps": [{
                            "action": "tunnelRequest",
                            "config": {
                                "method": "GET",
                                "url": "%secretIcalUrl%"
                            }
                        }]
                    }]
                }
                """;

        MvcResult result = mvc.perform(post("/widget").contentType(MediaType.APPLICATION_JSON).content(FULL_WIDGET_JSON)
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
                    ((java.util.Map<String, ?>) widget.getFrontendConfiguration()).get("lookAhead"));
            assertEquals(0,
                    ((java.util.Map<String, ?>) widget.getFrontendConfiguration()).get("lookBack"));
            assertEquals(null,
                    ((java.util.Map<String, ?>) widget.getFrontendConfiguration()).get("secretIcalUrl"));

            assertEquals(1, ((java.util.Map<String, ?>) widget.getSecretsConfiguration()).size());
            assertEquals("https://.....",
                    ((java.util.Map<String, ?>) widget.getSecretsConfiguration()).get("secretIcalUrl"));
        });
    }

    @Test
    public void invalidCreateWidget() throws Exception {
        final String INVALID_WIDGET_JSON = """
                {
                    "widgetType": "",
                    "version": "1.0.0",
                    "configuration": {},
                    "configurationModel": []
                }
                """;

        mvc.perform(post("/widget").contentType(MediaType.APPLICATION_JSON).content(INVALID_WIDGET_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableContent()).andExpect(content()
                        .string(equalTo(
                                "{\"status\":422,\"message\":\"Validation failed\",\"errors\":{\"widgetType\":[\"Widget type cannot be blank\"]}}")));
    }
}
