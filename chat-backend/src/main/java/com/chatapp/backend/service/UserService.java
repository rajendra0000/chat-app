package com.chatapp.backend.service;

import com.chatapp.backend.dto.UserDto;
import com.chatapp.backend.dto.UserSearchDTO;
import com.chatapp.backend.model.User;

import java.util.List;

public interface UserService {

    UserDto getUserById(Integer id);

    //List<UserDto> getAllUsers();

    UserDto updateProfileDetails(UserDto dto);

    UserDto getProfileDetails();

    UserDto getUserByPhone(String phone);

    UserSearchDTO searchUserByPhone(String phone);

    //void deleteUser(Integer id);
}
