package com.chatapp.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class MessageResponseDTO {
    private Integer id;
    private Integer conversationId;
    private Integer senderId;
    private String text;
    private String timestamp;
    private Boolean read;
    private Boolean edited;
    private Boolean deleted;
    private List<DocumentDTO> documents;

    @Data
    public static class DocumentDTO {
        private Integer id;
        private String url;
        private String fileName;
        private String fileType;
    }
}