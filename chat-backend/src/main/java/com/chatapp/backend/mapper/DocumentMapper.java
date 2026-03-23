package com.chatapp.backend.mapper;

import com.chatapp.backend.dto.DocumentDto;
import com.chatapp.backend.model.Document;

public class DocumentMapper {

    public static DocumentDto mapToDto(Document doc) {
        if (doc == null) return null;

        DocumentDto dto = new DocumentDto();
        dto.setId(doc.getId());
        dto.setExternalId(doc.getExternalId());
        dto.setStorageEnv(doc.getStorageEnv());
        dto.setPath(doc.getPath());
        dto.setCreatedAt(doc.getCreatedAt());
        dto.setUpdatedAt(doc.getUpdatedAt());
        return dto;
    }

    public static Document mapToEntity(DocumentDto dto) {
        if (dto == null) return null;

        Document doc = new Document();
        doc.setId(dto.getId());
        doc.setExternalId(dto.getExternalId());
        doc.setStorageEnv(dto.getStorageEnv());
        doc.setPath(dto.getPath());
        doc.setCreatedAt(dto.getCreatedAt());
        doc.setUpdatedAt(dto.getUpdatedAt());
        return doc;
    }
}
