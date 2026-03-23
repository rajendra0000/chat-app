package com.chatapp.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "chat_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"chat_id", "user_id"})
})
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)     //Member belongs to one chat
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne                              //Member is a user
    @JoinColumn(name = "user_id")
    @NotNull
    private User user;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isAdmin;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean blocked;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean hiddenForUser;

    @OneToMany(mappedBy = "chatMember", cascade = CascadeType.ALL)      //Message sent by a member
    @ToString.Exclude
    private List<ChatMessage> messages;

   @Transient
    public Integer getChatId() {
        return chat != null ? chat.getId() : null;
    }

    @Transient
    public String getRole() {
        return isAdmin ? "ADMIN" : "MEMBER";
    }

    @Transient
    public String setRole(String role) {
        if ("ADMIN".equals(role)) {
            this.isAdmin = true;
        } else if ("MEMBER".equals(role)) {
            this.isAdmin = false;
        } else {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        return role;
    }

}
