package com.chatapp.backend.service;

import com.chatapp.backend.dto.UserDto;
import com.chatapp.backend.dto.UserSearchDTO;

import java.util.List;

public interface UserService {

    UserDto getUserById(Integer id);

    //List<UserDto> getAllUsers();

    UserDto updateUser(Integer id, UserDto userDto);

    UserDto getUserByPhone(String phone);

    UserSearchDTO searchUserByPhone(String phone);

    //void deleteUser(Integer id);
}
