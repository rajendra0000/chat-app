package com.chatapp.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chatapp.backend.model.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User>findByPhone(String phone);
    boolean existsByPhone(String phone);
}