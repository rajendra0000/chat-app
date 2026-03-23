package com.chatapp.backend.repository;

import com.chatapp.backend.model.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Integer> {

    /** All reactions for a given message, grouped for aggregation */
    List<MessageReaction> findByMessage_Id(Integer messageId);

    /** Check if a specific user already has this emoji on this message */
    Optional<MessageReaction> findByMessage_IdAndReactor_IdAndEmoji(
            Integer messageId, Integer userId, String emoji);

    /** Find any reaction by this user on this message (any emoji) — used for one-per-user enforcement */
    Optional<MessageReaction> findByMessage_IdAndReactor_Id(Integer messageId, Integer userId);

    /**
     * Return emoji → count map for a message.
     * Used to send the full reaction summary over WebSocket.
     */
    @Query("SELECT r.emoji, COUNT(r) FROM MessageReaction r WHERE r.message.id = :messageId GROUP BY r.emoji")
    List<Object[]> countByEmojiForMessage(@Param("messageId") Integer messageId);
}
