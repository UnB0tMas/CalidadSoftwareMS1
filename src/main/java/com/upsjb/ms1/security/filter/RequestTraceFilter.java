package com.upsjb.ms1.security.filter;

import com.upsjb.ms1.util.RequestMetadataUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = RequestMetadataUtil.resolveRequestId(request);
        String correlationId = RequestMetadataUtil.resolveCorrelationId(request, requestId);

        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);

        response.setHeader(RequestMetadataUtil.REQUEST_ID_HEADER, requestId);
        response.setHeader(RequestMetadataUtil.CORRELATION_ID_HEADER, correlationId);

        filterChain.doFilter(request, response);
    }
}