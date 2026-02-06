package nl.kabisa.dashboarding.widget;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import nl.kabisa.dashboarding.widget.orm.WidgetRepository;

import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static nl.kabisa.dashboarding.widget.WidgetTestFixtures.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
public class WidgetEndpointsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private WidgetRepository widgetRepository;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setUp() {
        widgetRepository.deleteAll();

        // Start WireMock server on port 8089
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        configureFor("localhost", 8089);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    private UUID extractIdFromResponse(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content);
        String idString = root.get("id").asText();
        return UUID.fromString(idString);
    }

    @Test
    public void widgetEndpointNotFound() throws Exception {
        MvcResult creationResult = mvc
                .perform(post("/widget").contentType(MediaType.APPLICATION_JSON).content(FULL_WIDGET_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        UUID widgetId = extractIdFromResponse(creationResult);

        mvc.perform(
                get("/widget/" + widgetId.toString() + "/endpoint/does-not-exist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void useCustomWidgetEndpoint() throws Exception {
        // Stub external API response
        stubFor(get(urlPathEqualTo("/api/calendar/events"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"events\": [{\"id\": 1, \"title\": \"Meeting\"}]}")));

        MvcResult creationResult = mvc
                .perform(post("/widget").contentType(MediaType.APPLICATION_JSON).content(FULL_WIDGET_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        UUID widgetId = extractIdFromResponse(creationResult);

        mvc.perform(
                get("/widget/" + widgetId.toString() + "/endpoint/calendar").contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(content().toEqual("{}"))
                // .andExpect(jsonPath("$.widgetId").value("Validation failed"))
                .andExpect(jsonPath("$.endpoint").value("Missing configuration value"));

        // // Verify the backend called the external API
        // verify(getRequestedFor(urlPathEqualTo("/api/calendar/events"))
        // .withHeader("Accept", equalTo("application/json")));
    }

}
