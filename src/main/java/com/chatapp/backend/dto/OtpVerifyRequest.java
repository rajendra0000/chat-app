package com.chatapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OtpVerifyRequest {
    private String phone;
    private String otp;
    private boolean userExist;
    private boolean success;

    public OtpVerifyRequest(String phone, String otp) {
        this.phone = phone;
        this.otp = otp;
    }
}