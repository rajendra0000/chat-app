package com.chatapp.backend.dto;

import java.util.List;

import lombok.Data;

@Data
public class ConversationDTO {
    private Integer id;
    private String name; // For private chats: other user's name; for groups: group name
    private String lastMessage;
    private String timestamp;
    private int unreadCount;
    private String status;
    private boolean deleted;
    private String chatType; // e.g., "PRIVATE" or "GROUP"
    private boolean isBlockedByCurrentUser; // Is the other user blocked by me?
    private boolean isCurrentUserBlocked; // Am I blocked by the other user?
    private List<Integer> participants; // List of user IDs in the conversation
    private String avatarUrl; // Profile picture URL: other user's pic (PRIVATE) or group pic (GROUP)
}