package com.chatapp.backend.service;

import com.chatapp.backend.dto.DocumentDto;

public interface DocumentsService {

    DocumentDto saveDocument(DocumentDto documentDto);

    DocumentDto getDocumentById(Integer id);

    void deleteDocument(Integer id);
}
