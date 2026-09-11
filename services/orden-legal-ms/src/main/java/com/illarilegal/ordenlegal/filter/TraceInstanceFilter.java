package com.illarilegal.ordenlegal.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceInstanceFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-ID";
    private static final String INSTANCE_HEADER = "X-Instance-ID";

    @Value("${eureka.instance.instance-id:${spring.application.name}:${server.port}}")
    private String instanceId;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put("traceId", traceId);
        MDC.put("instanceId", instanceId);
        response.setHeader(TRACE_HEADER, traceId);
        response.setHeader(INSTANCE_HEADER, instanceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
            MDC.remove("instanceId");
        }
    }
}
