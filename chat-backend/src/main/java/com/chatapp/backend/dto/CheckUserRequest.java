package com.chatapp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /auth/check-user
 * Only needs phone — no email required for this lookup.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CheckUserRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Phone must be in E.164 format, e.g. +919876543210")
    private String phone;
}
