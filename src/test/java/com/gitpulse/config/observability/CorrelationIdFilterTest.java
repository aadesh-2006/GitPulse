package com.gitpulse.config.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    @DisplayName("Should generate a new UUID correlation ID when X-Correlation-Id header is absent")
    void doFilter_GeneratesCorrelationIdWhenAbsent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcValueInsideChain = new AtomicReference<>();

        FilterChain chain = (req, resp) -> {
            mdcValueInsideChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedId).isNotNull().isNotBlank();
        assertThat(UUID.fromString(generatedId)).isNotNull(); // Validates UUID format
        assertThat(mdcValueInsideChain.get()).isEqualTo(generatedId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull(); // Cleared in finally
    }

    @Test
    @DisplayName("Should preserve existing X-Correlation-Id header when present")
    void doFilter_PreservesExistingCorrelationId() throws ServletException, IOException {
        String existingId = "custom-trace-id-12345";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcValueInsideChain = new AtomicReference<>();

        FilterChain chain = (req, resp) -> {
            mdcValueInsideChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(existingId);
        assertThat(mdcValueInsideChain.get()).isEqualTo(existingId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should generate a new correlation ID when header is blank whitespace")
    void doFilter_GeneratesCorrelationIdWhenBlank() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcValueInsideChain = new AtomicReference<>();

        FilterChain chain = (req, resp) -> {
            mdcValueInsideChain.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        };

        filter.doFilter(request, response, chain);

        String generatedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(generatedId).isNotNull().isNotBlank();
        assertThat(UUID.fromString(generatedId)).isNotNull();
        assertThat(mdcValueInsideChain.get()).isEqualTo(generatedId);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Should clean up MDC context even when downstream filter throws an exception")
    void doFilter_CleansUpMdcOnException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain failingChain = (req, resp) -> {
            assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNotNull();
            throw new RuntimeException("Simulated filter chain failure");
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, failingChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated filter chain failure");

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
