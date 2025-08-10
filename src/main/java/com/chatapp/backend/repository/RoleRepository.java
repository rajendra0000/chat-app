package com.chatapp.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.chatapp.backend.model.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);
}