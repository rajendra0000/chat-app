package com.chatapp.backend.dto;

import lombok.Data;

@Data
public class ConversationDTO {
    private Integer id;
    private String name;
    private String lastMessage;
    private String timestamp;
    private int unreadCount;
    private String status;
}