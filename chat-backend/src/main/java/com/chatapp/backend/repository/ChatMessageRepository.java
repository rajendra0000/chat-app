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

    /**
     * Paginated query WITHOUT @EntityGraph to avoid HHH90003004.
     * Hibernate must not JOIN FETCH collections when pagination is active — it would
     * load all rows into memory. Use findByIdsWithAttachments() as a second query
     * to batch-load attachments for only the current page's message IDs.
     */
    Page<ChatMessage> findByChatMember_Chat_IdOrderByCreatedAtDesc(Integer chatId, Pageable pageable);

    /**
     * Batch-loads attachments (and their documents) for a specific set of message IDs.
     * Call this AFTER pagination to hydrate only the current page — avoids the N+1 and
     * the full-table-fetch that @EntityGraph + pagination causes.
     */
    @EntityGraph(attributePaths = {"attachments", "attachments.document"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT m FROM ChatMessage m WHERE m.id IN :ids")
    List<ChatMessage> findByIdsWithAttachments(@Param("ids") List<Integer> ids);

    Optional<ChatMessage> findByMessageUuid(String messageUuid);

    @Query("SELECT m FROM ChatMessage m LEFT JOIN FETCH m.attachments a LEFT JOIN FETCH a.document WHERE m.id = :id")
    Optional<ChatMessage> findByIdWithAttachments(@Param("id") Integer id);
}

