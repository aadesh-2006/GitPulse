package com.gitpulse.config.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitPulseMetrics gitPulseMetrics;

    @Test
    @DisplayName("GET /actuator/health should return UP status")
    void testActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("GET /actuator/metrics should expose registered GitPulse custom and JVM metrics")
    void testActuatorMetricsList() throws Exception {
        gitPulseMetrics.incrementJobCreated();

        mockMvc.perform(get("/actuator/metrics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItem(GitPulseMetrics.METRIC_ANALYSIS_JOBS)))
                .andExpect(jsonPath("$.names", hasItem(GitPulseMetrics.METRIC_ANALYSIS_JOBS_ACTIVE)));
    }

    @Test
    @DisplayName("GET /actuator/metrics/gitpulse.analysis.jobs should return detailed metric dimensions")
    void testActuatorSpecificMetric() throws Exception {
        gitPulseMetrics.incrementJobCreated();

        mockMvc.perform(get("/actuator/metrics/" + GitPulseMetrics.METRIC_ANALYSIS_JOBS)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(GitPulseMetrics.METRIC_ANALYSIS_JOBS)))
                .andExpect(jsonPath("$.availableTags[?(@.tag == 'status')]").exists());
    }

    @Test
    @DisplayName("GET /actuator/health/liveness should return UP status")
    void testActuatorHealthLiveness() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("GET /actuator/health/readiness should return UP status")
    void testActuatorHealthReadiness() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("GET /actuator/env should return 404 Not Found (sensitive endpoint unexposed)")
    void testActuatorEnvNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/env")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /actuator/beans should return 404 Not Found (sensitive endpoint unexposed)")
    void testActuatorBeansNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/beans")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /actuator/configprops should return 404 Not Found (sensitive endpoint unexposed)")
    void testActuatorConfigPropsNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/configprops")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
