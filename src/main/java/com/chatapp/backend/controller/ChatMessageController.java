package com.chatapp.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-messages")
public class ChatMessageController {

    @PostMapping
    public void sendMessage() {}

    @GetMapping("/{id}")
    public void getMessageById(@PathVariable Long id) {}

    @PutMapping("/{id}")
    public void updateMessage(@PathVariable Long id) {}

    @DeleteMapping("/{id}")
    public void deleteMessage(@PathVariable Long id) {}

    @GetMapping("/chat/{chatId}")
    public void getMessagesByChatId(@PathVariable Long chatId) {}
}
