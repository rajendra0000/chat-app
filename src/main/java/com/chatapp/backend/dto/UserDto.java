package com.chatapp.backend.dto;

import java.sql.Timestamp;

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

    private String phone;

    private String fullName;

    private String email;

    private String dateOfBirth;

    private String gender;
    
    private String address;

    private Timestamp createdAt;
    
}
