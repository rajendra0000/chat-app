package com.chatapp.backend.service.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDto getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserMapper.mapToDto(user);
    }

    @Override
    public UserDto getUserByPhone(String phone) {
        if (phone == null || phone.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number must be at least 3 digits");
        }

        return userRepository.findByPhone(phone)
                .map(UserMapper::mapToDto)
                .orElse(null);
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

        return userRepository.findByPhone(phone)
                .map(user -> {
                    UserSearchDTO dto = new UserSearchDTO();
                    dto.setUserId(user.getId());
                    dto.setName(user.getName());
                    dto.setPhone(user.getPhone());
                    return dto;
                })
                .orElse(null);
    }

    @Override
    public UserDto updateProfileDetails(UserDto dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String phone = auth.getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (dto.getFullName() != null) user.setName(dto.getFullName());
        if (dto.getDateOfBirth() != null) user.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getGender() != null) user.setGender(dto.getGender());
        if (dto.getAddress() != null) user.setAddress(dto.getAddress());

        User updatedUser = userRepository.save(user);

        UserDto updatedDto = new UserDto();
        updatedDto.setFullName(updatedUser.getName());
        updatedDto.setDateOfBirth(updatedUser.getDateOfBirth());
        updatedDto.setGender(updatedUser.getGender());
        updatedDto.setAddress(updatedUser.getAddress());
        return updatedDto;
    }

    @Override
    public UserDto getProfileDetails() {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserDto dto = new UserDto();
        dto.setFullName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setGender(user.getGender());
        dto.setAddress(user.getAddress());
        return dto;
    }
}
