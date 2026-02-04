package nl.kabisa.dashboarding.widget;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.kabisa.dashboarding.widget.orm.WidgetRepository;

import org.springframework.http.MediaType;

import static nl.kabisa.dashboarding.widget.WidgetTestFixtures.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
public class WidgetEndpointsControllerTest {

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
    public void useCustomWidgetEndpoint() throws Exception {
        MvcResult creationResult = mvc
                .perform(post("/widget").contentType(MediaType.APPLICATION_JSON).content(FULL_WIDGET_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        UUID widgetId = extractIdFromResponse(creationResult);

        mvc.perform(
                get("/widgets/" + widgetId.toString() + "/endpoint/calendar").contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"success\",\"widgetId\":\"" + widgetId.toString() + "\"}"));

    }

}
