package com.chatapp.backend.mapper;

import com.chatapp.backend.dto.UserDto;
import com.chatapp.backend.model.User;

public class UserMapper {

    public static UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setPhone(user.getPhone());
        dto.setFullName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setGender(user.getGender());
        dto.setAddress(user.getAddress());
        return dto;
    }

    public static User mapToEntity(UserDto dto) {
        User user = new User();
        user.setId(dto.getId());
        user.setPhone(dto.getPhone());
        user.setName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setGender(dto.getGender());
        user.setAddress(dto.getAddress());
        return user;
    }
}
