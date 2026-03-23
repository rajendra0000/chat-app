package com.chatapp.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher; // --- ADDED IMPORT ---
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays; // --- ADDED IMPORT ---
import java.util.List;   // --- ADDED IMPORT ---

public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JWTGenerator tokenGenerator;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    // --- ADDED: A list of paths to be excluded from JWT validation ---
    private static final List<String> SKIP_PATHS = Arrays.asList(
        "/auth/**",
        "/chat/**",
        "/index.html",
        "/chat.html",
        "/css/**",
        "/js/**",
        "/favicon.ico",
        "/default-avatar.jpg"
    );

    // --- ADDED: A utility to match paths ---
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // --- ADDED: Bypass logic for public paths ---
        String path = request.getRequestURI();
        boolean shouldSkip = SKIP_PATHS.stream().anyMatch(p -> pathMatcher.match(p, path));

        if (shouldSkip) {
            filterChain.doFilter(request, response); // Skip the token check and proceed
            return;
        }
        // --- END OF ADDED LOGIC ---

        // Your existing token validation logic now only runs for protected routes
        String token = getJWTFromRequest(request);
        if(StringUtils.hasText(token) && tokenGenerator.validateToken(token)) {
            String phone = tokenGenerator.getPhoneFromJWT(token);
            UserDetails userDetails = customUserDetailsService.loadUserByUsername(phone);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null,
                    userDetails.getAuthorities());
        
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        filterChain.doFilter(request, response);
    }

    private String getJWTFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}