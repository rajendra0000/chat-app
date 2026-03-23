package com.chatapp.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.chatapp.backend.model.Chat;
import com.chatapp.backend.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    ChatMessage findFirstByChatMember_ChatOrderByCreatedAtDesc(Chat chat);

    long countByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(Chat chat, Integer userId);

    @EntityGraph(attributePaths = {"attachments", "attachments.document"}, type = EntityGraph.EntityGraphType.LOAD)
    List<ChatMessage> findByChatMember_Chat_IdOrderByCreatedAtAsc(Integer chatId);

    List<ChatMessage> findByChatMember_ChatAndChatMember_UserIdNotAndReadFalse(Chat chat, Integer userId);

    @EntityGraph(attributePaths = {"attachments", "attachments.document"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<ChatMessage> findByChatMember_Chat_IdOrderByCreatedAtDesc(Integer chatId, Pageable pageable);

    Optional<ChatMessage> findByMessageUuid(String messageUuid);

    @Query("SELECT m FROM ChatMessage m LEFT JOIN FETCH m.attachments a LEFT JOIN FETCH a.document WHERE m.id = :id")
    Optional<ChatMessage> findByIdWithAttachments(@Param("id") Integer id);
}

