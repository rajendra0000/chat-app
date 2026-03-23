package com.chatapp.backend.dto;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class ChatMessageDto {

    private Integer id;

    private Integer senderId;

    private Integer chatMemberId;

    private String text;

    private Boolean edited;

    private Boolean deleted;

    private Boolean isRead;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private List<ChatMessageAttachmentDto> attachments;
}
