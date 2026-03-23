package com.chatapp.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OtpRequest {

    /**
     * E.164 international phone number (e.g. "+919876543210" or "+14155551234").
     * Used as the unique user identifier in the system.
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Phone must be in E.164 format, e.g. +919876543210")
    private String phone;

    /**
     * Email address where the OTP will be delivered (e.g. "user@gmail.com").
     * OTP is sent via email — free, worldwide, unrestricted via Resend.
     */
    @NotBlank(message = "Email is required to send OTP")
    @Email(message = "Must be a valid email address")
    private String email;

    private String otp;

    public OtpRequest(String phone, String email) {
        this.phone = phone;
        this.email = email;
    }
}
