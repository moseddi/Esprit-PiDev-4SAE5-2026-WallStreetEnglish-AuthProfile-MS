package tn.esprit.usermanagementservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.usermanagementservice.config.TestSecurityConfig;
import tn.esprit.usermanagementservice.repository.UserProfileRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("StatsController Tests")
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileRepository userProfileRepository;

    @Test
    void getDashboardStats_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/users/stats/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void getHeatmap_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/users/stats/heatmap"))
                .andExpect(status().isOk());
    }

    @Test
    void getDailyActivity_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/users/stats/daily-activity"))
                .andExpect(status().isOk());
    }

    @Test
    void getWeekdayActivity_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/users/stats/weekday-activity"))
                .andExpect(status().isOk());
    }

    @Test
    void getAdvancedStats_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/users/stats/advanced-stats"))
                .andExpect(status().isOk());
    }
}