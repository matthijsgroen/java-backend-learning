package nl.kabisa.dashboarding.dashboard;

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

import nl.kabisa.dashboarding.dashboard.orm.Dashboard;
import nl.kabisa.dashboarding.dashboard.orm.DashboardRepository;

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
        dashboardRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("testuser@test.local");
        testUser.setPasswordHash(passwordEncoder.encode("testpassword"));
        testUser = userRepository.save(testUser);
        authHeader = jwtTestHelper.bearerHeader(testUser.getId(), testUser.getUsername());
    }

    @Test
    void listAvailableDashboardsReturnsSortedSummaries() throws Exception {
        var beta = new Dashboard("Beta", "{}");
        var alpha = new Dashboard("Alpha", "{}");
        var deleted = new Dashboard("Deleted", "{}");
        deleted.setDeletedAt(LocalDateTime.now());

        dashboardRepository.saveAll(List.of(beta, deleted, alpha));

        mvc.perform(get("/dashboards")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha"))
                .andExpect(jsonPath("$[1].name").value("Beta"))
                .andExpect(jsonPath("$[0].id").value(notNullValue()))
                .andExpect(jsonPath("$[0].createdAt").value(notNullValue()))
                .andExpect(jsonPath("$[0].modifiedAt").value(nullValue()));
    }
}
