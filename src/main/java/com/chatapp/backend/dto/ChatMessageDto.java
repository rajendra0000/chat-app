package com.chatapp.backend.dto;

import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

@Data
public class ChatMessageDto {

    private Integer id;

    private Integer chatMemberId;

    private String content;

    private Boolean isEdited;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private List<ChatMessageAttachmentDto> attachments; 
}
