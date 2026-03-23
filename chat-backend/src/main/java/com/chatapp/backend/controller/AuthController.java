package com.chatapp.backend.controller;

import com.chatapp.backend.dto.CheckUserRequest;
import com.chatapp.backend.dto.OtpRequest;
import com.chatapp.backend.dto.OtpVerifyRequest;
import com.chatapp.backend.dto.OtpVerifyResponse;
import com.chatapp.backend.dto.UserDto;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.OtpService;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final OtpService otpService;
    private final UserRepository userRepository;

    @Autowired
    public AuthController(OtpService otpService, UserRepository userRepository) {
        this.otpService = otpService;
        this.userRepository = userRepository;
    }

    @PostMapping("/check-user")
    public ResponseEntity<Map<String, Object>> checkUser(@Valid @RequestBody CheckUserRequest request) {
        Map<String, Object> response = new HashMap<>();
        boolean userExists = userRepository.existsByPhone(request.getPhone());
        response.put("userExists", userExists);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@Valid @RequestBody OtpRequest request) {
        // Cast to impl to use the two-argument sendOtp(phone, email)
        // email is the delivery channel; phone is the identity key
        com.chatapp.backend.service.impl.OtpServiceImpl impl =
                (com.chatapp.backend.service.impl.OtpServiceImpl) otpService;
        String result = impl.sendOtp(request.getPhone(), request.getEmail());
        if (result != null && !result.isEmpty()) {
            return ResponseEntity.ok("OTP sent to your email successfully");
        }
        return new ResponseEntity<>("Error sending OTP, try again", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<OtpVerifyResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        OtpVerifyResponse response = otpService.verifyOtp(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody UserDto reg) {
        UserDto userDto = otpService.registerUser(reg);
        log.info("User registered: {}", userDto.getPhone());
        Map<String, Object> response = new HashMap<>();

        if (userDto != null) {
            response.put("success", true);
            response.put("userData", userDto);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
