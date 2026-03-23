package com.chatapp.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

import com.chatapp.backend.service.ChatMetrics;

@Component
public class WebSocketSecurityInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSecurityInterceptor.class);

    private final JWTGenerator jwtGenerator;
    private final CustomUserDetailsService customUserDetailsService;
    private final ChatMetrics chatMetrics;

    @Autowired
    public WebSocketSecurityInterceptor(JWTGenerator jwtGenerator,
                                        CustomUserDetailsService customUserDetailsService,
                                        ChatMetrics chatMetrics) {
        this.jwtGenerator = jwtGenerator;
        this.customUserDetailsService = customUserDetailsService;
        this.chatMetrics = chatMetrics;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);

        if (token != null && jwtGenerator.validateToken(token)) {
            try {
                String phone = jwtGenerator.getPhoneFromJWT(token);
                log.debug("WebSocket JWT valid, phone: {}", phone);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(phone);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("WebSocket authenticated: {}", phone);
                attributes.put("principal", authentication);
                attributes.put("phone", phone);
                attributes.put("token", token);

                chatMetrics.incrementWebSocketConnections();
                return true;
            } catch (Exception ex) {
                log.warn("WebSocket JWT processing failed: {}", ex.getMessage());
            }
        } else {
            log.warn("WebSocket JWT validation failed or token not found");
        }

        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        SecurityContextHolder.clearContext();
        chatMetrics.incrementWebSocketDisconnections();
    }

    private String extractToken(ServerHttpRequest request) {
        URI uri = request.getURI();
        String query = uri.getQuery();

        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}
