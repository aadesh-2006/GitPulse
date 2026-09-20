package com.gitpulse.config.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpRequestLoggingFilterTest {

    private HttpRequestLoggingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new HttpRequestLoggingFilter();
    }

    @Test
    @DisplayName("Should successfully execute filter chain for API requests")
    void doFilter_StandardApiRequest_ExecutesSuccessfully() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/repositories");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        boolean[] chainExecuted = {false};
        FilterChain chain = (req, resp) -> chainExecuted[0] = true;

        assertThatCode(() -> filter.doFilter(request, response, chain)).doesNotThrowAnyException();
        assertThat(chainExecuted[0]).isTrue();
    }

    @Test
    @DisplayName("Should execute filter chain without error for health probe requests")
    void doFilter_ActuatorHealth_ExecutesSuccessfully() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        boolean[] chainExecuted = {false};
        FilterChain chain = (req, resp) -> chainExecuted[0] = true;

        assertThatCode(() -> filter.doFilter(request, response, chain)).doesNotThrowAnyException();
        assertThat(chainExecuted[0]).isTrue();
    }

    @Test
    @DisplayName("Should execute filter chain without error for nested health probe requests")
    void doFilter_ActuatorHealthProbes_ExecutesSuccessfully() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health/readiness");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        boolean[] chainExecuted = {false};
        FilterChain chain = (req, resp) -> chainExecuted[0] = true;

        assertThatCode(() -> filter.doFilter(request, response, chain)).doesNotThrowAnyException();
        assertThat(chainExecuted[0]).isTrue();
    }

    @Test
    @DisplayName("Should bubble exceptions thrown by filter chain while completing finally block")
    void doFilter_ExceptionInChain_PropagatesException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/repositories/1/analyses");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain failingChain = (req, resp) -> {
            throw new RuntimeException("Simulated filter failure");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, failingChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated filter failure");
    }
}
