package com.chatapp.backend.security;

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

@Component
public class WebSocketSecurityInterceptor implements HandshakeInterceptor {

    private final JWTGenerator jwtGenerator;
    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public WebSocketSecurityInterceptor(JWTGenerator jwtGenerator,
                                        CustomUserDetailsService customUserDetailsService) {
        this.jwtGenerator = jwtGenerator;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);

        if (token != null && jwtGenerator.validateToken(token)) {
            try {
                String phone = jwtGenerator.getPhoneFromJWT(token);
                System.out.println("JWT valid, phone: " + phone);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(phone);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("authentication : " + authentication);
                attributes.put("principal", authentication);
                attributes.put("phone", phone);
                attributes.put("token", token);

                return true;
            } catch (Exception ex) {
                System.out.println("WebSocket JWT processing failed: " + ex.getMessage());
            }
        } else {
            System.out.println("JWT validation failed or token not found");
        }

        response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        SecurityContextHolder.clearContext();
    }

    private String extractToken(ServerHttpRequest request) {
        URI uri = request.getURI();
        String query = uri.getQuery();

        // Try to get token from query parameters
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1].split("&")[0];
        }

        // Fallback: get token from Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}
