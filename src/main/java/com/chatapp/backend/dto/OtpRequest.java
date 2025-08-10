package com.chatapp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OtpRequest {
    private String phone;
    private String otp;

    public OtpRequest(String phone) {
        this.phone = phone;
    }
}

