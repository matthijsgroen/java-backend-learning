package nl.kabisa.dashboarding.widget;

import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.equalTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
public class WidgetControllerTest {

    @Autowired
    private MockMvc mvc;

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

        mvc.perform(post("/widget").contentType(MediaType.APPLICATION_JSON).content(FULL_WIDGET_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content()
                        .string(equalTo("{\"id\":\"widget-123\",\"message\":\"Widget created successfully\"}")));
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

        mvc.perform(post("/widget").contentType(MediaType.APPLICATION_JSON).content(FULL_WIDGET_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content()
                        .string(equalTo("{\"id\":\"widget-123\",\"message\":\"Widget created successfully\"}")));
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
                        "scope": "backend",
                        "storage": "encrypted"
                    }, {
                        "id": "lookAhead",
                        "type": "integer",
                        "scope": "frontend"
                    }, {
                        "id": "lookAhead",
                        "type": "integer",
                        "scope": "backend"
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

        mvc.perform(post("/widget").contentType(MediaType.APPLICATION_JSON).content(FULL_WIDGET_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content()
                        .string(equalTo("{\"id\":\"widget-123\",\"message\":\"Widget created successfully\"}")));
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
