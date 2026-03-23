package com.chatapp.backend.dto;

import lombok.Data;

@Data
public class ChatMessageAttachmentDto {

    private Integer id;

    private Integer chatMessageId;

    private Integer documentId;

    /** Original filename (e.g. "voice-1711111111.webm") */
    private String fileName;

    /** MIME type (e.g. "audio/webm", "image/jpeg") */
    private String fileType;

    /** Cloudinary CDN URL (permanent, no expiry) */
    private String url;
}
