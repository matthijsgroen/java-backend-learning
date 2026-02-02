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
    public void createWidget() throws Exception {
        final String WIDGET_JSON = """
                {
                    "widgetClass": "google-calendar-widget",
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
        mvc.perform(post("/widget").contentType(MediaType.APPLICATION_JSON).content(WIDGET_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content()
                        .string(equalTo("{\"id\":\"widget-123\",\"message\":\"Widget created successfully\"}")));
    }

}
