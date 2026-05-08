package com.upsjb.ms1.security.filter;

import com.upsjb.ms1.shared.audit.AuditContext;
import com.upsjb.ms1.shared.audit.AuditContextHolder;
import com.upsjb.ms1.util.IpAddressUtil;
import com.upsjb.ms1.util.RequestMetadataUtil;
import com.upsjb.ms1.util.UserAgentUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestAuditFilter extends OncePerRequestFilter {

    private final Clock clock;

    public RequestAuditFilter(Clock clock) {
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveAttributeOrHeader(
                request,
                RequestTraceFilter.REQUEST_ID_ATTRIBUTE,
                RequestMetadataUtil.resolveRequestId(request)
        );

        String correlationId = resolveAttributeOrHeader(
                request,
                RequestTraceFilter.CORRELATION_ID_ATTRIBUTE,
                RequestMetadataUtil.resolveCorrelationId(request, requestId)
        );

        AuditContext context = new AuditContext(
                requestId,
                correlationId,
                IpAddressUtil.extractClientIp(request),
                UserAgentUtil.extractUserAgent(request),
                request.getMethod(),
                RequestMetadataUtil.resolvePath(request),
                null,
                null,
                Instant.now(clock)
        );

        try {
            AuditContextHolder.set(context);
            filterChain.doFilter(request, response);
        } finally {
            AuditContextHolder.clear();
        }
    }

    private String resolveAttributeOrHeader(
            HttpServletRequest request,
            String attribute,
            String fallback
    ) {
        Object value = request.getAttribute(attribute);

        if (value instanceof String text && !text.isBlank()) {
            return text;
        }

        return fallback;
    }
}