package com.movieobserver.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") ||
               path.startsWith("/images/") || path.startsWith("/webjars/") ||
               path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String ip = request.getRemoteAddr();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();
            log.info("Request: {} {} | Status: {} | Time: {} ms | IP: {}", method, uri, status, duration, ip);
        }
    }
}
