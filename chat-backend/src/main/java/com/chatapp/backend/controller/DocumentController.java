package com.chatapp.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @GetMapping
    public void getAllDocuments() {}

    @GetMapping("/{id}")
    public void getDocumentById(@PathVariable Long id) {}

    @PostMapping
    public void uploadDocument() {}

    @DeleteMapping("/{id}")
    public void deleteDocument(@PathVariable Long id) {}
}
