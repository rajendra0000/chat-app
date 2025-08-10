package com.chatapp.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chat_members")
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)     //Member belongs to one chat
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne                              //Member is a user
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "chatMember", cascade = CascadeType.ALL)      //Message sent by a member
    @ToString.Exclude
    private List<ChatMessage> messages;

   @Transient
    public Integer getChatId() {
        return chat != null ? chat.getId() : null;
    }

}
