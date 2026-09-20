package com.gitpulse.config.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Filter that logs incoming HTTP requests and their completion status with monotonic execution duration.
 * <p>
 * Positioned right after {@link CorrelationIdFilter} in the filter chain so that MDC correlation ID is
 * present in the log output. Skips high-frequency actuator health checks to prevent log pollution.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startNanos = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (!shouldSkipLogging(request)) {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
                log.info("HTTP request completed: method={}, path={}, status={}, durationMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        durationMs);
            }
        }
    }

    private boolean shouldSkipLogging(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && (uri.equals("/actuator/health") || uri.startsWith("/actuator/health/"));
    }
}
