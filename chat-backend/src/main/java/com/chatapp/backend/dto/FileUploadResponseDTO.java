package com.chatapp.backend.dto;

import lombok.Data;

@Data
public class FileUploadResponseDTO {
    private String url;
    private String fileName;
    private Integer documentId;

    public FileUploadResponseDTO() {
    }

    public FileUploadResponseDTO(String url, String fileName ,Integer documentId) {
        this.url = url;
        this.fileName = fileName;
        this.documentId = documentId;
    }
}