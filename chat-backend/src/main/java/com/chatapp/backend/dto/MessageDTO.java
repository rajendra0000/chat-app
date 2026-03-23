package com.chatapp.backend.dto;

import lombok.Data;

@Data
public class MessageDTO {

    private Integer id;
    private Integer senderId;
    private String text;
    private String timestamp;
    private boolean read;
    
}

