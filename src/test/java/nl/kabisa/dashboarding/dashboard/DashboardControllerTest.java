package nl.kabisa.dashboarding.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DashboardRepository dashboardRepository;

    @BeforeEach
    void setUp() {
        dashboardRepository.deleteAll();
    }

    @Test
    void listAvailableDashboardsReturnsSortedSummaries() throws Exception {
        var beta = new Dashboard("Beta", "{}");
        var alpha = new Dashboard("Alpha", "{}");
        var deleted = new Dashboard("Deleted", "{}");
        deleted.setDeletedAt(LocalDateTime.now());

        dashboardRepository.saveAll(List.of(beta, deleted, alpha));

        mvc.perform(get("/dashboards").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[1].name").value("Beta"))
                .andExpect(jsonPath("$[0].id").value(notNullValue()))
                .andExpect(jsonPath("$[0].createdAt").value(notNullValue()))
                .andExpect(jsonPath("$[0].modifiedAt").value(nullValue()));
    }
}