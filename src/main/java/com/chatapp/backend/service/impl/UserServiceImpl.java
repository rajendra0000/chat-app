package com.chatapp.backend.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.chatapp.backend.dto.UserDto;
import com.chatapp.backend.dto.UserSearchDTO;
import com.chatapp.backend.mapper.UserMapper;
import com.chatapp.backend.model.User;
import com.chatapp.backend.repository.UserRepository;
import com.chatapp.backend.service.UserService;

@Service
public class UserServiceImpl implements UserService{

    private UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDto getUserById(Integer id) {
       User user = new User();
       user = userRepository.findById(id).get();
       UserDto userDto = UserMapper.mapToDto(user);
       return userDto;

    }

    @Override
    public UserDto getUserByPhone(String phone) {
        if (phone == null || phone.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number must be at least 3 digits");
        }
        
        // String authPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // if (phone.equals(authPhone)) {
        //     return null; 
        // }
        
        
        Optional<User> userOpt = userRepository.findByPhone(phone);
        if (userOpt.isEmpty()) {
            return null; // Or throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
        User user = userOpt.get();
        return UserMapper.mapToDto(user);
    }

    @Override
    public UserSearchDTO searchUserByPhone(String phone) {
        if (phone == null || phone.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number must be at least 3 digits");
        }
        
        String authPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        
        if (phone.equals(authPhone)) {
            return null; 
        }
        
        
        Optional<User> userOpt = userRepository.findByPhone(phone);
        if (userOpt.isEmpty()) {
            return null; // Or throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        }
        User user = userOpt.get();
       
        UserSearchDTO dto = new UserSearchDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());

        return dto;
    }

    @Override
    public UserDto updateUser(Integer id, UserDto userDto) {
        throw new UnsupportedOperationException("Unimplemented method 'updateUser'");
    }

}
