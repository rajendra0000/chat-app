package com.chatapp.backend.dto;

import lombok.Data;

@Data
public class MessageRequestDTO {
    private Integer conversationId;
    private String text;
    
}
