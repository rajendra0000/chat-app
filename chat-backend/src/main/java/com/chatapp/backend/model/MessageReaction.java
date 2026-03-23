package com.chatapp.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

/**
 * Stores per-user emoji reactions on a chat message.
 * Unique constraint: one user can have at most one of each emoji per message.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "message_reactions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_reaction_message_user_emoji",
        columnNames = {"message_id", "user_id", "emoji"}
    )
)
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User reactor;

    @Column(nullable = false, length = 8)
    private String emoji;

    @CreationTimestamp
    private Timestamp createdAt;
}
