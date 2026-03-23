package com.chatapp.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityConstants {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public long getJwtExpiration() {
        return jwtExpiration;
    }
}
