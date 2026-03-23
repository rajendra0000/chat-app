package com.chatapp.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserSearchDTO {

    @NotNull(message = "User ID is required")
    private Integer userId;

    private String name;
    private String phone;
}