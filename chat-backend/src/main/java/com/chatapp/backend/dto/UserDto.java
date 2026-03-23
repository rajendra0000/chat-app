package com.chatapp.backend.dto;

import java.sql.Timestamp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserDto {

    private Integer id;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Phone must be in E.164 format, e.g. +919876543210")
    private String phone;

    @NotBlank(message = "Full name is required")
    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    private String fullName;

    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    private String dateOfBirth;

    private String gender;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    private Timestamp createdAt;
}
