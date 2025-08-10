package com.chatapp.backend.dto;

import lombok.Data;

@Data
public class ChatMessageAttachmentDto {

    private Integer id;

    private Integer chatMessageId;

    private Integer documentId;
}
