package com.chatapp.backend.config;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Production-tuned request logger.
 *
 * Rules:
 *  - ALWAYS log:  4xx / 5xx responses, slow requests (>2s), mutating methods (POST/PUT/DELETE/PATCH)
 *  - NEVER log:   health checks, root ping, static assets, WebSocket upgrade handshakes
 *  - DEBUG only:  fast, successful GET/HEAD requests (visible only when log level = DEBUG)
 *
 * The requestId is injected into MDC so all downstream log lines for the same
 * request share the same ID and can be correlated in Render's log stream.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    /** Requests slower than this are always logged at WARN regardless of method/status */
    private static final long SLOW_REQUEST_THRESHOLD_MS = 2_000;

    /** Methods that mutate state — always log these at INFO */
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();

        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String userId  = getUserId();
            String method  = request.getMethod();
            String path    = request.getRequestURI();
            int    status  = response.getStatus();

            boolean isSlow   = duration >= SLOW_REQUEST_THRESHOLD_MS;
            boolean isError  = status >= 400;
            boolean isMutate = MUTATING_METHODS.contains(method);

            if (isSlow) {
                log.warn("[{}] SLOW {} {} {} user={} duration={}ms",
                        requestId, method, path, status, userId, duration);
            } else if (isError) {
                log.warn("[{}] {} {} {} user={} duration={}ms",
                        requestId, method, path, status, userId, duration);
            } else if (isMutate) {
                log.info("[{}] {} {} {} user={} duration={}ms",
                        requestId, method, path, status, userId, duration);
            } else {
                // Fast, successful GET/HEAD — only emit at DEBUG (invisible by default)
                log.debug("[{}] {} {} {} user={} duration={}ms",
                        requestId, method, path, status, userId, duration);
            }

            MDC.remove("requestId");
        }
    }

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anon";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip entirely: health, root ping, static files, WebSocket upgrade
        return path.equals("/")
                || path.startsWith("/actuator")
                || path.startsWith("/chat")   // SockJS / WebSocket handshake path
                || path.endsWith(".js")
                || path.endsWith(".css")
                || path.endsWith(".html")
                || path.endsWith(".ico");
    }
}
