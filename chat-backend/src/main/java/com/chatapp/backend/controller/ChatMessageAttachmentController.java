package com.chatapp.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-message-attachments")
public class ChatMessageAttachmentController {

    @PostMapping
    public void addAttachment() {}

    @GetMapping("/{id}")
    public void getAttachmentById(@PathVariable Long id) {}

    @DeleteMapping("/{id}")
    public void deleteAttachment(@PathVariable Long id) {}

    @GetMapping("/message/{messageId}")
    public void getAttachmentsByMessageId(@PathVariable Long messageId) {}
}
