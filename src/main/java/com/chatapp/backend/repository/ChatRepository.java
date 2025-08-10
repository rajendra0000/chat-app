package com.chatapp.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.chatapp.backend.model.Chat;

public interface ChatRepository extends JpaRepository<Chat, Integer> {
    List<Chat> findByChatMembersUserPhone(String phone);
}
