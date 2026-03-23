package com.chatapp.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chatapp.backend.security.CustomUserDetailsService;

@RestController
public class TestController {
//     private static final Logger logger = LoggerFactory.getLogger(TestController.class);

//     @Autowired
//     private AuthenticationManager authenticationManager;

//     @Autowired
//     private CustomUserDetailsService customUserDetailsService;

//    @GetMapping("/test/auth")
//     public String testAuthentication(@RequestParam(required = false) String phone) {
//         if (phone == null) {
//             logger.error("Phone parameter is missing");
//             return "Error: Phone parameter is required";
//         }
//         try {
//             UserDetails userDetails = customUserDetailsService.loadUserByUsername(phone);
//             Authentication auth = authenticationManager.authenticate(
//                 new UsernamePasswordAuthenticationToken(
//                     userDetails, null, userDetails.getAuthorities()));
//             logger.debug("Authentication successful: {}", auth);
//             return "Authentication successful for phone: " + phone;
//         } catch (AuthenticationException ex) {
//             logger.error("Authentication failed for phone {}: {}", phone, ex.getMessage(), ex);
//             return "Authentication failed: " + ex.getMessage();
//         }
//     }

    @GetMapping("/test/auth")
    public String testAuthentication(@RequestParam(required = false) String phone) {
        return "hello everyone";
    }
      
}