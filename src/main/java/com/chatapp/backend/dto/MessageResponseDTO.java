package com.chatapp.backend.dto;

import lombok.Data;

@Data
public class MessageResponseDTO {
    private Integer id;
    private Integer conversationId;
    private Integer senderId;
    private String text;
    private String timestamp;
}
