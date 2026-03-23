package com.chatapp.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.UserDto;
import com.chatapp.backend.dto.UserSearchDTO;
import com.chatapp.backend.model.User;
import com.chatapp.backend.service.UserService;
import com.chatapp.backend.service.impl.OtpServiceImpl;

@RestController
@RequestMapping("/api")
public class UserController {
    //users
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public void getAllUsers() {}

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        logger.info("Request to /api/users/me");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Authentication: {}", authentication);
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            logger.warn("Unauthorized access: authentication is null or anonymous");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String phone = authentication.getName();
        logger.info("Authenticated phone: {}", phone);
        
        UserDto userDTO = userService.getUserByPhone(phone);
        logger.info("UserDTO: {}", userDTO);
        
        if (userDTO == null) {
            logger.warn("User not found for phone: {}", phone);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        
        return ResponseEntity.ok(userDTO);
    }

    @GetMapping("/profile/details")
    public ResponseEntity<UserDto> getProfileDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto userDetails = userService.getProfileDetails();
        return ResponseEntity.ok(userDetails);
    }

    @PutMapping("/profile/update-details")
    public ResponseEntity<UserDto> updateProfileDetails(@RequestBody UserDto dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDto updatedUser = userService.updateProfileDetails(dto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {}

    @GetMapping("/users/search")
    public ResponseEntity<UserDto> searchUsers(@RequestParam String phone) {
        try {
            UserDto user = userService.getUserByPhone(phone);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            return ResponseEntity.ok(user);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(null);
        }
    }

    /**
     * Returns the Kalori AI user profile so the frontend can start a DM with it.
     * The AI user has phone="AI_KALORI" which is not searchable via the normal search.
     */
    @GetMapping("/users/kalori")
    public ResponseEntity<UserDto> getKaloriUser() {
        try {
            UserDto kalori = userService.getUserByPhone(com.chatapp.backend.ai.AiUserInitializer.KALORI_PHONE);
            if (kalori == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(kalori);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{id}/chats")
    public void getUserChats(@PathVariable Long id) {}
}
