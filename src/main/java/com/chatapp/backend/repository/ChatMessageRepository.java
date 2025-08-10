package com.chatapp.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;


import com.chatapp.backend.model.Chat;
import com.chatapp.backend.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    ChatMessage findFirstByChatMember_ChatOrderByCreatedAtDesc(Chat chat);

    long countByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(Chat chat, Integer userId);

    List<ChatMessage> findByChatMember_Chat_IdOrderByCreatedAtAsc(Integer chatId);

    List<ChatMessage> findByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(Chat chat , Integer userId);
}
