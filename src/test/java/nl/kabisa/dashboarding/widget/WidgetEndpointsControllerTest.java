package nl.kabisa.dashboarding.widget;

import java.util.UUID;

import nl.kabisa.dashboarding.auth.JwtTestHelper;
import nl.kabisa.dashboarding.user.orm.User;
import nl.kabisa.dashboarding.user.orm.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import nl.kabisa.dashboarding.widget.orm.WidgetRepository;

import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "dashboarding.proxy.allowed-schemes=http,https",
        "dashboarding.proxy.allowed-hosts=localhost,api.example.com,data.trusted-service.com"
})
public class WidgetEndpointsControllerTest {

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

    private WireMockServer wireMockServer;
    private String baseUrl;

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

        // Start WireMock server on dynamic port
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        baseUrl = "http://localhost:" + wireMockServer.port();
        configureFor("localhost", wireMockServer.port());
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
                .perform(post("/widget")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WidgetTestFixtures.fullWidgetJson(baseUrl))
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        UUID widgetId = extractIdFromResponse(creationResult);

        mvc.perform(
                MockMvcRequestBuilders.get("/widget/" + widgetId.toString() + "/endpoint/does-not-exist")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void useCustomWidgetEndpoint() throws Exception {
        String mockCalendarResult = """
                {
                    "events": [
                        {
                            "id": 1,
                            "title": "Meeting"
                        }
                    ]
                }
                """;
        // Stub external API response for the calendar endpoint
        stubFor(WireMock.get(urlPathEqualTo("/ical/abcd1234"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockCalendarResult)));

        MvcResult creationResult = mvc
                .perform(post("/widget")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WidgetTestFixtures.fullWidgetJson(baseUrl)))
                .andReturn();

        UUID widgetId = extractIdFromResponse(creationResult);

        mvc.perform(
                MockMvcRequestBuilders.get("/widget/" + widgetId.toString() + "/endpoint/calendar")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", equalTo("application/json")))
                .andExpect(content().string(equalTo(mockCalendarResult)));

    }

}
