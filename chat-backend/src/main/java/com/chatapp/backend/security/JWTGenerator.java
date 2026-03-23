package com.chatapp.backend.security;

import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JWTGenerator {

    private static final Logger log = LoggerFactory.getLogger(JWTGenerator.class);

    private final Key key;
    private final long jwtExpiration;

    @Autowired
    public JWTGenerator(SecurityConstants securityConstants) {
        this.key = Keys.hmacShaKeyFor(securityConstants.getJwtSecret().getBytes());
        this.jwtExpiration = securityConstants.getJwtExpiration();
    }

    public String generateToken(Authentication authentication) {
        String phoneNumber = authentication.getName();
        return generateToken(phoneNumber);
    }

    public String generateToken(String phone) {
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + jwtExpiration);

        String token = Jwts.builder()
                .setSubject(phone)
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
        log.debug("Generated JWT token for phone: {}", phone);
        return token;
    }

    public String getPhoneFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            throw new AuthenticationCredentialsNotFoundException("JWT was expired or incorrect", ex.fillInStackTrace());
        }
    }
}