package com.chatapp.backend.controller;

import com.chatapp.backend.dto.OtpRequest;
import com.chatapp.backend.dto.OtpVerifyRequest;
import com.chatapp.backend.dto.OtpVerifyResponse;
import com.chatapp.backend.dto.UserDto;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.OtpService;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private OtpService otpService;
    private UserRepository userRepository;

    @Autowired
    public AuthController(OtpService otpService,UserRepository userRepository) {
        this.otpService = otpService;
        this.userRepository = userRepository;
    }

    @PostMapping("/check-user")
    public ResponseEntity<Map<String, Object>> checkUser(@RequestBody OtpRequest request) {
        Map<String, Object> response = new HashMap<>();
        boolean userExists = userRepository.existsByPhone(request.getPhone());

        if(userExists){
            response.put("userExists", true);
            return  ResponseEntity.ok(response);
        }
         if(!userExists){
            response.put("userExists", false);
            return  ResponseEntity.ok(response);
        }
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);

    }
    
    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestBody OtpRequest request) {
        String otp = otpService.sendOtp(request.getPhone());
        if(!otp.isEmpty() && otp.trim() != null){
        return ResponseEntity.ok("OTP sent successfully");
        }
        return new ResponseEntity<>("Error in sending otp , TRY AGAIN!!",HttpStatus.NOT_FOUND);
    }

    // @PostMapping("/verify-otp")
    // public ResponseEntity<String> verifyOtp(@RequestBody OtpVerifyRequest request) {
    //     boolean isValid = otpService.verifyOtp(request.getPhone(), request.getOtp());
    //     if (isValid) {
    //         return ResponseEntity.ok("OTP verified successfully");
    //     } else {
    //         return ResponseEntity.status(400).body("Invalid OTP");
    //     }
    // }

    @PostMapping("/verify-otp")
    public ResponseEntity<OtpVerifyResponse> verifyOtp(@RequestBody OtpVerifyRequest request) {
        OtpVerifyResponse response = otpService.verifyOtp(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response); // HTTP 200 OK
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response); // HTTP 401
        }
    }


    // @PostMapping("/register")
    // public ResponseEntity<String> registeration(@RequestBody UserDto reg) {
    //     UserDto userDto = otpService.registerUser(reg);
    //     if (userDto != null){
    //     return ResponseEntity.ok("registered successfully");
    //     }
    //     return new ResponseEntity<>("Registeration UNSUCCESSFUL!!",HttpStatus.NOT_FOUND);
    // }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registeration(@RequestBody UserDto reg) {
        UserDto userDto = otpService.registerUser(reg);
        System.out.println(userDto);
        Map<String, Object> response = new HashMap<>();
        
        if (userDto != null) {
            response.put("success", true);
            response.put("userData", userDto); 
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }
}
