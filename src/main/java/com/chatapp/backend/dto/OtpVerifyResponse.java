package com.chatapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OtpVerifyResponse {
    private String phone;
    private String otp;
    private boolean userExists;
    private boolean success;
    private String token;
}
